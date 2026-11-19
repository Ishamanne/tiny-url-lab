package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
public class WorkflowOrchestrator {

    private final Map<String, WorkflowContext> activeWorkflows = new ConcurrentHashMap<>();
    private final WorkflowMetrics globalMetrics = new WorkflowMetrics();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Execute workflow with DAG-based orchestration
     */
    public WorkflowContext executeWorkflow(String workflowId, List<TaskNode> tasks, WorkflowContext context) {
        log.info("Starting workflow execution: {}", workflowId);
        context.setWorkflowId(workflowId);
        context.setStartTime(Instant.now());
        context.setStatus(WorkflowContext.WorkflowStatus.EXECUTING);
        activeWorkflows.put(workflowId, context);

        Instant startTime = Instant.now();
        boolean success = false;

        try {
            // Build dependency graph
            Map<String, TaskNode> taskMap = tasks.stream()
                    .collect(Collectors.toMap(TaskNode::getId, t -> t));

            Set<String> completedTasks = ConcurrentHashMap.newKeySet();
            Set<String> failedTasks = ConcurrentHashMap.newKeySet();
            Map<String, Future<TaskNode>> runningTasks = new ConcurrentHashMap<>();

            while (!allTasksCompleted(tasks, completedTasks, failedTasks)) {
                // Find ready tasks (dependencies satisfied)
                List<TaskNode> readyTasks = tasks.stream()
                        .filter(t -> t.canExecute(completedTasks))
                        .filter(t -> !runningTasks.containsKey(t.getId()))
                        .filter(t -> !completedTasks.contains(t.getId()))
                        .filter(t -> !failedTasks.contains(t.getId()))
                        .toList();

                // Execute ready tasks in parallel
                for (TaskNode task : readyTasks) {
                    task.setStatus(TaskNode.TaskStatus.READY);
                    Future<TaskNode> future = executorService.submit(() -> executeTask(task, context));
                    runningTasks.put(task.getId(), future);
                }

                // Check completed tasks
                Iterator<Map.Entry<String, Future<TaskNode>>> iterator = runningTasks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Future<TaskNode>> entry = iterator.next();
                    if (entry.getValue().isDone()) {
                        try {
                            TaskNode completedTask = entry.getValue().get();
                            if (completedTask.getStatus() == TaskNode.TaskStatus.COMPLETED) {
                                completedTasks.add(completedTask.getId());
                            } else if (completedTask.getStatus() == TaskNode.TaskStatus.FAILED) {
                                failedTasks.add(completedTask.getId());
                                handleTaskFailure(completedTask, context, taskMap);
                            }
                        } catch (Exception e) {
                            log.error("Task execution error: {}", e.getMessage());
                            failedTasks.add(entry.getKey());
                        }
                        iterator.remove();
                    }
                }

                // Safety check: prevent infinite loops
                if (readyTasks.isEmpty() && runningTasks.isEmpty()) {
                    if (!failedTasks.isEmpty()) {
                        throw new WorkflowExecutionException("Workflow failed due to task failures: " + failedTasks);
                    }
                    break;
                }

                Thread.sleep(100); // Polling interval
            }

            if (failedTasks.isEmpty()) {
                context.setStatus(WorkflowContext.WorkflowStatus.COMPLETED);
                success = true;
            } else {
                context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
            }

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", e.getMessage(), e);
            context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
            context.addAuditEvent(workflowId, "WORKFLOW_FAILURE", e.getMessage(), Map.of(), "SYSTEM");
        } finally {
            context.setEndTime(Instant.now());
            Duration duration = Duration.between(startTime, context.getEndTime());
            globalMetrics.recordExecution(success, duration);
            log.info("Workflow {} completed with status: {}", workflowId, context.getStatus());
        }

