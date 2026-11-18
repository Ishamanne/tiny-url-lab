package com.example.tinyurllab.orchestration.scenarios;

import com.example.tinyurllab.orchestration.model.TaskNode;
import com.example.tinyurllab.orchestration.model.WorkflowContext;

import java.util.List;

/**
 * Interface for different workflow scenario implementations
 */
public interface WorkflowScenario {
    String getScenarioName();
    String getDescription();
    List<TaskNode> buildWorkflow();
    WorkflowContext initializeContext();
}
