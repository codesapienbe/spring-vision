# Getting Started with Spring Vision

Welcome to Spring Vision! This guide will help you get up and running with the framework quickly, whether you're a developer looking to integrate computer vision into your Spring Boot application or a contributor wanting to extend the framework.

## Table of Contents

- [Quick Start](#quick-start)
- [Installation](#installation)
- [Your First Detection](#your-first-detection)
- [Example Applications](#example-applications)
- [Configuration](#configuration)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

## Quick Start

### Prerequisites

- **Java 21+** (JDK 21 or higher recommended)
- **Maven 3.8+** or **Gradle 7.0+**
- **Spring Boot 3.2+**

### 30-Second Setup

1. **Add the starter dependency:**

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. **Enable Spring Vision in your application:**

```yaml
spring:
  vision:
    backend: opencv
    opencv:
      enabled: true
```

3. **Inject and use VisionTemplate:**

```java
@RestController
public class VisionController {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @PostMapping("/detect-faces")
    public List<Detection> detectFaces(@RequestParam("file") MultipartFile file) 
            throws IOException {
        return visionTemplate.detectFaces(file.getBytes());
    }
}
```

That's it! Your Spring Boot application now has computer vision capabilities.

## Installation

### Maven

Add the Spring Vision starter to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Vision Starter -->
    <dependency>
        <groupId>com.springvision</groupId>
        <artifactId>spring-vision-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot Starter Web (if building REST APIs) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Thymeleaf (if building web UIs) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.springvision:spring-vision-starter:1.0.0-SNAPSHOT'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
}
```

### Local Development Setup

If you want to contribute or run the latest development version:

```bash
# Clone the repository
git clone <repository-url>
cd spring-vision

# Build and install locally
mvn clean install -DskipTests

# Run example applications
./test.sh --all
```

## Your First Detection

Let's create a simple face detection service step by step.

### 1. Basic Spring Boot Application

```java
@SpringBootApplication
public class VisionDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisionDemoApplication.class, args);
    }
}
```

### 2. Configuration

Create `application.yml`:

```yaml
spring:
  vision:
    backend: opencv                    # Use OpenCV backend
    opencv:
      enabled: true
      confidence-threshold: 0.7        # Higher confidence for better results
      min-face-size: 30               # Minimum face size in pixels
      
  servlet:
    multipart:
      max-file-size: 10MB             # Allow larger image uploads
      max-request-size: 10MB

server:
  port: 8080

logging:
  level:
    com.springvision: INFO            # Spring Vision logs
```

### 3. Create a Detection Controller

```java
@RestController
@RequestMapping("/api/vision")
@Slf4j
public class FaceDetectionController {
    
    private final VisionTemplate visionTemplate;
    
    public FaceDetectionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }
    
    @PostMapping("/detect-faces")
    public ResponseEntity<DetectionResponse> detectFaces(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "minConfidence", required = false, defaultValue = "0.5") 
            Double minConfidence) {
        
        try {
            // Validate input
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new DetectionResponse("File is empty", 0, Collections.emptyList()));
            }
            
            // Create detection query
            DetectionQuery query = DetectionQuery.builder()
                .type(DetectionType.FACE)
                .minConfidence(minConfidence)
                .maxDetections(50)
                .build();
            
            // Perform detection
            ImageData imageData = new ImageData(file.getBytes());
            List<Detection> detections = visionTemplate.detect(imageData, query);
            
            log.info("Detected {} faces in uploaded image", detections.size());
            
            return ResponseEntity.ok(new DetectionResponse(
                "Success", 
                detections.size(), 
                detections
            ));
            
        } catch (Exception e) {
            log.error("Face detection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DetectionResponse("Detection failed: " + e.getMessage(), 0, Collections.emptyList()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("backend", visionTemplate.getActiveBackend().getClass().getSimpleName());
        health.put("timestamp", Instant.now());
        return ResponseEntity.ok(health);
    }
}
```

### 4. Response DTO

```java
public record DetectionResponse(
    String message,
    int detectionCount,
    List<Detection> detections
) {}
```

### 5. Test Your API

Start your application and test it:

```bash
# Start the application
mvn spring-boot:run

# Test with curl (replace with path to your image)
curl -X POST "http://localhost:8080/api/vision/detect-faces?minConfidence=0.7" \
     -F "file=@/path/to/your/image.jpg" \
     -H "Content-Type: multipart/form-data"

# Check health
curl http://localhost:8080/api/vision/health
```

Expected response:

```json
{
  "message": "Success",
  "detectionCount": 2,
  "detections": [
    {
      "type": "FACE",
      "category": "FACE",
      "boundingBox": {
        "x": 0.25,
        "y": 0.30,
        "width": 0.15,
        "height": 0.20
      },
      "confidence": 0.89,
      "label": "Face",
      "attributes": {
        "category": "FACE"
      }
    }
  ]
}
```

## Example Applications

Spring Vision includes several complete example applications that demonstrate different use cases:

### 1. CLI Application (PicoCLI)

**Location**: `spring-vision-examples/picocli-application/`

Command-line interface for batch processing and automation:

```bash
cd spring-vision-examples/picocli-application
mvn spring-boot:run -Dspring-boot.run.arguments="--detect /path/to/image.jpg"

