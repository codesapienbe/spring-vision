# Quick Start Guide

[Docs Home](../index.md) · [MCP Setup](./mcp-setup.md) · [MCP Testing](./mcp-testing.md) · [API Usage](../development/API_USAGE.md)

This guide helps you go from zero to your first detection in minutes. Whether you want to use Spring Vision as a library in your Spring Boot application or as an MCP server, this guide covers both approaches.

## Prerequisites

- Java 21+
- Maven 3.9+
- Spring Boot 3.2+
- Optional: NVIDIA GPU + CUDA for acceleration (see [GPU Acceleration](../configuration/gpu.md))

## 🚀 Option 1: Use as MCP Server (Recommended)

The easiest way to get started is using Spring Vision as an MCP (Model Context Protocol) server, which works with AI assistants like Claude, Cursor, and other MCP-compatible tools.

### 1. Install with CLI Tool

For the easiest installation experience, run our CLI setup tool directly with JBang:

```bash
# Run the CLI setup tool to automatically download and configure everything
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar
```

That's it! The CLI tool will automatically:
- ✅ Check for JBang installation and guide you if needed
- 📦 Download the latest Spring Vision MCP Server JAR (~983MB)
- 💾 Store it locally in `~/.springvision/` (no re-downloads needed!)
- ℹ️ Show you how to configure your MCP client
- 🚀 Set up everything automatically with no manual steps required!

### 2. Configure Your MCP Client

After running the CLI tool, configure your MCP client (Claude Desktop, VS Code, Cursor, etc.) using the configuration shown by the CLI tool. Here's an example:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/youruser/.springvision/mcp-0.0.4.jar"]
    }
  }
}
```

### 3. Test the Setup

After configuration, restart your MCP client. You should now have access to Spring Vision tools for:
- Face detection and recognition
- Object detection
- Image classification
- Text extraction (OCR)
- Barcode/QR code scanning
- And many more capabilities!

Try asking your AI assistant: *"Count the number of faces in this image: [URL]"*

## 🏗️ Option 2: Use as Spring Boot Library

If you want to build Spring Vision directly into your Spring Boot application, use it as a library dependency.

### 1. Add Repository

Spring Vision artifacts are published to GitHub Packages. Add the repository to your POM:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/codesapienbe/spring-vision</url>
    </repository>
</repositories>
```

### 2. Add Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.4</version>
</dependency>
```

### 3. Basic Configuration

Spring Vision works out of the box with minimal configuration. Add this to your `application.yml`:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch
      device: cpu  # or gpu for GPU acceleration
```

### 4. Use in Your Code

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

### 5. Run and Test

Start your Spring Boot application:

```bash
./mvnw spring-boot:run
```

Test the endpoints:

```bash
# Health check
curl -s http://localhost:8080/actuator/health

# Test face detection (upload an image with faces)
curl -X POST -F "file=@face_image.jpg" http://localhost:8080/api/vision/faces

# Test object detection (upload any image)
curl -X POST -F "file=@any_image.jpg" http://localhost:8080/api/vision/objects
```

## ⚙️ Advanced Configuration

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

## 🌐 GPU Acceleration (Optional)

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

Or build and run with GPU support:

```bash
# Build with GPU support
mvn clean package -P gpu

# Run with GPU profile
java -jar target/your-app.jar --spring.profiles.active=gpu
```

**Details:** [GPU Acceleration Guide](../configuration/gpu.md)

## 🛠️ Manual Build (Developers)

If you want to build from source:

```bash
# Clone the repository
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

# Build with models (downloads YOLO/RetinaFace during build)
mvn clean install -Pdownload-models

# Or use the Makefile (includes model download):
make build  # Downloads and bundles models
make run    # Runs the MCP server
```

## Next Steps

- **[MCP Setup Guide](./mcp-setup.md)** - Detailed MCP server configuration
- **[MCP Testing Guide](./mcp-testing.md)** - Test all MCP tools with examples
- **[API Reference](../development/API_USAGE.md)** - Complete REST API documentation
- **[Architecture](../architecture/architecture.md)** - Understand the framework design
- **[Configuration](../configuration/config.md)** - Fine-tune settings for your environment
