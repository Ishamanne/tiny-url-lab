package com.example.tinyurllab.orchestration.controller;

import com.example.tinyurllab.orchestration.engine.WorkflowOrchestrator;
import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;
import com.example.tinyurllab.orchestration.model.WorkflowMetrics;
import com.example.tinyurllab.orchestration.scenarios.AmbiguousScenario;
import com.example.tinyurllab.orchestration.scenarios.BrownfieldScenario;
import com.example.tinyurllab.orchestration.scenarios.GreenfieldScenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for triggering and monitoring agentic workflows
 */
@RestController
@RequestMapping("/api/orchestration")
@RequiredArgsConstructor
@Slf4j
public class OrchestrationController {

    private final WorkflowOrchestrator orchestrator;
    private final GreenfieldScenario greenfieldScenario;
    private final BrownfieldScenario brownfieldScenario;
    private final AmbiguousScenario ambiguousScenario;

    @PostMapping("/workflows/greenfield")
    public ResponseEntity<WorkflowContext> executeGreenfieldWorkflow() {
        log.info("Executing Greenfield scenario");
        WorkflowContext context = greenfieldScenario.initializeContext();
        List<TaskNode> tasks = greenfieldScenario.buildWorkflow();
        WorkflowContext result = orchestrator.executeWorkflow(context.getWorkflowId(), tasks, context);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workflows/brownfield")
    public ResponseEntity<WorkflowContext> executeBrownfieldWorkflow() {
        log.info("Executing Brownfield scenario");
        WorkflowContext context = brownfieldScenario.initializeContext();
        List<TaskNode> tasks = brownfieldScenario.buildWorkflow();
        WorkflowContext result = orchestrator.executeWorkflow(context.getWorkflowId(), tasks, context);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/workflows/ambiguous")
    public ResponseEntity<WorkflowContext> executeAmbiguousWorkflow() {
        log.info("Executing Ambiguous scenario");
        WorkflowContext context = ambiguousScenario.initializeContext();
        List<TaskNode> tasks = ambiguousScenario.buildWorkflow();
        WorkflowContext result = orchestrator.executeWorkflow(context.getWorkflowId(), tasks, context);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<WorkflowContext> getWorkflowStatus(@PathVariable String workflowId) {
        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(context);
    }

    @GetMapping("/workflows/{workflowId}/audit")
    public ResponseEntity<List<WorkflowContext.AuditEvent>> getAuditTrail(@PathVariable String workflowId) {
        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(context.getAuditTrail());
    }

    @GetMapping("/workflows/{workflowId}/decisions")
    public ResponseEntity<List<WorkflowContext.Decision>> getDecisionLineage(@PathVariable String workflowId) {
        WorkflowContext context = orchestrator.getWorkflowContext(workflowId);
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(context.getDecisionLineage());
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        WorkflowMetrics metrics = orchestrator.getGlobalMetrics();
        Map<String, Object> metricsMap = Map.of(
                "totalExecutions", metrics.getTotalExecutions().get(),
                "successfulExecutions", metrics.getSuccessfulExecutions().get(),
                "failedExecutions", metrics.getFailedExecutions().get(),
                "successRate", metrics.getSuccessRate(),
                "retriesPerformed", metrics.getRetryFrequency(),
                "rollbacksPerformed", metrics.getRollbacksPerformed().get(),
                "averageExecutionTimeMs", metrics.getAverageExecutionTimeMs(),
                "mttr", metrics.getMTTR()
        );
        return ResponseEntity.ok(metricsMap);
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<Map<String, String>>> listScenarios() {
        List<Map<String, String>> scenarios = List.of(
                Map.of("name", greenfieldScenario.getScenarioName(), "description", greenfieldScenario.getDescription()),
                Map.of("name", brownfieldScenario.getScenarioName(), "description", brownfieldScenario.getDescription()),
                Map.of("name", ambiguousScenario.getScenarioName(), "description", ambiguousScenario.getDescription())
        );
        return ResponseEntity.ok(scenarios);
    }
}
