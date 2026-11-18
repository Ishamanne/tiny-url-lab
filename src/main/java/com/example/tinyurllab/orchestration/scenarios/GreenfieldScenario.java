package com.example.tinyurllab.orchestration.scenarios;

import com.example.tinyurllab.orchestration.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GREENFIELD SCENARIO: Build URL Shortener from scratch
 * Demonstrates full SDLC automation with new system development
 */
@Component
public class GreenfieldScenario implements WorkflowScenario {

    @Override
    public String getScenarioName() {
        return "Greenfield: URL Shortener System";
    }

    @Override
    public String getDescription() {
        return "Build a complete URL shortener service from scratch with core APIs, analytics, and reliability features";
    }

    @Override
    public List<TaskNode> buildWorkflow() {
        List<TaskNode> tasks = new ArrayList<>();

        // Stage 1: Requirement Analysis
        TaskNode reqAnalysis = TaskNode.builder()
                .id("req-001")
                .name("Analyze Requirements")
                .description("Parse and normalize URL shortener requirements")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .entryGates(List.of(
                        GateCheck.builder()
                                .id("gate-req-001")
                                .name("Requirements Completeness Check")
                                .type(GateCheck.GateType.CUSTOM)
                                .condition("Requirements must be clear and actionable")
                                .build()
                ))
                .build();
        tasks.add(reqAnalysis);

        // Stage 2: Architecture Design
        TaskNode archDesign = TaskNode.builder()
                .id("arch-001")
                .name("Design System Architecture")
                .description("Design URL shortener architecture with components and data flow")
                .stage(WorkflowStage.ARCHITECTURE_DESIGN)
                .type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of("req-001"))
                .requiresHumanApproval(true)
                .approvalReason("Critical architectural decisions require human review")
                .entryGates(List.of(
                        GateCheck.builder()
                                .id("gate-arch-001")
                                .name("Requirements Validated")
                                .type(GateCheck.GateType.CUSTOM)
                                .build()
                ))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-arch-002")
                                .name("Architecture Review")
                                .type(GateCheck.GateType.CUSTOM)
                                .condition("Architecture must be scalable and maintainable")
                                .build()
                ))
                .build();
        tasks.add(archDesign);

        // Stage 3: Task Decomposition
        TaskNode taskDecomp = TaskNode.builder()
                .id("decomp-001")
                .name("Decompose into Tasks")
                .description("Break down architecture into implementation tasks")
                .stage(WorkflowStage.TASK_DECOMPOSITION)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of("arch-001"))
                .build();
        tasks.add(taskDecomp);

        // Stage 4: Implementation (parallel tasks)
        TaskNode implModel = TaskNode.builder()
                .id("impl-001")
                .name("Implement Domain Model")
                .description("Create ShortUrl and Analytics entities")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("decomp-001"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-impl-001")
                                .name("Code Quality Check")
                                .type(GateCheck.GateType.CODE_QUALITY)
                                .build()
                ))
                .build();
        tasks.add(implModel);

        TaskNode implService = TaskNode.builder()
                .id("impl-002")
                .name("Implement URL Service")
                .description("Create URL shortening and resolution service")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("impl-001"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-impl-002")
                                .name("Security Scan")
                                .type(GateCheck.GateType.SECURITY_SCAN)
                                .build()
                ))
                .build();
        tasks.add(implService);

        TaskNode implApi = TaskNode.builder()
                .id("impl-003")
                .name("Implement REST APIs")
                .description("Create API endpoints with validation")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("impl-002"))
                .build();
        tasks.add(implApi);

        TaskNode implAnalytics = TaskNode.builder()
                .id("impl-004")
                .name("Implement Analytics")
                .description("Add click tracking and analytics features")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("impl-001"))
                .build();
        tasks.add(implAnalytics);

        // Stage 5: Testing (parallel unit and integration tests)
        TaskNode testUnit = TaskNode.builder()
                .id("test-001")
                .name("Run Unit Tests")
                .description("Execute unit tests for all components")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("impl-003", "impl-004"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-test-001")
                                .name("Test Coverage Check")
                                .type(GateCheck.GateType.TEST_COVERAGE)
                                .condition("Code coverage >= 80%")
                                .build()
                ))
                .build();
        tasks.add(testUnit);

        TaskNode testIntegration = TaskNode.builder()
                .id("test-002")
                .name("Run Integration Tests")
                .description("Execute end-to-end API tests")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("impl-003", "impl-004"))
                .build();
        tasks.add(testIntegration);

        // Stage 6: Documentation
        TaskNode docApi = TaskNode.builder()
                .id("doc-001")
                .name("Generate API Documentation")
                .description("Create OpenAPI/Swagger documentation")
                .stage(WorkflowStage.DOCUMENTATION)
                .type(TaskNode.TaskType.DOCUMENTATION)
                .dependencies(List.of("test-001"))
                .build();
        tasks.add(docApi);

        // Stage 7: Validation
        TaskNode validate = TaskNode.builder()
                .id("val-001")
                .name("Validate Release")
                .description("Run all quality gates and compliance checks")
                .stage(WorkflowStage.VALIDATION)
                .type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of("test-001", "test-002", "doc-001"))
                .entryGates(List.of(
                        GateCheck.builder()
                                .id("gate-val-001")
                                .name("All Tests Passed")
                                .type(GateCheck.GateType.CUSTOM)
                                .build(),
                        GateCheck.builder()
                                .id("gate-val-002")
                                .name("Security Compliance")
                                .type(GateCheck.GateType.SECURITY_SCAN)
                                .build(),
                        GateCheck.builder()
                                .id("gate-val-003")
                                .name("Performance Baseline")
                                .type(GateCheck.GateType.PERFORMANCE_THRESHOLD)
                                .build()
                ))
                .requiresHumanApproval(true)
                .approvalReason("Final release validation requires human sign-off")
                .build();
        tasks.add(validate);

        // Stage 8: Release Readiness
        TaskNode releaseReady = TaskNode.builder()
                .id("release-001")
                .name("Release Readiness Check")
                .description("Final checks before deployment")
                .stage(WorkflowStage.RELEASE_READINESS)
                .type(TaskNode.TaskType.CHECKPOINT)
                .dependencies(List.of("val-001"))
                .build();
        tasks.add(releaseReady);

        return tasks;
    }

    @Override
    public WorkflowContext initializeContext() {
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("greenfield-" + UUID.randomUUID());
        context.setScenarioType("GREENFIELD");
        context.setRequirement("Build a URL shortener service with core APIs, analytics, and reliability features");
        context.getGlobalContext().put("project_type", "greenfield");
        context.getGlobalContext().put("tech_stack", Arrays.asList("Spring Boot", "JPA", "H2"));
        return context;
    }
}
