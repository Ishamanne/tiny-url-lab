package com.example.tinyurllab.orchestration.governance;

import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Policy guardrails for security, compliance, and change control
 * Ensures autonomous agents operate within defined boundaries
 */
@Slf4j
@Component
public class PolicyGuardrails {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(password|secret|api[_-]?key|token|credential|private[_-]?key)",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_TASK_RETRIES = 3;
    private static final long MAX_EXECUTION_TIME_MS = 300_000; // 5 minutes

    public List<GuardrailViolation> validateTask(TaskNode task, WorkflowContext context) {
        List<GuardrailViolation> violations = new ArrayList<>();

        // Security guardrails
        violations.addAll(checkSecurityPolicies(task, context));

        // Compliance guardrails
        violations.addAll(checkCompliancePolicies(task, context));

        // Change control guardrails
        violations.addAll(checkChangeControlPolicies(task, context));

        // Resource limits
        violations.addAll(checkResourceLimits(task, context));

        return violations;
    }

    private List<GuardrailViolation> checkSecurityPolicies(TaskNode task, WorkflowContext context) {
        List<GuardrailViolation> violations = new ArrayList<>();

        // Check for sensitive data in context
        for (var entry : task.getContext().entrySet()) {
            if (entry.getValue() instanceof String value) {
                if (SENSITIVE_PATTERN.matcher(value).find()) {
                    violations.add(GuardrailViolation.builder()
                            .severity(Severity.HIGH)
                            .category("SECURITY")
                            .message("Potential sensitive data detected in task context: " + entry.getKey())
                            .recommendation("Remove sensitive data or use secure vault")
                            .build());
                    log.warn("Security violation: Sensitive data in task {} context", task.getId());
                }
            }
        }

        // Check for high-risk operations without approval
        if (isHighRiskOperation(task) && !task.isRequiresHumanApproval()) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.MEDIUM)
                    .category("SECURITY")
                    .message("High-risk operation requires human approval: " + task.getName())
                    .recommendation("Add requiresHumanApproval flag to task")
                    .build());
        }

        return violations;
    }

    private List<GuardrailViolation> checkCompliancePolicies(TaskNode task, WorkflowContext context) {
        List<GuardrailViolation> violations = new ArrayList<>();

        // Check audit trail completeness
        if (task.getStatus() == TaskNode.TaskStatus.COMPLETED && task.getEndTime() == null) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.MEDIUM)
                    .category("COMPLIANCE")
                    .message("Task marked completed but no end time recorded")
                    .recommendation("Ensure proper audit trail maintenance")
                    .build());
        }

        // Check approval documentation
        if (task.isRequiresHumanApproval() && !context.getApprovals().containsKey(task.getId())) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.HIGH)
                    .category("COMPLIANCE")
                    .message("Task requires approval but no approval recorded")
                    .recommendation("Obtain and document approval before proceeding")
                    .build());
        }

        return violations;
    }

    private List<GuardrailViolation> checkChangeControlPolicies(TaskNode task, WorkflowContext context) {
        List<GuardrailViolation> violations = new ArrayList<>();

        // Check for breaking changes without approval
        if (task.getContext().containsKey("breaking_change") &&
            Boolean.TRUE.equals(task.getContext().get("breaking_change")) &&
            !task.isRequiresHumanApproval()) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.HIGH)
                    .category("CHANGE_CONTROL")
                    .message("Breaking change detected without approval gate")
                    .recommendation("Add human approval checkpoint for breaking changes")
                    .build());
        }

        // Check rollback plan exists
        if (isDeploymentTask(task) && !task.getContext().containsKey("rollback_plan")) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.MEDIUM)
                    .category("CHANGE_CONTROL")
                    .message("Deployment task missing rollback plan")
                    .recommendation("Define rollback strategy")
                    .build());
        }

        return violations;
    }

    private List<GuardrailViolation> checkResourceLimits(TaskNode task, WorkflowContext context) {
        List<GuardrailViolation> violations = new ArrayList<>();

        // Check retry limits
        if (task.getRetryCount() > MAX_TASK_RETRIES) {
            violations.add(GuardrailViolation.builder()
                    .severity(Severity.HIGH)
                    .category("RESOURCE_LIMIT")
                    .message("Task exceeded maximum retry limit: " + task.getRetryCount())
                    .recommendation("Investigate root cause and implement fix")
                    .build());
        }

        // Check execution time
        if (task.getStartTime() != null && task.getEndTime() != null) {
            long duration = task.getEndTime().toEpochMilli() - task.getStartTime().toEpochMilli();
            if (duration > MAX_EXECUTION_TIME_MS) {
                violations.add(GuardrailViolation.builder()
                        .severity(Severity.MEDIUM)
                        .category("RESOURCE_LIMIT")
                        .message("Task execution time exceeded limit: " + duration + "ms")
                        .recommendation("Optimize task execution or increase timeout")
                        .build());
            }
        }

        return violations;
    }

    private boolean isHighRiskOperation(TaskNode task) {
        // Define high-risk operations
        return task.getType() == TaskNode.TaskType.CODE_GENERATION &&
               task.getContext().containsKey("production_deployment");
    }

    private boolean isDeploymentTask(TaskNode task) {
        return task.getName().toLowerCase().contains("deploy") ||
               task.getName().toLowerCase().contains("release");
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @lombok.Data
    @lombok.Builder
    public static class GuardrailViolation {
        private Severity severity;
        private String category;
        private String message;
        private String recommendation;
    }
}
