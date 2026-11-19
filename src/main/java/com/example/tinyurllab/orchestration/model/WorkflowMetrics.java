package com.example.tinyurllab.orchestration.model;

import lombok.Data;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks workflow reliability metrics: success rate, retries, MTTR, latency
 */
@Data
public class WorkflowMetrics {
    private final AtomicInteger totalExecutions = new AtomicInteger(0);
    private final AtomicInteger successfulExecutions = new AtomicInteger(0);
    private final AtomicInteger failedExecutions = new AtomicInteger(0);
    private final AtomicInteger retriesPerformed = new AtomicInteger(0);
    private final AtomicInteger rollbacksPerformed = new AtomicInteger(0);
    private final AtomicInteger replansPerformed = new AtomicInteger(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong totalRecoveryTimeMs = new AtomicLong(0);

    public void recordExecution(boolean success, Duration duration) {
        totalExecutions.incrementAndGet();
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
        totalExecutionTimeMs.addAndGet(duration.toMillis());
    }

    public void recordRetry() {
        retriesPerformed.incrementAndGet();
    }

    public void recordRollback() {
        rollbacksPerformed.incrementAndGet();
    }

    public void recordReplan() {
        replansPerformed.incrementAndGet();
    }

    public void recordRecoveryTime(Duration duration) {
        totalRecoveryTimeMs.addAndGet(duration.toMillis());
    }

    public double getSuccessRate() {
        int total = totalExecutions.get();
        return total == 0 ? 0.0 : (double) successfulExecutions.get() / total * 100;
    }

    public double getAverageExecutionTimeMs() {
        int total = totalExecutions.get();
        return total == 0 ? 0.0 : (double) totalExecutionTimeMs.get() / total;
    }

    public double getMTTR() {
        int failures = failedExecutions.get();
        return failures == 0 ? 0.0 : (double) totalRecoveryTimeMs.get() / failures;
    }

    public int getRetryFrequency() {
        return retriesPerformed.get();
    }

    public int getReplanCount() {
        return replansPerformed.get();
    }
}
