# Competition Setup Guide

This guide explains how to set up and run a programming competition using the Submission Tester.

## Overview

The Submission Tester is designed for programming competitions where:
- Each participant submits exactly one Java file
- Submissions are tested against input/output files
- Performance constraints are enforced (1 second, 64MB)
- Results are ranked by execution time

## Setup Steps

### 1. Prepare the Competition Directory

Create the following directory structure:

```
competition/
├── submissions/          # Participant submissions
├── tests/               # Test files
├── reports/             # Generated reports (optional)
└── README.md            # Competition instructions
```

### 2. Create Test Files

For each challenge, create input and expected output files:

**Example: Banking Challenge (`tests/bankacc.in`)**
```
8
d 6 1000
q 4
d 4 500
q 4
w 4 750
w 6 200
q 6
q 4
```

**Expected Output (`tests/bankacc.out`)**
```
s
0
s
500
f
s
800
500
```

### 3. Participant Instructions

Provide participants with:

1. **Challenge description** (e.g., banking transactions problem)
2. **Submission format requirements**:
   - Single Java file
   - Required header: `/* USER: username TASK: taskname */`
   - Class name must match filename
   - Must have `main(String[] args)` method
   - Read from `System.in`, write to `System.out`

3. **Example submission**:
```java
/* USER: alice TASK: bankacc */
import java.util.*;
import java.io.*;

public class alice_solution {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Implementation here
    }
}
```

### 4. Running the Competition

1. **Build the tester**:
   ```bash
   ./gradlew clean build
   ```

2. **Run the competition**:
   ```bash
   ./gradlew run --args="/path/to/competition-root"
   ```
   
   Or using the JAR:
   ```bash
   java -jar build/libs/submission-tester-1.0.0.jar /path/to/competition-root
   ```

3. **Review results**:
   - Check console output for real-time progress
   - Review detailed report: `submission_report_YYYYMMDD_HHMMSS.txt`
   - Check leaderboard: `leaderboard_YYYYMMDD_HHMMSS.csv`

## Competition Workflow

### Before the Competition

1. **Design challenges** with clear input/output specifications
2. **Create comprehensive test cases** covering edge cases
3. **Test the submission format** with sample solutions
4. **Prepare participant instructions** and examples

### During the Competition

1. **Collect submissions** in the submissions directory
2. **Run the tester** to validate all submissions
3. **Monitor for issues** (compilation errors, timeouts, etc.)
4. **Generate leaderboard** for real-time results

### After the Competition

1. **Review detailed reports** for analysis
2. **Export results** to CSV for further processing
3. **Analyze performance** patterns and common issues
4. **Provide feedback** to participants

## Example Competition: Banking Challenge

### Challenge Description

Participants must implement a banking transaction system that:
- Handles deposits, withdrawals, and balance queries
- Validates transaction constraints
- Processes input in the specified format

### Test Cases

Create multiple test files to validate:
- Basic functionality
- Edge cases (large amounts, non-existent accounts)
- Performance under load
- Error handling

### Expected Results

The tester will:
1. Compile each submission
2. Run against all test cases
3. Measure execution time and memory usage
4. Generate performance-based leaderboard

## Troubleshooting

### Common Issues

1. **Compilation Errors**:
   - Check class name matches filename
   - Verify header format is correct
   - Ensure proper Java syntax

2. **Test Failures**:
   - Verify input/output format matches specification
   - Check for trailing whitespace in output
   - Ensure proper line endings

3. **Performance Issues**:
   - Monitor for infinite loops
   - Check memory usage patterns
   - Verify timeout settings

### Debug Mode

Enable detailed logging:
```bash
./gradlew run --args="/path/to/competition-root" -Dlogback.configurationFile=logback-debug.xml
```

## Customization

### Adding New Challenges

1. Create test files with `.in` and `.out` extensions
2. Update participant instructions
3. Test with sample solutions
4. Run full competition simulation

### Modifying Constraints

Edit the following constants:
- `Submission.isSuccessful()`: Performance thresholds
- `TestExecutionService.TIMEOUT_SECONDS`: Timeout limit
- `TestExecutionService.MEMORY_LIMIT_BYTES`: Memory limit

### Extending Functionality

The modular design allows for:
- Custom test frameworks
- Different programming languages
- Advanced scoring algorithms
- Integration with external systems

## Best Practices

1. **Test thoroughly** before running the competition
2. **Provide clear instructions** to participants
3. **Create comprehensive test cases** covering edge cases
4. **Monitor system resources** during execution
5. **Backup results** regularly
6. **Document any issues** for future improvements

## Support

For issues or questions:
1. Check the logs for detailed error messages
2. Verify file formats and permissions
3. Test with simple examples first
4. Review the source code for customization options
