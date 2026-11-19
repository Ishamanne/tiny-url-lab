package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Dedicated component for audit logging and decision lineage tracking
 * Provides audit-grade traceability for all workflow actions
 */
@Slf4j
@Component
public class AuditLogger {

    public void logWorkflowStart(String workflowId, String scenarioType, String requirement) {
        log.info("=== Workflow Started ===");
        log.info("Workflow ID: {}", workflowId);
        log.info("Scenario: {}", scenarioType);
        log.info("Requirement: {}", requirement);
    }

    public void logWorkflowComplete(WorkflowContext context) {
        Duration duration = Duration.between(context.getStartTime(), context.getEndTime());

        log.info("=== Workflow Completed ===");
        log.info("Workflow ID: {}", context.getWorkflowId());
        log.info("Status: {}", context.getStatus());
        log.info("Duration: {}ms", duration.toMillis());
        log.info("Total Events: {}", context.getAuditTrail().size());
        log.info("Approvals: {}", context.getApprovals().size());
        log.info("Retries: {}", context.getRetryCounters().size());
    }

    public void logTaskStart(TaskNode task, WorkflowContext context) {
        log.debug("Task Started: {} [{}] - Stage: {}", task.getId(), task.getName(), task.getStage());

        context.addAuditEvent(
                task.getId(),
                "TASK_STARTED",
                "Task execution started: " + task.getName(),
                Map.of(
                        "stage", task.getStage().toString(),
                        "type", task.getType().toString(),
                        "has_dependencies", !task.getDependencies().isEmpty()
                ),
                "AGENT"
        );
    }

    public void logTaskComplete(TaskNode task, WorkflowContext context) {
        Duration duration = Duration.between(task.getStartTime(), task.getEndTime());

        log.debug("Task Completed: {} [{}] - Duration: {}ms",
                task.getId(), task.getName(), duration.toMillis());

        context.addAuditEvent(
                task.getId(),
                "TASK_COMPLETED",
                "Task execution completed successfully",
                Map.of(
                        "duration_ms", duration.toMillis(),
                        "outputs_count", task.getOutputs().size()
                ),
                "AGENT"
        );
    }

    public void logTaskFailure(TaskNode task, WorkflowContext context, String reason) {
        log.error("Task Failed: {} [{}] - Reason: {}", task.getId(), task.getName(), reason);

        context.addAuditEvent(
                task.getId(),
                "TASK_FAILED",
                "Task execution failed: " + reason,
                Map.of(
                        "error_message", reason,
                        "retry_count", task.getRetryCount()
                ),
                "AGENT"
        );
    }

    public void logApprovalRequest(String taskId, String reason, WorkflowContext context) {
        log.info("Approval Required: Task {} - Reason: {}", taskId, reason);

        context.addAuditEvent(
                taskId,
                "APPROVAL_REQUESTED",
                "Human approval required: " + reason,
                Map.of("approval_reason", reason),
                "SYSTEM"
        );
    }

    public void logApprovalGranted(String taskId, WorkflowContext context) {
        log.info("Approval Granted: Task {}", taskId);

        context.addAuditEvent(
                taskId,
                "APPROVAL_GRANTED",
                "Human approval granted",
                Map.of("approval_status", "APPROVED"),
                "HUMAN"
        );
    }

    public void logGateValidation(String taskId, String gateName, boolean passed, WorkflowContext context) {
        String eventType = passed ? "GATE_PASSED" : "GATE_FAILED";

        log.debug("Gate {}: {} - {}", eventType, gateName, taskId);

        context.addAuditEvent(
                taskId,
                eventType,
                gateName + (passed ? " passed" : " failed"),
                Map.of("gate_name", gateName, "passed", passed),
                "SYSTEM"
        );
    }

    public void logRetry(String taskId, int retryCount, WorkflowContext context) {
        log.warn("Retry Attempt: Task {} - Attempt {}", taskId, retryCount);

        context.addAuditEvent(
                taskId,
                "TASK_RETRY",
                "Retry attempt " + retryCount,
                Map.of("retry_count", retryCount),
                "SYSTEM"
        );
    }

    public void logRollback(String taskId, String reason, WorkflowContext context) {
        log.error("Rollback Triggered: Task {} - Reason: {}", taskId, reason);

        context.addAuditEvent(
                taskId,
                "TASK_ROLLBACK",
                "Rollback initiated: " + reason,
                Map.of("rollback_reason", reason),
                "SYSTEM"
        );
    }

    public void logReplan(String taskId, String strategy, String errorPattern, WorkflowContext context) {
        log.info("Dynamic Re-planning: Task {} - Strategy: {} - Pattern: {}",
                taskId, strategy, errorPattern);

        context.addAuditEvent(
                "REPLAN",
                "DYNAMIC_REPLAN_TRIGGERED",
                "Workflow re-planned using " + strategy,
                Map.of(
                        "failed_task", taskId,
                        "strategy", strategy,
                        "error_pattern", errorPattern
                ),
                "ORCHESTRATOR"
        );
    }

    public void logDecision(String taskId, String decision, String rationale,
                           Map<String, Object> decisionContext, WorkflowContext context) {
        log.info("Decision: Task {} - {} ({})", taskId, decision, rationale);

        context.addDecision(taskId, decision, rationale, decisionContext);
    }

    public String generateExecutionSummary(WorkflowContext context) {
        long totalTasks = context.getAuditTrail().stream()
                .filter(e -> "TASK_STARTED".equals(e.getEventType()))
                .count();

        long completedTasks = context.getAuditTrail().stream()
                .filter(e -> "TASK_COMPLETED".equals(e.getEventType()))
                .count();

        long failedTasks = context.getAuditTrail().stream()
                .filter(e -> "TASK_FAILED".equals(e.getEventType()))
                .count();

        long replans = context.getAuditTrail().stream()
                .filter(e -> "DYNAMIC_REPLAN_TRIGGERED".equals(e.getEventType()))
                .count();

        return String.format(
                "Execution Summary: %d total tasks, %d completed, %d failed, %d re-plans, %d approvals",
                totalTasks, completedTasks, failedTasks, replans, context.getApprovals().size()
        );
    }
}
