package com.submissiontester.model;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a student submission with metadata and test results.
 * This class follows the Single Responsibility Principle by only managing submission data.
 */
public class Submission {
    private final String username;
    private final String task;
    private final Path sourceFilePath;
    private TestResult testResult;
    private long executionTimeNanos;
    private long memoryUsageBytes;
    private boolean compilationSuccess;
    private String headerComment;
    private Path compiledClassPath;

    // Regex pattern to extract USER and TASK from header comment
    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "/\\*\\s*USER:\\s*(\\w+)\\s+TASK:\\s*(\\w+)\\s*\\*/"
    );

    /**
     * Creates a new submission by parsing the source file and extracting header information.
     *
     * @param sourceFilePath the path to the source file
     * @throws IllegalArgumentException if the header is invalid or missing
     */
    public Submission(Path sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
        
        // Parse header to extract username and task
        HeaderInfo headerInfo = parseHeader(sourceFilePath);
        this.username = headerInfo.username;
        this.task = headerInfo.task;
        this.headerComment = headerInfo.headerComment;
        
        this.testResult = TestResult.NOT_EXECUTED;
        this.executionTimeNanos = 0;
        this.memoryUsageBytes = 0;
        this.compilationSuccess = false;
    }

    /**
     * Creates a new submission with the specified parameters (for backward compatibility).
     *
     * @param username     the username of the student
     * @param task         the task identifier
     * @param sourceFilePath the path to the source file
     */
    public Submission(String username, String task, Path sourceFilePath) {
        this.username = username;
        this.task = task;
        this.sourceFilePath = sourceFilePath;
        this.testResult = TestResult.NOT_EXECUTED;
        this.executionTimeNanos = 0;
        this.memoryUsageBytes = 0;
        this.compilationSuccess = false;
        this.headerComment = "";
    }

    /**
     * Parses the header comment from the source file to extract username and task.
     *
     * @param sourceFilePath the path to the source file
     * @return HeaderInfo containing username, task, and the full header comment
     * @throws IllegalArgumentException if the header is invalid or missing
     */
    private HeaderInfo parseHeader(Path sourceFilePath) {
        try {
            String content = java.nio.file.Files.readString(sourceFilePath);
            String[] lines = content.split("\n");
            
            // Look for header comment in the first few lines
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                String line = lines[i].trim();
                
                if (line.startsWith("/*") && line.contains("USER:") && line.contains("TASK:")) {
                    Matcher matcher = HEADER_PATTERN.matcher(line);
                    if (matcher.find()) {
                        return new HeaderInfo(
                            matcher.group(1).trim(),
                            matcher.group(2).trim(),
                            line
                        );
                    }
                }
            }
            
            throw new IllegalArgumentException("Invalid or missing header comment in " + sourceFilePath.getFileName());
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading source file: " + e.getMessage());
        }
    }

    /**
     * Gets the username of the student who made this submission.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the task identifier for this submission.
     *
     * @return the task identifier
     */
    public String getTask() {
        return task;
    }

    /**
     * Gets the path to the source file for this submission.
     *
     * @return the source file path
     */
    public Path getSourceFilePath() {
        return sourceFilePath;
    }

    /**
     * Gets the header comment from the source file.
     *
     * @return the header comment
     */
    public String getHeaderComment() {
        return headerComment;
    }

    /**
     * Gets the test result for this submission.
     *
     * @return the test result
     */
    public TestResult getTestResult() {
        return testResult;
    }

    /**
     * Sets the test result for this submission.
     *
     * @param testResult the test result to set
     */
    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    /**
     * Gets the execution time in nanoseconds.
     *
     * @return the execution time in nanoseconds
     */
    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    /**
     * Sets the execution time in nanoseconds.
     *
     * @param executionTimeNanos the execution time to set
     */
    public void setExecutionTimeNanos(long executionTimeNanos) {
        this.executionTimeNanos = executionTimeNanos;
    }

    /**
     * Gets the memory usage in bytes.
     *
     * @return the memory usage in bytes
     */
    public long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }

    /**
     * Sets the memory usage in bytes.
     *
     * @param memoryUsageBytes the memory usage to set
     */
    public void setMemoryUsageBytes(long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }

    /**
     * Checks if the compilation was successful.
     *
     * @return true if compilation was successful, false otherwise
     */
    public boolean isCompilationSuccess() {
        return compilationSuccess;
    }

    /**
     * Sets the compilation success status.
     *
     * @param compilationSuccess the compilation success status to set
     */
    public void setCompilationSuccess(boolean compilationSuccess) {
        this.compilationSuccess = compilationSuccess;
    }

    /**
     * Gets the path to the compiled class file.
     *
     * @return the compiled class file path, or null if not compiled
     */
    public Path getCompiledClassPath() {
        return compiledClassPath;
    }

    /**
     * Sets the path to the compiled class file.
     *
     * @param compiledClassPath the compiled class file path
     */
    public void setCompiledClassPath(Path compiledClassPath) {
        this.compiledClassPath = compiledClassPath;
    }

    /**
     * Checks if the submission passed all tests and met performance constraints.
     *
     * @return true if the submission is successful, false otherwise
     */
    public boolean isSuccessful() {
        return compilationSuccess && 
               testResult == TestResult.PASSED && 
               executionTimeNanos <= 1_000_000_000L && // 1 second in nanoseconds
               memoryUsageBytes <= 64 * 1024 * 1024L; // 64MB in bytes
    }

    @Override
    public String toString() {
        return String.format("Submission{username='%s', task='%s', result=%s, time=%dns, memory=%d bytes}",
                username, task, testResult, executionTimeNanos, memoryUsageBytes);
    }

    /**
     * Helper class to hold header information.
     */
    private static class HeaderInfo {
        final String username;
        final String task;
        final String headerComment;

        HeaderInfo(String username, String task, String headerComment) {
            this.username = username;
            this.task = task;
            this.headerComment = headerComment;
        }
    }
}
