package com.example.tinyurllab.orchestration.controller;

import com.example.tinyurllab.orchestration.engine.WorkflowOrchestrator;
import com.example.tinyurllab.orchestration.model.*;
import com.example.tinyurllab.orchestration.scenarios.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for workflow orchestration
 * Provides endpoints to trigger scenarios, inspect status, approve gates, and retrieve metrics
 */
@Slf4j
@RestController
@RequestMapping("/api/orchestration")
@Tag(name = "Orchestration", description = "AI-driven SDLC workflow orchestration API")
@RequiredArgsConstructor
public class OrchestrationController {

    private final WorkflowOrchestrator orchestrator;
    private final GreenfieldScenario greenfieldScenario;
    private final BrownfieldScenario brownfieldScenario;
    private final AmbiguousScenario ambiguousScenario;

    @PostMapping("/run/{scenario}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run a workflow scenario",
               description = "Triggers execution of a workflow scenario (greenfield/brownfield/ambiguous)")
    public ResponseEntity<WorkflowExecutionResponse> runScenario(
            @PathVariable String scenario,
            @RequestBody(required = false) RunScenarioRequest request) {

        log.info("Starting workflow scenario: {}", scenario);

        WorkflowScenario workflowScenario = switch (scenario.toLowerCase()) {
            case "greenfield" -> greenfieldScenario;
            case "brownfield" -> brownfieldScenario;
            case "ambiguous" -> ambiguousScenario;
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        };

        // Initialize context
        WorkflowContext context = workflowScenario.initializeContext();
        if (request != null && request.getRequirement() != null) {
            context.setRequirement(request.getRequirement());
        }

        // Build workflow
        List<TaskNode> tasks = workflowScenario.buildWorkflow();

        // Execute workflow
        String workflowId = context.getWorkflowId();
        WorkflowContext result = orchestrator.executeWorkflow(workflowId, tasks, context);

        return ResponseEntity.ok(toExecutionResponse(result));
    }