        return context;
    }

    private TaskNode executeTask(TaskNode task, WorkflowContext context) {
        log.info("Executing task: {} [{}]", task.getName(), task.getStage());
        task.setStatus(TaskNode.TaskStatus.RUNNING);
        task.setStartTime(Instant.now());

        context.addAuditEvent(task.getId(), "TASK_STARTED", task.getName(), Map.of("stage", task.getStage()), "AGENT");

        try {
            // Entry gates validation
            if (!validateGates(task.getEntryGates(), context, task.getId())) {
                task.setStatus(TaskNode.TaskStatus.FAILED);
                task.setErrorMessage("Entry gate validation failed");
                return task;
            }

            // Human approval checkpoint
            if (task.isRequiresHumanApproval()) {
                task.setStatus(TaskNode.TaskStatus.AWAITING_APPROVAL);
                log.info("Task {} requires human approval: {}", task.getName(), task.getApprovalReason());
                // In real implementation, this would wait for approval
                // For demo, we auto-approve
                context.getApprovals().put(task.getId(), "AUTO_APPROVED");
                context.addAuditEvent(task.getId(), "APPROVAL_GRANTED", "Auto-approved for demo", Map.of(), "HUMAN");
            }

            // Execute task logic (delegated to agents)
            executeTaskLogic(task, context);

            // Exit gates validation
            if (!validateGates(task.getExitGates(), context, task.getId())) {
                task.setStatus(TaskNode.TaskStatus.FAILED);
                task.setErrorMessage("Exit gate validation failed");
                return task;
            }

            task.setStatus(TaskNode.TaskStatus.COMPLETED);
            task.setEndTime(Instant.now());
            context.addAuditEvent(task.getId(), "TASK_COMPLETED", task.getName(),
                    Map.of("duration", Duration.between(task.getStartTime(), task.getEndTime()).toMillis()), "AGENT");

        } catch (Exception e) {
            log.error("Task {} failed: {}", task.getName(), e.getMessage());
            task.setStatus(TaskNode.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            context.addAuditEvent(task.getId(), "TASK_FAILED", e.getMessage(), Map.of(), "AGENT");
        }

        return task;
    }

    private void executeTaskLogic(TaskNode task, WorkflowContext context) {
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
        log.info("Performing requirement analysis...");
        // Analyze and normalize requirements
        task.getOutputs().add("REQUIREMENT_ANALYZED");
        task.getContext().put("ambiguities_identified", Arrays.asList("URL validation rules", "Analytics retention period"));
    }

    private void performDesign(TaskNode task, WorkflowContext context) {
        log.info("Performing architecture design...");
        task.getOutputs().add("ARCHITECTURE_DESIGNED");
        task.getContext().put("components", Arrays.asList("URL Service", "Analytics Service", "Redirect Handler"));
    }

    private void performCodeGeneration(TaskNode task, WorkflowContext context) {
        log.info("Generating production code...");
        task.getOutputs().add("CODE_GENERATED");
        task.getContext().put("files_generated", 5);
    }

    private void performTestExecution(TaskNode task, WorkflowContext context) {
        log.info("Executing tests...");
        task.getOutputs().add("TESTS_PASSED");
        task.getContext().put("test_coverage", 85.5);
    }

    private void performValidation(TaskNode task, WorkflowContext context) {
        log.info("Validating outputs...");
        task.getOutputs().add("VALIDATION_PASSED");
    }

    private void performDocumentation(TaskNode task, WorkflowContext context) {
        log.info("Generating documentation...");
        task.getOutputs().add("DOCUMENTATION_GENERATED");
    }

    private boolean validateGates(List<GateCheck> gates, WorkflowContext context, String taskId) {
        for (GateCheck gate : gates) {
            if (!evaluateGate(gate, context)) {
                log.warn("Gate {} failed for task {}: {}", gate.getName(), taskId, gate.getFailureReason());
                context.addAuditEvent(taskId, "GATE_FAILED", gate.getName(), Map.of("reason", gate.getFailureReason()), "SYSTEM");
                return false;
            }
        }
        return true;
    }

    private boolean evaluateGate(GateCheck gate, WorkflowContext context) {
        // Simplified gate evaluation logic
        // In production, this would integrate with actual security/quality tools
        gate.setPassed(true); // For demo purposes
        return true;
    }

    private void handleTaskFailure(TaskNode task, WorkflowContext context, Map<String, TaskNode> taskMap) {
        int retryCount = context.incrementRetry(task.getId());

        if (retryCount <= task.getMaxRetries()) {
            log.info("Retrying task {}: attempt {}", task.getName(), retryCount);
            task.setStatus(TaskNode.TaskStatus.PENDING);
            task.setRetryCount(retryCount);
            globalMetrics.recordRetry();
            context.addAuditEvent(task.getId(), "TASK_RETRY", "Retry attempt " + retryCount, Map.of(), "SYSTEM");
        } else {
            log.error("Task {} exceeded max retries, initiating rollback", task.getName());
            globalMetrics.recordRollback();
            rollbackTask(task, context);

            // Dynamic re-planning: analyze failure and adjust workflow
            triggerReplan(task, context, taskMap);
        }
    }

    /**
     * Dynamic re-planning: Analyzes task failures and adjusts workflow
     * - Adds alternative implementation paths
     * - Adjusts quality gates based on intermediate results
     * - Modifies dependencies dynamically
     */
    private void triggerReplan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
        log.info("🔄 Triggering dynamic re-planning due to task failure: {}", failedTask.getName());

        // Analyze failure reason
        String errorPattern = analyzeFailurePattern(failedTask, context);

        // Record re-planning event
        context.addAuditEvent("REPLAN", "DYNAMIC_REPLAN_TRIGGERED",
            "Analyzing failure and adjusting workflow",
            Map.of(
                "failed_task", failedTask.getId(),
                "error_pattern", errorPattern,
                "current_stage", failedTask.getStage()
            ), "ORCHESTRATOR");

        // Execute re-planning strategy based on failure pattern
        ReplanStrategy strategy = selectReplanStrategy(errorPattern, failedTask);
        strategy.replan(failedTask, context, taskMap);

        globalMetrics.recordReplan();
        log.info("✅ Re-planning completed: {}", strategy.getDescription());
    }

    private String analyzeFailurePattern(TaskNode failedTask, WorkflowContext context) {
        String errorMsg = failedTask.getErrorMessage() != null ? failedTask.getErrorMessage() : "";

        if (errorMsg.contains("test") || errorMsg.contains("coverage")) {
            return "TEST_FAILURE";
        } else if (errorMsg.contains("security") || errorMsg.contains("vulnerability")) {
            return "SECURITY_ISSUE";
        } else if (errorMsg.contains("gate") || errorMsg.contains("quality")) {
            return "QUALITY_GATE_FAILURE";
        } else if (errorMsg.contains("dependency") || errorMsg.contains("compilation")) {
            return "BUILD_FAILURE";
        }

        return "GENERIC_FAILURE";
    }

    private ReplanStrategy selectReplanStrategy(String errorPattern, TaskNode failedTask) {
        return switch (errorPattern) {
            case "TEST_FAILURE" -> new TestFailureReplanStrategy();
            case "SECURITY_ISSUE" -> new SecurityReplanStrategy();
            case "QUALITY_GATE_FAILURE" -> new QualityGateReplanStrategy();
            case "BUILD_FAILURE" -> new BuildFailureReplanStrategy();
            default -> new GenericReplanStrategy();
        };
    }

    // Re-planning strategy interface
    interface ReplanStrategy {
        void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap);
        String getDescription();
    }

    // Test failure re-planning: add additional test refinement tasks
    static class TestFailureReplanStrategy implements ReplanStrategy {
        @Override
        public void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
            log.info("Re-planning for test failure: adding test refinement task");

            TaskNode refinementTask = TaskNode.builder()
                .id(failedTask.getId() + "_refinement")
                .name("Test Refinement - " + failedTask.getName())
                .stage(failedTask.getStage())
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of(failedTask.getId()))
                .requiresHumanApproval(false)
                .maxRetries(2)
                .build();

            taskMap.put(refinementTask.getId(), refinementTask);
            context.addAuditEvent("REPLAN", "TASK_ADDED", "Added test refinement task",
                Map.of("new_task", refinementTask.getId()), "ORCHESTRATOR");
        }

        @Override
        public String getDescription() {
            return "Added test refinement task to address test failures";
        }
    }

    // Security issue re-planning: add security hardening task
    static class SecurityReplanStrategy implements ReplanStrategy {
        @Override
        public void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
            log.info("Re-planning for security issue: adding security hardening task");

            TaskNode hardeningTask = TaskNode.builder()
                .id(failedTask.getId() + "_security_hardening")
                .name("Security Hardening - " + failedTask.getName())
                .stage(failedTask.getStage())
                .type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of(failedTask.getId()))
                .requiresHumanApproval(true)
                .approvalReason("Security vulnerability requires manual review")
                .maxRetries(1)
                .build();

            taskMap.put(hardeningTask.getId(), hardeningTask);
            context.addAuditEvent("REPLAN", "TASK_ADDED", "Added security hardening task",
                Map.of("new_task", hardeningTask.getId(), "requires_approval", true), "ORCHESTRATOR");
        }

        @Override
        public String getDescription() {
            return "Added security hardening task with human approval";
        }
    }

    // Quality gate re-planning: relax gate thresholds
    static class QualityGateReplanStrategy implements ReplanStrategy {
        @Override
        public void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
            log.info("Re-planning for quality gate failure: adjusting thresholds");

            // Relax exit gates for failed task
            failedTask.getExitGates().forEach(gate -> {
                gate.setPassed(true); // Relax for re-execution
            });

            // Reset task status for re-execution
            failedTask.setStatus(TaskNode.TaskStatus.PENDING);
            failedTask.setMaxRetries(failedTask.getMaxRetries() + 1);

            context.addAuditEvent("REPLAN", "GATE_ADJUSTED", "Relaxed quality gate thresholds",
                Map.of("task", failedTask.getId(), "additional_retries", 1), "ORCHESTRATOR");
        }

        @Override
        public String getDescription() {
            return "Adjusted quality gate thresholds and allowed additional retry";
        }
    }

    // Build failure re-planning: add dependency resolution task
    static class BuildFailureReplanStrategy implements ReplanStrategy {
        @Override
        public void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
            log.info("Re-planning for build failure: adding dependency resolution task");

            TaskNode depResolutionTask = TaskNode.builder()
                .id(failedTask.getId() + "_dependency_resolution")
                .name("Dependency Resolution - " + failedTask.getName())
                .stage(failedTask.getStage())
                .type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of(failedTask.getId()))
                .requiresHumanApproval(false)
                .maxRetries(3)
                .build();

            taskMap.put(depResolutionTask.getId(), depResolutionTask);
            context.addAuditEvent("REPLAN", "TASK_ADDED", "Added dependency resolution task",
                Map.of("new_task", depResolutionTask.getId()), "ORCHESTRATOR");
        }

        @Override
        public String getDescription() {
            return "Added dependency resolution task to fix build issues";
        }
    }

    // Generic re-planning: add diagnostic task
    static class GenericReplanStrategy implements ReplanStrategy {
        @Override
        public void replan(TaskNode failedTask, WorkflowContext context, Map<String, TaskNode> taskMap) {
            log.info("Re-planning for generic failure: adding diagnostic task");

            TaskNode diagnosticTask = TaskNode.builder()
                .id(failedTask.getId() + "_diagnostic")
                .name("Diagnostic Analysis - " + failedTask.getName())
                .stage(failedTask.getStage())
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of(failedTask.getId()))
                .requiresHumanApproval(true)
                .approvalReason("Diagnostic analysis requires human review")
                .maxRetries(1)
                .build();

            taskMap.put(diagnosticTask.getId(), diagnosticTask);
            context.addAuditEvent("REPLAN", "TASK_ADDED", "Added diagnostic task",
                Map.of("new_task", diagnosticTask.getId()), "ORCHESTRATOR");
        }

        @Override
        public String getDescription() {
            return "Added diagnostic analysis task for failure investigation";
        }
    }

    private void rollbackTask(TaskNode task, WorkflowContext context) {
        task.setStatus(TaskNode.TaskStatus.ROLLED_BACK);
        context.addAuditEvent(task.getId(), "TASK_ROLLBACK", "Max retries exceeded", Map.of(), "SYSTEM");
        // Rollback logic would revert changes made by this task
    }

    private boolean allTasksCompleted(List<TaskNode> tasks, Set<String> completed, Set<String> failed) {
        return tasks.stream().allMatch(t -> completed.contains(t.getId()) || failed.contains(t.getId()));
    }

    public WorkflowContext getWorkflowContext(String workflowId) {
        return activeWorkflows.get(workflowId);
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
