# Contributing to Spring Vision

Welcome to the Spring Vision project! This guide will help you understand how to contribute effectively to this computer vision framework for Spring Boot applications.

## Table of Contents

- [Quick Start for Contributors](#quick-start-for-contributors)
- [Development Setup](#development-setup)
- [Architecture Overview](#architecture-overview)
- [Code Standards](#code-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Backend Development](#backend-development)
- [Example Applications](#example-applications)
- [Issue Reporting](#issue-reporting)
- [Getting Help](#getting-help)

## Quick Start for Contributors

### Prerequisites

- **Java 21+** (JDK 21 or higher)
- **Maven 3.8+** 
- **Git**
- **Linux/macOS/Windows** (OpenCV native libraries included for all platforms)

### Clone and Build

```bash
git clone <repository-url>
cd spring-vision
mvn clean install -DskipTests
```

### Run Examples

```bash
# Test all examples
./test.sh --all

# Test individual components
./test.sh --cli    # CLI application
./test.sh --api    # REST APIs (GWT/Vaadin)
./test.sh --web    # Web applications
```

## Development Setup

### IDE Configuration

**IntelliJ IDEA / VS Code with Java Extensions:**

1. Import as Maven project
2. Set Project SDK to Java 21+
3. Enable annotation processing
4. Install recommended plugins:
   - Checkstyle
   - SonarLint
   - Spring Boot support

### Code Style

The project uses **Spotless** and **Checkstyle** for consistent formatting:

```bash
# Apply code formatting
mvn spotless:apply

# Check style compliance
mvn checkstyle:check
```

### Environment Variables

```bash
# Optional: Disable Docker Compose during development
export SPRING_DOCKER_COMPOSE_ENABLED=false

# Optional: Custom server port for examples
export SERVER_PORT=8080
```

## Architecture Overview

### Core Modules

```
spring-vision/
├── spring-vision-core/           # Core framework and backends
├── spring-vision-autoconfigure/  # Spring Boot auto-configuration
├── spring-vision-starter/        # Starter with REST API
├── spring-vision-facebytes/      # FaceBytes integration
└── spring-vision-examples/       # Example applications
    ├── picocli-application/       # CLI interface
    ├── basic-face-detection/      # Simple web app
    ├── gwt-application/           # GWT frontend
    ├── vaadin-application/        # Vaadin UI with security
    ├── javafx-application/        # Desktop GUI
    ├── compreface-example/        # CompreFace integration
    └── deepface-example/          # DeepFace integration
```

### Key Abstractions

#### VisionBackend Interface

```java
public interface VisionBackend {
    // Primary detection method
    List<Detection> detect(ImageData imageData, DetectionQuery query);
    
    // Legacy methods (deprecated in 2.0)
    @Deprecated
    List<Detection> detectFaces(ImageData imageData);
    
    // Optional capabilities
    default List<float[]> extractEmbeddings(ImageData imageData) { ... }
    default boolean verify(ImageData a, ImageData b, String metric, double threshold) { ... }
    default ImageData obscureFaces(ImageData imageData) { ... }
}
```

#### DetectionQuery Builder

```java
DetectionQuery query = DetectionQuery.builder()
    .type(DetectionType.FACE)
    .categories(Set.of(DetectionCategory.FACE, DetectionCategory.EYE))
    .minConfidence(0.7)
    .maxDetections(10)
    .build();
```

### Current Backends

| Backend | Detection Types | Capabilities | Status |
|---------|----------------|--------------|---------|
| `OpenCvVisionBackend` | FACE, OBJECT | Embeddings, Obscuring | ✅ Production |
| `FaceBytesBackend` | FACE | Embeddings, Analysis | ✅ Production |
| `CompreFaceVisionBackend` | FACE | REST API | ✅ Production |
| `DeepFaceVisionBackend` | FACE | REST API | ✅ Production |
| `MediaPipeVisionBackend` | FACE, HAND, POSE | Multi-modal | 🚧 In Progress |
| `YoloVisionBackend` | OBJECT | ONNX Runtime | 📋 Planned |
| `InsightFaceVisionBackend` | FACE | High Accuracy | 📋 Planned |

## Code Standards

### Senior-Level Requirements

**IMPORTANT**: All contributors must read and follow our comprehensive coding standards before submitting any code.

All code must meet senior Spring/Java engineer standards:

#### 1. Security-First Approach

- **Address OWASP Top-10 vulnerabilities** in all code
- **Validate all inputs**, especially image data and user parameters
- **Implement proper size and format restrictions**
- **Use secure error handling** without information leakage
- **Never log sensitive data** (image contents, credentials, etc.)

#### 2. Comprehensive Documentation

- **Every public class and method must have Javadoc**
- **Include parameter descriptions, return values, and exception details**
- **Provide usage examples for complex APIs**
- **Document security considerations and performance implications**

#### 3. Modern Java Practices (JDK 21+)

```java
// ✅ Use records for data carriers
public record DetectionResult(
    List<Detection> detections,
    ProcessingMetrics metrics,
    Optional<String> errorMessage
) {}

// ✅ Use sealed interfaces for controlled hierarchies
public sealed interface VisionCapability 
    permits EmbeddingCapability, AnnotationCapability, BarcodeCapability {
}

// ✅ Use pattern matching and switch expressions
public String formatConfidence(Detection detection) {
    return switch (detection.confidence()) {
        case double conf when conf >= 0.9 -> "High confidence: %.2f".formatted(conf);
        case double conf when conf >= 0.7 -> "Medium confidence: %.2f".formatted(conf);
        case double conf -> "Low confidence: %.2f".formatted(conf);
    };
}

// ✅ Use virtual threads for I/O operations
@Async
public CompletableFuture<List<Detection>> detectAsync(ImageData imageData) {
    return CompletableFuture.supplyAsync(() -> {
        return detect(imageData, DetectionQuery.defaultQuery());
    }, Executors.newVirtualThreadPerTaskExecutor());
}
```

#### 4. Naming Conventions

```java
// ✅ Good: Explicit, domain-specific names
public class OpenCvVisionBackend implements VisionBackend {
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;
    
    public List<Detection> detectFacesWithConfidence(ImageData inputImage, double threshold) {
        // Implementation
    }
}

// ❌ Bad: Abbreviated, unclear names
public class OCVBackend {
    private static final double DEF_CONF = 0.5;
    
    public List<Detection> detect(ImageData img, double t) {
        // Implementation
    }
}
```

#### 5. Error Handling Standards

```java
// ✅ Comprehensive error handling with context
public List<Detection> detectFaces(ImageData imageData) {
    Objects.requireNonNull(imageData, "ImageData cannot be null");
    
    if (imageData.getData().length == 0) {
        throw new IllegalArgumentException("Image data cannot be empty");
    }
    
    try {
        Mat image = decodeImage(imageData);
        if (image.empty()) {
            logger.warn("Failed to decode image data of size: {} bytes", imageData.getData().length);
            return Collections.emptyList();
        }
        
        return performDetection(image);
        
    } catch (Exception e) {
        logger.error("Face detection failed for image of size: {} bytes", 
                   imageData.getData().length, e);
        throw new VisionProcessingException(
            "Failed to detect faces: " + e.getMessage(), e);
    }
}
```

### Structured Logging Standards

**ALL logging must be structured JSON format with consistent fields:**

```java
// ✅ Structured logging with contextual metadata
@Slf4j
public class OpenCvVisionBackend implements VisionBackend {
    
    public List<Detection> detectFaces(ImageData imageData) {
        String correlationId = UUID.randomUUID().toString();
        
        logger.info("Starting face detection: correlationId={}, imageSize={}, backend=opencv", 
                   correlationId, imageData.getData().length);
        
        long startTime = System.currentTimeMillis();
        try {
            List<Detection> results = performDetection(imageData);
            
            logger.info("Face detection completed: correlationId={}, detectionCount={}, " +
                       "processingTimeMs={}, backend=opencv", 
                       correlationId, results.size(), 
                       System.currentTimeMillis() - startTime);
            
            return results;
            
        } catch (Exception e) {
            logger.error("Face detection failed: correlationId={}, backend=opencv, error={}", 
                        correlationId, e.getMessage(), e);
            throw new VisionProcessingException("Detection failed", e);
        }
    }
}
```

**Required log fields:**
- `timestamp` (automatic)
- `level` (ERROR, WARN, INFO, DEBUG)
- `component` (class/backend name)
- `message` (human-readable)
- `correlation_id` (for request tracing)
- `backend` (which vision backend)
- Context-specific fields (imageSize, detectionCount, etc.)

**Log levels:**
- `ERROR`: Actionable issues requiring immediate attention
- `WARN`: Concerning but recoverable situations
- `INFO`: Business events and major processing milestones
- `DEBUG`: Technical implementation details

### Configuration Guidelines

Use `@ConfigurationProperties` for externalized configuration:

```java
@ConfigurationProperties(prefix = "spring.vision.mybackend")
public record MyBackendProperties(
    @DefaultValue("false")
    boolean enabled,
    
    @DefaultValue("0.8")
    @Min(0.0) @Max(1.0)
    double confidenceThreshold,
    
    @DefaultValue("~/.spring-vision/models/")
    String modelPath
) {
    // Validation logic if needed
}
```

## Testing Guidelines

### Testing Requirements

- **90%+ test coverage**: All public methods must have unit tests
- **Integration tests**: Test component interactions
- **Security tests**: Test input validation and security measures
- **Performance tests**: Benchmark critical operations

### Manual Testing First

The framework prioritizes manual testing through example applications:

```bash
# Comprehensive testing
./test.sh --all

# Individual component testing
./test.sh --cli
./test.sh --api  
./test.sh --web
```

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn jacoco:report
```

### Test Naming Convention

```java
@Test
@DisplayName("Should detect faces successfully when image contains valid faces")
void shouldDetectFacesSuccessfully() {
    // Test implementation
}

@Test
@DisplayName("Should reject oversized images with clear error message")
void shouldRejectOversizedImages() {
    byte[] oversizedImage = new byte[MAX_IMAGE_SIZE + 1];
    
    assertThatThrownBy(() -> visionTemplate.detectFaces(oversizedImage))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Image size exceeds maximum allowed size");
}
```

### Integration Testing

Create integration tests for new backends:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.vision.backend=opencv",
    "spring.vision.opencv.enabled=true"
})
class OpenCvBackendIntegrationTest {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Test
    void shouldDetectFacesInSampleImage() {
        // Load test image
        byte[] imageData = loadTestImage("sample-face.jpg");
        
        // Perform detection
        List<Detection> detections = visionTemplate.detectFaces(imageData);
        
        // Verify results
        assertThat(detections).isNotEmpty();
        assertThat(detections.get(0).getConfidence()).isGreaterThan(0.5);
        assertThat(detections.get(0).getBoundingBox()).isNotNull();
    }
}
```

### Performance Testing

```java
@Test
void shouldProcessImageWithinTimeLimit() {
    byte[] imageData = loadTestImage("large-image.jpg");
    
    long startTime = System.currentTimeMillis();
    List<Detection> detections = visionTemplate.detectFaces(imageData);
    long processingTime = System.currentTimeMillis() - startTime;
    
    assertThat(processingTime).isLessThan(5000); // 5 second limit
    assertThat(detections).isNotNull();
}
```

## Pull Request Process

### Before Submitting

1. **Read the standards**: Ensure you understand all coding requirements
2. **Test thoroughly**: Run all tests and ensure they pass
3. **Format code**: Apply Spotless formatting
4. **Check style**: Run Checkstyle validation
5. **Update docs**: Update documentation if needed

### Pull Request Template

When submitting a pull request, include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] All tests pass
- [ ] Manual testing via examples completed

## Security
- [ ] Input validation implemented
- [ ] Error handling secure
- [ ] No sensitive data exposed
- [ ] OWASP Top-10 considerations addressed

## Documentation
- [ ] Javadoc updated
- [ ] README updated if needed
- [ ] API documentation updated

## Checklist
- [ ] Code follows project standards
- [ ] 90%+ test coverage maintained
- [ ] No TODO comments without issue references
- [ ] Dependencies properly versioned
- [ ] Structured logging implemented
```

### Commit Message Guidelines

#### Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

#### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc.)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Build process or auxiliary tool changes

#### Examples

```
feat(backend): add InsightFace backend with ONNX support

- Implement face detection using RetinaFace model
- Add embedding extraction with ArcFace
- Include model auto-download with SHA-256 verification
- Add comprehensive error handling and logging

Closes #123
```

```
fix(autoconfigure): resolve bean creation issue

Fix conditional bean creation when vision backend is disabled.

Fixes #456
```

### Code Review Process

#### Review Criteria

- ✅ **Security**: No vulnerabilities, proper input validation
- ✅ **Performance**: No memory leaks, efficient algorithms
- ✅ **Documentation**: Complete Javadoc, clear README updates
- ✅ **Logging**: Structured JSON logs with proper levels
- ✅ **Testing**: Manual testing via examples, integration tests
- ✅ **Compatibility**: No breaking changes to public APIs
- ✅ **Style**: Follows project conventions and passes checks

#### Review Process

1. **Automated checks** must pass (tests, style, security)
2. **At least one maintainer** must approve
3. **All conversations** must be resolved
4. **Documentation** must be updated if needed

## Backend Development

### Creating a New Backend

1. **Implement VisionBackend interface:**

```java
@Component
@ConditionalOnProperty(value = "spring.vision.mybackend.enabled", havingValue = "true")
public class MyVisionBackend implements VisionBackend, EmbeddingCapability {
    
    @Override
    public List<Detection> detect(ImageData imageData, DetectionQuery query) {
        // Implementation
    }
    
    @Override
    public BackendHealthInfo getHealthInfo() {
        return BackendHealthInfo.builder()
            .backendName("MyBackend")
            .status(HealthStatus.UP)
            .version("1.0.0")
            .build();
    }
}
```

2. **Add configuration properties:**

```java
@ConfigurationProperties(prefix = "spring.vision.mybackend")
public class MyBackendProperties {
    private boolean enabled = false;
    private String modelPath = "~/.spring-vision/models/my-model.bin";
    private double confidenceThreshold = 0.5;
    
    // getters/setters
}
```

3. **Register in auto-configuration:**

```java
@Configuration
@EnableConfigurationProperties(MyBackendProperties.class)
public class MyBackendAutoConfiguration {
    
    @Bean
    @ConditionalOnProperty(value = "spring.vision.mybackend.enabled", havingValue = "true")
    public MyVisionBackend myVisionBackend(MyBackendProperties properties) {
        return new MyVisionBackend(properties);
    }
}
```

4. **Add to META-INF/services:**

```
# spring-vision-core/src/main/resources/META-INF/services/com.springvision.core.VisionBackend
com.springvision.core.backend.MyVisionBackend
```

### Backend Implementation Guidelines

#### Security Requirements

```java
// ✅ Validate all inputs
public List<Detection> detect(ImageData imageData, DetectionQuery query) {
    Objects.requireNonNull(imageData, "ImageData cannot be null");
    Objects.requireNonNull(query, "DetectionQuery cannot be null");
    
    if (imageData.getData().length > MAX_IMAGE_SIZE) {
        throw new IllegalArgumentException("Image size exceeds maximum limit");
    }
    
    // Sanitize query parameters
    DetectionQuery sanitizedQuery = sanitizeQuery(query);
    
    return performDetection(imageData, sanitizedQuery);
}

// ✅ Never log sensitive data
logger.info("Detection completed: detectionCount={}, processingTimeMs={}", 
           results.size(), processingTime);
// ❌ Never: logger.debug("Image data: {}", Arrays.toString(imageData.getData()));
```

#### Resource Management

```java
@PreDestroy
public void shutdown() {
    try {
        if (nativeModel != null) {
            nativeModel.close();
        }
        if (executorService != null) {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    } catch (Exception e) {
        logger.warn("Error during backend shutdown", e);
    }
}
```

## Example Applications

### Creating New Examples

1. **Create module structure:**

```
spring-vision-examples/my-example/
├── pom.xml
├── src/main/java/
│   └── com/springvision/examples/myexample/
├── src/main/resources/
└── README.md
```

2. **Add parent dependency:**

```xml
<parent>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-examples</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

3. **Follow example patterns:**
   - **CLI**: Use PicoCLI for command-line interfaces
   - **Web**: Use Spring MVC with Thymeleaf
   - **REST API**: Use Spring Web with JSON responses
   - **Desktop**: Use JavaFX or Swing
   - **Modern Web**: Use Vaadin or integrate with React/Angular

### URL Support Pattern

All examples should support both local files and public URLs:

```java
private boolean isHttpUrl(String source) {
    return source.startsWith("http://") || source.startsWith("https://");
}

private byte[] readImageData(String source) throws IOException {
    if (isHttpUrl(source)) {
        return downloadImageBytes(source);
    } else {
        return Files.readAllBytes(Paths.get(source));
    }
}

private byte[] downloadImageBytes(String url) throws IOException {
    validatePublicHost(new URL(url));
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_MS / 1000))
        .GET()
        .build();
        
    HttpResponse<byte[]> response = httpClient.send(request, 
        HttpResponse.BodyHandlers.ofByteArray());
        
    if (response.body().length > MAX_DOWNLOAD_BYTES) {
        throw new IOException("Downloaded image exceeds size limit");
    }
    
    return response.body();
}
```

## Issue Reporting

### Bug Reports

When reporting bugs, please include:

- **Environment**: OS, Java version, Maven version
- **Steps to reproduce**: Clear, step-by-step instructions
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Logs**: Relevant error logs (sanitized of sensitive data)
- **Screenshots**: If applicable

### Feature Requests

When requesting features, please include:

- **Use case**: Why this feature is needed
- **Proposed solution**: How you envision it working
- **Alternatives considered**: Other approaches you've thought about
- **Impact**: How this affects existing functionality

## Getting Help

- **Documentation**: Check `docs/` directory for comprehensive guides
- **Examples**: See `spring-vision-examples/` modules for working implementations
- **GitHub Issues**: Create GitHub issue with reproduction steps
- **GitHub Discussions**: Use GitHub Discussions for questions and general discussion

## Recognition

Contributors will be recognized in:

- **README.md**: For significant contributions
- **Release notes**: For each release
- **Contributors list**: On GitHub

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help newcomers learn the codebase
- Prioritize code quality and security
- Document your changes thoroughly

---

Thank you for contributing to Spring Vision! 🎯 Your contributions help make computer vision accessible to every Spring Boot developer. 