package com.example.tinyurllab.orchestration.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Deterministic first-pass analysis of submitted requirements.
 * This makes extracted intent inspectable without claiming LLM-level interpretation.
 */
public final class RequirementAnalyzer {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Map<String, String> TOPICS = topics();
    private static final List<String> VAGUE_TERMS = List.of(
            "improve", "better", "fast", "quick", "user-friendly", "robust", "scalable", "optimize"
    );

    private RequirementAnalyzer() {}

    public static Analysis analyze(String requirement) {
        String normalized = WHITESPACE.matcher(requirement == null ? "" : requirement.trim())
                .replaceAll(" ");
        String lower = normalized.toLowerCase(Locale.ROOT);

        List<String> topics = TOPICS.entrySet().stream()
                .filter(entry -> lower.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();

        List<String> ambiguities = new ArrayList<>();
        for (String term : VAGUE_TERMS) {
            if (lower.contains(term)) {
                ambiguities.add("Define a measurable target or acceptance condition for '" + term + "'");
            }
        }
        if (topics.isEmpty()) {
            ambiguities.add("No supported URL-shortener capability was identified by the first-pass analyzer");
        }

        List<String> criteria = topics.stream()
                .map(topic -> "Address the identified capability: " + topic)
                .toList();
        if (criteria.isEmpty()) {
            criteria = List.of("Human review is needed to translate the requirement into testable acceptance criteria");
        }

        List<String> assumptions = ambiguities.isEmpty()
                ? List.of("No obvious vague terms were detected; human review should confirm these candidate criteria")
                : List.of("The first-pass analyzer cannot infer missing thresholds, scope, or operational constraints");

        return new Analysis(normalized, topics, criteria, ambiguities, assumptions);
    }

    private static Map<String, String> topics() {
        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("shorten", "URL creation and redirection");
        topics.put("short url", "URL creation and redirection");
        topics.put("redirect", "URL creation and redirection");
        topics.put("analytic", "click analytics");
        topics.put("click", "click analytics");
        topics.put("rate limit", "request rate limiting");
        topics.put("throttl", "request rate limiting");
        topics.put("expir", "URL expiration");
        topics.put("alias", "custom aliases");
        topics.put("performance", "performance");
        topics.put("latency", "performance");
        topics.put("secure", "security controls");
        topics.put("security", "security controls");
        return topics;
    }

    public record Analysis(
            String normalizedRequirement,
            List<String> detectedTopics,
            List<String> candidateAcceptanceCriteria,
            List<String> ambiguities,
            List<String> assumptions) {}
}
