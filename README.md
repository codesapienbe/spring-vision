<div align="center">
  <a href="https://github.com/spring-vision/spring-vision">
    <img src="https://raw.githubusercontent.com/spring-vision/spring-vision/main/assets/logo.png" alt="Spring Vision Logo" width="200">
  </a>
  <h1 align="center">Spring Vision</h1>
  <p align="center">
    <strong>The Ultimate Computer Vision Framework for Spring Boot Applications</strong>
    <br />
    <br />
    <a href="https://github.com/spring-vision/spring-vision/actions/workflows/build.yml">
      <img src="https://img.shields.io/github/actions/workflow/status/spring-vision/spring-vision/build.yml?branch=main&style=for-the-badge&logo=github" alt="Build Status">
    </a>
    <a href="https://search.maven.org/artifact/com.springvision/spring-vision-starter">
      <img src="https://img.shields.io/maven-central/v/com.springvision/spring-vision-starter.svg?style=for-the-badge&logo=apache-maven" alt="Maven Central">
    </a>
    <a href="https://github.com/spring-vision/spring-vision/blob/main/LICENSE">
      <img src="https://img.shields.io/github/license/spring-vision/spring-vision?style=for-the-badge" alt="License">
    </a>
    <a href="https://github.com/spring-vision/spring-vision/stargazers">
      <img src="https://img.shields.io/github/stars/spring-vision/spring-vision?style=for-the-badge&logo=github" alt="GitHub stars">
    </a>
    <a href="https://github.com/spring-vision/spring-vision/network/members">
      <img src="https://img.shields.io/github/forks/spring-vision/spring-vision?style=for-the-badge&logo=github" alt="GitHub forks">
    </a>
  </p>
</div>

**Spring Vision** is a production-ready computer vision framework that brings powerful AI capabilities to your Spring Boot applications. Detect faces, recognize objects, analyze emotions, and build intelligent applications with just a few lines of code.

## ✨ Why Spring Vision?

- **🚀 Zero-Configuration Setup**: Get started in minutes with auto-configuration and sensible defaults.
- **🎨 Production-Ready Features**: Enterprise security, real-time monitoring, and high performance for production workloads.
- **🤖 DJL-Powered**: Built on Deep Java Library for modern AI model management and inference.
- **🔌 Pluggable Backends**: DJL (default), OpenCV, FaceBytes, YOLO, MediaPipe support.
- **🌐 Cross-Platform**: Works on Linux, macOS, and Windows.
- **🏗️ Spring Boot Native**: Health checks, metrics, and async support with virtual threads.

## 🚀 Getting Started

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>1.0.5</version>
</dependency>
```

### 2. Use in Your Code

```java
@RestController
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect-faces")
    public List<Detection> detectFaces(@RequestParam("file") MultipartFile file) {
        return visionTemplate.detectFaces(file.getBytes());
    }
}
```

That's it! Your Spring Boot application now has state-of-the-art computer vision capabilities powered by DJL.

## ⚙️ Configuration

### Default Configuration (CPU)

Spring Vision works out of the box with DJL and PyTorch:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: PyTorch
      confidence-threshold: 0.5
```

### GPU Acceleration

For GPU acceleration, use the GPU profile:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: PyTorch
      confidence-threshold: 0.5
      
spring:
  profiles:
    active: gpu
```

### Legacy Backends

You can still use legacy backends if needed:

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

## 🚀 DJL Backend Features

- **Automatic Model Management**: DJL downloads and caches models automatically
- **Multi-Engine Support**: PyTorch, ONNX Runtime, TensorFlow
- **GPU Acceleration**: CUDA support for faster inference
- **Model Zoo Integration**: Access to pre-trained models from DJL model zoo
- **Memory Efficient**: Optimized memory usage and cleanup

## Modules

Spring Vision is a modular framework that allows you to plug in different computer vision backends and features.

### Face Recognition Modules

- **CompreFace**: Face detection, recognition, and analysis using the CompreFace REST API.
- **DeepFace**: Face recognition and analysis using the DeepFace library.
- **FaceBytes**: A lightweight, native face detection and recognition engine.
- **InsightFace**: High-accuracy face recognition using the InsightFace library.

### Computer Vision Modules

- **MediaPipe**: A framework for building multimodal applied machine learning pipelines.
- **YOLO**: Real-time object detection using the YOLO (You Only Look Once) model.
- **Tesseract**: OCR (Optical Character Recognition) for extracting text from images.

### Specialized Modules

- **Cyber**: A module for detecting cyber threats, such as QR code phishing and shoulder surfing.
- **Health**: A module for health-related computer vision tasks, such as fall detection and vital sign monitoring.

### Infrastructure Modules

- **Persistence**: A module for persisting computer vision data, such as face embeddings and detection results.
- **Starter**: A starter module that provides auto-configuration and a unified API for all backends.

## Architecture

Spring Vision is designed with a layered architecture that separates the application layer from the computer vision backends.

- **Application Layer**: This layer includes the REST API, web UI, and CLI.
- **Framework Core**: This layer provides the core functionality of the framework, such as the `VisionTemplate`, configuration, and utilities.
- **Backends**: This layer includes the different computer vision backends, such as OpenCV, YOLO, and MediaPipe.
- **Integrations**: This layer includes integrations with other systems, such as persistence, health monitoring, and robotics.

## GPU Acceleration

Spring Vision supports GPU acceleration for faster inference. To enable GPU acceleration, you need to:

1.  **Build with the `gpu` profile**: `mvn clean install -P gpu`
2.  **Configure the `execution-provider`**: `spring.vision.execution-provider=gpu`

For more information, see the [GPU Acceleration](docs/gpu.md) documentation.

## Contributing

We welcome contributions! Please see the [Contributing](docs/contributing.md) guide for more information.
