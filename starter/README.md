# Spring Vision Starter

Spring Boot starter for the Spring Vision framework, providing easy integration of computer vision capabilities into Spring Boot applications.

## Quick Start

### Maven

Add the starter dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>1.0</version>
</dependency>
```

### Gradle

Add the starter dependency to your `build.gradle`:

```gradle
implementation 'io.github.codesapienbe.springvision:spring-vision-starter:1.0'
```

### Usage

Simply inject the `VisionTemplate` into your components:

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
                System.out.println("Bounding box: " + detection.boundingBox());
            });
        }
    }
}
```

## Features

### Automatic Configuration

The starter automatically configures:

- **Vision Backend**: OpenCV backend with face and object detection
- **Vision Template**: Unified interface for all vision operations
- **Health Monitoring**: Spring Boot Actuator health indicators
- **Metrics Collection**: Micrometer-based metrics for monitoring
- **Lifecycle Management**: Proper initialization and cleanup

### Supported Operations

- **Face Detection**: Detect human faces in images
- **Object Detection**: Detect various objects using OpenCV algorithms
- **Multiple Detection Types**: Process multiple detection types in a single call
- **Health Monitoring**: Monitor backend health and performance
- **Metrics Collection**: Track detection counts, processing times, and error rates

### Configuration

Configure the framework using application properties:

```yaml
vision:
  enabled: true
  backend: opencv
  opencv:
    enabled: true
    confidence-threshold: 0.8
    min-face-size-ratio: 0.1
    max-face-size-ratio: 0.8
    gpu-acceleration: false
    max-image-size: 10485760  # 10MB
  health:
    enabled: true
    check-interval: 30000     # 30 seconds
    max-response-time: 5000   # 5 seconds
  metrics:
    enabled: true
    collection-interval: 60000 # 1 minute
    detailed: false
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

## Health Monitoring

Health monitoring is automatically configured and available at:

- `/actuator/health` - Overall application health
- `/actuator/health/vision` - Vision backend health

Example health response:

```json
{
  "status": "UP",
  "components": {
    "vision": {
      "status": "UP",
      "details": {
        "backend": "opencv",
        "backendName": "OpenCV Vision Backend",
        "status": "HEALTHY",
        "responseTime": 45,
        "supportedDetectionTypes": 2
      }
    }
  }
}
```

## Metrics

Metrics are automatically collected and available at:

- `/actuator/metrics` - All available metrics
- `/actuator/metrics/vision.detections.total` - Total detection count
- `/actuator/metrics/vision.detections.face` - Face detection count
- `/actuator/metrics/vision.detections.object` - Object detection count
- `/actuator/metrics/vision.processing.time` - Processing time histograms
- `/actuator/metrics/vision.errors.total` - Error count
- `/actuator/metrics/vision.backend.health` - Backend health status

## Examples

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
        } else {
            System.out.println("Backend is unhealthy!");
        }
    }
}
```

## Sample Application

A complete sample application is included in the test package. To run it:

```bash
mvn spring-boot:run -pl spring-vision-starter
```

The sample application demonstrates:

- Face detection on sample images
- Object detection capabilities
- Multiple detection types
- Health monitoring
- Metrics collection

## Testing

Comprehensive tests are included to verify:

- Auto-configuration works correctly
- Dependencies are properly included
- Configuration properties are respected
- Health monitoring and metrics work
- Error handling is robust

Run the tests:

```bash
mvn test -pl spring-vision-starter
```

## Customization

### Custom Backend

Provide your own backend implementation:

```java

@Configuration
public class CustomVisionConfiguration {
    @Bean
    @Primary
    public VisionBackend customVisionBackend() {
        return new CustomVisionBackend();
    }
}
```

### Custom Configuration

Override default configuration:

```java

@Configuration
public class CustomVisionConfiguration {
    @Bean
    public VisionProperties customVisionProperties() {
        VisionProperties properties = new VisionProperties();
        properties.setBackend("custom");
        properties.getOpencv().setConfidenceThreshold(0.9);
        return properties;
    }
}
```

### Exclude Auto-Configuration

Exclude specific auto-configuration classes:

```java

@SpringBootApplication(exclude = {
        VisionAutoConfiguration.class
})
public class MyApplication {
    // Custom configuration...
}
```

## Dependencies

The starter includes:

- **Spring Vision Core** - Core framework components
- **Spring Vision Autoconfigure** - Auto-configuration classes
- **Spring Boot Starter** - Spring Boot integration
- **Spring Boot Actuator** - Health monitoring and metrics
- **Micrometer Core** - Metrics collection
- **OpenCV** - Computer vision library (4.8.1)
- **JavaCV** - Java bindings for OpenCV (1.5.9)

## Requirements

- **Java**: 21 or later
- **Spring Boot**: 3.0 or later
- **Maven**: 3.6 or later (or Gradle 7.0+)

## Troubleshooting

### Common Issues

#### OpenCV Not Available

**Error**: `OpenCV is not available on this system`

**Solution**: Ensure OpenCV is properly installed. The starter includes OpenCV dependencies, but native libraries must be available.

#### Memory Issues

**Error**: `OutOfMemoryError` during image processing

**Solution**: Increase JVM heap size:

```bash
java -Xmx4g -jar your-application.jar
```

#### Performance Issues

**Symptoms**: Slow face detection

**Solutions**:

- Reduce image size before processing
- Adjust confidence thresholds
- Enable GPU acceleration (if available)
- Use a more powerful machine

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.springvision: DEBUG
    org.bytedeco: DEBUG
```

### Health Checks

Monitor backend health:

```bash
curl http://localhost:8080/actuator/health/vision
```

## Contributing

See the main project [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](../LICENSE) file for details.

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting)
2. Review the [main project documentation](../README.md)
3. Open an issue on GitHub
4. Check the [OpenCV documentation](https://docs.opencv.org/)

## Version Compatibility

| Spring Vision | Spring Boot | Java | OpenCV | JavaCV |
|---------------|-------------|------|--------|--------|
| 1.0           | 3.0+        | 21+  | 4.8.1  | 1.5.9  |