# Other commands
mvn spring-boot:run -Dspring-boot.run.arguments="--embed /path/to/image.jpg"
mvn spring-boot:run -Dspring-boot.run.arguments="--verify /path/to/person1.jpg /path/to/person2.jpg"
mvn spring-boot:run -Dspring-boot.run.arguments="--obscure /path/to/input.jpg /path/to/output.jpg"

# Supports URLs too
mvn spring-boot:run -Dspring-boot.run.arguments="--detect https://example.com/image.jpg"
```

### 2. Web Application (Basic)

**Location**: `spring-vision-examples/basic-face-detection/`

Simple web interface with file upload:

```bash
cd spring-vision-examples/basic-face-detection
mvn spring-boot:run

# Open browser to http://localhost:8080
```

### 3. Advanced Web UI (Vaadin)

**Location**: `spring-vision-examples/vaadin-application/`

Modern web application with real-time processing, batch operations, and authentication:

```bash
cd spring-vision-examples/vaadin-application
mvn spring-boot:run

# Open browser to http://localhost:8080
# Login: user / password
```

### 4. Desktop Application (JavaFX)

**Location**: `spring-vision-examples/javafx-application/`

Cross-platform desktop GUI:

```bash
cd spring-vision-examples/javafx-application
mvn spring-boot:run
```

### 5. SPA-Ready API (GWT)

**Location**: `spring-vision-examples/gwt-application/`

REST API backend suitable for React, Angular, Vue.js frontends:

```bash
cd spring-vision-examples/gwt-application
mvn spring-boot:run

# API available at http://localhost:8080/api/vision/*
```

## Configuration

### Backend Selection

Spring Vision supports multiple vision backends. Configure them in `application.yml`:

```yaml
spring:
  vision:
    # Backend Selection (choose one)
    backend: opencv                    # opencv, facebytes, compreface, deepface
    
    # OpenCV Backend (default, no external dependencies)
    opencv:
      enabled: true
      confidence-threshold: 0.5
      min-face-size: 30
      max-face-size: 300
      
    # FaceBytes Backend (embedded ML models)
    facebytes:
      enabled: false
      model: "ArcFace"                 # ArcFace, VGG-Face, Facenet
      detector: "opencv"               # opencv, retinaface, mtcnn
      
    # CompreFace Backend (external service)
    compreface:
      enabled: false
      url: "http://localhost:8000"
      api-key: "${COMPREFACE_API_KEY}"
      
    # DeepFace Backend (external service)  
    deepface:
      enabled: false
      url: "http://localhost:5000"
```

### Performance Tuning

```yaml
spring:
  vision:
    performance:
      max-image-size: 10485760         # 10MB max image size
      processing-timeout: 30000        # 30 second timeout
      thread-pool-size: 10             # Concurrent processing threads
      
    security:
      max-download-size: 5242880       # 5MB max for URL downloads
      connect-timeout: 5000            # 5 second connection timeout
      read-timeout: 10000              # 10 second read timeout
```

### Environment Variables

For production deployments:

```bash
# Backend configuration
export SPRING_VISION_BACKEND=opencv
export SPRING_VISION_OPENCV_ENABLED=true
export SPRING_VISION_OPENCV_CONFIDENCE_THRESHOLD=0.7

# External service credentials
export COMPREFACE_API_KEY=your-api-key
export DEEPFACE_URL=http://deepface-service:5000

# Performance tuning
export SPRING_VISION_PERFORMANCE_MAX_IMAGE_SIZE=20971520  # 20MB
export SPRING_VISION_PERFORMANCE_THREAD_POOL_SIZE=20
```

## Common Use Cases

### 1. Face Detection in Images

```java
@Service
public class FaceDetectionService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<Detection> findFaces(byte[] imageData) {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.7)
            .build();
            
        return visionTemplate.detect(new ImageData(imageData), query);
    }
}
```

### 2. Face Verification (Same Person?)

```java
@Service
public class FaceVerificationService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public boolean areSamePerson(byte[] image1, byte[] image2) {
        return visionTemplate.verify(
            new ImageData(image1),
            new ImageData(image2),
            "cosine",    // similarity metric
            0.35         // threshold
        );
    }
}
```

### 3. Face Embeddings for Search

```java
@Service
public class FaceSearchService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<float[]> extractFaceEmbeddings(byte[] imageData) {
        return visionTemplate.extractEmbeddings(new ImageData(imageData));
    }
    
    public double calculateSimilarity(float[] embedding1, float[] embedding2) {
        // Cosine similarity calculation
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
```

### 4. Privacy Protection (Face Blurring)

```java
@Service
public class PrivacyService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public byte[] blurFaces(byte[] imageData) {
        ImageData obscured = visionTemplate.obscureFaces(new ImageData(imageData));
        return obscured.getData();
    }
}
```

### 5. Processing Images from URLs

```java
@Service
public class ImageProcessingService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public List<Detection> detectFacesFromUrl(String imageUrl) throws Exception {
        // Download image securely
        byte[] imageData = downloadImage(imageUrl);
        
