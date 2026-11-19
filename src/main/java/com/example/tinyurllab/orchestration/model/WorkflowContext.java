package com.example.tinyurllab.orchestration.model;

import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains cross-stage context, decision lineage, and execution state
 */
@Data
public class WorkflowContext {
    private String workflowId;
    private String scenarioType; // GREENFIELD, BROWNFIELD, AMBIGUOUS
    private String requirement;

    private Map<String, Object> globalContext = new ConcurrentHashMap<>();
    private Map<String, Object> stageOutputs = new ConcurrentHashMap<>();
    private List<Decision> decisionLineage = Collections.synchronizedList(new ArrayList<>());
    private List<AuditEvent> auditTrail = Collections.synchronizedList(new ArrayList<>());

    private Instant startTime;
    private Instant endTime;
    private WorkflowStatus status = WorkflowStatus.INITIALIZED;

    private Map<String, String> approvals = new ConcurrentHashMap<>();
    private Map<String, Integer> retryCounters = new ConcurrentHashMap<>();

    public enum WorkflowStatus {
        INITIALIZED, PLANNING, EXECUTING, PAUSED,
        AWAITING_APPROVAL, COMPLETED, FAILED, ROLLED_BACK
    }

    @Data
    public static class Decision {
        private String taskId;
        private String decision;
        private String rationale;
        private Map<String, Object> context;
        private Instant timestamp;
    }

    @Data
    public static class AuditEvent {
        private String eventId;
        private String taskId;
        private String eventType;
        private String description;
        private Map<String, Object> metadata;
        private Instant timestamp;
        private String actor; // AGENT or HUMAN
    }

    public void addDecision(String taskId, String decision, String rationale, Map<String, Object> context) {
        Decision d = new Decision();
        d.setTaskId(taskId);
        d.setDecision(decision);
        d.setRationale(rationale);
        d.setContext(context);
        d.setTimestamp(Instant.now());
        decisionLineage.add(d);
    }

    public void addAuditEvent(String taskId, String eventType, String description, Map<String, Object> metadata, String actor) {
        AuditEvent event = new AuditEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTaskId(taskId);
        event.setEventType(eventType);
        event.setDescription(description);
        event.setMetadata(metadata);
        event.setTimestamp(Instant.now());
        event.setActor(actor);
        auditTrail.add(event);
    }

    public int incrementRetry(String taskId) {
        return retryCounters.merge(taskId, 1, Integer::sum);
    }
}
