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
    <version>1.0</version>
</dependency>
```

## 2) Minimal Configuration

application.yml:

```yaml
spring:
  vision:
    enabled: true
    backend: opencv # or mediapipe, yolo, facebytes, etc.
```

## 3) Use the API

```java

@RestController
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect-faces")
    public List<Detection> detectFaces(@RequestParam("file") MultipartFile file) throws IOException {
        return visionTemplate.detectFaces(file.getBytes());
    }
}
```

## Optional: GPU Build

- Build with GPU: `mvn clean install -P gpu`
- Configure: `spring.vision.execution-provider=gpu`
- Details: [GPU Acceleration](../configuration/gpu.md)

## Next Steps

- Explore backends and features: [Modules Overview](../architecture/modules.md)
- Tune properties: [Configuration & Properties](../configuration/config.md)
- Understand runtime ops: [Runtime & Operations](../configuration/runtime.md)

