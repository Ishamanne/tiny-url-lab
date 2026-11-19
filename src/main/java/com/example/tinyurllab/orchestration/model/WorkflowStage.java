package com.example.tinyurllab.orchestration.model;

/**
 * SDLC stages in the agentic orchestration workflow
 */
public enum WorkflowStage {
    REQUIREMENT_ANALYSIS("Requirement Analysis", "Interpret and normalize requirements"),
    ARCHITECTURE_DESIGN("Architecture Design", "Design system architecture and components"),
    TASK_DECOMPOSITION("Task Decomposition", "Break down into actionable tasks with dependencies"),
    IMPLEMENTATION("Implementation", "Generate production-quality code"),
    TESTING("Testing", "Execute unit and integration tests"),
    DOCUMENTATION("Documentation", "Generate technical documentation"),
    VALIDATION("Validation", "Validate outputs and check quality gates"),
    RELEASE_READINESS("Release Readiness", "Final checks before deployment");

    private final String displayName;
    private final String description;

    WorkflowStage(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
