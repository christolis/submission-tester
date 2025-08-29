package com.submissiontester.service;

import com.submissiontester.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Service for compiling Java source files.
 */
public class CompilationService {
    private static final Logger logger = LoggerFactory.getLogger(CompilationService.class);

    /**
     * Compiles a submission's source file.
     *
     * @param submission the submission to compile
     * @return true if compilation was successful, false otherwise
     */
    public boolean compileSubmission(Submission submission) {
        Path sourceFile = submission.getSourceFilePath();
        Path outputDir = createOutputDirectory(submission);

        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                logger.error("No Java compiler available. Make sure to run with JDK, not JRE.");
                return false;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(
                Arrays.asList(sourceFile)
            );

            List<String> options = Arrays.asList(
                "-d", outputDir.toString(),
                "-cp", System.getProperty("java.class.path")
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                logger.warn("Compilation failed for {}: {}", 
                           submission.getUsername(), getCompilationErrors(diagnostics));
            } else {
                logger.debug("Compilation successful for: {}", submission.getUsername());
                // Set the compiled class path
                String className = sourceFile.getFileName().toString().replace(".java", "");
                Path compiledClassPath = outputDir.resolve(className + ".class");
                submission.setCompiledClassPath(compiledClassPath);
            }

            fileManager.close();
            return success;

        } catch (Exception e) {
            logger.error("Error during compilation for {}: {}", submission.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Creates an output directory for compiled classes.
     *
     * @param submission the submission
     * @return the output directory path
     */
    private Path createOutputDirectory(Submission submission) {
        try {
            Path outputDir = Files.createTempDirectory("compiled_" + submission.getUsername() + "_");
            logger.debug("Created output directory: {}", outputDir);
            return outputDir;
        } catch (IOException e) {
            logger.error("Failed to create output directory for {}", submission.getUsername(), e);
            throw new RuntimeException("Failed to create output directory", e);
        }
    }

    /**
     * Extracts compilation error messages from diagnostics.
     *
     * @param diagnostics the compilation diagnostics
     * @return formatted error messages
     */
    private String getCompilationErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder errors = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors.append(diagnostic.getMessage(Locale.getDefault())).append("; ");
            }
        }
        return errors.toString();
    }
}
