package com.submissiontester.service;

import com.submissiontester.config.CompetitionConfig;
import com.submissiontester.model.Submission;
import com.submissiontester.model.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating comprehensive test reports and CSV leaderboards.
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final CompetitionConfig config;

    /**
     * Creates a new ReportService with the given configuration.
     *
     * @param config the competition configuration
     */
    public ReportService(CompetitionConfig config) {
        this.config = config;
    }

    /**
     * Creates a new ReportService with default configuration.
     */
    public ReportService() {
        this.config = CompetitionConfig.of(Path.of("."));
    }

    /**
     * Generates a comprehensive report of all test results.
     *
     * @param submissions the list of tested submissions
     */
    public void generateReport(List<Submission> submissions) {
        if (submissions.isEmpty()) {
            logger.warn("No submissions to report on");
            return;
        }

        String reportContent = buildReportContent(submissions);
        Path reportPath = generateReportFile(reportContent);
        
        logger.info("Report generated successfully: {}", reportPath);
        System.out.println(reportContent);
        
        // Generate CSV leaderboard
        generateCSVLeaderboard(submissions);
    }

    /**
     * Generates a CSV leaderboard file with successful submissions ranked by performance.
     *
     * @param submissions the list of tested submissions
     */
    public void generateCSVLeaderboard(List<Submission> submissions) {
        // Filter successful submissions and sort by execution time
        List<Submission> successfulSubmissions = submissions.stream()
            .filter(Submission::isSuccessful)
            .sorted((s1, s2) -> Long.compare(s1.getExecutionTimeNanos(), s2.getExecutionTimeNanos()))
            .collect(Collectors.toList());

        if (successfulSubmissions.isEmpty()) {
            logger.warn("No successful submissions to include in leaderboard");
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        
        // CSV header
        csvContent.append("Rank,Username,Task,Execution Time (ns),Memory Usage (KB),Compilation Success\n");
        
        // CSV data
        for (int i = 0; i < successfulSubmissions.size(); i++) {
            Submission submission = successfulSubmissions.get(i);
            csvContent.append(String.format("%d,%s,%s,%d,%.2f,%s\n",
                i + 1,
                submission.getUsername(),
                submission.getTask(),
                submission.getExecutionTimeNanos(),
                submission.getMemoryUsageBytes() / 1024.0,
                submission.isCompilationSuccess() ? "YES" : "NO"
            ));
        }

        // Write CSV file
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path csvPath = config.reportsDirectory().resolve("leaderboard_" + timestamp + ".csv");
            Files.write(csvPath, csvContent.toString().getBytes());
            logger.info("CSV leaderboard generated successfully: {}", csvPath);
        } catch (IOException e) {
            logger.error("Failed to write CSV leaderboard file", e);
        }
    }

    /**
     * Builds the report content as a formatted string.
     *
     * @param submissions the list of submissions
     * @return the formatted report content
     */
    private String buildReportContent(List<Submission> submissions) {
        StringBuilder report = new StringBuilder();
        
        // Header
        report.append("=".repeat(80)).append("\n");
        report.append("SUBMISSION TEST REPORT\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("=".repeat(80)).append("\n\n");

        // Summary statistics
        Map<TestResult, Long> resultCounts = submissions.stream()
            .collect(Collectors.groupingBy(Submission::getTestResult, Collectors.counting()));

        report.append("SUMMARY STATISTICS\n");
        report.append("-".repeat(40)).append("\n");
        report.append("Total submissions: ").append(submissions.size()).append("\n");
        report.append("Successful: ").append(resultCounts.getOrDefault(TestResult.PASSED, 0L)).append("\n");
        report.append("Failed: ").append(resultCounts.getOrDefault(TestResult.FAILED, 0L)).append("\n");
        report.append("Compilation errors: ").append(resultCounts.getOrDefault(TestResult.COMPILATION_ERROR, 0L)).append("\n");
        report.append("Runtime errors: ").append(resultCounts.getOrDefault(TestResult.RUNTIME_ERROR, 0L)).append("\n");
        report.append("Timeouts: ").append(resultCounts.getOrDefault(TestResult.TIMEOUT, 0L)).append("\n");
        report.append("Not executed: ").append(resultCounts.getOrDefault(TestResult.NOT_EXECUTED, 0L)).append("\n\n");

        // Performance statistics
        List<Submission> successfulSubmissions = submissions.stream()
            .filter(s -> s.getTestResult() == TestResult.PASSED)
            .collect(Collectors.toList());

        if (!successfulSubmissions.isEmpty()) {
            double avgExecutionTime = successfulSubmissions.stream()
                .mapToLong(Submission::getExecutionTimeNanos)
                .average()
                .orElse(0.0);

            double avgMemoryUsage = successfulSubmissions.stream()
                .mapToLong(Submission::getMemoryUsageBytes)
                .average()
                .orElse(0.0);

            report.append("PERFORMANCE STATISTICS (Successful Submissions Only)\n");
            report.append("-".repeat(50)).append("\n");
            report.append("Average execution time: ").append(String.format("%.2f ms (%.0f ns)", avgExecutionTime / 1_000_000, avgExecutionTime));
            report.append("Average memory usage: ").append(String.format("%.2f", avgMemoryUsage / 1024)).append(" KB\n\n");
        }

        // Detailed results
        report.append("DETAILED RESULTS\n");
        report.append("-".repeat(40)).append("\n");
        
        for (Submission submission : submissions) {
            report.append(String.format("%-20s | %-15s | %-10s | %-8s | %-8s\n",
                submission.getUsername(),
                submission.getTestResult(),
                formatTime(submission.getExecutionTimeNanos()),
                formatMemory(submission.getMemoryUsageBytes()),
                submission.isCompilationSuccess() ? "YES" : "NO"
            ));
        }

        report.append("\n");
        report.append("LEGEND\n");
        report.append("-".repeat(20)).append("\n");
        report.append("Time: Execution time in nanoseconds (ns)\n");
        report.append("Memory: Memory usage in kilobytes (KB)\n");
        report.append("Compiled: Whether compilation was successful\n");

        return report.toString();
    }

    /**
     * Generates a report file and returns its path.
     *
     * @param reportContent the report content
     * @return the path to the generated report file
     */
    private Path generateReportFile(String reportContent) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path reportPath = config.reportsDirectory().resolve("submission_report_" + timestamp + ".txt");
            Files.write(reportPath, reportContent.getBytes());
            return reportPath;
        } catch (IOException e) {
            logger.error("Failed to write report file", e);
            throw new RuntimeException("Failed to write report file", e);
        }
    }

    /**
     * Formats execution time from nanoseconds to a readable string.
     *
     * @param nanos execution time in nanoseconds
     * @return formatted time string
     */
    private String formatTime(long nanos) {
        if (nanos == 0) return "N/A";
        return String.format("%.2f", nanos / 1_000_000.0);
    }

    /**
     * Formats memory usage from bytes to a readable string.
     *
     * @param bytes memory usage in bytes
     * @return formatted memory string
     */
    private String formatMemory(long bytes) {
        if (bytes == 0) return "N/A";
        return String.format("%.2f", bytes / 1024.0 / 1024.0);
    }
}
