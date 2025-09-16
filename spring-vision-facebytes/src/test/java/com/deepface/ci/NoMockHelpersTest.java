package com.deepface.ci;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CI test that ensures no mock helper methods or stub implementations remain in the codebase.
 * 
 * This test enforces the "fail-fast with clear guidance" principle by detecting any
 * remaining mock/stub code that could mask configuration issues in production.
 * 
 * The test scans the source code for patterns that indicate mock implementations
 * and fails the build if any are found, ensuring production readiness.
 */
public class NoMockHelpersTest {

    // Patterns that indicate mock/stub implementations
    private static final List<Pattern> FORBIDDEN_PATTERNS = Arrays.asList(
        // Mock method names
        Pattern.compile("\\bmock\\w*\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bstub\\w*\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bfake\\w*\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // Mock data generation
        Pattern.compile("\\bmockEmbedding\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmockAge\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmockGender\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmockEmotion\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmockRace\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bgenerateMock\\w*\\b", Pattern.CASE_INSENSITIVE),
        
        // Mock return statements
        Pattern.compile("\\breturn\\s+mock\\w*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\breturn\\s+stub\\w*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\breturn\\s+fake\\w*", Pattern.CASE_INSENSITIVE),
        
        // Suspicious patterns that might indicate fallback mocks
        Pattern.compile("//\\s*TODO[:\\s]*.*mock", Pattern.CASE_INSENSITIVE),
        Pattern.compile("//\\s*FIXME[:\\s]*.*mock", Pattern.CASE_INSENSITIVE),
        Pattern.compile("//\\s*mock.*fallback", Pattern.CASE_INSENSITIVE),
        Pattern.compile("//\\s*temporary.*mock", Pattern.CASE_INSENSITIVE)
    );
    
    // Files/directories to exclude from scanning
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "target/",
        ".git/",
        ".idea/",
        "test/",  // Allow mock patterns in test files
        "NoMockHelpersTest.java"  // Exclude this test file itself
    );
    
    // File extensions to scan
    private static final List<String> SCANNED_EXTENSIONS = Arrays.asList(
        ".java", ".kt", ".scala"
    );
    
    @Test
    public void testNoMockHelpersInProductionCode() throws IOException {
        Path sourceRoot = findSourceRoot();
        List<Path> javaFiles = findJavaFiles(sourceRoot);
        List<MockViolation> violations = new ArrayList<>();
        
        for (Path javaFile : javaFiles) {
            if (shouldSkipFile(javaFile)) {
                continue;
            }
            
            List<String> lines = Files.readAllLines(javaFile);
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                
                for (Pattern pattern : FORBIDDEN_PATTERNS) {
                    if (pattern.matcher(line).find()) {
                        violations.add(new MockViolation(
                            javaFile, 
                            lineNumber + 1, 
                            line.trim(), 
                            pattern.pattern()
                        ));
                    }
                }
            }
        }
        
        // Assert no violations found
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Found ").append(violations.size())
                   .append(" mock/stub implementations in production code:\n\n");
            
            for (MockViolation violation : violations) {
                message.append("File: ").append(violation.file.toString())
                       .append("\n")
                       .append("Line ").append(violation.lineNumber).append(": ")
                       .append(violation.line)
                       .append("\n")
                       .append("Pattern: ").append(violation.pattern)
                       .append("\n\n");
            }
            
            message.append("Mock implementations must be removed from production code.\n");
            message.append("Use fail-fast DeepFaceException with configuration guidance instead.\n");
            message.append("See README.md ONNX Model Configuration section for proper setup.\n");
            
