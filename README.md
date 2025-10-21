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

**Spring Vision** is a computer vision framework that brings AI capabilities to your Spring Boot applications. Built on the Deep Java Library (DJL), it provides a simple API for common computer vision tasks like face detection, object recognition, and image analysis.

## ✨ Features

- **🚀 Easy Setup**: Get started quickly with auto-configuration and sensible defaults.
- **🤖 DJL-Powered**: Built on Deep Java Library for modern AI model management.
- **🔌 Multiple Backends**: Support for DJL (PyTorch, ONNX), OpenCV, and other vision libraries.
- **🌐 Cross-Platform**: Works on Linux, macOS, and Windows.
- **🏗️ Spring Boot Integration**: Health checks, metrics, and Spring ecosystem compatibility.

## 🚀 Getting Started

### Quick Install (Recommended for End Users)

For the easiest installation experience, use our automated installer:

```bash
# Download the installer JAR (replace with actual download URL)
curl -L -o spring-vision-installer.jar https://github.com/codesapienbe/spring-vision/releases/download/v0.0.1/spring-vision-installer.jar

# Run the installer
java -jar spring-vision-installer.jar install
```

That's it! The installer will:
- ✅ Check system requirements
- 📦 Install Spring Vision to `~/.springvision/`
- ⚙️ Configure MCP settings for Claude Desktop
- 🚀 Create run scripts

### Manual Installation

Spring Vision includes an MCP (Model Context Protocol) server that provides computer vision tools. You can run it manually with JBang:

```bash
# Clone the repository
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

# Run with JBang (builds and runs automatically)
jbang run.java
```

Or use the Makefile:

```bash
make run
```

### Using as a Library

#### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

#### 2. Use in Your Code

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

### Basic Setup

Spring Vision works out of the box with minimal configuration:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch  # or tensorflow, onnx
      device: cpu      # or gpu for GPU acceleration
```

### GPU Support (Optional)

For GPU acceleration, use:

```yaml
spring:
  config:
    activate:
      on-profile: gpu
  vision:
    djl:
      device: gpu
```

## 🚀 DJL Backend Features

- **Automatic Model Management**: DJL downloads and caches models automatically
- **Multi-Engine Support**: PyTorch, ONNX Runtime, TensorFlow
- **GPU Acceleration**: CUDA support for faster inference (optional)
- **Model Zoo Integration**: Access to pre-trained models from DJL model zoo

## Current Capabilities

Spring Vision 0.0.1 provides these computer vision capabilities:

- **Face Detection** - Detect and locate faces in images
- **Object Detection** - Identify and classify objects
- **Text Recognition** - Extract text using OCR
- **Image Classification** - Classify images into categories
- **Pose Estimation** - Detect human body poses
- **Emotion Analysis** - Recognize facial emotions

More capabilities will be added in future releases.

## Architecture

Spring Vision uses a simple, layered architecture:

- **Application Layer**: REST API for computer vision operations
- **Framework Core**: `VisionTemplate` provides unified API for all vision tasks
- **DJL Backend**: Deep Java Library handles model management and inference
- **Vision Capabilities**: Modular detection and analysis capabilities

## CLI Installer Commands

The Spring Vision installer provides several commands:

```bash
# Install Spring Vision
java -jar spring-vision-installer.jar install

# Check installation status
java -jar spring-vision-installer.jar status

# Update to latest version
java -jar spring-vision-installer.jar update

# Uninstall Spring Vision
java -jar spring-vision-installer.jar uninstall

# Show help
java -jar spring-vision-installer.jar --help
```

## GPU Acceleration (Optional)

For GPU acceleration, use the `gpu` profile:

```bash
mvn clean package -P gpu
java -jar target/your-app.jar --spring.profiles.active=gpu
```

**Note**: GPU support requires NVIDIA CUDA drivers and is optional for development.

## Contributing

We welcome contributions! Please see the [Contributing](docs/contributing.md) guide for more information.
