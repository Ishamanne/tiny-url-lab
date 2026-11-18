package com.example.tinyurllab.orchestration.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

/**
 * Represents a node in the workflow DAG with dependencies and execution context
 */
@Data
@Builder
public class TaskNode {
    private String id;
    private String name;
    private String description;
    private WorkflowStage stage;
    private TaskType type;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Builder.Default
    private List<String> outputs = new ArrayList<>();

    @Builder.Default
    private List<GateCheck> entryGates = new ArrayList<>();

    @Builder.Default
    private List<GateCheck> exitGates = new ArrayList<>();

    private boolean requiresHumanApproval;
    private String approvalReason;

    private Instant startTime;
    private Instant endTime;
    private int retryCount;

    @Builder.Default
    private int maxRetries = 3;

    private String errorMessage;
    private String agentId;

    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    public enum TaskType {
        ANALYSIS, DESIGN, CODE_GENERATION, TEST_EXECUTION,
        VALIDATION, DOCUMENTATION, MANUAL_REVIEW, CHECKPOINT
    }

    public enum TaskStatus {
        PENDING, READY, RUNNING, AWAITING_APPROVAL,
        COMPLETED, FAILED, SKIPPED, ROLLED_BACK
    }

    public boolean canExecute(Set<String> completedTasks) {
        if (status != TaskStatus.PENDING && status != TaskStatus.READY) {
            return false;
        }
        return completedTasks.containsAll(dependencies);
    }

    public boolean isTerminalState() {
        return status == TaskStatus.COMPLETED ||
               status == TaskStatus.FAILED ||
               status == TaskStatus.SKIPPED;
    }
}
