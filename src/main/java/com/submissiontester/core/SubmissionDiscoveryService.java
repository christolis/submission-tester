package com.submissiontester.core;

import com.submissiontester.config.CompetitionConfig;
import com.submissiontester.model.Submission;
import com.submissiontester.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for discovering student submissions in a directory structure.
 * For the competition format, each Java file is treated as an individual submission.
 */
public class SubmissionDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionDiscoveryService.class);

    /**
     * Discovers all Java file submissions using the competition configuration.
     * Each Java file is treated as an individual submission.
     *
     * @param config the competition configuration
     * @return list of discovered submissions
     */
    public static List<Submission> discoverSubmissions(CompetitionConfig config) {
        return discoverSubmissions(config.submissionsDirectory());
    }

    /**
     * Discovers all Java file submissions in the specified directory.
     * Each Java file is treated as an individual submission.
     *
     * @param submissionsPath the root path containing submissions
     * @return list of discovered submissions
     */
    public static List<Submission> discoverSubmissions(Path submissionsPath) {
        List<Submission> submissions = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(submissionsPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".java"))
                 .filter(TestUtils::isValidJavaFile)
                 .forEach(path -> {
                     try {
                         Submission submission = new Submission(path);
                         submissions.add(submission);
                         logger.debug("Discovered submission: {} -> {}", submission.getUsername(), path);
                     } catch (IllegalArgumentException e) {
                         logger.warn("Skipping file with invalid header: {} - {}", path, e.getMessage());
                     }
                 });
        } catch (IOException e) {
            logger.error("Error discovering submissions", e);
            throw new RuntimeException("Failed to discover submissions", e);
        }

        return submissions;
    }

    /**
     * Discovers all submissions for a given task in the specified directory.
     * This method filters submissions by task name.
     *
     * @param submissionsPath the root path containing submissions
     * @param taskName the name of the task to look for
     * @return list of discovered submissions for the specified task
     */
    public static List<Submission> discoverSubmissions(Path submissionsPath, String taskName) {
        List<Submission> allSubmissions = discoverSubmissions(submissionsPath);
        
        return allSubmissions.stream()
            .filter(submission -> taskName.equals(submission.getTask()))
            .toList();
    }

    /**
     * Discovers all submissions for a given task using the competition configuration.
     *
     * @param config the competition configuration
     * @param taskName the name of the task to look for
     * @return list of discovered submissions for the specified task
     */
    public static List<Submission> discoverSubmissions(CompetitionConfig config, String taskName) {
        return discoverSubmissions(config.submissionsDirectory(), taskName);
    }
}
