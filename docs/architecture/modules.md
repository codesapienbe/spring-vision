[Docs Home](../index.md) · [Getting Started](../getting-started/start.md) · [Architecture](./architecture.md) · [Config](../configuration/config.md)

# Spring Vision Architecture & Capabilities

**Version**: 0.0.4
**Last Updated**: October 21, 2025

## Overview

Spring Vision uses a modern, capability-based architecture built on the Deep Java Library (DJL). Instead of separate modules, functionality is organized around **detection capabilities** that can be mixed and matched as needed.

## Current Architecture

Spring Vision 0.0.1 consists of these main components:

### Core Components

1. **spring-vision-core** - Main framework with VisionTemplate and capabilities
2. **spring-vision-starter** - Auto-configuration and REST API
3. **spring-vision-mcp** - MCP server integration

### Quick Start (Single Dependency)

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

This gives you:
- ✅ REST API endpoints (`/api/vision/*`)
- ✅ Auto-configuration
- ✅ All detection capabilities
- ✅ DJL backend integration
- ✅ **Bundled Models**: YOLO, RetinaFace, and utility libraries included in JAR

### Configuration

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch  # or tensorflow, onnx
      device: cpu      # or gpu
      confidence-threshold: 0.5
```
## Detection Capabilities

Spring Vision provides these built-in detection capabilities:

### ✅ **Production Ready (Models Included in JAR)**
- **Face Detection** - High-accuracy RetinaFace model (`detectFaces()`)
- **Object Detection** - YOLOv8 models (`detectObjects()`)
- **Pose Estimation** - YOLOv8 pose models (`detectPoses()`)
- **Barcode Scanning** - ZXing library (`scanBarcodes()`)
- **Metadata Extraction** - EXIF/GPS data (`extractMetadata()`)
- **Image Annotation** - Drawing utilities (`annotate()`)

### 🤖 **AI-Powered (Runtime Download)**
- **Text Detection (OCR)** - DJL OCR models (`extractText()`)
- **Image Classification** - ResNet/Inception (`classifyImage()`)
- **Segmentation** - Instance/Semantic models (`segmentImage()`)
- **Action Recognition** - Activity detection (`recognizeActions()`)
- **Face Embeddings** - Recognition vectors (`extractEmbeddings()`)

### 🔒 **Security & Safety**
- **NSFW Detection** - Content filtering (`detectNSFW()`)
- **Deepfake Detection** - AI-generated media (`detectDeepfake()`)
- **Threat Detection** - Weapons/objects (`detectThreats()`)
- **Biometric Authentication** - Face-based auth (`authenticateAccess()`)

### ❤️ **Health & Wellness**
- **Fall Detection** - Pose-based analysis (`detectFall()`)
- **Stress Analysis** - Emotion-based detection (`analyzeStress()`)
- **Heart Rate** - rPPG from faces (`detectHeartRate()`)
- **Demographics** - Age/gender estimation (`detectDemographics()`)

### 🚧 **Placeholder Implementations**
- **Hand Detection** - Uses object detection (`detectHands()`)
- **Emotion Detection** - Mock implementation (`detectEmotions()`)

### 📊 **Utilities**
- **Face Embeddings** - Generate face vectors (`extractEmbeddings()`)
- **Face Counting** - Count faces (`countFaces()`)

## Usage Example

```java
@RestController
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("file") MultipartFile file) {
        ImageData imageData = ImageData.fromBytes(file.getBytes());

        // Use multiple capabilities
        VisionResult faces = visionTemplate.detectFaces(imageData);
        VisionResult objects = visionTemplate.detectObjects(imageData);
        VisionResult emotions = visionTemplate.detectEmotions(imageData);

        return ResponseEntity.ok(Map.of(
            "faces", faces.detections(),
            "objects", objects.detections(),
            "emotions", emotions.detections()
        ));
    }
}
```

## Advanced Configuration

Fine-tune individual capabilities:

```yaml
spring:
  vision:
    djl:
      # Global settings
      confidence-threshold: 0.5
      max-concurrent-inferences: 16

      # Face detection settings
      face-detection:
        model: mtcnn  # retinaface, mtcnn, lightface
        confidence-threshold: 0.7
        max-faces: 200

      # Object detection settings
      object-detection:
        model: ssd  # ssd, yolo
        backbone: resnet50
        confidence-threshold: 0.6
        top-k: 10
```
```

## Backend Compatibility

Spring Vision maintains backward compatibility with legacy backends:

### Legacy Backend Configuration

```yaml
spring:
  vision:
    djl:
      enabled: false  # Disable DJL
    opencv:
      enabled: true
    facebytes:
      enabled: true
```

**Note:** Legacy backends are deprecated. DJL is recommended for all new projects.

