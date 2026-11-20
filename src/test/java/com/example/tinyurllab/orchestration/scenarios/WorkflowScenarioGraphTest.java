package com.example.tinyurllab.orchestration.scenarios;

import com.example.tinyurllab.orchestration.model.TaskNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowScenarioGraphTest {
    @Test
    void everyScenarioDependencyReferencesAnExistingTask() {
        List<WorkflowScenario> scenarios = List.of(
                new GreenfieldScenario(),
                new BrownfieldScenario(),
                new AmbiguousScenario()
        );

        for (WorkflowScenario scenario : scenarios) {
            List<TaskNode> tasks = scenario.buildWorkflow();
            Set<String> taskIds = tasks.stream()
                    .map(TaskNode::getId)
                    .collect(Collectors.toSet());

            for (TaskNode task : tasks) {
                for (String dependency : task.getDependencies()) {
                    assertTrue(taskIds.contains(dependency),
                            () -> scenario.getScenarioName() + " task " + task.getId()
                                    + " references missing dependency " + dependency);
                }
            }
        }
    }
}
