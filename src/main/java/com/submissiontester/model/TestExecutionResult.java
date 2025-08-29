package com.submissiontester.model;

/**
 * Represents the result of a test execution including performance metrics.
 */
public class TestExecutionResult {
    private final TestResult testResult;
    private final long executionTimeNanos;
    private final long memoryUsageBytes;
    private final String errorMessage;

    public TestExecutionResult(TestResult testResult, long executionTimeNanos, long memoryUsageBytes) {
        this(testResult, executionTimeNanos, memoryUsageBytes, null);
    }

    public TestExecutionResult(TestResult testResult, long executionTimeNanos, long memoryUsageBytes, String errorMessage) {
        this.testResult = testResult;
        this.executionTimeNanos = executionTimeNanos;
        this.memoryUsageBytes = memoryUsageBytes;
        this.errorMessage = errorMessage;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    @Override
    public String toString() {
        return String.format("TestExecutionResult{result=%s, time=%dns, memory=%d bytes, error='%s'}",
                testResult, executionTimeNanos, memoryUsageBytes, errorMessage);
    }
}