## Migration from Legacy Backends

If you're migrating from legacy backends:

1. **Update configuration** to use DJL settings
2. **Replace backend-specific code** with VisionTemplate calls
3. **Test thoroughly** - DJL models may have different behavior
4. **Update dependencies** to latest versions

See [Configuration Guide](../configuration/config.md) for detailed migration steps.

---

## Summary

Spring Vision 0.0.1 provides a modern, capability-based architecture with:

- **🎯 Single dependency** for all computer vision features
- **🚀 Zero-configuration** setup with intelligent defaults
- **🔧 Capability-based API** for maximum flexibility
- **🤖 DJL-powered** AI models with automatic management
- **🔄 Backward compatibility** with legacy backends

**Ready to build?** → [Getting Started Guide](../getting-started/start.md)

**Need the full API?** → [API Reference](../development/API_USAGE.md)

**Common Capabilities:**

- Face detection
- Face recognition
- Face verification
- Face analysis (age, gender, emotion)

**Standard Configuration Pattern:**

```properties
spring.vision.{module}.enabled=true
spring.vision.{module}.confidence-threshold=0.7
spring.vision.{module}.max-detections=10
# For API-based modules (compreface, deepface, insightface)
spring.vision.{module}.base-url=http://localhost:PORT
spring.vision.{module}.api-key=your-key
spring.vision.{module}.timeout-seconds=30
spring.vision.{module}.max-retries=3
# For native modules (facebytes)
spring.vision.{module}.model-path=~/.spring-vision/models/{module}
spring.vision.{module}.detector-backend=opencv
spring.vision.{module}.recognition-model=VGG-Face
spring.vision.{module}.enable-auto-download=true
```

**Usage Example:**

```java

@RestController
@RequestMapping("/api/faces")
public class FaceController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);

        return ResponseEntity.ok(detections);
    }
}
```

### Category 2: Computer Vision Modules

**Modules:** mediapipe, yolo, tesseract

**Standard Configuration Pattern:**

```properties
spring.vision.{module}.enabled=true
spring.vision.{module}.model-path=~/.spring-vision/models/{module}
spring.vision.{module}.confidence-threshold=0.7
spring.vision.{module}.max-detections=10
spring.vision.{module}.enable-auto-download=true
spring.vision.{module}.download-timeout-seconds=300
```

**Usage Example:**

```java

@Service
public class VisionService {

    @Autowired
    private VisionTemplate visionTemplate;

    // Object detection
    public List<Detection> detectObjects(ImageData image) {
        return visionTemplate.detectObjects(image);
    }

    // Text extraction
    public List<Detection> extractText(ImageData image) {
        return visionTemplate.detect(image, DetectionType.TEXT);
    }
}
```

### Category 3: Specialized Modules

**Modules:** cyber, health

**Standard Configuration Pattern:**

```properties
spring.vision.{module}.enabled=true
spring.vision.{module}.confidence-threshold=0.7
# Feature-specific settings
spring.vision.{module}.{feature}.enabled=true
spring.vision.{module}.{feature}.sensitivity=0.8
spring.vision.{module}.{feature}.{property}=value
```

**Example: Cyber Security Module**

```properties
spring.vision.cyber.enabled=true
spring.vision.cyber.qr-code.sensitivity=0.7
spring.vision.cyber.qr-code.validate-urls=true
spring.vision.cyber.shoulder-surfing.enabled=true
spring.vision.cyber.shoulder-surfing.sensitivity=0.8
```

**Usage Example:**

```java

@Service
public class SecurityService {

    @Autowired
    private VisionTemplate visionTemplate;

    public List<Detection> detectThreats(List<ImageData> images) {
        return visionTemplate.detectThreat(images);
    }

    public List<Detection> detectEavesdropping(List<ImageData> frames) {
        return visionTemplate.detectEavesdropping(frames);
    }
}
```

### Category 4: Infrastructure Modules

**Modules:** persistence, starter

**Persistence Configuration:**

```properties
spring.vision.vector.enabled=true
spring.vision.vector.provider=postgres
spring.datasource.url=jdbc:postgresql://localhost:5432/springvision
spring.datasource.username=postgres
spring.datasource.password=password
```

**Starter Usage:**

```xml
<!-- Include starter for zero-config setup -->
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>starter</artifactId>
    <version>1.0</version>
</dependency>
```

## README Template

Every module README MUST follow this structure:

```markdown
# Spring Vision {Module Name}

Brief description of what the module does.

## Features

- Bullet list of key features
- What makes this module unique
- Supported capabilities

## Getting Started

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>{module}</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
spring.vision.{module}.enabled=true
# List all key properties with defaults
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
public class ExampleController {

    @Autowired
    private VisionTemplate visionTemplate;

    // Show realistic example
}
```

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| ...      | ...  | ...     | ...         |

