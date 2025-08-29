package com.submissiontester.model;

/**
 * Represents the result of a test execution.
 */
public enum TestResult {
    NOT_EXECUTED("Not executed"),
    PASSED("Passed"),
    FAILED("Failed"),
    TIMEOUT("Timeout"),
    COMPILATION_ERROR("Compilation error"),
    RUNTIME_ERROR("Runtime error");

    private final String description;

    TestResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
