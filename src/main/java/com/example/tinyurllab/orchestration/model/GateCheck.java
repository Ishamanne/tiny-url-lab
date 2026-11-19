package com.example.tinyurllab.orchestration.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Represents a quality gate or checkpoint that must pass before proceeding
 */
@Data
@Builder
public class GateCheck {
    private String id;
    private String name;
    private GateType type;
    private String condition;
    private boolean passed;
    private String failureReason;
    private Map<String, Object> metadata;

    public enum GateType {
        SECURITY_SCAN,
        CODE_QUALITY,
        TEST_COVERAGE,
        COMPLIANCE_CHECK,
        PERFORMANCE_THRESHOLD,
        DEPENDENCY_CHECK,
        BREAKING_CHANGE_DETECTION,
        CUSTOM
    }
}
