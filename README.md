
<div align="center">
  <a href="https://github.com/spring-vision/spring-vision">
    <img src="https://raw.githubusercontent.com/spring-vision/spring-vision/main/docs/images/spring-vision-logo.png" alt="Spring Vision Logo" width="200">
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
- **🔌 Pluggable Backends**: Switch between OpenCV, FaceBytes, YOLO, MediaPipe, and more.
- **🌐 Cross-Platform**: Works on Linux, macOS, and Windows.
- **🏗️ Spring Boot Native**: Health checks, metrics, and async support with virtual threads.

## 🚀 Getting Started

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0</version>
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

That's it! Your Spring Boot application now has state-of-the-art computer vision capabilities.

## 🎨 Features

- **🎯 Face Recognition & Verification**: Verify identities and extract face embeddings.
- **🎯 Object Detection & Recognition**: Detect and recognize objects in images.
- **🔒 Privacy Protection**: Automatically blur faces for privacy.
- **🎯 Batch Processing**: Process thousands of images efficiently.

## 🔌 Supported Backends

| Backend | Capabilities | Performance | Use Case |
|---|---|---|---|
| **🎯 FaceBytes** | Face recognition, verification, analysis | ⭐⭐⭐⭐⭐ | Production face recognition |
| **⚡ OpenCV** | Face detection, object detection | ⭐⭐⭐⭐⭐ | High-performance detection |
| **🎯 YOLO** | Real-time object detection | ⭐⭐⭐⭐⭐ | Object recognition |
| **🎯 MediaPipe** | Face, hand, pose detection | ⭐⭐⭐⭐ | Multi-modal detection |
| **☁️ CompreFace** | Enterprise face recognition | ⭐⭐⭐ | Cloud-based recognition |
| **🧠 DeepFace** | Advanced deep learning | ⭐⭐⭐ | Research & development |
| **🔍 InsightFace** | State-of-the-art recognition | ⭐⭐⭐⭐⭐ | High-accuracy recognition |

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Vision Framework                    │
├─────────────────────────────────────────────────────────────┤
│  🎨 Application Layer (Web UI, REST API, CLI/Desktop)         │
├─────────────────────────────────────────────────────────────┤
│  🧠 Framework Core (VisionTemplate)                         │
├─────────────────────────────────────────────────────────────┤
│  🎯 Backend Implementations (FaceBytes, OpenCV, YOLO, etc.)   │
└─────────────────────────────────────────────────────────────┘
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](docs/CONTRIBUTING.md) for more details.

### Areas Needing Contribution

- **🔌 New Backends**: MediaPipe, YOLO, InsightFace integrations.
- **⚡ Performance**: Optimization and benchmarking.
- **📖 Documentation**: Tutorials, examples, and guides.
- **🧪 Testing**: Integration tests and example improvements.
- **🔒 Security**: Security audits and improvements.

## 🗺️ Roadmap

### 🚀 Version 1.1 (Q2 2024)

- ✅ **MediaPipe Backend**: Google's ML framework integration.
- ✅ **YOLO Backend**: Real-time object detection.
- ⚡ **Performance Optimizations**: Enhanced throughput and latency.
- 🔒 **Enhanced Security**: Advanced security features.

### 🎯 Version 1.2 (Q3 2024)

- ✅ **InsightFace Backend**: State-of-the-art face recognition.
- ✅ **Batch Processing**: High-throughput image processing.
- ☁️ **Cloud Deployment**: Kubernetes and cloud-native guides.
- ✅ **Advanced Metrics**: Enhanced monitoring and observability.

## 💡 Planned Features

### ❤️ `spring-vision-health`

A new module focused on health-related computer vision tasks is under active development.

-   **Real-time Heart Rate Monitoring**: Implement real-time heart rate monitoring from a video source.
-   **Brain Tumor Classification**: Develop a deep learning model for brain tumor classification from MRI scans (glioma, meningioma, pituitary, no tumor) using the BRISC 2025 dataset.
-   **Fall Detection**: Create a module for detecting falls from video streams, aimed at monitoring elderly or at-risk individuals.
-   **Stress Level Analysis**: Implement a feature to analyze stress levels based on facial expressions and other physiological signals from video.

### 🔒 `spring-vision-cyber`

This upcoming module will focus on applying computer vision to cybersecurity challenges.

-   **Visual QR Code Hijacking Detection**: Detect and flag suspicious QR codes that may lead to malicious websites.
-   **Shoulder Surfing Prevention**: Analyze video streams to detect and alert when someone is looking over a user's shoulder at a screen.
-   **Physical Access Monitoring**: Use face recognition to monitor and log access to secure areas.

### 🤖 `spring-vision-robotics`

This module will be dedicated to industrial automation and robotics.

-   **Automated Defect Detection**: Implement models to identify defects in products on a production line from a video feed.
-   **Robotic Arm Guidance**: Provide visual input to guide robotic arms for pick-and-place operations.
-   **Component Verification**: Verify that the correct components are used during assembly.

## 💬 Community

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/spring-vision/spring-vision/issues)
- **🎯 Feature Requests**: [GitHub Discussions](https://github.com/spring-vision/spring-vision/discussions)
- **❓ Questions**: [Stack Overflow](https://stackoverflow.com/questions/tagged/spring-vision)

## 🙏 Acknowledgments

Spring Vision builds upon excellent open-source projects like OpenCV, Spring Boot, and more. See the full list in our [documentation](docs/ACKNOWLEDGMENTS.md).

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
