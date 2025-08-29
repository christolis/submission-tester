# Submission Tester

A Java application for automatically testing student submissions with performance constraints and comprehensive reporting.

## Features

- **Automatic Submission Discovery**: Scans directories to find student submissions
- **Compilation Testing**: Compiles Java source files and reports compilation errors
- **Performance Monitoring**: Tracks execution time and memory usage
- **Comprehensive Reporting**: Generates detailed reports with statistics and individual results
- **Timeout Protection**: Prevents infinite loops and excessive resource usage
- **Multi-threaded Processing**: Efficiently handles multiple submissions

## Requirements

- Java 21 or higher
- Gradle 8.14+ (included via wrapper)
- JUnit 5 for test execution

## Building the Project

```bash
# Using Gradle Wrapper
./gradlew clean build

# Create executable JAR
./gradlew shadowJar
```

This will create an executable JAR file in the `build/libs` directory.

## Usage

```bash
java -jar build/libs/submission-tester-1.0.0.jar <root-directory>
```

### Parameters

- `root-directory`: Path to the competition root directory (should contain submissions/, tests/, reports/ subdirectories)

### Competition Format

The submission tester is designed for programming competitions where:

1. **Each Java file is an individual submission**
2. **Header parsing**: Each submission must have a header comment in the format:
   ```java
   /* USER: username TASK: taskname */
   ```
3. **Single file per participant**: Each participant submits exactly one Java file

### Directory Structure

The competition root directory should have the following structure:

```
competition-root/
├── submissions/          # Participant submissions
│   ├── alice_solution.java
│   ├── bob_solution.java
│   └── charlie_solution.java
├── tests/               # Test files (.in/.out)
│   ├── bankacc.in
│   └── bankacc.out
├── reports/             # Generated reports (auto-created)
└── README.md            # Competition instructions
```

Each Java file must:
- Have a valid header comment with USER and TASK information
- Contain a public class with the same name as the file (without .java extension)
- Have a `main(String[] args)` method

### Example Submission

```java
/* USER: alice TASK: bankacc */
import java.util.*;
import java.io.*;

public class alice_solution {
    public static void main(String[] args) {
        // Solution implementation
        Scanner scanner = new Scanner(System.in);
        // ... rest of the code
    }
}
```

## Performance Constraints

The application enforces the following performance constraints:

- **Execution Time**: Maximum 1 second per test execution
- **Memory Usage**: Maximum 64MB per submission
- **Test Timeout**: 10 seconds for the entire test suite

## Output

The application generates:

1. **Console Output**: Real-time progress and summary
2. **Report File**: Detailed report saved in `reports/submission_report_YYYYMMDD_HHMMSS.txt`
3. **CSV Leaderboard**: Performance-based ranking saved in `reports/leaderboard_YYYYMMDD_HHMMSS.csv`

### Report Contents

- Summary statistics (total submissions, success/failure counts)
- Performance statistics (average execution time and memory usage)
- Detailed results for each submission
- Compilation and execution status

### CSV Leaderboard

The CSV leaderboard contains:
- **Rank**: Performance-based ranking (fastest first)
- **Username**: Participant's username
- **Task**: Task/challenge name
- **Execution Time**: Time in milliseconds
- **Memory Usage**: Memory usage in MB
- **Compilation Success**: Whether compilation was successful

Only submissions that pass all tests and meet performance constraints are included in the leaderboard.

## Architecture

The application follows a modular architecture with clear separation of concerns:

### Core Components

- **SubmissionTester**: Main orchestrator class
- **SubmissionDiscoveryService**: Discovers and validates submissions
- **CompilationService**: Handles Java compilation
- **TestExecutionService**: Executes tests with performance monitoring
- **ReportService**: Generates comprehensive reports

### Model Classes

- **Submission**: Represents a student submission with metadata
- **TestResult**: Enum for different test outcomes
- **TestExecutionResult**: Contains test results with performance metrics

## Configuration

The application can be configured by modifying the constants in the service classes:

- `TestExecutionService.TIMEOUT_SECONDS`: Test execution timeout
- `TestExecutionService.MEMORY_LIMIT_BYTES`: Memory usage limit
- `Submission.isSuccessful()`: Performance constraint thresholds

## Test Files

For input/output testing, place test files in the following locations:
- `tests/` directory
- `test-data/` directory
- `test_files/` directory
- Current directory (`.`)

Test files should follow the naming convention:
- Input files: `taskname.in` (e.g., `bankacc.in`)
- Expected output files: `taskname.out` (e.g., `bankacc.out`)

The application will automatically discover and use these test files for validation.

## Logging

The application uses SLF4J with Logback for logging. Log levels can be configured in `logback.xml` or through system properties.

## Extending the Application

### Adding New Test Types

1. Create a new test selector in `TestSelectorFactory`
2. Implement custom test discovery logic
3. Update the `TestExecutionService` to handle the new test type

### Custom Performance Constraints

1. Modify the `Submission.isSuccessful()` method
2. Update the `TestExecutionService` validation logic
3. Adjust the `ReportService` reporting format

### Custom Report Formats

1. Extend the `ReportService` class
2. Implement custom report generation methods
3. Add new report formats (HTML, JSON, etc.)

## Troubleshooting

### Common Issues

1. **"No Java compiler available"**: Ensure you're running with JDK, not JRE
2. **"Submissions directory does not exist"**: Check the directory path
3. **"No source file found"**: Verify Java files exist in student directories

### Debug Mode

Enable debug logging by setting the log level to DEBUG:

```bash
java -Dlogback.configurationFile=logback-debug.xml -jar submission-tester.jar ...
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
