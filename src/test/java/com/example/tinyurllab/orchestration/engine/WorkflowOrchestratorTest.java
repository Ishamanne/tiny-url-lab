package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.model.*;
import com.example.tinyurllab.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WorkflowOrchestratorTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    private WorkflowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new WorkflowOrchestrator(urlShortenerService);
    }

    @Test
    void executeWorkflow_LinearDependencies_Success() {
        // Arrange
        List<TaskNode> tasks = new ArrayList<>();

        TaskNode task1 = TaskNode.builder()
                .id("task-1")
                .name("Task 1")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        tasks.add(task1);

        TaskNode task2 = TaskNode.builder()
                .id("task-2")
                .name("Task 2")
                .stage(WorkflowStage.ARCHITECTURE_DESIGN)
                .type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of("task-1"))
                .build();
        tasks.add(task2);

        TaskNode task3 = TaskNode.builder()
                .id("task-3")
                .name("Task 3")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("task-2"))
                .build();
        tasks.add(task3);

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-1");
        context.setRequirement("Test linear workflow");

        // Act
        WorkflowContext result = orchestrator.executeWorkflow("test-workflow-1", tasks, context);

        // Assert
        assertNotNull(result);
        assertEquals(WorkflowContext.WorkflowStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getAuditTrail().size() > 0);
    }

    @Test
    void executeWorkflow_ParallelTasks_Success() {
        // Arrange
        List<TaskNode> tasks = new ArrayList<>();

        TaskNode task1 = TaskNode.builder()
                .id("task-1")
                .name("Common Dependency")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        tasks.add(task1);

        TaskNode task2 = TaskNode.builder()
                .id("task-2")
                .name("Parallel Task 1")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("task-1"))
                .build();
        tasks.add(task2);

        TaskNode task3 = TaskNode.builder()
                .id("task-3")
                .name("Parallel Task 2")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of("task-1"))
                .build();
        tasks.add(task3);

        TaskNode task4 = TaskNode.builder()
                .id("task-4")
                .name("Merge Task")
                .stage(WorkflowStage.TESTING)
                .type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of("task-2", "task-3"))
                .build();
        tasks.add(task4);

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-2");
        context.setRequirement("Test parallel workflow");

        // Act
        WorkflowContext result = orchestrator.executeWorkflow("test-workflow-2", tasks, context);

        // Assert
        assertNotNull(result);
        assertEquals(WorkflowContext.WorkflowStatus.COMPLETED, result.getStatus());
    }

    @Test
    void executeWorkflow_SimpleExecution_Success() {
        // Arrange
        List<TaskNode> tasks = new ArrayList<>();

        TaskNode task1 = TaskNode.builder()
                .id("task-1")
                .name("Simple Task")
                .stage(WorkflowStage.ARCHITECTURE_DESIGN)
                .type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of())
                .build();
        tasks.add(task1);

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-3");
        context.setRequirement("Test simple workflow");

        // Act
        WorkflowContext result = orchestrator.executeWorkflow("test-workflow-3", tasks, context);

        // Assert
        assertNotNull(result);
        assertEquals(WorkflowContext.WorkflowStatus.COMPLETED, result.getStatus());
    }

    @Test
    void executeWorkflow_WithQualityGates_Success() {
        // Arrange
        List<TaskNode> tasks = new ArrayList<>();

        TaskNode task1 = TaskNode.builder()
                .id("task-1")
                .name("Task with Gates")
                .stage(WorkflowStage.IMPLEMENTATION)
                .type(TaskNode.TaskType.CODE_GENERATION)
                .dependencies(List.of())
                .entryGates(List.of(
                        GateCheck.builder()
                                .id("gate-1")
                                .name("Entry Gate")
                                .type(GateCheck.GateType.CUSTOM)
                                .build()
                ))
                .exitGates(List.of(
                        GateCheck.builder()
                                .id("gate-2")
                                .name("Exit Gate")
                                .type(GateCheck.GateType.CODE_QUALITY)
                                .build()
                ))
                .build();
        tasks.add(task1);

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-4");
        context.setRequirement("Test quality gates");

        // Act
        WorkflowContext result = orchestrator.executeWorkflow("test-workflow-4", tasks, context);

        // Assert
        assertNotNull(result);
        assertEquals(WorkflowContext.WorkflowStatus.COMPLETED, result.getStatus());
    }

    @Test
    void executeWorkflow_ContextPreservation_Success() {
        // Arrange
        List<TaskNode> tasks = new ArrayList<>();

        TaskNode task1 = TaskNode.builder()
                .id("task-1")
                .name("Task 1")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        tasks.add(task1);

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-5");
        context.setRequirement("Test context preservation");
        context.getGlobalContext().put("test-key", "test-value");

        // Act
        WorkflowContext result = orchestrator.executeWorkflow("test-workflow-5", tasks, context);

        // Assert
        assertNotNull(result);
        assertEquals("test-value", result.getGlobalContext().get("test-key"));
        assertFalse(result.getStageOutputs().isEmpty());
    }

    @Test
    void getMetrics_ReturnsGlobalMetrics() {
        // Act
        WorkflowMetrics metrics = orchestrator.getGlobalMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.getSuccessRate() >= 0.0);
        assertTrue(metrics.getAverageExecutionTimeMs() >= 0.0);
    }

    @Test
    void getWorkflowContext_ReturnsCorrectContext() {
        // Arrange
        List<TaskNode> tasks = List.of(
                TaskNode.builder()
                        .id("task-1")
                        .name("Task 1")
                        .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                        .type(TaskNode.TaskType.ANALYSIS)
                        .dependencies(List.of())
                        .build()
        );

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("test-workflow-6");
        orchestrator.executeWorkflow("test-workflow-6", tasks, context);

        // Act
        WorkflowContext retrieved = orchestrator.getWorkflowContext("test-workflow-6");

        // Assert
        assertNotNull(retrieved);
        assertEquals("test-workflow-6", retrieved.getWorkflowId());
    }

    // Removed: testReplanStrategy tests - replan strategies were removed to simplify workflow execution
}