    @GetMapping("/status/{workflowId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get workflow status", description = "Retrieves the current status and audit trail of a running or completed workflow")
    public ResponseEntity<WorkflowStatusResponse> getStatus(@PathVariable String workflowId) {
        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);

        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toStatusResponse(context));
    }

    @PostMapping("/approve/{workflowId}/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a workflow task", description = "Provides human approval for tasks requiring HITL checkpoint")
    public ResponseEntity<ApprovalResponse> approveTask(
            @PathVariable String workflowId,
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request) {

        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);

        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        // Record approval
        context.getApprovals().put(taskId, request.getApprover() + ": " + request.getComments());
        context.addAuditEvent(taskId, "APPROVAL_GRANTED",
                "Human approval received",
                Map.of("approver", request.getApprover(), "comments", request.getComments()),
                "HUMAN");

        log.info("Task {} approved by {} for workflow {}", taskId, request.getApprover(), workflowId);

        ApprovalResponse response = new ApprovalResponse();
        response.setWorkflowId(workflowId);
        response.setTaskId(taskId);
        response.setApproved(true);
        response.setApprover(request.getApprover());
        response.setTimestamp(java.time.Instant.now());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reject/{workflowId}/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a workflow task", description = "Rejects a task requiring human approval, stopping the workflow")
    public ResponseEntity<ApprovalResponse> rejectTask(
            @PathVariable String workflowId,
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request) {

        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);

        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        // Record rejection
        context.addAuditEvent(taskId, "APPROVAL_REJECTED",
                "Human approval rejected - workflow stopped",
                Map.of("approver", request.getApprover(), "reason", request.getComments()),
                "HUMAN");

        context.setStatus(WorkflowContext.WorkflowStatus.FAILED);

        log.warn("Task {} rejected by {} for workflow {}", taskId, request.getApprover(), workflowId);

        ApprovalResponse response = new ApprovalResponse();
        response.setWorkflowId(workflowId);
        response.setTaskId(taskId);
        response.setApproved(false);
        response.setApprover(request.getApprover());
        response.setTimestamp(java.time.Instant.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orchestration metrics", description = "Retrieves reliability metrics including success rate, retry frequency, MTTR, and latency")
    public ResponseEntity<MetricsResponse> getMetrics() {
        WorkflowMetrics metrics = orchestrator.getGlobalMetrics();

        MetricsResponse response = new MetricsResponse();
        response.setTotalExecutions(metrics.getTotalExecutions().get());
        response.setSuccessfulExecutions(metrics.getSuccessfulExecutions().get());
        response.setFailedExecutions(metrics.getFailedExecutions().get());
        response.setSuccessRate(metrics.getSuccessRate());
        response.setRetriesPerformed(metrics.getRetriesPerformed().get());
        response.setRollbacksPerformed(metrics.getRollbacksPerformed().get());
        response.setReplansPerformed(metrics.getReplansPerformed().get());
        response.setAverageExecutionTimeMs(metrics.getAverageExecutionTimeMs());
        response.setMttr(metrics.getMTTR());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit/{workflowId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit trail", description = "Retrieves complete audit trail and decision lineage for a workflow")
    public ResponseEntity<AuditTrailResponse> getAuditTrail(@PathVariable String workflowId) {
        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);

        if (context == null) {
            return ResponseEntity.notFound().build();
        }

        AuditTrailResponse response = new AuditTrailResponse();
        response.setWorkflowId(workflowId);
        response.setScenarioType(context.getScenarioType());
        response.setRequirement(context.getRequirement());
        response.setAuditEvents(context.getAuditTrail().stream()
                .map(this::toAuditEventDto)
                .collect(Collectors.toList()));
        response.setDecisions(context.getDecisionLineage().stream()
                .map(this::toDecisionDto)
                .collect(Collectors.toList()));
        response.setApprovals(context.getApprovals());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/scenarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all available scenarios", description = "Lists all workflow scenarios with descriptions and task counts")
    public ResponseEntity<List<ScenarioInfo>> getAllScenarios() {
        List<ScenarioInfo> scenarios = Arrays.asList(
                toScenarioInfo(greenfieldScenario),
                toScenarioInfo(brownfieldScenario),
                toScenarioInfo(ambiguousScenario)
        );

        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/{scenario}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get scenario tasks", description = "Retrieves task breakdown for a specific scenario")
    public ResponseEntity<ScenarioTasksResponse> getScenarioTasks(@PathVariable String scenario) {
        WorkflowScenario workflowScenario = switch (scenario.toLowerCase()) {
            case "greenfield" -> greenfieldScenario;
            case "brownfield" -> brownfieldScenario;
            case "ambiguous" -> ambiguousScenario;
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        };

        List<TaskNode> tasks = workflowScenario.buildWorkflow();

        ScenarioTasksResponse response = new ScenarioTasksResponse();
        response.setScenarioName(workflowScenario.getScenarioName());
        response.setDescription(workflowScenario.getDescription());
        response.setTotalTasks(tasks.size());
        response.setTasks(tasks.stream()
                .map(this::toTaskInfo)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflows")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all workflow executions", description = "Lists all workflow executions with status and timestamps")
    public ResponseEntity<List<WorkflowSummary>> getAllWorkflows() {
        Map<String, WorkflowContext> contexts = orchestrator.getAllWorkflowContexts();

        List<WorkflowSummary> workflows = contexts.values().stream()
                .map(this::toWorkflowSummary)
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(workflows);
    }

    // DTO Mapping Methods

    private WorkflowExecutionResponse toExecutionResponse(WorkflowContext context) {
        WorkflowExecutionResponse response = new WorkflowExecutionResponse();
        response.setWorkflowId(context.getWorkflowId());
        response.setScenarioType(context.getScenarioType());
        response.setRequirement(context.getRequirement());
        response.setStatus(context.getStatus().toString());
        response.setStartTime(context.getStartTime());
        response.setEndTime(context.getEndTime());

        if (context.getStartTime() != null && context.getEndTime() != null) {
            response.setDurationMs(Duration.between(context.getStartTime(), context.getEndTime()).toMillis());
        }

        response.setTotalEvents(context.getAuditTrail().size());
        response.setApprovalsRequired(context.getApprovals().size());

        return response;
    }

    private WorkflowStatusResponse toStatusResponse(WorkflowContext context) {
        WorkflowStatusResponse response = new WorkflowStatusResponse();
        response.setWorkflowId(context.getWorkflowId());
        response.setScenarioType(context.getScenarioType());
        response.setStatus(context.getStatus().toString());
        response.setStartTime(context.getStartTime());
        response.setEndTime(context.getEndTime());

        // Calculate progress
        long totalEvents = context.getAuditTrail().size();
        long completedTasks = context.getAuditTrail().stream()
                .filter(e -> "TASK_COMPLETED".equals(e.getEventType()))
                .count();
        long failedTasks = context.getAuditTrail().stream()
                .filter(e -> "TASK_FAILED".equals(e.getEventType()))
                .count();

        response.setTotalEvents(totalEvents);
        response.setCompletedTasks(completedTasks);
        response.setFailedTasks(failedTasks);
        response.setPendingApprovals(context.getApprovals().entrySet().stream()
                .filter(e -> e.getValue().contains("PENDING"))
                .count());

        // Recent events (last 10)
        response.setRecentEvents(context.getAuditTrail().stream()
                .skip(Math.max(0, totalEvents - 10))
                .map(this::toAuditEventDto)
                .collect(Collectors.toList()));

        return response;
    }

    private AuditEventDto toAuditEventDto(WorkflowContext.AuditEvent event) {
        AuditEventDto dto = new AuditEventDto();
        dto.setEventId(event.getEventId());
        dto.setTaskId(event.getTaskId());
        dto.setEventType(event.getEventType());
        dto.setDescription(event.getDescription());
        dto.setMetadata(event.getMetadata());
        dto.setTimestamp(event.getTimestamp());
        dto.setActor(event.getActor());
        return dto;
    }

    private DecisionDto toDecisionDto(WorkflowContext.Decision decision) {
        DecisionDto dto = new DecisionDto();
        dto.setTaskId(decision.getTaskId());
        dto.setDecision(decision.getDecision());
        dto.setRationale(decision.getRationale());
        dto.setContext(decision.getContext());
        dto.setTimestamp(decision.getTimestamp());
        return dto;
    }

    private ScenarioInfo toScenarioInfo(WorkflowScenario scenario) {
        List<TaskNode> tasks = scenario.buildWorkflow();

        ScenarioInfo info = new ScenarioInfo();
        info.setScenarioName(scenario.getScenarioName());
        info.setDescription(scenario.getDescription());
        info.setTotalTasks(tasks.size());
        info.setStages(tasks.stream()
                .map(task -> task.getStage().toString())
                .distinct()
                .collect(Collectors.toList()));

        return info;
    }

    private TaskInfo toTaskInfo(TaskNode task) {
        TaskInfo info = new TaskInfo();
        info.setTaskId(task.getId());
        info.setTaskName(task.getName());
        info.setDescription(task.getDescription());
        info.setStage(task.getStage().toString());
        info.setType(task.getType().toString());
        info.setDependencies(task.getDependencies());
        info.setRequiresApproval(task.isRequiresHumanApproval());
        info.setHasEntryGates(task.getEntryGates() != null && !task.getEntryGates().isEmpty());
        info.setHasExitGates(task.getExitGates() != null && !task.getExitGates().isEmpty());

        return info;
    }

    private WorkflowSummary toWorkflowSummary(WorkflowContext context) {
        WorkflowSummary summary = new WorkflowSummary();
        summary.setWorkflowId(context.getWorkflowId());
        summary.setScenarioType(context.getScenarioType());
        summary.setRequirement(context.getRequirement());
        summary.setStatus(context.getStatus().toString());
        summary.setStartTime(context.getStartTime());
        summary.setEndTime(context.getEndTime());

        if (context.getStartTime() != null && context.getEndTime() != null) {
            summary.setDurationMs(Duration.between(context.getStartTime(), context.getEndTime()).toMillis());
        }

        summary.setTotalEvents(context.getAuditTrail().size());
        summary.setApprovalsCount(context.getApprovals().size());

        return summary;
    }

    // DTOs

    @Data
    public static class RunScenarioRequest {
        private String requirement;
        private Map<String, Object> parameters;
    }

    @Data
    public static class WorkflowExecutionResponse {
        private String workflowId;
        private String scenarioType;
        private String requirement;
        private String status;
        private java.time.Instant startTime;
        private java.time.Instant endTime;
        private Long durationMs;
        private Integer totalEvents;
        private Integer approvalsRequired;
    }

    @Data
    public static class WorkflowStatusResponse {
        private String workflowId;
        private String scenarioType;
        private String status;
        private java.time.Instant startTime;
        private java.time.Instant endTime;
        private Long totalEvents;
        private Long completedTasks;
        private Long failedTasks;
        private Long pendingApprovals;
        private List<AuditEventDto> recentEvents;
    }

    @Data
    public static class ApprovalRequest {
        private String approver;
        private String comments;
    }

    @Data
    public static class ApprovalResponse {
        private String workflowId;
        private String taskId;
        private Boolean approved;
        private String approver;
        private java.time.Instant timestamp;
    }

    @Data
    public static class MetricsResponse {
        private Integer totalExecutions;
        private Integer successfulExecutions;
        private Integer failedExecutions;
        private Double successRate;
        private Integer retriesPerformed;
        private Integer rollbacksPerformed;
        private Integer replansPerformed;
        private Double averageExecutionTimeMs;
        private Double mttr;
    }

    @Data
    public static class AuditTrailResponse {
        private String workflowId;
        private String scenarioType;
        private String requirement;
        private List<AuditEventDto> auditEvents;
        private List<DecisionDto> decisions;
        private Map<String, String> approvals;
    }

    @Data
    public static class AuditEventDto {
        private String eventId;
        private String taskId;
        private String eventType;
        private String description;
        private Map<String, Object> metadata;
        private java.time.Instant timestamp;
        private String actor;
    }

    @Data
    public static class DecisionDto {
        private String taskId;
        private String decision;
        private String rationale;
        private Map<String, Object> context;
        private java.time.Instant timestamp;
    }

    @Data
    public static class ScenarioInfo {
        private String scenarioName;
        private String description;
        private Integer totalTasks;
        private List<String> stages;
    }

    @Data
    public static class ScenarioTasksResponse {
        private String scenarioName;
        private String description;
        private Integer totalTasks;
        private List<TaskInfo> tasks;
    }

    @Data
    public static class TaskInfo {
        private String taskId;
        private String taskName;
        private String description;
        private String stage;
        private String type;
        private List<String> dependencies;
        private Boolean requiresApproval;
        private Boolean hasEntryGates;
        private Boolean hasExitGates;
    }

    @Data
    public static class WorkflowSummary {
        private String workflowId;
        private String scenarioType;
        private String requirement;
        private String status;
        private java.time.Instant startTime;
        private java.time.Instant endTime;
        private Long durationMs;
        private Integer totalEvents;
        private Integer approvalsCount;
    }
}
