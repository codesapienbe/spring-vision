# Spring Vision User Guide

A comprehensive guide for using the Spring Vision framework in your applications.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Quick Start](#quick-start)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [Advanced Features](#advanced-features)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)
8. [Performance Tuning](#performance-tuning)
9. [Security Considerations](#security-considerations)
10. [Monitoring and Logging](#monitoring-and-logging)

## Getting Started

### Prerequisites

- **Java**: 21 or later
- **Spring Boot**: 3.0 or later
- **Maven**: 3.6 or later (or Gradle 7.0+)
- **OpenCV**: 4.8.1 (automatically included via dependencies)

### Installation

#### Maven

Add the Spring Vision starter to your `pom.xml`:

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle

Add the Spring Vision starter to your `build.gradle`:

```gradle
implementation 'com.springvision:spring-vision-starter:1.0.0-SNAPSHOT'
```

### System Requirements

- **Memory**: Minimum 2GB RAM, recommended 4GB+
- **Storage**: 100MB for OpenCV libraries
- **CPU**: Multi-core recommended for better performance
- **OS**: Windows, macOS, or Linux

## Quick Start

### 1. Add Dependency

Add the Spring Vision starter to your project as shown in the installation section.

### 2. Configure Application

Create `application.yml` in your `src/main/resources`:

```yaml
vision:
  enabled: true
  backend: opencv
  opencv:
    confidence-threshold: 0.8
  health:
    enabled: true
  metrics:
    enabled: true
```

### 3. Use Vision Template

Inject and use the `VisionTemplate` in your components:

```java
@Component
public class ImageService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public void processImage(byte[] imageData) {
        VisionResult result = visionTemplate.detectFaces(imageData);
        
        if (result.hasDetections()) {
            result.detections().forEach(detection -> {
                System.out.println("Face detected with confidence: " + detection.confidence());
            });
        }
    }
}
```

### 4. Run Your Application

Start your Spring Boot application:

```bash
mvn spring-boot:run
```

The Spring Vision framework will be automatically configured and ready to use.

## Configuration

### Basic Configuration

```yaml
vision:
  enabled: true                    # Enable/disable vision framework
  backend: opencv                  # Backend to use (opencv, mediapipe, yolo)
```

### OpenCV Configuration

```yaml
vision:
  opencv:
    enabled: true                  # Enable OpenCV backend
    confidence-threshold: 0.8      # Minimum confidence for detections
    min-face-size-ratio: 0.1      # Minimum face size as ratio of image
    max-face-size-ratio: 0.8      # Maximum face size as ratio of image
    gpu-acceleration: false        # Enable GPU acceleration (if available)
    max-image-size: 10485760      # Maximum image size in bytes (10MB)
```

### Health Monitoring

```yaml
vision:
  health:
    enabled: true                  # Enable health monitoring
    check-interval: 30000          # Health check interval in milliseconds
    max-response-time: 5000        # Maximum response time for health checks
```

### Metrics Collection

```yaml
vision:
  metrics:
    enabled: true                  # Enable metrics collection
    collection-interval: 60000     # Metrics collection interval in milliseconds
    detailed: false                # Enable detailed metrics
```

### Environment Variables

You can also configure using environment variables:

```bash
export VISION_ENABLED=true
export VISION_BACKEND=opencv
export VISION_OPENCV_CONFIDENCE_THRESHOLD=0.8
export VISION_HEALTH_ENABLED=true
export VISION_METRICS_ENABLED=true
```

## Usage Examples

### Face Detection

```java
@Service
public class FaceDetectionService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<FaceDetection> detectFaces(byte[] imageData) {
        VisionResult result = visionTemplate.detectFaces(imageData);
        
        return result.detections().stream()
            .map(detection -> new FaceDetection(
                detection.confidence(),
                detection.boundingBox()
            ))
            .collect(Collectors.toList());
    }
    
    public boolean hasFaces(byte[] imageData) {
        VisionResult result = visionTemplate.detectFaces(imageData);
        return result.hasDetections();
    }
    
    public int countFaces(byte[] imageData) {
        VisionResult result = visionTemplate.detectFaces(imageData);
        return result.detectionCount();
    }
}
```

### Object Detection

```java
@Service
public class ObjectDetectionService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<ObjectDetection> detectObjects(byte[] imageData) {
        VisionResult result = visionTemplate.detectObjects(imageData);
        
        return result.detections().stream()
            .map(detection -> new ObjectDetection(
                detection.label(),
                detection.confidence(),
                detection.boundingBox()
            ))
            .collect(Collectors.toList());
    }
    
    public List<ObjectDetection> detectObjectsByType(byte[] imageData, String objectType) {
        VisionResult result = visionTemplate.detectObjects(imageData);
        
        return result.detections().stream()
            .filter(detection -> objectType.equals(detection.label()))
            .map(detection -> new ObjectDetection(
                detection.label(),
                detection.confidence(),
                detection.boundingBox()
            ))
            .collect(Collectors.toList());
    }
}
```

### Multiple Detection Types

```java
@Service
public class MultiDetectionService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public DetectionResults detectAll(byte[] imageData) {
        List<DetectionType> types = List.of(DetectionType.FACE, DetectionType.OBJECT);
        List<VisionResult> results = visionTemplate.detectMultiple(imageData, types);
        
        return new DetectionResults(
            results.get(0).detections(), // Face detections
            results.get(1).detections()  // Object detections
        );
    }
    
    public Map<DetectionType, List<Detection>> detectByType(byte[] imageData) {
        List<DetectionType> types = List.of(DetectionType.FACE, DetectionType.OBJECT);
        List<VisionResult> results = visionTemplate.detectMultiple(imageData, types);
        
        Map<DetectionType, List<Detection>> detectionsByType = new HashMap<>();
        for (int i = 0; i < types.size(); i++) {
            detectionsByType.put(types.get(i), results.get(i).detections());
        }
        
        return detectionsByType;
    }
}
```

### Health Monitoring

```java
@Component
public class HealthCheckService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public void checkHealth() {
        if (visionTemplate.isBackendHealthy()) {
            var healthInfo = visionTemplate.getBackendHealthInfo();
            System.out.println("Backend is healthy: " + healthInfo.statusMessage());
            System.out.println("Response time: " + healthInfo.responseTimeMs() + "ms");
        } else {
            System.out.println("Backend is unhealthy!");
        }
    }
    
    public BackendHealthInfo getHealthInfo() {
        return visionTemplate.getBackendHealthInfo();
    }
    
    public boolean isHealthy() {
        return visionTemplate.isBackendHealthy();
    }
}
```

### REST API Usage

If you're using the REST API, you can make HTTP requests:

```java
@Service
public class VisionApiService {
    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:8080/api/vision";
    
    public VisionApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public DetectionResponse detectFaces(byte[] imageData) {
        DetectionRequest request = DetectionRequest.builder()
            .imageData(imageData)
            .build();
        
        return restTemplate.postForObject(
            baseUrl + "/detect/faces",
            request,
            DetectionResponse.class
        );
    }
    
    public HealthResponse getHealth() {
        return restTemplate.getForObject(
            baseUrl + "/health",
            HealthResponse.class
        );
    }
}
```

## Advanced Features

### Custom Backend Configuration

```java
@Configuration
public class CustomVisionConfiguration {
    @Bean
    @Primary
    public VisionBackend customVisionBackend() {
        return new CustomVisionBackend();
    }
    
    @Bean
    public VisionProperties customVisionProperties() {
        VisionProperties properties = new VisionProperties();
        properties.setBackend("custom");
        properties.getOpencv().setConfidenceThreshold(0.9);
        return properties;
    }
}
```

### Custom Health Indicators

```java
@Component
public class CustomVisionHealthIndicator implements HealthIndicator {
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Override
    public Health health() {
        if (visionTemplate.isBackendHealthy()) {
            return Health.up()
                .withDetail("backend", visionTemplate.getBackendId())
                .withDetail("version", visionTemplate.getBackendVersion())
                .build();
        } else {
            return Health.down()
                .withDetail("backend", visionTemplate.getBackendId())
                .withDetail("error", "Backend is unhealthy")
                .build();
        }
    }
}
```

### Custom Metrics

```java
@Component
public class CustomVisionMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter detectionCounter;
    private final Timer processingTimer;
    
    public CustomVisionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.detectionCounter = Counter.builder("vision.detections.custom")
            .description("Custom detection counter")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("vision.processing.custom")
            .description("Custom processing timer")
            .register(meterRegistry);
    }
    
    public void recordDetection(String type) {
        detectionCounter.increment();
    }
    
    public Timer.Sample startProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void stopProcessing(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}
```

### Image Processing Pipeline

```java
@Service
public class ImageProcessingPipeline {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public ProcessingResult processImage(byte[] imageData) {
        // Step 1: Validate image
        if (!isValidImage(imageData)) {
            throw new IllegalArgumentException("Invalid image data");
        }
        
        // Step 2: Detect faces
        VisionResult faceResult = visionTemplate.detectFaces(imageData);
        
        // Step 3: Detect objects
        VisionResult objectResult = visionTemplate.detectObjects(imageData);
        
        // Step 4: Analyze results
        return analyzeResults(faceResult, objectResult);
    }
    
    private boolean isValidImage(byte[] imageData) {
        return imageData != null && imageData.length > 0 && imageData.length <= 10 * 1024 * 1024;
    }
    
    private ProcessingResult analyzeResults(VisionResult faceResult, VisionResult objectResult) {
        return ProcessingResult.builder()
            .faceCount(faceResult.detectionCount())
            .objectCount(objectResult.detectionCount())
            .averageConfidence((faceResult.averageConfidence() + objectResult.averageConfidence()) / 2)
            .processingTime(faceResult.processingTimeMs() + objectResult.processingTimeMs())
            .build();
    }
}
```

## Troubleshooting

### Common Issues

#### 1. OpenCV Not Available

**Error**: `OpenCV is not available on this system`

**Solution**: 
- Ensure OpenCV dependencies are included in your project
- Check that native libraries are available for your platform
- Verify JavaCV is properly configured

#### 2. Memory Issues

**Error**: `OutOfMemoryError` during image processing

**Solution**:
- Increase JVM heap size: `java -Xmx4g -jar your-application.jar`
- Reduce image size before processing
- Process images in batches

#### 3. Slow Performance

**Symptoms**: Slow face detection or object detection

**Solutions**:
- Reduce image resolution before processing
- Adjust confidence thresholds
- Enable GPU acceleration (if available)
- Use a more powerful machine

#### 4. No Detections Found

**Symptoms**: No faces or objects detected in images

**Solutions**:
- Check image quality and lighting
- Verify image contains detectable objects
- Lower confidence threshold
- Try different detection types

### Debug Mode

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    com.springvision: DEBUG
    org.bytedeco: DEBUG
    org.springframework.web: DEBUG
```

### Health Checks

Monitor backend health:

```bash
curl http://localhost:8080/actuator/health/vision
```

### Metrics

Check performance metrics:

```bash
curl http://localhost:8080/actuator/metrics/vision.detections.total
curl http://localhost:8080/actuator/metrics/vision.processing.time
```

## Best Practices

### Performance

1. **Image Size**: Keep images under 5MB for optimal performance
2. **Image Format**: Use JPEG for photos, PNG for graphics with transparency
3. **Resolution**: Higher resolution doesn't always improve detection accuracy
4. **Batch Processing**: Use multiple detection endpoint for multiple types
5. **Caching**: Cache results for repeated images

### Error Handling

1. **Always check for errors** in detection results
2. **Implement retry logic** for transient failures
3. **Log correlation IDs** for debugging
4. **Handle edge cases** like empty images or invalid formats

### Security

1. **Validate image files** before processing
2. **Sanitize file names** and paths
3. **Implement authentication** for production
4. **Use HTTPS** in production environments
5. **Limit file uploads** to prevent abuse

### Monitoring

1. **Track processing times** and throughput
2. **Monitor detection accuracy** and confidence scores
3. **Set up alerts** for backend health issues
4. **Log correlation IDs** for request tracing
5. **Monitor memory usage** and garbage collection

## Performance Tuning

### JVM Settings

```bash
# Increase heap size
java -Xmx4g -Xms2g -jar your-application.jar

# Enable G1GC for better performance
java -XX:+UseG1GC -jar your-application.jar

# Enable parallel processing
java -XX:+UseParallelGC -jar your-application.jar
```

### Application Settings

```yaml
# Optimize for performance
vision:
  opencv:
    gpu-acceleration: true
    max-image-size: 5242880  # 5MB
    confidence-threshold: 0.7

# Optimize Spring Boot
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

### Thread Pool Configuration

```java
@Configuration
public class ThreadPoolConfiguration {
    @Bean
    public ExecutorService visionExecutor() {
        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }
}
```

## Security Considerations

### Input Validation

```java
@Component
public class ImageValidator {
    public void validateImage(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        if (imageData.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds maximum limit");
        }
        
        // Validate image format
        if (!isValidImageFormat(imageData)) {
            throw new IllegalArgumentException("Invalid image format");
        }
    }
    
    private boolean isValidImageFormat(byte[] imageData) {
        // Check for common image format signatures
        return imageData.length >= 4 && (
            (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) || // JPEG
            (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && imageData[2] == 0x4E && imageData[3] == 0x47) || // PNG
            (imageData[0] == 0x47 && imageData[1] == 0x49 && imageData[2] == 0x46) // GIF
        );
    }
}
```

### Authentication and Authorization

```java
@RestController
@RequestMapping("/api/vision")
public class SecureVisionController {
    @PostMapping("/detect/faces")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DetectionResponse> detectFaces(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        // Log user activity
        logger.info("User {} requested face detection", authentication.getName());
        
        // Process image
        // ...
    }
}
```

## Monitoring and Logging

### Structured Logging

```java
@Component
public class VisionLogger {
    private static final Logger logger = LoggerFactory.getLogger(VisionLogger.class);
    
    public void logDetection(String correlationId, DetectionType type, int count, long processingTime) {
        logger.info("Detection completed", Map.of(
            "correlationId", correlationId,
            "detectionType", type.getCode(),
            "detectionCount", count,
            "processingTimeMs", processingTime
        ));
    }
    
    public void logError(String correlationId, String error, Throwable exception) {
        logger.error("Detection failed", Map.of(
            "correlationId", correlationId,
            "error", error
        ), exception);
    }
}
```

### Custom Metrics

```java
@Component
public class CustomMetrics {
    private final MeterRegistry meterRegistry;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordDetection(String type, double confidence) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        // Record detection
        Counter.builder("vision.detections")
            .tag("type", type)
            .register(meterRegistry)
            .increment();
        
        // Record confidence
        Gauge.builder("vision.confidence")
            .tag("type", type)
            .register(meterRegistry, () -> confidence);
        
        sample.stop(Timer.builder("vision.processing.time")
            .tag("type", type)
            .register(meterRegistry));
    }
}
```

### Health Monitoring

```java
@Component
public class VisionHealthMonitor {
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void checkHealth() {
        if (!visionTemplate.isBackendHealthy()) {
            // Send alert
            alertService.sendAlert("Vision backend is unhealthy");
        }
    }
}
```

This user guide provides comprehensive information for using the Spring Vision framework effectively in your applications. For additional support, refer to the API documentation and troubleshooting sections. 
