package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manages human-in-the-loop (HITL) approval gates
 * Handles approval requests, validation, and state transitions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalGateManager {

    private final AuditLogger auditLogger;

    /**
     * Checks if a task requires human approval based on configuration or runtime conditions
     */
    public boolean requiresApproval(TaskNode task, WorkflowContext context) {
        // Explicit approval requirement
        if (task.isRequiresHumanApproval()) {
            return true;
        }

        // Dynamic approval based on context
        // e.g., security violations, high-risk changes, production deployments
        boolean hasSecurityViolation = context.getAuditTrail().stream()
                .anyMatch(e -> "SECURITY_VIOLATION".equals(e.getEventType()));

        boolean isHighRiskStage = task.getStage().toString().contains("RELEASE") ||
                task.getStage().toString().contains("DEPLOYMENT");

        return hasSecurityViolation || isHighRiskStage;
    }

    /**
     * Processes approval gate for a task
     * In production, this would wait for actual human input
     * For demo, we simulate approval
     */
    public boolean processApprovalGate(TaskNode task, WorkflowContext context) {
        if (validateApprovals(task.getId(), context)) {
            String approval = context.getApprovals().get(task.getId());
            if (approval != null && !"PENDING".equals(approval)) {
                task.setStatus(TaskNode.TaskStatus.RUNNING);
                log.info("Recorded approval found for task {}; resuming", task.getId());
                return true;
            }
        }

        String reason = task.getApprovalReason();
        if (reason == null || reason.isBlank()) {
            reason = "High-impact task requires human approval";
            task.setApprovalReason(reason);
        }
        log.info("⚠️  HUMAN APPROVAL REQUIRED");
        log.info("Task: {} [{}]", task.getId(), task.getName());
        log.info("Reason: {}", task.getApprovalReason());

        task.setStatus(TaskNode.TaskStatus.AWAITING_APPROVAL);

        auditLogger.logApprovalRequest(task.getId(), task.getApprovalReason(), context);
        context.setStatus(WorkflowContext.WorkflowStatus.AWAITING_APPROVAL);
        context.getApprovals().put(task.getId(), "PENDING");
        log.info("Approval paused; use /approve or /reject to continue");
        return false;
    }

    /**
     * Simulates human approval for demo purposes
     * In production, this would integrate with an approval system
     */
    private boolean simulateApproval(TaskNode task, WorkflowContext context) {
        // Auto-approve for demo (all approvals granted)
        // In production:
        // - Send notification to approvers
        // - Wait for response via API/UI
        // - Track approval SLA
        // - Escalate if timeout

        return true; // Always approve for demo
    }

    /**
     * Validates that all required approvals have been granted
     */
    public boolean validateApprovals(String taskId, WorkflowContext context) {
        return context.getApprovals().containsKey(taskId);
    }

    /**
     * Records manual approval (called from API endpoint)
     */
    public void recordApproval(String taskId, String approver, String comments, WorkflowContext context) {
        context.getApprovals().put(taskId, approver + ": " + comments);
        auditLogger.logApprovalGranted(taskId, context);

        log.info("Manual approval recorded: {} approved by {}", taskId, approver);
    }

    /**
     * Records manual rejection (called from API endpoint)
     */
    public void recordRejection(String taskId, String approver, String reason, WorkflowContext context) {
        context.addAuditEvent(
                taskId,
                "APPROVAL_REJECTED",
                "Approval rejected by " + approver,
                java.util.Map.of("approver", approver, "reason", reason),
                "HUMAN"
        );

        log.warn("Manual rejection recorded: {} rejected by {} - Reason: {}", taskId, approver, reason);
    }

    /**
     * Gets approval statistics for metrics
     */
    public ApprovalStats getApprovalStats(WorkflowContext context) {
        long requested = context.getAuditTrail().stream()
                .filter(e -> "APPROVAL_REQUESTED".equals(e.getEventType()))
                .count();

        long granted = context.getAuditTrail().stream()
                .filter(e -> "APPROVAL_GRANTED".equals(e.getEventType()))
                .count();

        long rejected = context.getAuditTrail().stream()
                .filter(e -> "APPROVAL_REJECTED".equals(e.getEventType()))
                .count();

        return new ApprovalStats(requested, granted, rejected);
    }

    public record ApprovalStats(long requested, long granted, long rejected) {
        public double approvalRate() {
            return requested == 0 ? 0.0 : (double) granted / requested * 100;
        }
    }
}
