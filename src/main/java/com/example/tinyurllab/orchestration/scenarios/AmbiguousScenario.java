package com.example.tinyurllab.orchestration.scenarios;

import com.example.tinyurllab.orchestration.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AMBIGUOUS SCENARIO: "Improve URL shortener performance"
 * Demonstrates handling vague requirements with clarification and decomposition
 */
@Component
public class AmbiguousScenario implements WorkflowScenario {

    @Override
    public String getScenarioName() {
        return "ambiguous";
    }

    @Override
    public String getDescription() {
        return "Ambiguous: Handle vague requirement 'improve performance' by identifying ambiguities and proposing solutions";
    }

    @Override
    public List<TaskNode> buildWorkflow() {
        List<TaskNode> tasks = new ArrayList<>();

        // Stage 1: Clarify Ambiguous Requirement
        TaskNode clarifyReq = TaskNode.builder()
                .id("amb-001")
                .name("Clarify Ambiguous Requirements")
                .description("Identify what 'performance' means: latency, throughput, or resource usage?")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        tasks.add(clarifyReq);

        TaskNode clarifyDecision = TaskNode.builder()
                .id("amb-clarify")
                .name("Review Requirement Ambiguities")
                .description("Review extracted topics and ambiguities and confirm scope")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.MANUAL_REVIEW)
                .dependencies(List.of("amb-001"))
                .requiresHumanApproval(true)
                .approvalReason("Confirm the analyzed requirement scope before profiling")
                .build();
        tasks.add(clarifyDecision);

        // Stage 2: Performance Profiling
        TaskNode profileSystem = TaskNode.builder()
                .id("amb-002")
                .name("Profile System Performance")
                .description("Identify bottlenecks: database queries, URL generation, analytics")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of("amb-clarify"))
                .build();
        tasks.add(profileSystem);

        // Stage 3: Propose Solutions
        TaskNode proposeSolutions = TaskNode.builder()
                .id("amb-003")
                .name("Propose Optimization Strategies")
                .description("Generate multiple solutions: caching, indexing, query optimization")
                .stage(WorkflowStage.ARCHITECTURE_DESIGN)
                .type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of("amb-002"))
                .requiresHumanApproval(true)
                .approvalReason("Multiple optimization approaches available - need human selection")
                .build();
        tasks.add(proposeSolutions);

        // Stage 4: Implementation (chosen strategy)
        TaskNode implCaching = TaskNode.builder()
                .id("amb-004")
                .name("Implement Redis Caching")
                .description("Add Redis cache for frequently accessed short URLs")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("amb-003"))
                .build();
        tasks.add(implCaching);

        TaskNode implIndexing = TaskNode.builder()
                .id("amb-005")
                .name("Add Database Indexes")
                .description("Add indexes on shortCode and accessedAt columns")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("amb-003"))
                .build();
        tasks.add(implIndexing);

        // Stage 5: Performance Testing
        TaskNode testPerformance = TaskNode.builder()
                .id("amb-006")
                .name("Run Performance Tests")
                .description("Load test and compare before/after metrics")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("amb-004", "amb-005"))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-amb-001")
                                .name("Performance Improvement Validation")
                                .type(GateCheck.GateType.PERFORMANCE_THRESHOLD)
                                .condition("Latency improved by >= 30%")
                                .build()
                ))
                .build();
        tasks.add(testPerformance);

        // Stage 6: Document Improvements
        TaskNode document = TaskNode.builder()
                .id("amb-007")
                .name("Document Performance Improvements")
                .description("Document baseline, changes, and results")
                .stage(WorkflowStage.DOCUMENTATION)
                .type(TaskNode.TaskType.DOCUMENTATION)
                .dependencies(List.of("amb-006"))
                .build();
        tasks.add(document);

        // Stage 7: Validation
        TaskNode validate = TaskNode.builder()
                .id("amb-008")
                .name("Validate Performance Goals Met")
                .description("Confirm performance targets achieved")
                .stage(WorkflowStage.VALIDATION)
                .type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of("document"))
                .build();
        tasks.add(validate);

        return tasks;
    }

    @Override
    public WorkflowContext initializeContext() {
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("ambiguous-" + UUID.randomUUID());
        context.setScenarioType("AMBIGUOUS");
        context.setRequirement("Improve URL shortener performance");
        context.getGlobalContext().put("project_type", "ambiguous");
        context.getGlobalContext().put("ambiguities", Arrays.asList(
                "What aspect of performance: latency, throughput, resource usage?",
                "What is the target improvement percentage?",
                "What is acceptable trade-off in complexity?"
        ));
        context.getGlobalContext().put("proposed_solutions", Arrays.asList(
                "Redis caching for hot URLs",
                "Database query optimization",
                "Database indexing improvements",
                "Connection pooling tuning"
        ));
        return context;
    }
}
