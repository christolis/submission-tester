package com.submissiontester;

import com.submissiontester.model.Submission;
import com.submissiontester.model.TestResult;
import com.submissiontester.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for the Submission Tester application.
 */
public class SubmissionTesterTest {

    @Test
    public void testSubmissionCreation() {
        Path sourceFile = Path.of("test/Solution.java");
        Submission submission = new Submission("testuser", "testtask", sourceFile);
        
        assertEquals("testuser", submission.getUsername());
        assertEquals("testtask", submission.getTask());
        assertEquals(sourceFile, submission.getSourceFilePath());
        assertEquals(TestResult.NOT_EXECUTED, submission.getTestResult());
        assertFalse(submission.isCompilationSuccess());
    }

    @Test
    public void testSubmissionSuccessCriteria() {
        Path sourceFile = Path.of("test/Solution.java");
        Submission submission = new Submission("testuser", "testtask", sourceFile);
        
        // Initially should not be successful
        assertFalse(submission.isSuccessful());
        
        // Set all success criteria
        submission.setCompilationSuccess(true);
        submission.setTestResult(TestResult.PASSED);
        submission.setExecutionTimeNanos(500_000_000L); // 0.5 seconds
        submission.setMemoryUsageBytes(32 * 1024 * 1024L); // 32MB
        
        // Now should be successful
        assertTrue(submission.isSuccessful());
    }

    @Test
    public void testSubmissionFailureCriteria() {
        Path sourceFile = Path.of("test/Solution.java");
        Submission submission = new Submission("testuser", "testtask", sourceFile);
        
        submission.setCompilationSuccess(true);
        submission.setTestResult(TestResult.PASSED);
        
        // Test execution time limit
        submission.setExecutionTimeNanos(2_000_000_000L); // 2 seconds (exceeds 1 second limit)
        submission.setMemoryUsageBytes(32 * 1024 * 1024L);
        assertFalse(submission.isSuccessful());
        
        // Test memory limit
        submission.setExecutionTimeNanos(500_000_000L);
        submission.setMemoryUsageBytes(128 * 1024 * 1024L); // 128MB (exceeds 64MB limit)
        assertFalse(submission.isSuccessful());
    }

    @Test
    public void testTestUtilsFormatting() {
        assertEquals("500.00 ms", TestUtils.formatDuration(500_000_000L));
        assertEquals("32.00 MB", TestUtils.formatMemory(32 * 1024 * 1024L));
        assertEquals("1.00 KB", TestUtils.formatMemory(1024L));
    }

    @Test
    public void testTempDirectoryCreation(@TempDir Path tempDir) throws Exception {
        Path testDir = TestUtils.createTempDirectory("test_");
        assertTrue(Files.exists(testDir));
        assertTrue(Files.isDirectory(testDir));
        
        // Clean up
        TestUtils.deleteDirectory(testDir);
    }
}
