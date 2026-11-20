package com.example.tinyurllab.orchestration.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementAnalyzerTest {
    @Test
    void extractsTopicsAndFlagsVagueRequirements() {
        RequirementAnalyzer.Analysis analysis = RequirementAnalyzer.analyze(
                "  Improve performance and add click analytics  ");

        assertEquals("Improve performance and add click analytics", analysis.normalizedRequirement());
        assertTrue(analysis.detectedTopics().contains("performance"));
        assertTrue(analysis.detectedTopics().contains("click analytics"));
        assertTrue(analysis.candidateAcceptanceCriteria().size() >= 2);
        assertTrue(analysis.ambiguities().stream().anyMatch(value -> value.contains("improve")));
    }

    @Test
    void reportsUnsupportedRequirementsAsAmbiguous() {
        RequirementAnalyzer.Analysis analysis = RequirementAnalyzer.analyze("Add a new capability");

        assertTrue(analysis.detectedTopics().isEmpty());
        assertTrue(analysis.ambiguities().stream()
                .anyMatch(value -> value.contains("No supported URL-shortener capability")));
    }
}
