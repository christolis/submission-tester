package com.submissiontester.core;

import com.submissiontester.config.CompetitionConfig;
import com.submissiontester.model.Submission;
import com.submissiontester.model.TestResult;
import com.submissiontester.model.TestExecutionResult;
import com.submissiontester.service.CompilationService;
import com.submissiontester.service.TestExecutionService;
import com.submissiontester.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main class for testing student submissions with performance constraints.
 * This class orchestrates the compilation, testing, and reporting process.
 */
public class SubmissionTester {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionTester.class);
    
    private final CompilationService compilationService;
    private final TestExecutionService testExecutionService;
    private final ReportService reportService;
    private final ExecutorService executorService;
    private final CompetitionConfig config;

    /**
     * Creates a new SubmissionTester with the given configuration.
     *
     * @param config the competition configuration
     */
    public SubmissionTester(CompetitionConfig config) {
        this.config = config;
        this.compilationService = new CompilationService();
        this.testExecutionService = new TestExecutionService(config);
        this.reportService = new ReportService(config);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a new SubmissionTester with default configuration.
     */
    public SubmissionTester() {
        this(CompetitionConfig.of(Path.of(".")));
    }

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments: [root-directory]
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar submission-tester.jar <root-directory>");
            System.err.println("  root-directory: Path to the competition root directory");
            System.err.println("                  (should contain submissions/, tests/, reports/ subdirectories)");
            System.exit(1);
        }

        String rootDir = args[0];
        Path rootPath = Paths.get(rootDir);

        try {
            // Create and validate configuration
            CompetitionConfig config = CompetitionConfig.of(rootPath);
            config.validate();
            
            logger.info("Configuration loaded successfully:");
            logger.info(config.toString());

            SubmissionTester tester = new SubmissionTester(config);
            List<Submission> results = tester.testSubmissions();
            tester.generateReport(results);
            
        } catch (Exception e) {
            logger.error("Error during submission testing", e);
            System.exit(1);
        }
    }

    /**
     * Tests all submissions using the configuration.
     * Each Java file is treated as an individual submission.
     *
     * @return list of tested submissions with results
     */
    public List<Submission> testSubmissions() {
        logger.info("Starting submission testing");
        logger.info("Root directory: {}", config.rootDirectory());
        logger.info("Submissions directory: {}", config.submissionsDirectory());

        // Discover all submissions (each Java file is a submission)
        List<Submission> submissions = SubmissionDiscoveryService.discoverSubmissions(config);
        logger.info("Discovered {} submissions to test", submissions.size());

        // Process each submission
        for (Submission submission : submissions) {
            processSubmission(submission);
        }

        logger.info("Completed testing {} submissions", submissions.size());
        return submissions;
    }

    /**
     * Tests all submissions in the specified directory.
     * Each Java file is treated as an individual submission.
     * This method is provided for backward compatibility.
     *
     * @param submissionsDirectory the directory containing student submissions
     * @return list of tested submissions with results
     */
    public List<Submission> testSubmissions(String submissionsDirectory) {
        logger.info("Starting submission testing");
        logger.info("Submissions directory: {}", submissionsDirectory);

        Path submissionsPath = Paths.get(submissionsDirectory);
        if (!submissionsPath.toFile().exists()) {
            throw new IllegalArgumentException("Submissions directory does not exist: " + submissionsDirectory);
        }

        // Discover all submissions (each Java file is a submission)
        List<Submission> submissions = SubmissionDiscoveryService.discoverSubmissions(submissionsPath);
        logger.info("Discovered {} submissions to test", submissions.size());

        // Process each submission
        for (Submission submission : submissions) {
            processSubmission(submission);
        }

        logger.info("Completed testing {} submissions", submissions.size());
        return submissions;
    }

    /**
     * Tests all submissions for a specific task in the specified directory.
     * This method is provided for backward compatibility.
     *
     * @param submissionsDirectory the directory containing student submissions
     * @param taskName the name of the task to test
     * @return list of tested submissions with results
     */
    public List<Submission> testSubmissions(String submissionsDirectory, String taskName) {
        logger.info("Starting submission testing for task: {}", taskName);
        logger.info("Submissions directory: {}", submissionsDirectory);

        Path submissionsPath = Paths.get(submissionsDirectory);
        if (!submissionsPath.toFile().exists()) {
            throw new IllegalArgumentException("Submissions directory does not exist: " + submissionsDirectory);
        }

        // Discover submissions for the specific task
        List<Submission> submissions = SubmissionDiscoveryService.discoverSubmissions(submissionsPath, taskName);
        logger.info("Discovered {} submissions to test for task: {}", submissions.size(), taskName);

        // Process each submission
        for (Submission submission : submissions) {
            processSubmission(submission);
        }

        logger.info("Completed testing {} submissions for task: {}", submissions.size(), taskName);
        return submissions;
    }

    /**
     * Processes a single submission through compilation and testing.
     *
     * @param submission the submission to process
     */
    private void processSubmission(Submission submission) {
        logger.info("Processing submission: {} (Task: {})", submission.getUsername(), submission.getTask());

        try {
            // Step 1: Compile the submission
            boolean compilationSuccess = compilationService.compileSubmission(submission);
            submission.setCompilationSuccess(compilationSuccess);

            if (!compilationSuccess) {
                submission.setTestResult(TestResult.COMPILATION_ERROR);
                logger.warn("Compilation failed for submission: {}", submission.getUsername());
                return;
            }

            // Step 2: Execute tests with performance monitoring
            TestExecutionResult executionResult = testExecutionService.executeTests(submission);
            
            submission.setTestResult(executionResult.getTestResult());
            submission.setExecutionTimeNanos(executionResult.getExecutionTimeNanos());
            submission.setMemoryUsageBytes(executionResult.getMemoryUsageBytes());

            logger.info("Submission {} completed with result: {}", 
                       submission.getUsername(), submission.getTestResult());

        } catch (Exception e) {
            logger.error("Error processing submission: {}", submission.getUsername(), e);
            submission.setTestResult(TestResult.RUNTIME_ERROR);
        }
    }

    /**
     * Generates a comprehensive report of all test results.
     *
     * @param submissions the list of tested submissions
     */
    public void generateReport(List<Submission> submissions) {
        logger.info("Generating test report...");
        reportService.generateReport(submissions);
        logger.info("Report generated successfully");
    }

    /**
     * Shuts down the executor service and releases resources.
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
