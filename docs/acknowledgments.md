# Acknowledgments

[Docs Home](./index.md) · [FAQ](./faq.md)

Spring Vision builds upon excellent open-source projects:

- Spring Boot
- OpenCV and bytedeco bindings
- ONNX Runtime
- MediaPipe
- Ultralytics YOLO
- Tesseract OCR

Thanks to all contributors and the open-source community.

# Getting Started

[Docs Home](./index.md) · [Architecture](./architecture.md) · [Modules](./modules.md) · [Config](./config.md)

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
    <groupId>com.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>1.1</version>
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
- Details: [GPU Acceleration](./gpu.md)

## Next Steps

- Explore backends and features: [Modules Overview](./modules.md)
- Tune properties: [Configuration & Properties](./config.md)
- Understand runtime ops: [Runtime & Operations](./runtime.md)

