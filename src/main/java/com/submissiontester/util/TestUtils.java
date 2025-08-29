package com.submissiontester.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Utility class providing helper methods for testing and validation.
 */
public class TestUtils {

    /**
     * Validates if a file is a valid Java source file.
     *
     * @param filePath the path to the file to validate
     * @return true if the file is a valid Java source file, false otherwise
     */
    public static boolean isValidJavaFile(Path filePath) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return false;
        }

        String fileName = filePath.getFileName().toString();
        if (!fileName.endsWith(".java")) {
            return false;
        }

        try {
            String content = Files.readString(filePath);
            return isValidJavaContent(content);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validates if the content appears to be valid Java code.
     *
     * @param content the content to validate
     * @return true if the content appears to be valid Java, false otherwise
     */
    private static boolean isValidJavaContent(String content) {
        if (content != null && !content.trim().isEmpty()) {
            // Basic Java syntax checks
            boolean hasClass = Pattern.compile("\\bclass\\s+\\w+").matcher(content).find();
            boolean hasBrackets = content.contains("{") && content.contains("}");
            boolean hasSemicolons = content.contains(";");
            
            return hasClass && hasBrackets && hasSemicolons;
        }
        return false;
    }

    /**
     * Formats a duration in nanoseconds to a human-readable string.
     *
     * @param nanos duration in nanoseconds
     * @return formatted duration string
     */
    public static String formatDuration(long nanos) {
        if (nanos < 1000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f μs", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }

    /**
     * Formats memory usage in bytes to a human-readable string.
     *
     * @param bytes memory usage in bytes
     * @return formatted memory string
     */
    public static String formatMemory(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Creates a temporary directory with a specific prefix.
     *
     * @param prefix the prefix for the temporary directory
     * @return the path to the created temporary directory
     * @throws IOException if the directory cannot be created
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Safely deletes a directory and all its contents.
     *
     * @param directory the directory to delete
     * @throws IOException if the directory cannot be deleted
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Log but don't throw - some files might be locked
                        System.err.println("Could not delete: " + path);
                    }
                });
        }
    }
}
