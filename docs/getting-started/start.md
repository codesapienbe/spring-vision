# Getting Started

[Docs Home](../index.md) · [Architecture](../architecture/architecture.md) · [Modules](../architecture/modules.md) · [Config](../configuration/config.md)

This guide helps you go from zero to your first detection in minutes.

## Prerequisites

- Java 21+
- Maven 3.9+
- Spring Boot 3.2+
- Optional: NVIDIA GPU + CUDA for acceleration (see [GPU Acceleration](./gpu.md))

## 1) Add Dependency

Add the starter to your Spring Boot app:

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

## 2) Configuration

Spring Vision now uses DJL (Deep Java Library) as the primary backend with intelligent auto-configuration. Add this to your `application.yml`:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch
      device: cpu  # or gpu for GPU acceleration
      confidence-threshold: 0.5
```

For GPU acceleration, use the `gpu` profile:

```yaml
spring:
  config:
    activate:
      on-profile: gpu
  vision:
    djl:
      device: gpu
```

Legacy backends are still supported:

```yaml
spring:
  vision:
    djl:
      enabled: false
    opencv:
      enabled: true
    facebytes:
      enabled: true
```

## 3) Use the API

Inject the `VisionTemplate` and start using computer vision features:

```java
@RestController
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect-faces")
    public ResponseEntity<Map<String, Object>> detectFaces(@RequestParam("file") MultipartFile file) {
        try {
            VisionResult result = visionTemplate.detectFaces(ImageData.fromBytes(file.getBytes()));
            return ResponseEntity.ok(Map.of(
                "faces", result.detections(),
                "count", result.detectionCount(),
                "confidence", result.averageConfidence()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analyze-image")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("file") MultipartFile file) {
        try {
            // Advanced analysis combining multiple capabilities
            VisionResult faces = visionTemplate.detectFaces(ImageData.fromBytes(file.getBytes()));
            VisionResult objects = visionTemplate.detectObjects(ImageData.fromBytes(file.getBytes()));
            VisionResult emotions = visionTemplate.detectEmotions(ImageData.fromBytes(file.getBytes()));

            return ResponseEntity.ok(Map.of(
                "faces", faces.detections(),
                "objects", objects.detections(),
                "emotions", emotions.detections(),
                "totalDetections", faces.detectionCount() + objects.detectionCount() + emotions.detectionCount()
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

## 4) Advanced Configuration

Customize model settings in `application.yml`:

```yaml
spring:
  vision:
    djl:
      face-detection:
        model: mtcnn  # or retinaface for better accuracy
        confidence-threshold: 0.7
      object-detection:
        model: ssd
        backbone: resnet50
        confidence-threshold: 0.6
```

## 5) GPU Acceleration (Optional)

For GPU acceleration, use the `gpu` profile:

```bash
# Build with GPU support
mvn clean package -P gpu

# Run with GPU profile
java -jar target/your-app.jar --spring.profiles.active=gpu
```

Or configure in `application.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: gpu
  vision:
    djl:
      device: gpu
```

**Details:** [GPU Acceleration Guide](../configuration/gpu.md)

## Next Steps

- Explore all capabilities: [API Reference](../development/API_USAGE.md)
- Understand architecture: [Modules Overview](../architecture/modules.md)
- Fine-tune configuration: [Configuration Guide](../configuration/config.md)
- Production deployment: [Runtime Operations](../configuration/runtime.md)
- Learn about models: [Models Reference](../architecture/models.md)

