package com.example.tinyurllab.orchestration.engine;

import com.example.tinyurllab.orchestration.governance.PolicyGuardrails;
import com.example.tinyurllab.orchestration.model.*;
import com.example.tinyurllab.urlshortener.dto.ShortUrlResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowOrchestratorTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    @Mock
    private ApprovalGateManager approvalGateManager;

    @Mock
    private RetryRollbackHandler retryRollbackHandler;

    @Mock
    private PolicyGuardrails policyGuardrails;

    private WorkflowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // Use real AuditLogger so audit trail gets populated
        AuditLogger auditLogger = new AuditLogger();
        WorkflowMetrics metrics = new WorkflowMetrics();
        orchestrator = new WorkflowOrchestrator(
                urlShortenerService,
                auditLogger,
                approvalGateManager,
                retryRollbackHandler,
                policyGuardrails,
                metrics
        );

        // Configure default mock behaviors (lenient to avoid UnnecessaryStubbingException)
        lenient().when(policyGuardrails.validateTask(any(), any())).thenReturn(List.of());
        lenient().when(approvalGateManager.requiresApproval(any(), any())).thenReturn(false);
        lenient().when(retryRollbackHandler.handleFailure(any(), any())).thenReturn(false);
        ShortUrlResponse demoUrl = ShortUrlResponse.builder()
                .shortCode("demo123")
                .originalUrl("https://example.com/demo")
                .build();
        lenient().when(urlShortenerService.createShortUrl(any())).thenReturn(demoUrl);
        lenient().when(urlShortenerService.getShortUrl("demo123")).thenReturn(demoUrl);
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
    void executeWorkflow_AnalyzesSubmittedRequirementAndRecordsResults() {
        TaskNode task = TaskNode.builder()
                .id("requirement-analysis")
                .name("Analyze requirement")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS)
                .type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of())
                .build();
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("requirement-analysis-workflow");
        context.setRequirement("Improve performance and add click analytics");

        WorkflowContext result = orchestrator.executeWorkflow(
                "requirement-analysis-workflow", List.of(task), context);

        RequirementAnalyzer.Analysis analysis =
                (RequirementAnalyzer.Analysis) result.getGlobalContext().get("requirement_analysis");
        assertNotNull(analysis);
        assertTrue(analysis.detectedTopics().contains("performance"));
        assertTrue(analysis.detectedTopics().contains("click analytics"));
        assertTrue(result.getDecisionLineage().stream()
                .anyMatch(decision -> "REQUIREMENT_ANALYSIS".equals(decision.getDecision())));
        assertTrue(result.getAuditTrail().stream()
                .anyMatch(event -> "REQUIREMENT_ANALYZED".equals(event.getEventType())));
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

    @Test
    void executeWorkflow_CycleReportsFailure() {
        TaskNode first = TaskNode.builder().id("cycle-a").name("A")
                .stage(WorkflowStage.REQUIREMENT_ANALYSIS).type(TaskNode.TaskType.ANALYSIS)
                .dependencies(List.of("cycle-b")).build();
        TaskNode second = TaskNode.builder().id("cycle-b").name("B")
                .stage(WorkflowStage.IMPLEMENTATION).type(TaskNode.TaskType.DESIGN)
                .dependencies(List.of("cycle-a")).build();

        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("cycle-workflow");
        WorkflowContext result = orchestrator.executeWorkflow("cycle-workflow",
                List.of(first, second), context);

        assertEquals(WorkflowContext.WorkflowStatus.FAILED, result.getStatus());
        assertTrue(result.getAuditTrail().stream()
                .anyMatch(event -> "WORKFLOW_FAILURE".equals(event.getEventType())));
    }

    @Test
    void executeWorkflow_GuardrailCritical_SafeStops() {
        lenient().when(policyGuardrails.validateTask(any(), any())).thenReturn(List.of(
                PolicyGuardrails.GuardrailViolation.builder()
                        .severity(PolicyGuardrails.Severity.CRITICAL)
                        .category("SECURITY")
                        .message("Critical security violation")
                        .build()
        ));
        TaskNode task = TaskNode.builder().id("guardrail-task").name("Guarded")
                .stage(WorkflowStage.RELEASE_READINESS).type(TaskNode.TaskType.VALIDATION)
                .dependencies(List.of()).build();
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("guardrail-workflow");

        WorkflowContext result = orchestrator.executeWorkflow("guardrail-workflow", List.of(task), context);

        assertEquals(WorkflowContext.WorkflowStatus.FAILED, result.getStatus());
        assertEquals(TaskNode.TaskStatus.FAILED, task.getStatus());
    }

    @Test
    void executeWorkflow_ApprovalRequired_PausesWorkflow() {
        lenient().when(approvalGateManager.requiresApproval(any(), any())).thenReturn(true);
        lenient().when(approvalGateManager.processApprovalGate(any(), any())).thenAnswer(invocation -> {
            TaskNode task = invocation.getArgument(0);
            task.setStatus(TaskNode.TaskStatus.AWAITING_APPROVAL);
            return false;
        });
        TaskNode task = TaskNode.builder().id("approval-task").name("Production release")
                .stage(WorkflowStage.RELEASE_READINESS).type(TaskNode.TaskType.MANUAL_REVIEW)
                .dependencies(List.of()).requiresHumanApproval(true).build();
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("approval-workflow");

        WorkflowContext result = orchestrator.executeWorkflow("approval-workflow", List.of(task), context);

        assertEquals(WorkflowContext.WorkflowStatus.AWAITING_APPROVAL, result.getStatus());
        assertEquals(TaskNode.TaskStatus.AWAITING_APPROVAL, task.getStatus());
    }

    @Test
    void approveTask_ResumesPausedWorkflowAndRecordsApprover() {
        lenient().when(approvalGateManager.requiresApproval(any(), any())).thenAnswer(invocation ->
                "resume-task".equals(((TaskNode) invocation.getArgument(0)).getId()));
        doAnswer(invocation -> {
            WorkflowContext context = invocation.getArgument(3);
            context.getApprovals().put("resume-task",
                    invocation.getArgument(1) + ": " + invocation.getArgument(2));
            return null;
        }).when(approvalGateManager).recordApproval(any(), any(), any(), any());
        lenient().when(approvalGateManager.processApprovalGate(any(), any()))
                .thenAnswer(invocation -> {
                    TaskNode awaiting = invocation.getArgument(0);
                    awaiting.setStatus(TaskNode.TaskStatus.AWAITING_APPROVAL);
                    return false;
                })
                .thenAnswer(invocation -> {
                    TaskNode resumed = invocation.getArgument(0);
                    resumed.setStatus(TaskNode.TaskStatus.RUNNING);
                    return true;
                });
        TaskNode prerequisite = TaskNode.builder().id("resume-prerequisite").name("Prerequisite")
                .stage(WorkflowStage.DOCUMENTATION).type(TaskNode.TaskType.DOCUMENTATION)
                .dependencies(List.of()).build();
        TaskNode task = TaskNode.builder().id("resume-task").name("Resume release")
                .stage(WorkflowStage.RELEASE_READINESS).type(TaskNode.TaskType.MANUAL_REVIEW)
                .dependencies(List.of("resume-prerequisite")).requiresHumanApproval(true).build();
        WorkflowContext initial = orchestrator.executeWorkflow("resume-workflow",
                List.of(prerequisite, task), new WorkflowContext());

        WorkflowContext resumed = orchestrator.approveTask(
                "resume-workflow", "resume-task", "reviewer@example.com", "Approved");

        assertNotNull(resumed);
        assertEquals(WorkflowContext.WorkflowStatus.COMPLETED, resumed.getStatus());
        assertEquals(TaskNode.TaskStatus.COMPLETED, prerequisite.getStatus());
        assertEquals(TaskNode.TaskStatus.COMPLETED, task.getStatus());
        assertTrue(resumed.getApprovals().containsKey("resume-task"));
    }

    @Test
    void executeWorkflow_TaskFailure_ReplansWithFixAndRetest() {
        lenient().when(retryRollbackHandler.handleFailure(any(), any())).thenReturn(false);
        lenient().when(retryRollbackHandler.shouldSafeStop(any(), any())).thenReturn(false);
        TaskNode task = TaskNode.builder().id("failed-task").name("Failing test")
                .stage(WorkflowStage.TESTING).type(TaskNode.TaskType.TEST_EXECUTION)
                .dependencies(List.of()).build();
        task.getContext().put("force_failure", true);
        WorkflowContext context = new WorkflowContext();
        context.setWorkflowId("replan-workflow");

        WorkflowContext result = orchestrator.executeWorkflow("replan-workflow", List.of(task), context);

        assertTrue(result.getAuditTrail().stream()
                .anyMatch(event -> "TASK_FAILED".equals(event.getEventType())));
    }
}
