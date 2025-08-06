# Cursor IDE Rules for Spring-Vision

This document defines the coding conventions, standards, and best practices for the Spring-Vision project. All contributors must follow these rules to maintain code quality, consistency, and long-term maintainability.

## 🎯 Core Principles

### 1. Security-First Approach

- **OWASP Top-10 Compliance**: All code must address common security vulnerabilities
- **Input Validation**: Validate and sanitize all inputs, especially image data
- **Error Handling**: Never expose sensitive information in error messages
- **Least Privilege**: Use minimal required permissions for all operations
- **Dependency Security**: Pin dependencies and regularly update for security patches

### 2. Spring Boot Best Practices

- **Autoconfiguration**: Follow Spring Boot's autoconfiguration patterns
- **Conditional Beans**: Use `@ConditionalOn*` annotations appropriately
- **Configuration Properties**: Use `@ConfigurationProperties` for externalized configuration
- **Actuator Integration**: Expose health checks and metrics via Spring Boot Actuator
- **Starter Pattern**: Follow the Spring Boot starter pattern for easy integration

### 3. Modern Java Standards (Java 21+)

- **Records**: Use records for immutable data transfer objects
- **Pattern Matching**: Leverage pattern matching for switch expressions and instanceof
- **Virtual Threads**: Use virtual threads for I/O-bound operations
- **Text Blocks**: Use text blocks for multi-line strings
- **Sealed Classes**: Use sealed classes for type hierarchies where appropriate

## 📝 Code Style & Formatting

### Java Code Style

```java
// ✅ Good: Clear, descriptive naming
public record VisionResult(
    DetectionType type,
    double confidence,
    BoundingBox boundingBox,
    Map<String, Object> metadata
) {
    // Implementation
}

// ❌ Bad: Unclear naming
public record VR(String t, double c, BB bb, Map<String, Object> m) {
    // Implementation
}
```

### Package Structure

```
com.springvision
├── core/           # Core domain classes and interfaces
├── autoconfigure/  # Spring Boot autoconfiguration
├── starter/        # Spring Boot starter
├── template/       # Template classes (VisionTemplate)
├── model/          # Data models and records
├── exception/      # Custom exceptions
├── spi/           # Service Provider Interfaces
└── util/          # Utility classes
```

### Naming Conventions

