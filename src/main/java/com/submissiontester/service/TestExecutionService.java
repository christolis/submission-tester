package com.submissiontester.service;

import com.submissiontester.config.CompetitionConfig;
import com.submissiontester.model.Submission;
import com.submissiontester.model.TestExecutionResult;
import com.submissiontester.model.TestResult;
import com.submissiontester.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Service for executing tests on compiled submissions with performance monitoring.
 * Supports input/output file testing for competition challenges.
 */
public class TestExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionService.class);
    
    private final CompetitionConfig config;

    /**
     * Creates a new TestExecutionService with the given configuration.
     *
     * @param config the competition configuration
     */
    public TestExecutionService(CompetitionConfig config) {
        this.config = config;
    }

    /**
     * Creates a new TestExecutionService with default configuration.
     */
    public TestExecutionService() {
        this.config = CompetitionConfig.of(Path.of("."));
    }

    /**
     * Executes tests for a submission with performance monitoring.
     *
     * @param submission the submission to test
     * @return the test execution result
     */
    public TestExecutionResult executeTests(Submission submission) {
        logger.debug("Executing tests for submission: {}", submission.getUsername());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<TestExecutionResult> future = executor.submit(() -> runTests(submission));

        try {
            return future.get(config.executionTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("Test execution timed out for submission: {}", submission.getUsername());
            future.cancel(true);
            return new TestExecutionResult(TestResult.TIMEOUT, 0, 0, "Test execution timed out");
        } catch (Exception e) {
            logger.error("Error during test execution for submission: {}", submission.getUsername(), e);
            return new TestExecutionResult(TestResult.RUNTIME_ERROR, 0, 0, e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Runs the actual tests for a submission.
     *
     * @param submission the submission to test
     * @return the test execution result
     */
    private TestExecutionResult runTests(Submission submission) {
        try {
            // Execute the input/output tests and get performance metrics
            TestExecutionResult result = executeInputOutputTests(submission);
            
            // Check memory usage (convert to KB for reporting)
            long memoryUsedKB = result.getMemoryUsageBytes() / 1024;
            if (memoryUsedKB > (config.memoryLimitBytes() / 1024)) {
                logger.warn("Memory limit exceeded for submission: {} ({} KB)", 
                           submission.getUsername(), memoryUsedKB);
                return new TestExecutionResult(TestResult.FAILED, 
                                             result.getExecutionTimeNanos(), 
                                             result.getMemoryUsageBytes(), 
                                             "Memory limit exceeded");
            }

            return result;

        } catch (Exception e) {
            logger.error("Exception during test execution for submission: {}", submission.getUsername(), e);
            return new TestExecutionResult(TestResult.RUNTIME_ERROR, 0, 0, e.getMessage());
        }
    }

    /**
     * Executes input/output file tests for the submission.
     * This method handles the banking challenge and similar competitions.
     *
     * @param submission the submission to test
     * @return the test execution result
     */
    private TestExecutionResult executeInputOutputTests(Submission submission) {
        try {
            // Get the task name to determine test files
            String taskName = submission.getTask();
            
            // Look for test input/output file pairs
            List<TestFilePair> testPairs = findTestFilePairs(taskName);
            
            if (testPairs.isEmpty()) {
                logger.warn("No test file pairs found for task: {}", taskName);
                return new TestExecutionResult(TestResult.FAILED, 0, 0, "No test file pairs found");
            }

            int passedTests = 0;
            int totalTests = testPairs.size();
            StringBuilder errorMessages = new StringBuilder();
            long totalExecutionTimeNanos = 0;
            long totalMemoryUsageBytes = 0;

            for (TestFilePair testPair : testPairs) {
                try {
                    TestExecutionResult testResult = executeSingleTest(submission, testPair);
                    if (testResult.getTestResult() == TestResult.PASSED) {
                        passedTests++;
                        totalExecutionTimeNanos += testResult.getExecutionTimeNanos();
                        totalMemoryUsageBytes = Math.max(totalMemoryUsageBytes, testResult.getMemoryUsageBytes());
                    } else {
                        errorMessages.append("Failed test: ").append(testPair.getInputFile().getFileName()).append("; ");
                    }
                } catch (Exception e) {
                    errorMessages.append("Error in test: ").append(testPair.getInputFile().getFileName()).append(": ").append(e.getMessage()).append("; ");
                }
            }

            if (passedTests == totalTests) {
                // Calculate average execution time and max memory usage
                long avgExecutionTimeNanos = totalExecutionTimeNanos / totalTests;
                return new TestExecutionResult(TestResult.PASSED, avgExecutionTimeNanos, totalMemoryUsageBytes, null);
            } else {
                return new TestExecutionResult(TestResult.FAILED, 0, 0, 
                    String.format("%d/%d tests passed. %s", passedTests, totalTests, errorMessages.toString()));
            }

        } catch (Exception e) {
            return new TestExecutionResult(TestResult.RUNTIME_ERROR, 0, 0, e.getMessage());
        }
    }

    /**
     * Finds test input/output file pairs for a given task.
     *
     * @param taskName the task name
     * @return list of test file pairs (input, expected output)
     */
    private List<TestFilePair> findTestFilePairs(String taskName) {
        List<TestFilePair> testPairs = new ArrayList<>();
        
        // Use configuration to find test files
        for (Path testLocation : config.getTestLocationPaths()) {
            if (Files.exists(testLocation)) {
                try (var stream = Files.list(testLocation)) {
                    // Find all .in files for this task
                    List<Path> inputFiles = stream.filter(path -> path.toString().endsWith(".in"))
                                                 .filter(path -> path.getFileName().toString().startsWith(taskName))
                                                 .toList();
                    
                    // For each input file, find its corresponding output file
                    for (Path inputFile : inputFiles) {
                        String baseName = inputFile.getFileName().toString().replace(".in", "");
                        Path outputFile = testLocation.resolve(baseName + ".out");
                        
                        if (Files.exists(outputFile)) {
                            testPairs.add(new TestFilePair(inputFile, outputFile));
                            logger.debug("Found test pair: {} -> {}", inputFile.getFileName(), outputFile.getFileName());
                        } else {
                            logger.warn("No corresponding output file found for: {}", inputFile.getFileName());
                        }
                    }
                } catch (IOException e) {
                    logger.debug("Error listing directory: {}", testLocation, e);
                }
            }
        }

        return testPairs;
    }
    
    /**
     * Represents a pair of test files (input and expected output).
     */
    private static class TestFilePair {
        private final Path inputFile;
        private final Path expectedOutputFile;
        
        public TestFilePair(Path inputFile, Path expectedOutputFile) {
            this.inputFile = inputFile;
            this.expectedOutputFile = expectedOutputFile;
        }
        
        public Path getInputFile() {
            return inputFile;
        }
        
        public Path getExpectedOutputFile() {
            return expectedOutputFile;
        }
    }

    /**
     * Executes a single test with input/output file comparison.
     *
     * @param submission the submission to test
     * @param testPair the test file pair (input and expected output)
     * @return TestExecutionResult with performance metrics
     */
    private TestExecutionResult executeSingleTest(Submission submission, TestFilePair testPair) {
        Instant startTime = Instant.now();
        long initialMemory = getCurrentMemoryUsage();
        
        try {
            // Create temporary output file for the program's output
            Path actualOutputFile = Files.createTempFile("actual_output_", ".out");
            
            // Get the compiled class file path
            Path compiledClassPath = submission.getCompiledClassPath();
            if (compiledClassPath == null || !Files.exists(compiledClassPath)) {
                logger.error("Compiled class not found for submission: {}", submission.getUsername());
                return new TestExecutionResult(TestResult.FAILED, 0, 0, "Compiled class not found");
            }
            
            // Get the class name from the file name (remove .java extension)
            String className = submission.getSourceFilePath().getFileName().toString().replace(".java", "");
            
            // Build the command to run the program
            ProcessBuilder processBuilder = new ProcessBuilder(
                "java", 
                "-cp", compiledClassPath.getParent().toString(),
                className
            );
            
            // Redirect input and output
            processBuilder.redirectInput(testPair.getInputFile().toFile());
            processBuilder.redirectOutput(actualOutputFile.toFile());
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Wait for completion with timeout
            boolean completed = process.waitFor(config.executionTimeoutSeconds(), TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                logger.warn("Test timed out for submission: {} with input: {}", 
                           submission.getUsername(), testPair.getInputFile().getFileName());
                return new TestExecutionResult(TestResult.TIMEOUT, 0, 0, "Test timed out");
            }
            
            // Check if the process exited successfully
            if (process.exitValue() != 0) {
                // Read error output for debugging
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                logger.warn("Program failed for submission: {} with input: {}. Exit code: {}. Error: {}", 
                           submission.getUsername(), testPair.getInputFile().getFileName(), 
                           process.exitValue(), errorOutput);
                return new TestExecutionResult(TestResult.RUNTIME_ERROR, 0, 0, "Program failed with exit code " + process.exitValue());
            }
            
            // Compare the actual output with expected output
            boolean result = compareFiles(actualOutputFile, testPair.getExpectedOutputFile());
            
            // Calculate performance metrics
            Duration executionTime = Duration.between(startTime, Instant.now());
            long finalMemory = getCurrentMemoryUsage();
            long memoryUsed = finalMemory - initialMemory;
            
            if (!result) {
                // Log the difference for debugging
                String actualContent = Files.readString(actualOutputFile).trim();
                String expectedContent = Files.readString(testPair.getExpectedOutputFile()).trim();
                logger.debug("Output mismatch for submission: {} with input: {}. Expected: '{}', Got: '{}'", 
                           submission.getUsername(), testPair.getInputFile().getFileName(), 
                           expectedContent, actualContent);
                
                // Clean up temporary file
                Files.deleteIfExists(actualOutputFile);
                return new TestExecutionResult(TestResult.FAILED, executionTime.toNanos(), memoryUsed, "Output mismatch");
            }
            
            // Clean up temporary file
            Files.deleteIfExists(actualOutputFile);
            
            return new TestExecutionResult(TestResult.PASSED, executionTime.toNanos(), memoryUsed, null);

        } catch (Exception e) {
            Duration executionTime = Duration.between(startTime, Instant.now());
            long finalMemory = getCurrentMemoryUsage();
            long memoryUsed = finalMemory - initialMemory;
            
            logger.error("Error executing test for submission: {} with input: {}", 
                        submission.getUsername(), testPair.getInputFile().getFileName(), e);
            return new TestExecutionResult(TestResult.RUNTIME_ERROR, executionTime.toNanos(), memoryUsed, e.getMessage());
        }
    }

    /**
     * Compares two files for equality.
     *
     * @param actual the actual output file
     * @param expected the expected output file
     * @return true if files are equal, false otherwise
     */
    private boolean compareFiles(Path actual, Path expected) {
        try {
            String actualContent = Files.readString(actual).replace("\r\n", "\n").trim();
            String expectedContent = Files.readString(expected).replace("\r\n", "\n").trim();
            return actualContent.equals(expectedContent);
        } catch (IOException e) {
            logger.error("Error comparing files", e);
            return false;
        }
    }

    /**
     * Gets the current memory usage in bytes.
     * Uses a more accurate method than just heap memory.
     *
     * @return current memory usage in bytes
     */
    private long getCurrentMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
