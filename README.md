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

## 📖 Documentation

For comprehensive documentation, including API references, architecture diagrams, and guides, please visit our [**documentation portal**](docs/index.md).

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
    <artifactId>starter</artifactId>
    <version>1.1</version>
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

## 🚀 GPU Acceleration Support

Spring Vision now supports GPU-accelerated inference using NVIDIA CUDA for significantly improved performance on compatible hardware.

### Building with GPU Support

To build the project with GPU support, use the `gpu` Maven profile:

```bash
# Build with GPU support
mvn clean install -P gpu

# Build specific module with GPU support
cd core
mvn clean install -P gpu
```

### Runtime Configuration

Configure the execution provider in your `application.properties`:

```properties
# For CPU execution (default)
vision.execution-provider=cpu
# For GPU execution (requires CUDA drivers and GPU build)
vision.execution-provider=gpu
```

### Requirements for GPU Acceleration

To use GPU acceleration, you need:

1. **NVIDIA GPU**: CUDA-compatible GPU (Compute Capability 3.5 or higher)
2. **CUDA Toolkit**: Version 11.x or 12.x installed
3. **cuDNN**: Compatible version installed
4. **GPU Build**: Project built with `-P gpu` profile

### Performance Benefits

GPU acceleration can provide significant performance improvements:

- **Face Detection**: Up to 5-10x faster
- **Object Recognition**: Up to 8-15x faster
- **Batch Processing**: Up to 20x faster for large batches

### Automatic Fallback

If GPU acceleration fails to initialize (e.g., missing drivers or incompatible hardware), Spring Vision will automatically fall back to CPU execution with a warning logged:

```
WARN: Failed to configure CUDA execution provider. Falling back to CPU.
```

### Example Configuration

```yaml
vision:
  execution-provider: gpu  # or 'cpu' for CPU-only
  backend: opencv
  enabled: true
  fail-fast: true
  opencv:
    enabled: true
    confidence-threshold: 0.7
```

## 🎨 Features

- **🎯 Face Recognition & Verification**: Verify identities and extract face embeddings.
- **🎯 Object Detection & Recognition**: Detect and recognize objects in images.
- **🔒 Privacy Protection**: Automatically blur faces for privacy.
- **🎯 Batch Processing**: Process thousands of images efficiently.

## 🔌 Supported Backends

| Backend                | Capabilities                                                                | Performance | Use Case                    |
|------------------------|-----------------------------------------------------------------------------|-------------|-----------------------------|
| **🎯 FaceBytes**       | Face recognition, verification, analysis                                    | ⭐⭐⭐⭐⭐       | Production face recognition |
| **⚡ OpenCV**           | Face detection, object detection                                            | ⭐⭐⭐⭐⭐       | High-performance detection  |
| **🎯 YOLO**            | Real-time object detection                                                  | ⭐⭐⭐⭐⭐       | Object recognition          |
| **🎯 MediaPipe**       | Face, hand, pose detection                                                  | ⭐⭐⭐⭐        | Multi-modal detection       |
| **☁️ CompreFace**      | Enterprise face recognition                                                 | ⭐⭐⭐         | Cloud-based recognition     |
| **🧠 DeepFace**        | Advanced deep learning                                                      | ⭐⭐⭐         | Research & development      |
| **🔍 InsightFace**     | State-of-the-art recognition                                                | ⭐⭐⭐⭐⭐       | High-accuracy recognition   |
| ❤️ **Health (v2.0)**   | Real-time heart rate, tumor classification, fall detection, stress analysis | TBD         | Healthcare                  |
| 🔒 **Cyber (v2.0)**    | QR code hijacking, shoulder surfing prevention, physical access monitoring  | TBD         | Cybersecurity               |
| 🤖 **Robotics (v2.0)** | Automated defect detection, robotic arm guidance, component verification    | TBD         | Industrial Automation       |

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

- **🔌 New Backends**: We are always looking for new backend integrations.
- **⚡ Performance**: Optimization and benchmarking for existing and new backends.
- **📖 Documentation**: Tutorials, examples, and guides for new features.
- **🧪 Testing**: More integration tests and examples for different use cases.
- **🔒 Security**: Security audits and improvements are always welcome.

## 🗺️ Roadmap

### ✅ **Version 1.1 (Released Q2 2024)**

- **New Backends**: Integrated MediaPipe and YOLO backends for multi-modal and real-time object detection.
- **Performance**: Major performance optimizations, enhancing throughput and latency.
- **Security**: Added advanced security features for enterprise-grade deployments.

### 🚀 **Version 1.2 (Q3 2024)**

- **InsightFace Backend**: Integration of the state-of-the-art face recognition model.
- **Batch Processing**: High-throughput image processing capabilities.
- **Cloud-Native Deployment**: Official guides for Kubernetes and other cloud platforms.
- **Advanced Metrics**: Deeper monitoring and observability into the vision pipelines.

### 🎯 **Version 2.0 (Q4 2024)**

- **New Modules**: Introduction of `spring-vision-health`, `spring-vision-cyber`, and `spring-vision-robotics`.
- **API Overhaul**: A major revision of the API for improved usability, performance, and consistency.
- **Community Focus**: Expanded documentation, more examples, and streamlined contribution process.

## 💡 Planned Features (v2.0)

### ❤️ `spring-vision-health`

A new module focused on health-related computer vision tasks.

- **Real-time Heart Rate Monitoring**: Monitor heart rate from a video source.
- **Brain Tumor Classification**: Classify brain tumors from MRI scans (glioma, meningioma, pituitary).
- **Fall Detection**: Detect falls from video streams to monitor at-risk individuals.
- **Stress Level Analysis**: Analyze stress levels from facial expressions and other physiological signals.

### 🔒 `spring-vision-cyber`

This module will apply computer vision to cybersecurity challenges.

- **Visual QR Code Hijacking Detection**: Detect and flag suspicious QR codes.
- **Shoulder Surfing Prevention**: Analyze video streams to detect and alert when someone is looking over a user's shoulder.
- **Physical Access Monitoring**: Use face recognition to monitor and log access to secure areas.

### 🤖 `spring-vision-robotics`

This module will be dedicated to industrial automation and robotics.

- **Automated Defect Detection**: Identify defects in products on a production line from a video feed.
- **Robotic Arm Guidance**: Provide visual input to guide robotic arms for pick-and-place operations.
- **Component Verification**: Verify correct component usage during assembly.

## 💬 Community

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/spring-vision/spring-vision/issues)
- **🎯 Feature Requests**: [GitHub Discussions](https://github.com/spring-vision/spring-vision/discussions)
- **❓ Questions**: [Stack Overflow](https://stackoverflow.com/questions/tagged/spring-vision)

## 🙏 Acknowledgments

Spring Vision builds upon excellent open-source projects like OpenCV, Spring Boot, and more. See the full list in our [documentation](docs/ACKNOWLEDGMENTS.md).

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