- **Classes**: PascalCase (e.g., `VisionTemplate`, `OpenCvBackend`)
- **Methods**: camelCase (e.g., `detectFaces`, `processImage`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_CONFIDENCE_THRESHOLD`)
- **Packages**: lowercase (e.g., `com.springvision.core`)
- **Files**: Match class names exactly

## 🔧 Documentation Standards

### Javadoc Requirements

Every public class, interface, and method must have comprehensive Javadoc:

```java
/**
 * Template for computer vision operations providing a unified interface
 * across different vision backends (OpenCV, MediaPipe, YOLO, etc.).
 * 
 * <p>This template follows the Spring template pattern, providing a
 * consistent API regardless of the underlying vision backend implementation.
 * It handles common concerns like error handling, metrics collection, and
 * result transformation.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * @Autowired
 * private VisionTemplate visionTemplate;
 * 
 * public void detectFaces(byte[] imageData) {
 *     VisionResult result = visionTemplate.detectFaces(imageData);
 *     // Process results
 * }
 * }</pre>
 * 
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionBackend
 * @see VisionResult
 */
public class VisionTemplate {
    
    /**
     * Detects faces in the provided image data.
     * 
     * <p>This method processes the image using the configured vision backend
     * and returns detection results including bounding boxes and confidence
     * scores for each detected face.</p>
     * 
     * @param imageData the image data to process, must not be null
     * @return a {@link VisionResult} containing face detection results
     * @throws IllegalArgumentException if imageData is null or empty
     * @throws VisionProcessingException if the vision backend fails to process the image
     * @throws SecurityException if the image fails security validation
     */
    public VisionResult detectFaces(byte[] imageData) {
        // Implementation
    }
}
```

### README Documentation

- **Getting Started**: Clear setup instructions with examples
- **API Reference**: Comprehensive API documentation
- **Configuration**: All configuration options with examples
- **Examples**: Real-world usage examples
- **Troubleshooting**: Common issues and solutions

## 🧪 Testing Standards

### Test Structure

```java
@ExtendWith(MockitoExtension.class)
class VisionTemplateTest {
    
    @Mock
    private VisionBackend mockBackend;
    
    @InjectMocks
    private VisionTemplate visionTemplate;
    
    @Test
    @DisplayName("Should detect faces successfully")
    void shouldDetectFacesSuccessfully() {
        // Given
        byte[] imageData = loadTestImage("face.jpg");
        VisionResult expectedResult = createMockResult();
        when(mockBackend.detectFaces(imageData)).thenReturn(expectedResult);
        
        // When
        VisionResult actualResult = visionTemplate.detectFaces(imageData);
        
        // Then
        assertThat(actualResult).isEqualTo(expectedResult);
        verify(mockBackend).detectFaces(imageData);
    }
    
    @Test
    @DisplayName("Should throw exception for null image data")
    void shouldThrowExceptionForNullImageData() {
        // Given
        byte[] nullImageData = null;
        
        // When & Then
        assertThatThrownBy(() -> visionTemplate.detectFaces(nullImageData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Image data must not be null");
    }
}
```

### Test Coverage Requirements

- **Unit Tests**: 90%+ line coverage for all public methods
- **Integration Tests**: Test all backend integrations
- **Security Tests**: Test input validation and security measures
- **Performance Tests**: Benchmark critical operations

## 🔒 Security Guidelines

### Input Validation

```java
// ✅ Good: Comprehensive validation
public VisionResult detectFaces(byte[] imageData) {
    validateImageData(imageData);
    validateImageFormat(imageData);
    validateImageSize(imageData);
    
    return backend.detectFaces(imageData);
}

private void validateImageData(byte[] imageData) {
    if (imageData == null) {
        throw new IllegalArgumentException("Image data must not be null");
    }
    if (imageData.length == 0) {
        throw new IllegalArgumentException("Image data must not be empty");
    }
    if (imageData.length > MAX_IMAGE_SIZE) {
        throw new IllegalArgumentException("Image size exceeds maximum allowed size");
    }
}
```

### Error Handling

```java
// ✅ Good: Secure error handling
try {
    return backend.processImage(imageData);
} catch (Exception e) {
    logger.error("Failed to process image", e);
    throw new VisionProcessingException("Failed to process image", e);
}

// ❌ Bad: Exposing sensitive information
} catch (Exception e) {
    throw new RuntimeException("Failed to process image: " + e.getMessage());
}
```

## 📊 Logging Standards

### Structured Logging

```java
// ✅ Good: Structured logging with context
private static final Logger logger = LoggerFactory.getLogger(VisionTemplate.class);

public VisionResult detectFaces(byte[] imageData) {
    String correlationId = generateCorrelationId();
    
    logger.info("Starting face detection", Map.of(
        "correlationId", correlationId,
        "imageSize", imageData.length,
        "operation", "face_detection"
    ));
    
    try {
        VisionResult result = backend.detectFaces(imageData);
        
        logger.info("Face detection completed", Map.of(
            "correlationId", correlationId,
            "facesDetected", result.detections().size(),
            "confidence", result.averageConfidence()
        ));
        
        return result;
    } catch (Exception e) {
        logger.error("Face detection failed", Map.of(
            "correlationId", correlationId,
            "error", e.getClass().getSimpleName()
        ), e);
        throw e;
    }
}
```

### Log Levels

- **ERROR**: Actionable errors that require immediate attention
- **WARN**: Concerning situations that may need investigation
- **INFO**: Business events and important operations
- **DEBUG**: Technical details for troubleshooting

## 🚀 Performance Guidelines

### Memory Management

```java
// ✅ Good: Resource management
public VisionResult processImage(byte[] imageData) {
    try (var imageBuffer = allocateImageBuffer(imageData.length)) {
        // Process image
        return result;
    } // Buffer automatically released
}

// ❌ Bad: Potential memory leaks
public VisionResult processImage(byte[] imageData) {
    var imageBuffer = allocateImageBuffer(imageData.length);
    // Process image without cleanup
    return result;
}
```

### Async Operations

```java
// ✅ Good: Virtual threads for I/O operations
@Async
public CompletableFuture<VisionResult> detectFacesAsync(byte[] imageData) {
    return CompletableFuture.supplyAsync(() -> {
        return backend.detectFaces(imageData);
    }, virtualThreadExecutor);
}
```

## 🔧 Configuration Standards

### Configuration Properties

```java
@ConfigurationProperties(prefix = "vision")
public record VisionProperties(
    @DefaultValue("opencv")
    String backend,
    
    @DefaultValue("0.8")
    @Min(0.0) @Max(1.0)
    double confidenceThreshold,
    
    @DefaultValue("10485760") // 10MB
    @Min(1024) @Max(104857600) // 1KB to 100MB
    long maxImageSize,
    
    List<String> allowedImageTypes
) {
    // Validation logic
}
```

## 📋 Code Review Checklist

Before submitting code for review, ensure:

- [ ] All public APIs have comprehensive Javadoc
- [ ] Unit tests cover all public methods (90%+ coverage)
- [ ] Integration tests for backend interactions
- [ ] Security validation for all inputs
- [ ] Proper error handling without information leakage
- [ ] Structured logging with appropriate levels
- [ ] Performance considerations for resource usage
- [ ] Configuration follows Spring Boot patterns
- [ ] Code follows naming conventions
- [ ] No TODO comments without issue references
- [ ] All dependencies are properly versioned
- [ ] Documentation is updated

## 🎯 Quality Gates

### Build Requirements

- All tests must pass
- Checkstyle validation must pass (warnings treated as errors)
- Spotless formatting must be applied
- No security vulnerabilities in dependencies
- Documentation coverage for all public APIs

### Performance Requirements

- Image processing latency < 2 seconds for 1MB images
- Memory usage < 512MB for typical operations
- Throughput > 10 images/second on standard hardware

---

**Remember**: These rules are designed to create maintainable, secure, and high-quality code that can scale with the project. When in doubt, prioritize clarity, security, and maintainability over cleverness or premature optimization.
