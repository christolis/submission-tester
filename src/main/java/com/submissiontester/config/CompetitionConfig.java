package com.submissiontester.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Configuration class for the competition settings.
 */
public record CompetitionConfig(
    Path rootDirectory,
    Path submissionsDirectory,
    Path testsDirectory,
    Path reportsDirectory,
    long executionTimeoutSeconds,
    long memoryLimitBytes,
    List<String> testLocations
) {
    
    /**
     * Creates a new competition configuration with default settings.
     *
     * @param rootDirectory the root directory for the competition
     * @return the competition configuration
     */
    public static CompetitionConfig of(Path rootDirectory) {
        Objects.requireNonNull(rootDirectory, "Root directory cannot be null");
        
        return new CompetitionConfig(
            rootDirectory,
            rootDirectory.resolve("submissions"),
            rootDirectory.resolve("tests"),
            rootDirectory.resolve("reports"),
            10L, // 10 seconds timeout
            64L * 1024L * 1024L, // 64MB memory limit
            List.of("tests", "test-data", "test_files", ".")
        );
    }
    
    /**
     * Creates a new competition configuration with custom settings.
     *
     * @param rootDirectory the root directory for the competition
     * @param executionTimeoutSeconds custom execution timeout
     * @param memoryLimitBytes custom memory limit
     * @return the competition configuration
     */
    public static CompetitionConfig of(Path rootDirectory, long executionTimeoutSeconds, long memoryLimitBytes) {
        Objects.requireNonNull(rootDirectory, "Root directory cannot be null");
        
        return new CompetitionConfig(
            rootDirectory,
            rootDirectory.resolve("submissions"),
            rootDirectory.resolve("tests"),
            rootDirectory.resolve("reports"),
            executionTimeoutSeconds,
            memoryLimitBytes,
            List.of("tests", "test-data", "test_files", ".")
        );
    }
    
    /**
     * Creates a new competition configuration with custom directories.
     *
     * @param rootDirectory the root directory for the competition
     * @param submissionsDirectory custom submissions directory
     * @param testsDirectory custom tests directory
     * @param reportsDirectory custom reports directory
     * @return the competition configuration
     */
    public static CompetitionConfig of(Path rootDirectory, Path submissionsDirectory, Path testsDirectory, Path reportsDirectory) {
        Objects.requireNonNull(rootDirectory, "Root directory cannot be null");
        Objects.requireNonNull(submissionsDirectory, "Submissions directory cannot be null");
        Objects.requireNonNull(testsDirectory, "Tests directory cannot be null");
        Objects.requireNonNull(reportsDirectory, "Reports directory cannot be null");
        
        return new CompetitionConfig(
            rootDirectory,
            submissionsDirectory,
            testsDirectory,
            reportsDirectory,
            10L,
            64L * 1024L * 1024L,
            List.of("tests", "test-data", "test_files", ".")
        );
    }
    
    /**
     * Validates the configuration and creates necessary directories.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public void validate() {
        if (!rootDirectory.toFile().exists()) {
            throw new IllegalArgumentException("Root directory does not exist: " + rootDirectory);
        }
        
        if (!rootDirectory.toFile().isDirectory()) {
            throw new IllegalArgumentException("Root directory is not a directory: " + rootDirectory);
        }
        
        // Create directories if they don't exist
        createDirectoryIfNotExists(submissionsDirectory);
        createDirectoryIfNotExists(testsDirectory);
        createDirectoryIfNotExists(reportsDirectory);
    }
    
    /**
     * Creates a directory if it doesn't exist.
     *
     * @param directory the directory to create
     */
    private void createDirectoryIfNotExists(Path directory) {
        try {
            if (!directory.toFile().exists()) {
                directory.toFile().mkdirs();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create directory: " + directory, e);
        }
    }
    
    /**
     * Gets the test locations as Path objects relative to the root directory.
     *
     * @return list of test location paths
     */
    public List<Path> getTestLocationPaths() {
        return testLocations.stream()
            .map(location -> {
                if (".".equals(location)) {
                    return rootDirectory;
                } else {
                    return rootDirectory.resolve(location);
                }
            })
            .toList();
    }
    
    /**
     * Gets a formatted string representation of the configuration.
     *
     * @return formatted configuration string
     */
    @Override
    public String toString() {
        return String.format(
            "Competition Configuration:\n" +
            "Root Directory: %s\n" +
            "Submissions Directory: %s\n" +
            "Tests Directory: %s\n" +
            "Reports Directory: %s\n" +
            "Execution Timeout: %d seconds\n" +
            "Memory Limit: %d bytes (%.2f MB)\n" +
            "Test Locations: %s",
            rootDirectory,
            submissionsDirectory,
            testsDirectory,
            reportsDirectory,
            executionTimeoutSeconds,
            memoryLimitBytes,
            memoryLimitBytes / (1024.0 * 1024.0),
            String.join(", ", testLocations)
        );
    }
}
