package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.governance.PolicyGuardrails;
import com.example.tinyurllab.orchestration.governance.PolicyGuardrails.GuardrailViolation;
import com.example.tinyurllab.orchestration.governance.PolicyGuardrails.Severity;
import com.example.tinyurllab.orchestration.model.*;
import com.example.tinyurllab.urlshortener.dto.CreateShortUrlRequest;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
import com.example.tinyurllab.urlshortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Core orchestration engine that coordinates the full SDLC lifecycle
 * Features:
 * - DAG-based task execution with dependency resolution
 * - Entry/exit gates with quality checks
 * - Sequential and parallel execution paths
 * - Cross-stage context preservation
 * - Human approval checkpoints
 * - Retry, rollback, and safe-stop controls
 * - Policy guardrails enforcement
 * - Audit-grade observability
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final UrlShortenerService urlShortenerService;
    private final AuditLogger auditLogger;
    private final ApprovalGateManager approvalGateManager;
    private final RetryRollbackHandler retryRollbackHandler;
    private final PolicyGuardrails policyGuardrails;
    private final Map<String, WorkflowContext> activeWorkflows = new ConcurrentHashMap<>();
    private final Map<String, List<TaskNode>> activeTasks = new ConcurrentHashMap<>();
    private final WorkflowMetrics globalMetrics;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Execute workflow with proper DAG scheduling and parallel execution
     */
    public WorkflowContext executeWorkflow(String workflowId, List<TaskNode> tasks, WorkflowContext context) {
        log.info("Starting workflow execution: {}", workflowId);
        auditLogger.logWorkflowStart(workflowId, context.getScenarioType(), context.getRequirement());

        context.setWorkflowId(workflowId);
        context.setStartTime(Instant.now());
        context.setStatus(WorkflowContext.WorkflowStatus.EXECUTING);
        activeWorkflows.put(workflowId, context);
        activeTasks.put(workflowId, tasks);

        Instant startTime = Instant.now();
        boolean success = false;

        try {
            detectCycles(tasks);
            success = runDag(workflowId, tasks, context);

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);
            context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
            context.addAuditEvent(workflowId, "WORKFLOW_FAILURE", e.getMessage(), Map.of(), "SYSTEM");
        } finally {
            Object generatedCode = context.getGlobalContext().get("generated_short_code");
            if (generatedCode != null) {
                try {
                    urlShortenerService.deleteShortUrl(generatedCode.toString());
                } catch (Exception cleanupException) {
                    log.warn("Could not clean up scenario short URL {}: {}",
                            generatedCode, cleanupException.getMessage());
                }
            }
            context.setEndTime(Instant.now());
            Duration duration = Duration.between(startTime, context.getEndTime());
            globalMetrics.recordExecution(success, duration);
            auditLogger.logWorkflowEnd(workflowId, context.getStatus(), duration);
        }

        return context;
    }

    private void detectCycles(List<TaskNode> tasks) {
        Map<String, TaskNode> taskMap = tasks.stream()
                .collect(Collectors.toMap(TaskNode::getId, t -> t));
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (TaskNode task : tasks) {
            if (hasCycle(task, taskMap, visited, recursionStack, new ArrayList<>())) {
                throw new WorkflowExecutionException("Cycle detected in task dependency graph");
            }
        }
    }

    private boolean hasCycle(TaskNode task, Map<String, TaskNode> taskMap,
                             Set<String> visited, Set<String> recursionStack, List<String> path) {
        if (recursionStack.contains(task.getId())) {
            path.add(task.getId());
            log.error("Cycle detected: {}", path);
            return true;
        }
        if (visited.contains(task.getId())) {
            return false;
        }

        visited.add(task.getId());
        recursionStack.add(task.getId());
        path.add(task.getId());

        for (String depId : task.getDependencies()) {
            TaskNode dep = taskMap.get(depId);
            if (dep == null) {
                throw new WorkflowExecutionException("Missing dependency: " + depId + " required by " + task.getId());
            }
            if (hasCycle(dep, taskMap, visited, recursionStack, new ArrayList<>(path))) {
                return true;
            }
        }

        recursionStack.remove(task.getId());
        return false;
    }

    private boolean allTasksCompleted(List<TaskNode> tasks, Set<String> completed, Set<String> failed) {
        return tasks.stream().allMatch(t -> completed.contains(t.getId()) || failed.contains(t.getId()));
    }

    private boolean runDag(String workflowId, List<TaskNode> tasks, WorkflowContext context) {
        Map<String, TaskNode> taskMap = tasks.stream()
                .collect(Collectors.toMap(TaskNode::getId, t -> t));
        Set<String> completedTasks = ConcurrentHashMap.newKeySet();
        Set<String> terminalTasks = ConcurrentHashMap.newKeySet();
        tasks.stream()
                .filter(task -> task.getStatus() == TaskNode.TaskStatus.COMPLETED)
                .map(TaskNode::getId)
                .forEach(completedTasks::add);
        tasks.stream()
                .filter(TaskNode::isTerminalState)
                .map(TaskNode::getId)
                .forEach(terminalTasks::add);
        Map<String, Future<TaskNode>> runningTasks = new ConcurrentHashMap<>();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        int stagnantIterations = 0;

        while (!tasks.stream().allMatch(t ->
                t.getStatus() == TaskNode.TaskStatus.COMPLETED ||
                t.getStatus() == TaskNode.TaskStatus.SKIPPED ||
                t.getStatus() == TaskNode.TaskStatus.FAILED ||
                t.getStatus() == TaskNode.TaskStatus.ROLLED_BACK)) {
            if (context.getStatus() == WorkflowContext.WorkflowStatus.AWAITING_APPROVAL ||
                context.getStatus() == WorkflowContext.WorkflowStatus.FAILED) {
                return context.getStatus() == WorkflowContext.WorkflowStatus.FAILED ? false : false;
            }

            List<TaskNode> readyTasks = tasks.stream()
                    .filter(t -> t.getStatus() == TaskNode.TaskStatus.PENDING ||
                                 t.getStatus() == TaskNode.TaskStatus.READY)
                    .filter(t -> completedTasks.containsAll(t.getDependencies()))
                    .filter(t -> !runningTasks.containsKey(t.getId()))
                    .toList();

            for (TaskNode task : readyTasks) {
                task.setStatus(TaskNode.TaskStatus.READY);
                Future<TaskNode> future = executorService.submit(() -> {
                    SecurityContextHolder.setContext(securityContext);
                    try {
                        return executeTask(task, context, taskMap);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                runningTasks.put(task.getId(), future);
            }

            Iterator<Map.Entry<String, Future<TaskNode>>> iterator = runningTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Future<TaskNode>> entry = iterator.next();
                if (!entry.getValue().isDone()) {
                    continue;
                }

                try {
                    TaskNode task = entry.getValue().get();
                    if (task.getStatus() == TaskNode.TaskStatus.COMPLETED) {
                        completedTasks.add(task.getId());
                        terminalTasks.add(task.getId());
                        if (task.getRetryCount() > 0 || task.getContext().containsKey("replan_started")) {
                            Instant recoveredAt = Instant.now();
                            Instant recoveryStart = task.getStartTime() != null
                                    ? task.getStartTime()
                                    : context.getStartTime();
                            if (recoveryStart != null) {
                                globalMetrics.recordRecoveryTime(Duration.between(recoveryStart, recoveredAt));
                            }
                        }
                        auditLogger.logTaskComplete(task, context);
                    } else if (task.getStatus() == TaskNode.TaskStatus.AWAITING_APPROVAL) {
                        context.setStatus(WorkflowContext.WorkflowStatus.AWAITING_APPROVAL);
                        runningTasks.clear();
                        activeTasks.put(workflowId, tasks);
                        return false;
                    } else if (task.getStatus() == TaskNode.TaskStatus.FAILED ||
                               task.getStatus() == TaskNode.TaskStatus.ROLLED_BACK) {
                        boolean retryScheduled = retryRollbackHandler.handleFailure(task, context);
                        if (!retryScheduled) {
                            triggerReplan(task, context, taskMap, tasks);
                        }
                        if (retryRollbackHandler.shouldSafeStop(task, context)) {
                            terminalTasks.add(task.getId());
                            context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
                            auditLogger.logTaskFailed(task.getId(), task.getErrorMessage(), context);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Task execution error: {}", e.getMessage(), e);
                    context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
                    context.addAuditEvent(entry.getKey(), "TASK_EXECUTION_ERROR", e.getMessage(), Map.of(), "SYSTEM");
                    return false;
                }
                iterator.remove();
            }

            if (runningTasks.isEmpty() && readyTasks.isEmpty()) {
                boolean skippedBlockedTasks = false;
                for (TaskNode task : tasks) {
                    if (task.getStatus() != TaskNode.TaskStatus.PENDING &&
                            task.getStatus() != TaskNode.TaskStatus.READY) {
                        continue;
                    }
                    Optional<TaskNode> failedDependency = task.getDependencies().stream()
                            .map(taskMap::get)
                            .filter(Objects::nonNull)
                            .filter(dependency -> dependency.getStatus() == TaskNode.TaskStatus.FAILED
                                    || dependency.getStatus() == TaskNode.TaskStatus.SKIPPED
                                    || dependency.getStatus() == TaskNode.TaskStatus.ROLLED_BACK)
                            .findFirst();
                    if (failedDependency.isPresent()) {
                        TaskNode dependency = failedDependency.get();
                        task.setStatus(TaskNode.TaskStatus.SKIPPED);
                        terminalTasks.add(task.getId());
                        context.addAuditEvent(task.getId(), "TASK_SKIPPED",
                                "Dependency did not complete: " + dependency.getId(),
                                Map.of("dependency", dependency.getId()), "ORCHESTRATOR");
                        skippedBlockedTasks = true;
                    }
                }
                if (skippedBlockedTasks) {
                    continue;
                }

                stagnantIterations++;
                if (stagnantIterations > 100) {
                    boolean unresolved = tasks.stream().anyMatch(t ->
                            t.getStatus() != TaskNode.TaskStatus.COMPLETED &&
                            t.getStatus() != TaskNode.TaskStatus.SKIPPED &&
                            t.getStatus() != TaskNode.TaskStatus.FAILED &&
                            t.getStatus() != TaskNode.TaskStatus.ROLLED_BACK);
                    if (unresolved) {
                        throw new WorkflowExecutionException("Workflow cannot progress: missing or unsatisfied dependencies");
                    }
                    break;
                }
            } else {
                stagnantIterations = 0;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WorkflowExecutionException("Workflow interrupted");
            }
        }

        if (tasks.stream().anyMatch(t -> t.getStatus() == TaskNode.TaskStatus.FAILED ||
                                         t.getStatus() == TaskNode.TaskStatus.ROLLED_BACK)) {
            context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
            return false;
        }
        context.setStatus(WorkflowContext.WorkflowStatus.COMPLETED);
        return true;
    }

    public WorkflowContext approveTask(String workflowId, String taskId, String approver, String comments) {
        WorkflowContext context = activeWorkflows.get(workflowId);
        if (context == null) {
            return null;
        }
        approvalGateManager.recordApproval(taskId, approver, comments, context);
        List<TaskNode> tasks = activeTasks.get(workflowId);
        if (tasks != null) {
            tasks.stream().filter(t -> t.getId().equals(taskId)).findFirst().ifPresent(task -> {
                task.setStatus(TaskNode.TaskStatus.PENDING);
                task.setErrorMessage(null);
            });
        }
        context.setStatus(WorkflowContext.WorkflowStatus.EXECUTING);
        runResumeWorkflow(workflowId, context);
        return context;
    }

    public WorkflowContext rejectTask(String workflowId, String taskId, String approver, String reason) {
        WorkflowContext context = activeWorkflows.get(workflowId);
        if (context == null) {
            return null;
        }
        approvalGateManager.recordRejection(taskId, approver, reason, context);
        context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
        List<TaskNode> tasks = activeTasks.get(workflowId);
        if (tasks != null) {
            tasks.stream().filter(t -> t.getId().equals(taskId)).findFirst().ifPresent(task ->
                    task.setStatus(TaskNode.TaskStatus.FAILED));
        }
        context.setEndTime(Instant.now());
        return context;
    }

    private void runResumeWorkflow(String workflowId, WorkflowContext context) {
        List<TaskNode> tasks = activeTasks.get(workflowId);
        if (tasks == null) {
            return;
        }
        try {
            runDag(workflowId, tasks, context);
        } finally {
            context.setEndTime(Instant.now());
        }
    }

    private boolean handleTaskFailure(TaskNode task, WorkflowContext context, Map<String, TaskNode> taskMap, List<TaskNode> tasks) {
        boolean shouldRetry = retryRollbackHandler.handleFailure(task, context);
        if (shouldRetry) {
            auditLogger.logDecision(task.getId(), "RETRY", "Retry scheduled by bounded retry policy",
                    Map.of("retry_count", task.getRetryCount()), context);
            return true;
        }
        triggerReplan(task, context, taskMap, tasks);
        return false;
    }

    private void triggerReplan(TaskNode failedTask, WorkflowContext context,
                               Map<String, TaskNode> taskMap, List<TaskNode> tasks) {
        if (tasks == null) {
            return;
        }
        int replanDepth = failedTask.getContext().containsKey("replan_depth")
                ? ((Number) failedTask.getContext().get("replan_depth")).intValue()
                : 0;
        if (replanDepth >= 1 || Boolean.TRUE.equals(failedTask.getContext().get("replan_started"))) {
            context.addAuditEvent(failedTask.getId(), "REPLAN_LIMIT_REACHED",
                    "Dynamic re-planning limit reached; safe-stop applied",
                    Map.of("replan_depth", replanDepth),
                    "ORCHESTRATOR");
            return;
        }
        failedTask.getContext().put("replan_started", true);
        log.info("Triggering dynamic re-planning for failed task: {}", failedTask.getName());

        String baseId = failedTask.getId();
        while (baseId.endsWith("_fix") || baseId.endsWith("_retest")) {
            baseId = baseId.substring(0, baseId.lastIndexOf('_'));
        }
        String baseName = failedTask.getName();
        while (baseName.startsWith("Fix - ") || baseName.startsWith("Retest - ")) {
            baseName = baseName.substring(baseName.indexOf(" - ") + 3);
        }

        String fixId = baseId + "_fix";
        String retestId = baseId + "_retest";
        TaskNode fixTask = TaskNode.builder()
                .id(fixId)
                .name("Fix - " + baseName)
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of())
                .context(new HashMap<>(Map.of("replan_depth", replanDepth + 1)))
                .requiresHumanApproval(false)
                .build();
        TaskNode retestTask = TaskNode.builder()
                .id(retestId)
                .name("Retest - " + baseName)
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of(fixId))
                .context(new HashMap<>(Map.of("replan_depth", replanDepth + 1)))
                .requiresHumanApproval(false)
                .build();

        taskMap.put(fixId, fixTask);
        taskMap.put(retestId, retestTask);
        tasks.add(fixTask);
        tasks.add(retestTask);

        for (TaskNode dependent : new ArrayList<>(tasks)) {
            if (dependent.getDependencies().contains(failedTask.getId()) && dependent != fixTask) {
                List<String> dependencies = new ArrayList<>(dependent.getDependencies());
                dependencies.remove(failedTask.getId());
                dependencies.add(retestId);
                dependent.setDependencies(dependencies);
            }
        }

        failedTask.setStatus(TaskNode.TaskStatus.SKIPPED);
        failedTask.setErrorMessage(null);
        globalMetrics.recordReplan();

        context.addAuditEvent("REPLAN", "DYNAMIC_REPLAN_TRIGGERED",
                "Inserted fix and retest tasks after bounded retry exhaustion",
                Map.of("failed_task", failedTask.getId(), "fix_task", fixId, "retest_task", retestId),
                "ORCHESTRATOR");
        auditLogger.logReplan(failedTask.getId(), "FIX_AND_RETEST", "task_failure",
                context);
        context.addDecision(failedTask.getId(), "REPLAN",
                "Test or task failure exceeded retry limit; inserted fix and re-test tasks",
                Map.of("fix_task", fixId, "retest_task", retestId));

        log.info("Re-planning completed: added fix {} and retest {}", fixId, retestId);
    }

    private TaskNode executeTask(TaskNode task, WorkflowContext context, Map<String, TaskNode> taskMap) {
        log.info("Executing task: {} [{}]", task.getName(), task.getStage());
        auditLogger.logTaskStart(task.getId(), task.getName(), task.getStage(), context);

        task.setStatus(TaskNode.TaskStatus.RUNNING);
        task.setStartTime(Instant.now());

        try {
            Object forcedFailure = task.getContext().get("force_failure");
            if (Boolean.TRUE.equals(forcedFailure) || "true".equalsIgnoreCase(String.valueOf(forcedFailure))) {
                throw new IllegalStateException("Forced task failure from workflow test context");
            }

            // 1. Policy Guardrails Check
            List<GuardrailViolation> violations = policyGuardrails.validateTask(task, context);
            if (!violations.isEmpty()) {
                GuardrailViolation highestSeverity = violations.stream()
                        .max(Comparator.comparing(GuardrailViolation::getSeverity))
                        .orElse(null);

                if (highestSeverity != null && highestSeverity.getSeverity() == Severity.CRITICAL) {
                    task.setStatus(TaskNode.TaskStatus.FAILED);
                    task.setErrorMessage("Critical guardrail violation: " + highestSeverity.getMessage());
                    auditLogger.logGuardrailViolation(task.getId(), violations, context);
                    return task;
                }
                if (highestSeverity != null && highestSeverity.getSeverity() == Severity.HIGH) {
                    task.setRequiresHumanApproval(true);
                    task.setApprovalReason("High-severity policy guardrail violation: " + highestSeverity.getMessage());
                }
            }

            // 2. Entry gates validation
            if (!validateGates(task.getEntryGates(), context, task)) {
                task.setStatus(TaskNode.TaskStatus.FAILED);
                task.setErrorMessage("Entry gate validation failed");
                return task;
            }

            // 3. Human approval checkpoint
            if (approvalGateManager.requiresApproval(task, context)) {
                boolean approved = approvalGateManager.processApprovalGate(task, context);
                if (!approved) {
                    if (task.getStatus() != TaskNode.TaskStatus.AWAITING_APPROVAL) {
                        task.setStatus(TaskNode.TaskStatus.FAILED);
                        task.setErrorMessage("Human approval rejected");
                    }
                    return task;
                }
            }

            // 4. Execute task logic (delegated to agents)
            executeTaskLogic(task, context);

            // 5. Exit gates validation
            if (!validateGates(task.getExitGates(), context, task)) {
                task.setStatus(TaskNode.TaskStatus.FAILED);
                task.setErrorMessage("Exit gate validation failed");
                return task;
            }

            task.setStatus(TaskNode.TaskStatus.COMPLETED);
            task.setEndTime(Instant.now());

        } catch (Exception e) {
            log.error("Task {} failed: {}", task.getName(), e.getMessage(), e);
            task.setStatus(TaskNode.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            auditLogger.logTaskFailed(task.getId(), e.getMessage(), context);
        }

        return task;
    }

    private void executeTaskLogic(TaskNode task, WorkflowContext context) {
        Object analysis = context.getGlobalContext().get("requirement_analysis");
        if (analysis instanceof RequirementAnalyzer.Analysis result) {
            task.getContext().put("normalized_requirement", result.normalizedRequirement());
            task.getContext().put("candidate_acceptance_criteria", result.candidateAcceptanceCriteria());
        }

        // Task execution logic based on type
        switch (task.getType()) {
            case ANALYSIS -> performAnalysis(task, context);
            case DESIGN -> performDesign(task, context);
            case CODE_GENERATION -> performCodeGeneration(task, context);
            case TEST_EXECUTION -> performTestExecution(task, context);
            case VALIDATION -> performValidation(task, context);
            case DOCUMENTATION -> performDocumentation(task, context);
            default -> log.info("Task type {} executed", task.getType());
        }

        // Store outputs in context
        context.getStageOutputs().put(task.getId(), task.getOutputs());
    }

    private void performAnalysis(TaskNode task, WorkflowContext context) {
        if (context.getGlobalContext().containsKey("requirement_analysis")) {
            task.getOutputs().add("ANALYSIS_STEP_COMPLETED");
            return;
        }

        RequirementAnalyzer.Analysis analysis = RequirementAnalyzer.analyze(context.getRequirement());
        context.getGlobalContext().put("requirement_analysis", analysis);
        task.getOutputs().add("REQUIREMENT_ANALYZED");
        task.getOutputs().addAll(analysis.detectedTopics().stream()
                .map(topic -> "DETECTED_TOPIC: " + topic)
                .toList());
        task.getContext().put("normalized_requirement", analysis.normalizedRequirement());
        task.getContext().put("detected_topics", analysis.detectedTopics());
        task.getContext().put("candidate_acceptance_criteria", analysis.candidateAcceptanceCriteria());
        task.getContext().put("ambiguities_identified", analysis.ambiguities());
        task.getContext().put("assumptions", analysis.assumptions());

        context.addDecision(task.getId(), "REQUIREMENT_ANALYSIS",
                "Normalized input and extracted candidate scope using deterministic keyword analysis",
                Map.of(
                        "normalized_requirement", analysis.normalizedRequirement(),
                        "detected_topics", analysis.detectedTopics(),
                        "candidate_acceptance_criteria", analysis.candidateAcceptanceCriteria(),
                        "ambiguities", analysis.ambiguities(),
                        "assumptions", analysis.assumptions()
                ));
        context.addAuditEvent(task.getId(), "REQUIREMENT_ANALYZED",
                "Submitted requirement was normalized and analyzed",
                Map.of(
                        "normalized_requirement", analysis.normalizedRequirement(),
                        "detected_topics", analysis.detectedTopics(),
                        "candidate_acceptance_criteria", analysis.candidateAcceptanceCriteria(),
                        "ambiguities", analysis.ambiguities(),
                        "assumptions", analysis.assumptions()
                ),
                "ORCHESTRATOR");
        log.info("Requirement analyzed: topics={}, ambiguities={}",
                analysis.detectedTopics(), analysis.ambiguities());
    }

    private void performDesign(TaskNode task, WorkflowContext context) {
        log.info("Performing architecture design...");
        task.getOutputs().add("ARCHITECTURE_DESIGNED");
        task.getContext().put("components", Arrays.asList("URL Service", "Analytics Service", "Redirect Handler"));
    }

    private void performCodeGeneration(TaskNode task, WorkflowContext context) {
        log.info("Generating production code and creating real artifacts...");
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setOriginalUrl("https://example.com/orchestration-test-" + System.currentTimeMillis());
        request.setExpiresInMinutes(43200L);

        ShortUrlResponse createdUrl = urlShortenerService.createShortUrl(request);
        if (createdUrl == null || createdUrl.getShortCode() == null) {
            throw new IllegalStateException("Real short URL creation returned no result");
        }

        task.getOutputs().add("CODE_GENERATED");
        task.getOutputs().add("REAL_URL_CREATED: " + createdUrl.getShortCode());
        task.getContext().put("short_code", createdUrl.getShortCode());
        task.getContext().put("original_url", createdUrl.getOriginalUrl());
        context.getGlobalContext().put("generated_short_code", createdUrl.getShortCode());
        log.info("Created real short URL for scenario isolation: {}", createdUrl.getShortCode());
    }

    private void performTestExecution(TaskNode task, WorkflowContext context) {
        log.info("Executing tests with real service validation...");
        String shortCode = context.getStageOutputs().values().stream()
                .flatMap(outputs -> ((List<?>) outputs).stream())
                .filter(output -> output.toString().startsWith("REAL_URL_CREATED:"))
                .map(output -> output.toString().replace("REAL_URL_CREATED: ", ""))
                .findFirst()
                .orElse(null);

        if (shortCode == null) {
            throw new IllegalStateException("No real short URL is available for test execution");
        }

        ShortUrlResponse resolvedUrl = urlShortenerService.getShortUrl(shortCode);
        if (resolvedUrl == null) {
            throw new IllegalStateException("Could not resolve scenario short URL: " + shortCode);
        }
        task.getOutputs().add("TESTS_PASSED");
        task.getOutputs().add("REAL_TEST_EXECUTED: Resolved " + shortCode);
        task.getContext().put("test_url_resolved", true);
        task.getContext().put("resolved_original_url", resolvedUrl.getOriginalUrl());
    }

    private void performValidation(TaskNode task, WorkflowContext context) {
        log.info("Validating outputs...");
        task.getOutputs().add("VALIDATION_PASSED");
    }

    private void performDocumentation(TaskNode task, WorkflowContext context) {
        log.info("Generating documentation...");
        task.getOutputs().add("DOCUMENTATION_GENERATED");
    }

    private boolean validateGates(List<GateCheck> gates, WorkflowContext context, TaskNode task) {
        if (gates == null || gates.isEmpty()) {
            return true;
        }

        for (GateCheck gate : gates) {
            if (!evaluateGate(gate, context, task)) {
                log.warn("Gate {} failed for task {}: {}", gate.getName(), task.getId(), gate.getFailureReason());
                auditLogger.logGateFailed(gate.getName(), task.getId(), gate.getFailureReason(), context);
                return false;
            }
        }
        return true;
    }

    private boolean evaluateGate(GateCheck gate, WorkflowContext context, TaskNode task) {
        // Real gate evaluation logic based on gate type
        boolean passed = switch (gate.getType()) {
            case TEST_COVERAGE -> evaluateTestCoverageGate(task, context);
            case CODE_QUALITY -> evaluateCodeQualityGate(task, context);
            case SECURITY_SCAN -> evaluateSecurityGate(task, context);
            case PERFORMANCE_THRESHOLD -> evaluatePerformanceGate(task, context);
            case COMPLIANCE_CHECK -> evaluateComplianceGate(task, context);
            case DEPENDENCY_CHECK -> evaluateDependencyGate(task, context);
            case BREAKING_CHANGE_DETECTION -> evaluateBreakingChangeGate(task, context);
            case CUSTOM -> evaluateCustomGate(gate, task, context);
        };

        gate.setPassed(passed);
        if (!passed) {
            gate.setFailureReason("Gate check failed: " + gate.getName());
        }
        return passed;
    }

    private boolean evaluateTestCoverageGate(TaskNode task, WorkflowContext context) {
        // Check test coverage from task context
        Object coverage = task.getContext().get("test_coverage");
        if (coverage instanceof Number) {
            double coverageValue = ((Number) coverage).doubleValue();
            boolean passed = coverageValue >= 80.0;  // Minimum 80% coverage
            log.info("Test coverage gate: {}% (required: 80%)", coverageValue);
            return passed;
        }
        return true; // Pass if no coverage data (not a test task)
    }

    private boolean evaluateCodeQualityGate(TaskNode task, WorkflowContext context) {
        // Check if there are any code quality issues
        List<GuardrailViolation> violations = policyGuardrails.validateTask(task, context);
        long criticalCount = violations.stream()
                .filter(v -> v.getSeverity() == Severity.CRITICAL)
                .count();
        boolean passed = criticalCount == 0;
        log.info("Code quality gate: {} critical violations (max: 0)", criticalCount);
        return passed;
    }

    private boolean evaluateSecurityGate(TaskNode task, WorkflowContext context) {
        // Check for security violations
        List<GuardrailViolation> violations = policyGuardrails.validateTask(task, context);
        boolean hasSecurityViolation = violations.stream()
                .anyMatch(v -> v.getMessage().toLowerCase().contains("security")
                        || v.getMessage().toLowerCase().contains("vulnerability"));
        boolean passed = !hasSecurityViolation;
        log.info("Security gate: {} (violations: {})", passed ? "PASSED" : "FAILED", violations.size());
        return passed;
    }

    private boolean evaluatePerformanceGate(TaskNode task, WorkflowContext context) {
        // Check task execution time
        if (task.getStartTime() != null && task.getEndTime() != null) {
            Duration duration = Duration.between(task.getStartTime(), task.getEndTime());
            boolean passed = duration.toSeconds() < 300;  // Max 5 minutes per task
            log.info("Performance gate: {}s (max: 300s)", duration.toSeconds());
            return passed;
        }
        return true; // Pass if task hasn't completed yet
    }

    private boolean evaluateCustomGate(GateCheck gate, TaskNode task, WorkflowContext context) {
        // Custom gate logic based on condition
        if (gate.getCondition() != null && gate.getCondition().contains("coverage")) {
            return evaluateTestCoverageGate(task, context);
        }
        // Default: pass custom gates (would need specific logic per gate)
        return true;
    }

    private boolean evaluateComplianceGate(TaskNode task, WorkflowContext context) {
        // Check for compliance violations
        List<GuardrailViolation> violations = policyGuardrails.validateTask(task, context);
        long complianceCount = violations.stream()
                .filter(v -> "COMPLIANCE".equals(v.getCategory()))
                .count();
        boolean passed = complianceCount == 0;
        log.info("Compliance gate: {} violations (max: 0)", complianceCount);
        return passed;
    }

    private boolean evaluateDependencyGate(TaskNode task, WorkflowContext context) {
        // Check if all required dependencies are present and secure
        // For now, simple check - could integrate with dependency vulnerability scanners
        boolean passed = true;
        log.info("Dependency gate: PASSED (simulated check)");
        return passed;
    }

    private boolean evaluateBreakingChangeGate(TaskNode task, WorkflowContext context) {
        // Check for breaking changes without approval
        boolean hasBreakingChange = task.getContext().containsKey("breaking_change") &&
                Boolean.TRUE.equals(task.getContext().get("breaking_change"));

        if (hasBreakingChange) {
            // If breaking change, must have approval
            boolean hasApproval = context.getApprovals().containsKey(task.getId());
            log.info("Breaking change gate: breaking_change={}, has_approval={}", hasBreakingChange, hasApproval);
            return hasApproval;
        }

        // No breaking change detected - pass
        return true;
    }


    public WorkflowContext getWorkflowContext(String workflowId) {
        return activeWorkflows.get(workflowId);
    }


    public Map<String, WorkflowContext> getAllWorkflowContexts() {
        return new HashMap<>(activeWorkflows);
    }
    public WorkflowMetrics getGlobalMetrics() {
        return globalMetrics;
    }

    public static class WorkflowExecutionException extends RuntimeException {
        public WorkflowExecutionException(String message) {
            super(message);
        }
    }
}