            fail(message.toString());
        }
    }
    
    @Test
    public void testNoMockAnalyzersClass() throws IOException {
        Path sourceRoot = findSourceRoot();
        
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            boolean mockAnalyzersExists = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .anyMatch(path -> path.getFileName().toString().equals("MockAnalyzers.java"));
                
            assertFalse(mockAnalyzersExists, 
                       "MockAnalyzers.java class should not exist in production code. " +
                       "All mock implementations should be removed.");
        }
    }
    
    @Test
    public void testNoTodoMockComments() throws IOException {
        Path sourceRoot = findSourceRoot();
        List<Path> javaFiles = findJavaFiles(sourceRoot);
        List<MockViolation> todoMocks = new ArrayList<>();
        
        Pattern todoMockPattern = Pattern.compile(
            "//.*TODO.*mock|//.*FIXME.*mock|//.*HACK.*mock", 
            Pattern.CASE_INSENSITIVE
        );
        
        for (Path javaFile : javaFiles) {
            if (shouldSkipFile(javaFile)) {
                continue;
            }
            
            List<String> lines = Files.readAllLines(javaFile);
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                
                if (todoMockPattern.matcher(line).find()) {
                    todoMocks.add(new MockViolation(
                        javaFile, 
                        lineNumber + 1, 
                        line.trim(), 
                        "TODO/FIXME referencing mock"
                    ));
                }
            }
        }
        
        if (!todoMocks.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Found TODO/FIXME comments referencing mock implementations:\n\n");
            
            for (MockViolation todo : todoMocks) {
                message.append("File: ").append(todo.file.toString())
                       .append("\nLine ").append(todo.lineNumber).append(": ")
                       .append(todo.line)
                       .append("\n\n");
            }
            
            message.append("All TODO/FIXME comments about mock implementations should be resolved.\n");
            message.append("Replace with proper fail-fast error handling or remove if completed.\n");
            
            fail(message.toString());
        }
    }
    
    @Test
    public void testProductionCodeQualityMarkers() throws IOException {
        Path sourceRoot = findSourceRoot();
        List<Path> javaFiles = findJavaFiles(sourceRoot);
        List<String> qualityIssues = new ArrayList<>();
        
        // Patterns that indicate code quality issues
        Pattern stubPattern = Pattern.compile("\\bstub\\b.*implementation", Pattern.CASE_INSENSITIVE);
        Pattern placeholderPattern = Pattern.compile("\\bplaceholder\\b.*implementation", Pattern.CASE_INSENSITIVE);
        Pattern temporaryPattern = Pattern.compile("\\btemporary\\b.*implementation", Pattern.CASE_INSENSITIVE);
        
        for (Path javaFile : javaFiles) {
            if (shouldSkipFile(javaFile)) {
                continue;
            }
            
            List<String> lines = Files.readAllLines(javaFile);
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                
                if (stubPattern.matcher(line).find() || 
                    placeholderPattern.matcher(line).find() || 
                    temporaryPattern.matcher(line).find()) {
                    
                    qualityIssues.add(String.format("%s:%d - %s", 
                        javaFile.toString(), lineNumber + 1, line.trim()));
                }
            }
        }
        
        if (!qualityIssues.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Found code quality markers indicating incomplete implementations:\n\n");
            
            for (String issue : qualityIssues) {
                message.append(issue).append("\n");
            }
            
            message.append("\nAll stub/placeholder/temporary implementations should be completed ");
            message.append("or replaced with proper fail-fast error handling.\n");
            
            fail(message.toString());
        }
    }
    
    private Path findSourceRoot() {
        // Try to find the source root by looking for src/main/java
        Path currentDir = Paths.get(".").toAbsolutePath();
        
        while (currentDir != null) {
            Path srcMainJava = currentDir.resolve("src/main/java");
            if (Files.exists(srcMainJava) && Files.isDirectory(srcMainJava)) {
                return srcMainJava;
            }
            currentDir = currentDir.getParent();
        }
        
        // Fallback to current directory
        return Paths.get("src/main/java");
    }
    
    private List<Path> findJavaFiles(Path sourceRoot) throws IOException {
        List<Path> javaFiles = new ArrayList<>();
        
        if (!Files.exists(sourceRoot)) {
            return javaFiles; // Return empty list if source root doesn't exist
        }
        
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> hasScannedExtension(path))
                 .forEach(javaFiles::add);
        }
        
        return javaFiles;
    }
    
    private boolean hasScannedExtension(Path path) {
        String fileName = path.getFileName().toString();
        return SCANNED_EXTENSIONS.stream()
               .anyMatch(fileName::endsWith);
    }
    
    private boolean shouldSkipFile(Path file) {
        String filePath = file.toString();
        
        return EXCLUDED_PATHS.stream()
               .anyMatch(excluded -> filePath.contains(excluded));
    }
    
    /**
     * Represents a violation where mock code was found in production sources.
     */
    private static class MockViolation {
        final Path file;
        final int lineNumber;
        final String line;
        final String pattern;
        
        MockViolation(Path file, int lineNumber, String line, String pattern) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.line = line;
            this.pattern = pattern;
        }
    }
} 