## Usage Examples

### Example 1: Basic Usage

(Code example)

### Example 2: Advanced Usage

(Code example)

## Performance

- Metrics about speed/accuracy
- Benchmark results if available

## Requirements

- Java 21+
- Spring Boot 3.2+
- Module-specific requirements

## Troubleshooting

### Common Issue 1

Problem description and solution

## Best Practices

1. List of recommendations
2. Tips for optimal usage

## License

See main project LICENSE file.

```

## Configuration Property Standards

### Naming Conventions

✅ **DO:**
```properties
spring.vision.module.feature-name=value
spring.vision.module.sub-feature.property-name=value
spring.vision.module.enable-auto-download=true
spring.vision.module.max-detections=10
```

❌ **DON'T:**

```properties
springvision.module.featureName=value  # Wrong prefix, camelCase
vision.module.feature=value            # Missing "spring"
spring.module.feature=value            # Missing "vision"
```

### Common Property Names

Use these standard names across all modules:

- `enabled` - Enable/disable the module
- `confidence-threshold` - Detection confidence (0.0-1.0)
- `max-detections` - Maximum number of detections
- `model-path` - Path to model files
- `base-url` - API endpoint URL
- `api-key` - API authentication key
- `timeout-seconds` - Request timeout
- `max-retries` - Maximum retry attempts
- `enable-auto-download` - Auto-download models

### Value Types

- Booleans: `true` or `false`
- Numbers: Plain integers or decimals
- Strings: No quotes needed
- Lists: Comma-separated values
- Paths: Use ~ for home directory

## Code Example Standards

### ✅ Good Example

```java

@RestController
@RequestMapping("/api/faces")
public class FaceController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);

        return ResponseEntity.ok(detections);
    }
}
```

**Why it's good:**

- Uses VisionTemplate (not direct backend)
- Realistic REST endpoint
- Proper error handling
- Shows auto-configuration in action

## Testing Standards

Every module SHOULD include:

1. **Unit Tests**: Test individual components
2. **Integration Tests**: Test with VisionTemplate
3. **Configuration Tests**: Test property binding
4. **Example Tests**: Verify README examples work

## Documentation Checklist

When adding or updating a module:

- [ ] README follows the template
- [ ] Maven dependency uses correct groupId
- [ ] All properties start with `spring.vision.{module}`
- [ ] Examples use VisionTemplate (not direct backend)
- [ ] Configuration table is complete
- [ ] Usage examples are realistic
- [ ] Troubleshooting section exists
- [ ] Performance metrics included
- [ ] Requirements clearly stated

## Migration Guide for Existing Modules

If updating an old module:

1. **Update README**
    - Follow the template above
    - Fix groupId if incorrect
    - Replace backend examples with VisionTemplate

2. **Update Configuration**
    - Ensure properties use `spring.vision.{module}` prefix
    - Add `enabled` property if missing
    - Standardize property names

3. **Update Examples**
    - Replace direct backend usage with VisionTemplate
    - Show auto-configuration
    - Add realistic REST controller examples

4. **Test**
    - Verify all examples work
    - Test property binding
    - Confirm VisionTemplate integration

## Review Checklist

Before marking a module as "aligned":

- [ ] README exists and follows template
- [ ] GroupId is `io.github.codesapienbe.springvision`
- [ ] Properties follow `spring.vision.{module}.*` pattern
- [ ] Examples use VisionTemplate exclusively
- [ ] Configuration table is accurate
- [ ] Module works with starter
- [ ] Auto-configuration is functional
- [ ] Tests pass

## Maintaining Consistency

### For Module Maintainers

1. **Follow this guide** when creating new modules
2. **Review PRs** for consistency violations
3. **Update examples** if core APIs change
4. **Keep README in sync** with actual capabilities

### For Contributors

1. **Read this guide** before contributing
2. **Match the pattern** of existing aligned modules
3. **Ask questions** if something is unclear
4. **Test examples** before submitting

## Success Metrics

A well-aligned module should enable:

- ⚡ **< 5 minutes** from dependency to first detection
- 📝 **3 steps** maximum to get started
- 🎯 **Zero** direct backend dependencies in examples
- 🔧 **Single** configuration file for all modules
- 📚 **Consistent** documentation across all modules

---

See also: [Getting Started](./start.md) · [Configuration](./config.md) · [Runtime](./runtime.md) · [Roadmap](./roadmap.md)

**Status**: This guide is the source of truth for module alignment.

**Questions?** Review MODULE_ALIGNMENT_SUMMARY.md for current status of all modules.
