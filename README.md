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

**Spring Vision** is a computer vision framework that brings AI capabilities to your Spring Boot applications. Built on the Deep Java Library (DJL), it provides a simple API for common computer vision tasks. **Core models (YOLO, RetinaFace) are bundled in the JAR** - no downloads required for production use!

## ✨ Features

- **🚀 Easy Setup**: Get started quickly with auto-configuration and sensible defaults.
- **📦 Self-Contained JARs**: Core AI models (YOLO, RetinaFace) bundled - no downloads needed!
- **🤖 DJL-Powered**: Built on Deep Java Library for modern AI model management.
- **🔌 Multiple Engines**: Support for PyTorch, ONNX Runtime, TensorFlow backends.
- **🌐 Cross-Platform**: Works on Linux, macOS, and Windows.
- **🏗️ Spring Boot Integration**: Health checks, metrics, and Spring ecosystem compatibility.
- **⚡ Production Ready**: 8+ computer vision capabilities ready for deployment.

## 🚀 Getting Started

### Quick Install (Recommended for End Users)

For the easiest installation experience, run our CLI setup tool directly with JBang:

```bash
# Run the CLI setup tool to automatically download and configure everything
jbang https://github.com/codesapienbe/spring-vision/releases/download/v0.0.3/cli-0.0.3.jar
```

That's it! The CLI tool will automatically:
- ✅ Check for JBang installation and guide you if needed
- 📦 Download the latest Spring Vision MCP Server JAR (~983MB)
- 💾 Store it locally in `~/.springvision/` (no re-downloads needed!)
- ℹ️ Show you how to configure your MCP client
- 🚀 Set up everything automatically with no manual steps required!

### Manual Installation

Spring Vision includes an MCP (Model Context Protocol) server with bundled AI models. Build and run manually:

```bash
# Clone the repository
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

# Build with models (downloads YOLO/RetinaFace during build)
mvn clean install -Pdownload-models

# Run the MCP server with JBang
jbang run.java
```

Or use the Makefile (includes model download):

```bash
make build  # Downloads and bundles models
make run    # Runs the server
```

### Using as a Library

#### 1. Add Repository

Spring Vision artifacts are published to GitHub Packages. Add the repository to your POM (no authentication required for public access):

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/codesapienbe/spring-vision</url>
    </repository>
</repositories>
```

#### 2. Add Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

#### 3. Use in Your Code

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

## 🤖 MCP Client Configuration

After running the CLI tool, configure your MCP client (Claude Desktop, VS Code, etc.) to use Spring Vision:

### Using the CLI Tool Output

The CLI tool will show you the exact configuration. Here's an example:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/youruser/.springvision/mcp-0.0.2.jar"]
    }
  }
}
```

### Manual Configuration

1. **Find your MCP config file:**
   - Claude Desktop: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - VS Code: `~/.cursor/mcp.json`
   - Other clients: Check their documentation

2. **Add Spring Vision configuration:**
   ```json
   {
     "mcpServers": {
       "spring-vision": {
         "command": "jbang",
         "args": ["~/.springvision/mcp-0.0.2.jar"]
       }
     }
   }
   ```

3. **Restart your MCP client** to load the new configuration

### Testing the Setup

After configuration, restart your MCP client. You should see Spring Vision tools available with capabilities like:
- Face detection and recognition
- Object detection
- Image classification
- Text extraction (OCR)
- And many more!

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

- **📦 Bundled Core Models**: YOLOv8 and RetinaFace models included in JAR (no downloads needed)
- **🔄 Runtime Downloads**: Advanced AI models download on-demand from DJL Model Zoo
- **⚙️ Multi-Engine Support**: PyTorch, ONNX Runtime, TensorFlow backends
- **🚀 GPU Acceleration**: CUDA support for faster inference (optional)
- **🎯 Production Ready**: 8+ computer vision capabilities with zero-configuration

## Current Capabilities

Spring Vision 0.0.2 provides these computer vision capabilities (all models included in JAR):

### ✅ **Production Ready**
- **Face Detection** - High-accuracy RetinaFace model (included)
- **Object Detection** - YOLOv8 models (included)
- **Pose Estimation** - YOLOv8 pose models (included)
- **Barcode/QR Scanning** - ZXing library (included)
- **Metadata Extraction** - EXIF/GPS/camera data (included)
- **Image Annotation** - Drawing utilities (included)

### 🤖 **AI-Powered (Runtime Download)**
- **Text Recognition (OCR)** - DJL OCR models
- **Image Classification** - ResNet/Inception models
- **Segmentation** - Instance/Semantic segmentation
- **Action Recognition** - Activity detection models
- **Face Embeddings** - Face recognition vectors

### 🛡️ **Security & Safety**
- **NSFW Detection** - Content filtering
- **Deepfake Detection** - AI-generated media detection
- **Threat Detection** - Weapons/object detection
- **Biometric Authentication** - Face-based access control

### ❤️ **Health & Wellness**
- **Fall Detection** - Pose-based fall analysis
- **Stress Analysis** - Emotion-based stress detection
- **Heart Rate** - rPPG analysis from faces
- **Demographics** - Age/gender estimation

## Architecture

Spring Vision uses a modern, capability-based architecture built on the Deep Java Library (DJL):

### Core Components
- **spring-vision-core** - Main framework with VisionTemplate and capabilities
- **spring-vision-starter** - Auto-configuration and REST API
- **spring-vision-mcp** - MCP server integration

### Architecture Layers
- **Application Layer**: REST API endpoints (`/api/vision/*`)
- **Framework Core**: `VisionTemplate` provides unified API for all vision tasks
- **DJL Backend**: Deep Java Library handles model management and inference
- **Vision Capabilities**: Modular detection and analysis capabilities

### Key Features
- **Single Dependency**: One starter brings all computer vision capabilities
- **Model Bundling**: Core models (YOLO, RetinaFace) included in JAR
- **Auto-Configuration**: Zero-configuration setup with intelligent defaults
- **Runtime Downloads**: Advanced models download on-demand

## CLI Setup Commands

The Spring Vision CLI tool provides several options:

```bash
# Basic setup (downloads latest version)
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar

# Force re-download even if JAR exists
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --force

# Show help
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --help

# Show version
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --version

# Disable colors (for CI/CD)
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --no-color

# Verbose output (for debugging)
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --verbose
```

### Managing Your Installation

```bash
# Update to latest version
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.2.jar --force

# Check JAR location
ls -la ~/.springvision/

# Remove downloaded JAR
rm ~/.springvision/mcp-*.jar
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