        // Detect faces
        return visionTemplate.detectFaces(imageData);
    }
    
    private byte[] downloadImage(String url) throws Exception {
        // Validate URL (prevent SSRF)
        URL parsedUrl = new URL(url);
        validatePublicHost(parsedUrl);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(parsedUrl.toURI())
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
            
        HttpResponse<byte[]> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofByteArray());
            
        if (response.body().length > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("Image too large");
        }
        
        return response.body();
    }
    
    private void validatePublicHost(URL url) throws Exception {
        String host = url.getHost();
        if ("localhost".equalsIgnoreCase(host) || 
            host.startsWith("127.") || 
            host.startsWith("10.") ||
            host.startsWith("192.168.")) {
            throw new IllegalArgumentException("Access to private hosts not allowed");
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. "No backend available" Error

**Problem**: Spring Vision can't find any enabled backends.

**Solution**:
```yaml
spring:
  vision:
    backend: opencv
    opencv:
      enabled: true
```

#### 2. OpenCV Native Library Error

**Problem**: `java.lang.UnsatisfiedLinkError: Can't load native library`

**Solution**: OpenCV native libraries are included automatically. If you see this error:

1. Check your Java version (requires Java 21+)
2. Verify your platform is supported (Linux x64, macOS x64/arm64, Windows x64)
3. Check for conflicting OpenCV installations

#### 3. Large Image Processing Fails

**Problem**: `IllegalArgumentException: Image size exceeds maximum limit`

**Solution**: Increase size limits:
```yaml
spring:
  vision:
    performance:
      max-image-size: 20971520  # 20MB
  servlet:
    multipart:
      max-file-size: 20MB
```

#### 4. Slow Performance

**Problem**: Face detection takes too long.

**Solutions**:
- Reduce image size before processing
- Increase thread pool size
- Use faster backend (OpenCV vs FaceBytes)
- Adjust confidence threshold

```yaml
spring:
  vision:
    opencv:
      confidence-threshold: 0.8  # Higher = faster but fewer detections
    performance:
      thread-pool-size: 20       # More parallel processing
```

#### 5. Memory Issues

**Problem**: `OutOfMemoryError` during processing.

**Solutions**:
- Increase JVM heap size: `-Xmx4g`
- Reduce image size limits
- Process images in batches
- Enable garbage collection logging

### Debugging

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    com.springvision: DEBUG
    org.bytedeco.opencv: DEBUG
    org.springframework.boot.autoconfigure: DEBUG
```

Check application health:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics/vision.detections.total
```

### Performance Monitoring

Monitor performance with Micrometer metrics:

```java
@Component
public class VisionMonitoring {
    
    @EventListener
    public void handleDetectionEvent(DetectionCompletedEvent event) {
        log.info("Detection completed: backend={}, detections={}, duration={}ms",
                event.getBackend(), 
                event.getDetectionCount(), 
                event.getDuration().toMillis());
    }
}
```

## Next Steps

### For Application Developers

1. **Explore Examples**: Run the example applications to see different integration patterns
2. **Read Architecture Guide**: Understand the framework design in `docs/ARCHITECTURE.md`
3. **Performance Tuning**: Optimize for your specific use case
4. **Production Deployment**: Follow deployment best practices

### For Contributors

1. **Read Contributing Guide**: See `docs/CONTRIBUTING.md` for development setup
2. **Understand Backend Architecture**: Learn how to create custom backends
3. **Check TODO List**: See `TODO.md` for areas needing contribution
4. **Join Discussions**: Participate in community discussions

### Advanced Topics

- **Custom Backend Development**: Create backends for specialized vision libraries
- **Multi-Backend Strategies**: Route different operations to optimal backends
- **Batch Processing**: Process large volumes of images efficiently
- **Integration Patterns**: Integrate with message queues, databases, and cloud services
- **Security Hardening**: Additional security measures for production

### Resources

- **API Documentation**: `docs/API_DOCUMENTATION.md`
- **Architecture Guide**: `docs/ARCHITECTURE.md`
- **Backend Integration**: `docs/DEEPFACE_INTEGRATION.md`, `docs/OPENCV_SETUP.md`
- **Deployment Guide**: `docs/DEPLOYMENT_GUIDE.md`
- **Example Applications**: `spring-vision-examples/*/README.md`

---

Welcome to the Spring Vision community! 🎯 Whether you're building the next great computer vision application or contributing to the framework itself, we're excited to see what you create. 