package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;
import com.example.tinyurllab.orchestration.model.WorkflowMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles retry logic and rollback mechanisms for failed tasks
 * Implements exponential backoff and rollback strategies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryRollbackHandler {

    private final AuditLogger auditLogger;
    private final WorkflowMetrics globalMetrics;

    /**
     * Handles task failure with retry logic
     * Returns true if task should be retried, false if rollback needed
     */
    public boolean handleFailure(TaskNode task, WorkflowContext context) {
        int retryCount = context.incrementRetry(task.getId());

        log.warn("Task failed: {} [{}] - Retry count: {}/{}",
                task.getId(), task.getName(), retryCount, task.getMaxRetries());

        if (retryCount <= task.getMaxRetries()) {
            // Retry the task
            scheduleRetry(task, retryCount, context);
            return true;
        } else {
            // Max retries exceeded - initiate rollback
            initiateRollback(task, context);
            return false;
        }
    }

    /**
     * Schedules a retry with exponential backoff
     */
    private void scheduleRetry(TaskNode task, int retryCount, WorkflowContext context) {
        long backoffMs = calculateBackoff(retryCount);

        log.info("Scheduling retry {} for task {} - Backoff: {}ms",
                retryCount, task.getId(), backoffMs);

        task.setStatus(TaskNode.TaskStatus.PENDING);
        task.setRetryCount(retryCount);

        globalMetrics.recordRetry();
        auditLogger.logRetry(task.getId(), retryCount, context);

        // In production, this would use a scheduler
        // For demo, we just reset status immediately
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry backoff interrupted");
        }
    }

    /**
     * Calculates exponential backoff time
     * Base: 1000ms, multiplier: 2^retry
     */
    private long calculateBackoff(int retryCount) {
        long baseBackoff = 100L;
        return (long) (baseBackoff * Math.pow(2, retryCount - 1));
    }

    /**
     * Initiates rollback for a failed task
     */
    private void initiateRollback(TaskNode task, WorkflowContext context) {
        log.error("Max retries exceeded for task {} - Initiating rollback", task.getId());

        task.setStatus(TaskNode.TaskStatus.ROLLED_BACK);

        String rollbackReason = String.format(
                "Max retries (%d) exceeded. Error: %s",
                task.getMaxRetries(),
                task.getErrorMessage()
        );

        globalMetrics.recordRollback();
        auditLogger.logRollback(task.getId(), rollbackReason, context);

        // Execute rollback logic
        performRollback(task, context);
    }

    /**
     * Performs actual rollback operations
     * Reverts changes made by the failed task
     */
    private void performRollback(TaskNode task, WorkflowContext context) {
        log.info("Performing rollback for task: {}", task.getId());

        // Rollback strategies based on task type
        switch (task.getType()) {
            case CODE_GENERATION -> rollbackCodeChanges(task, context);
            case VALIDATION -> rollbackValidation(task, context);
            case DOCUMENTATION -> rollbackDocumentation(task, context);
            default -> log.info("No specific rollback needed for task type: {}", task.getType());
        }

        // Remove task outputs from context
        context.getStageOutputs().remove(task.getId());

        log.info("Rollback completed for task: {}", task.getId());
    }

    private void rollbackCodeChanges(TaskNode task, WorkflowContext context) {
        log.info("Rolling back code changes for task: {}", task.getId());
        // In production:
        // - Revert Git commits
        // - Delete generated files
        // - Restore previous state
        // For demo:
        context.addAuditEvent(
                task.getId(),
                "ROLLBACK_CODE",
                "Code changes reverted",
                Map.of("files_reverted", 0),
                "SYSTEM"
        );
    }

    private void rollbackValidation(TaskNode task, WorkflowContext context) {
        log.info("Rolling back validation for task: {}", task.getId());
        // Clear validation results
        context.addAuditEvent(
                task.getId(),
                "ROLLBACK_VALIDATION",
                "Validation results cleared",
                Map.of(),
                "SYSTEM"
        );
    }

    private void rollbackDocumentation(TaskNode task, WorkflowContext context) {
        log.info("Rolling back documentation for task: {}", task.getId());
        // Remove generated documentation
        context.addAuditEvent(
                task.getId(),
                "ROLLBACK_DOCS",
                "Documentation reverted",
                Map.of(),
                "SYSTEM"
        );
    }

    /**
     * Implements safe-stop mechanism
     * Checks if workflow should be stopped based on failure severity
     */
    public boolean shouldSafeStop(TaskNode task, WorkflowContext context) {
        // Critical tasks that should stop entire workflow on failure
        boolean isCriticalTask = task.getStage().toString().contains("SECURITY") ||
                task.getStage().toString().contains("COMPLIANCE");

        // Check for cascading failures
        long recentFailures = context.getAuditTrail().stream()
                .filter(e -> "TASK_FAILED".equals(e.getEventType()))
                .filter(e -> e.getTimestamp().isAfter(
                        java.time.Instant.now().minusSeconds(60)
                ))
                .count();

        boolean hasCascadingFailures = recentFailures > 3;

        if (isCriticalTask || hasCascadingFailures) {
            log.error("SAFE-STOP triggered: Critical={}, Cascading={}",
                    isCriticalTask, hasCascadingFailures);

            context.addAuditEvent(
                    task.getId(),
                    "SAFE_STOP",
                    "Workflow stopped due to critical failure",
                    Map.of(
                            "critical_task", isCriticalTask,
                            "cascading_failures", hasCascadingFailures,
                            "recent_failures", recentFailures
                    ),
                    "SYSTEM"
            );

            context.setStatus(WorkflowContext.WorkflowStatus.FAILED);
            return true;
        }

        return false;
    }

    /**
     * Gets retry statistics for metrics
     */
    public RetryStats getRetryStats(WorkflowContext context) {
        int totalRetries = context.getRetryCounters().values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        int tasksWithRetries = context.getRetryCounters().size();

        double avgRetriesPerTask = tasksWithRetries == 0 ? 0.0 :
                (double) totalRetries / tasksWithRetries;

        return new RetryStats(totalRetries, tasksWithRetries, avgRetriesPerTask);
    }

    public record RetryStats(int totalRetries, int tasksWithRetries, double avgRetriesPerTask) {}
}
