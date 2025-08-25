# Spring Vision 🎯

A comprehensive **computer vision framework** for Spring Boot applications, providing unified APIs for face detection, object recognition, and image analysis across multiple vision backends.

![Java](https://img.shields.io/badge/Java-21+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

```yaml
spring:
  vision:
    backend: opencv
    opencv:
      enabled: true
```

### 3. Use

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

## ✨ Features

### 🔍 **Multi-Backend Architecture**
- **OpenCV** - High-performance native computer vision (default)
- **FaceBytes** - Embedded deep learning models  
- **CompreFace** - External recognition service
- **DeepFace** - Python-based deep learning
- **MediaPipe** - Google's ML framework *(coming soon)*
- **YOLO** - Real-time object detection *(coming soon)*
- **InsightFace** - State-of-the-art face recognition *(coming soon)*

### 🎨 **Rich Detection Capabilities**
- **Face Detection** - Locate faces in images with confidence scores
- **Face Recognition** - Extract embeddings and verify identity
- **Face Analysis** - Age, gender, emotion detection
- **Object Detection** - General object recognition
- **Privacy Protection** - Automatic face blurring/obscuring
- **Batch Processing** - High-throughput image processing

### 🏗️ **Spring Boot Native**
- **Auto-Configuration** - Zero-config setup for most use cases
- **Health Checks** - Monitor backend status via Actuator
- **Metrics Integration** - Micrometer metrics for observability  
- **Security** - Built-in SSRF protection and input validation
- **Async Support** - Non-blocking processing with virtual threads

### 🖥️ **Complete Example Applications**
- **CLI Application** - Command-line interface with PicoCLI
- **Web Application** - Simple file upload interface
- **Vaadin GUI** - Modern web UI with real-time updates
- **JavaFX Desktop** - Cross-platform desktop application
- **REST API** - Production-ready API endpoints

## 📊 **Supported Formats & Platforms**

| Feature | Support |
|---------|---------|
| **Image Formats** | JPEG, PNG, WebP, BMP, TIFF |
| **Input Sources** | File upload, URLs, byte arrays, streams |
| **Platforms** | Linux (x64), macOS (x64/ARM), Windows (x64) |
| **Java Versions** | 21, 22, 23+ |
| **Spring Boot** | 3.2+ |

## 🏃‍♂️ **Getting Started**

### Prerequisites

- **Java 21+** (JDK 21 or higher)
- **Maven 3.8+** or **Gradle 7.0+**
- **Spring Boot 3.2+**

### Installation Options

#### Option 1: Maven Central *(Coming Soon)*

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Option 2: Build from Source

```bash
git clone https://github.com/springvision/spring-vision.git
cd spring-vision
mvn clean install -DskipTests
```

### Try the Examples

```bash
# Test all examples
./test.sh --all

# Test individual components
./test.sh --cli    # CLI application
./test.sh --api    # REST APIs  
./test.sh --web    # Web applications
```

## 📖 **Documentation**

### **Core Documentation**

| Document | Description |
|----------|-------------|
| **[Getting Started Guide](docs/GETTING_STARTED.md)** | Step-by-step tutorial for new users |
| **[API Reference](docs/API_REFERENCE.md)** | Complete API documentation |
| **[Architecture Guide](docs/ARCHITECTURE.md)** | Framework design and internals |
| **[Contributing Guide](docs/CONTRIBUTING.md)** | How to contribute to the project |

### **Backend Integration Guides**

| Backend | Description | Documentation |
|---------|-------------|---------------|
| **OpenCV** | High-performance native computer vision | [OPENCV_SETUP.md](docs/OPENCV_SETUP.md) |
| **CompreFace** | Enterprise-grade recognition service | [COMPREFACE_INTEGRATION.md](docs/COMPREFACE_INTEGRATION.md) |
| **DeepFace** | Advanced deep learning models | [DEEPFACE_INTEGRATION.md](docs/DEEPFACE_INTEGRATION.md) |

### **Example Applications**

| Application | Description | Technologies |
|-------------|-------------|--------------|
| **[CLI Application](spring-vision-examples/picocli-application/README.md)** | Command-line interface | PicoCLI, Maven |
| **[Web Application](spring-vision-examples/basic-face-detection/README.md)** | Simple web UI | Spring MVC, Thymeleaf |
| **[Vaadin GUI](spring-vision-examples/vaadin-application/README.md)** | Modern web interface | Vaadin, WebSockets |
| **[JavaFX Desktop](spring-vision-examples/javafx-application/README.md)** | Desktop application | JavaFX, Spring Boot |
| **[REST API](spring-vision-examples/gwt-application/README.md)** | API for SPAs | Spring Web, JSON |

### **Additional Resources**

| Guide | Description |
|-------|-------------|
| **[Deployment Guide](docs/DEPLOYMENT_GUIDE.md)** | Production deployment strategies |
| **[User Guide](docs/USER_GUIDE.md)** | End-user documentation |

## 🔧 **Configuration**

Spring Vision supports flexible configuration through properties or environment variables. See the **[Getting Started Guide](docs/GETTING_STARTED.md#configuration)** for basic setup and **[API Reference](docs/API_REFERENCE.md#configuration-properties)** for complete options.

### Basic Configuration

```yaml
spring:
  vision:
    backend: opencv                    # Backend selection
    opencv:
      enabled: true
      confidence-threshold: 0.7
    performance:
      max-image-size: 10485760         # 10MB
      thread-pool-size: 10
```

For detailed configuration options, see the [API Reference](docs/API_REFERENCE.md#configuration-properties).

## 🏗️ **Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Vision Framework                    │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │   Web UI    │ │  REST API   │ │      CLI/Desktop       │ │
│  │  (Vaadin/   │ │ (Spring MVC)│ │    (JavaFX/PicoCLI)    │ │
│  │   GWT)      │ │             │ │                        │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Framework Core                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              VisionTemplate                            │ │
│  │         (Main API Entry Point)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               VisionBackend                            │ │
│  │            (Pluggable Backends)                       │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Backend Implementations                                    │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │  OpenCV  │ │FaceBytes │ │ CompreFace │ │  DeepFace   │ │
│  │ (Native) │ │(Embedded)│ │ (External)  │ │ (External)  │ │
│  └──────────┘ └──────────┘ └─────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**Key Components:**

- **VisionTemplate** - Main API entry point and orchestrator
- **VisionBackend** - Pluggable backend interface
- **DetectionQuery** - Flexible query system for multi-category detection
- **ImageData** - Secure image data wrapper with metadata
- **Detection** - Normalized detection results with bounding boxes

See the **[Architecture Guide](docs/ARCHITECTURE.md)** for detailed design information.

## 🔒 **Security**

Spring Vision is built with security as a first-class concern:

- **Input Validation** - Size limits, format validation, memory protection
- **SSRF Protection** - Host validation, protocol restrictions, timeout controls
- **Data Protection** - No sensitive logging, sanitized outputs, secure defaults

For complete security details, see **[Architecture Guide - Security Model](docs/ARCHITECTURE.md#security-model)**.

## 📈 **Performance**

### Benchmarks

| Backend | Images/sec | Latency (avg) | Memory Usage |
|---------|------------|---------------|--------------|
| OpenCV | 50-100 | 20-50ms | Low |
| FaceBytes | 20-40 | 50-100ms | Medium |
| CompreFace | 10-30 | 100-300ms | Low (external) |
| DeepFace | 5-15 | 200-500ms | Low (external) |

*Results may vary based on image size, hardware, and configuration.*

For performance tuning guidance, see **[Architecture Guide - Performance](docs/ARCHITECTURE.md#performance-considerations)**.

## 🔍 **Monitoring & Observability**

Built-in support for monitoring with Spring Boot Actuator and Micrometer:

- **Health Checks** - Backend status monitoring
- **Metrics** - Detection counts, response times, error rates
- **Structured Logging** - JSON logs with correlation IDs

See **[API Reference - Events and Metrics](docs/API_REFERENCE.md#events-and-metrics)** for implementation details.

## 🤝 **Contributing**

We welcome contributions! Please see our **[Contributing Guide](docs/CONTRIBUTING.md)** for:

- Development setup instructions
- Code standards and style guidelines  
- Testing requirements
- Pull request process
- Architecture guidelines for new backends

### Quick Contribution Setup

```bash
# Fork and clone
git clone https://github.com/yourusername/spring-vision.git
cd spring-vision

# Build and test
mvn clean install -DskipTests
./test.sh --all

# Create feature branch and make changes
git checkout -b feature/my-awesome-feature
mvn spotless:apply && ./test.sh --all

# Submit pull request
```

### Areas Needing Contribution

- **New Backends** - MediaPipe, YOLO, InsightFace integrations
- **Performance** - Optimization and benchmarking
- **Documentation** - Tutorials, examples, and guides
- **Testing** - Integration tests and example improvements
- **Security** - Security audits and improvements

## 📋 **Roadmap**

### Version 1.1 *(Q2 2024)*
- MediaPipe backend integration
- YOLO object detection backend  
- Performance optimizations
- Enhanced security features

### Version 1.2 *(Q3 2024)*
- InsightFace backend with high-accuracy recognition
- Batch processing improvements
- Cloud deployment guides
- Advanced metrics and monitoring

### Version 2.0 *(Q4 2024)*
- Remove deprecated APIs
- Enhanced multi-category detection
- Plugin architecture for custom backends
- Performance and memory optimizations

## 🐛 **Issues & Support**

- **🐛 Bug Reports** - [GitHub Issues](https://github.com/springvision/spring-vision/issues)
- **💡 Feature Requests** - [GitHub Discussions](https://github.com/springvision/spring-vision/discussions)
- **❓ Questions** - [Stack Overflow](https://stackoverflow.com/questions/tagged/spring-vision)
- **📖 Documentation** - [Project Wiki](https://github.com/springvision/spring-vision/wiki)

## 🙏 **Acknowledgments**

Spring Vision builds upon excellent open-source projects:

- **[OpenCV](https://opencv.org/)** - Computer vision library
- **[Spring Boot](https://spring.io/projects/spring-boot)** - Application framework
- **[ByteDeco JavaCV](https://github.com/bytedeco/javacv)** - Java bindings for OpenCV
- **[PicoCLI](https://picocli.info/)** - Command-line interface framework
- **[Vaadin](https://vaadin.com/)** - Modern web application framework

## 📄 **License**

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

**Made with ❤️ by the Spring Vision team**

*Empowering developers to build intelligent applications with computer vision*
