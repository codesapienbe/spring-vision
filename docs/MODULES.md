# Spring Vision Module Alignment Guide

**Version**: 1.0  
**Last Updated**: October 10, 2025

## Purpose

This guide ensures all Spring Vision modules follow a **consistent, predictable pattern** for maximum developer productivity and ease of use.

## The Golden Standard: 3-Step Integration

Every module in the Spring Vision ecosystem follows this exact pattern:

### Step 1: Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>{module}</artifactId>
    <version>1.0</version>
</dependency>
```

**Rules:**

- GroupId MUST be `io.github.codesapienbe.springvision`
- ArtifactId MUST follow pattern `{module}`
- Version MUST be specified (no version ranges)

### Step 2: Configure via Properties

```properties
# Enable the module
spring.vision.{module}.enabled=true
# Core settings (common across modules)
spring.vision.{module}.confidence-threshold=0.7
spring.vision.{module}.max-detections=10
# Module-specific settings
spring.vision.{module}.{feature}.{property}=value
```

**Rules:**

- All properties MUST start with `spring.vision.{module}`
- `enabled` property MUST exist (default: false)
- `confidence-threshold` SHOULD exist for detection modules
- Use kebab-case for property names
- Group related settings with sub-prefixes

### Step 3: Use VisionTemplate

```java

@Autowired
private VisionTemplate visionTemplate;

List<Detection> results = visionTemplate.detectFaces(imageData);
```

**Rules:**

- NO direct backend autowiring in examples
- ALL examples MUST use VisionTemplate
- Show autoconfiguration in action
- Demonstrate the unified API

## Module Categories

### Category 1: Face Recognition Modules

**Modules:** compreface, deepface, facebytes, insightface

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

### ❌ Bad Example

```java

@Service
public class FaceService {

    @Autowired
    private CompreFaceBackend backend;  // ❌ Direct backend autowiring

    public void detectFaces(byte[] data) {
        backend.detect(data);  // ❌ No return value shown
    }
}
```

**Why it's bad:**

- Direct backend dependency (defeats VisionTemplate purpose)
- Not showing the unified API
- Incomplete example

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

**Status**: This guide is the source of truth for module alignment.

**Questions?** Review MODULE_ALIGNMENT_SUMMARY.md for current status of all modules.

