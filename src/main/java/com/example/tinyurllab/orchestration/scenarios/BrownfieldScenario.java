package com.example.tinyurllab.orchestration.scenarios;

import com.example.tinyurllab.orchestration.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * BROWNFIELD SCENARIO: Add rate limiting to existing URL Shortener
 * Demonstrates enhancement of existing system with impact analysis
 */
@Component
public class BrownfieldScenario implements WorkflowScenario {

    @Override
    public String getScenarioName() {
        return "Brownfield: Add Rate Limiting Feature";
    }

    @Override
    public String getDescription() {
        return "Enhance existing URL shortener with rate limiting to prevent abuse";
    }

    @Override
    public List<TaskNode> buildWorkflow() {
        List<TaskNode> tasks = new ArrayList<>();

        // Stage 1: Analyze Existing Codebase
        TaskNode codebaseAnalysis = TaskNode.builder()
                .id("brown-001")
                .name("Analyze Existing Codebase")
                .description("Identify impacted modules, services, APIs for rate limiting")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        tasks.add(codebaseAnalysis);

        // Stage 2: Impact Analysis
        TaskNode impactAnalysis = TaskNode.builder()
                .id("brown-002")
                .name("Perform Impact Analysis")
                .description("Identify breaking changes and affected components")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of("brown-001"))
                .entryGates(List.of(
                        GateCheck.builder()
                                .id("gate-brown-001")
                                .name("Breaking Change Detection")
                                .type(GateCheck.GateType.BREAKING_CHANGE_DETECTION)
                                .build()
                ))
                .requiresHumanApproval(true)
                .approvalReason("Impact analysis shows changes to public APIs")
                .build();
        tasks.add(impactAnalysis);

        // Stage 3: Design Enhancement
        TaskNode designEnhancement = TaskNode.builder()
                .id("brown-003")
                .name("Design Rate Limiting Strategy")
                .description("Design token bucket or sliding window rate limiter")
                .stage(WorkflowStage.ARCHITECTURE_DESIGN)
                .type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of("brown-002"))
                .build();
        tasks.add(designEnhancement);

        // Stage 4: Implementation
        TaskNode implRateLimiter = TaskNode.builder()
                .id("brown-004")
                .name("Implement Rate Limiter")
                .description("Add rate limiting logic and storage")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("brown-003"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-brown-002")
                                .name("Code Quality Check")
                                .type(GateCheck.GateType.CODE_QUALITY)
                                .build()
                ))
                .build();
        tasks.add(implRateLimiter);

        TaskNode implIntegration = TaskNode.builder()
                .id("brown-005")
                .name("Integrate with Existing APIs")
                .description("Add rate limiting interceptor to controllers")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("brown-004"))
                .build();
        tasks.add(implIntegration);

        // Stage 5: Regression Testing
        TaskNode testRegression = TaskNode.builder()
                .id("brown-006")
                .name("Run Regression Tests")
                .description("Ensure existing functionality still works")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("brown-005"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-brown-003")
                                .name("No Regression Detected")
                                .type(GateCheck.GateType.CUSTOM)
                                .build()
                ))
                .build();
        tasks.add(testRegression);

        TaskNode testNewFeature = TaskNode.builder()
                .id("brown-007")
                .name("Test Rate Limiting")
                .description("Test rate limiting behavior and edge cases")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("brown-005"))
                .build();
        tasks.add(testNewFeature);

        // Stage 6: Update Documentation
        TaskNode updateDocs = TaskNode.builder()
                .id("brown-008")
                .name("Update Documentation")
                .description("Document rate limiting behavior and configuration")
                .stage(WorkflowStage.DOCUMENTATION)
                .type(TaskNode.TaskType.DOCUMENTATION)
                .dependencies(List.of("brown-006", "brown-007"))
                .build();
        tasks.add(updateDocs);

        // Stage 7: Validation
        TaskNode validation = TaskNode.builder()
                .id("brown-009")
                .name("Validate Enhancement")
                .description("Validate all tests pass and no breaking changes")
                .stage(WorkflowStage.VALIDATION)
                .type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of("updateDocs"))
                .requiresHumanApproval(true)
                .approvalReason("Feature enhancement requires approval")
                .build();
        tasks.add(validation);

        return tasks;
    }

    @Override
    public WorkflowContext initializeContext() {
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("brownfield-" + UUID.randomUUID());
        context.setScenarioType("BROWNFIELD");
        context.setRequirement("Add rate limiting to prevent URL shortener abuse");
        context.getGlobalContext().put("project_type", "brownfield");
        context.getGlobalContext().put("existing_codebase", true);
        context.getGlobalContext().put("impacted_modules", Arrays.asList("UrlShortenerController", "UrlShortenerService"));
        return context;
    }
}
