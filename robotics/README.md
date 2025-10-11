# Spring Vision Robotics Module

The **Spring Vision Robotics** module provides specialized computer vision capabilities for industrial automation and robotics applications. It enables automated defect detection, robotic arm guidance, and component verification in manufacturing and assembly processes.

## Features

### 🔍 Automated Defect Detection

Identify defects in products on production lines from video feeds:

- Surface defects (scratches, dents, cracks)
- Dimensional defects (misalignments, incorrect sizes)
- Color variations and finish issues
- Missing components or features

### 🤖 Robotic Arm Guidance

Provide visual input to guide robotic arms for pick-and-place operations:

- 3D position coordinates of target objects
- Orientation angles (roll, pitch, yaw)
- Recommended grasp points for robotic grippers
- Confidence scores for position estimates

### ✅ Component Verification

Verify correct component usage during assembly processes:

- Component type and model identification
- Part number verification
- Component orientation checks
- Placement correctness validation

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>robotics</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
spring.vision.robotics.enabled=true
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/robotics")
public class RoboticsController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect-defects")
    public ResponseEntity<List<Detection>> detectDefects(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> defects = visionTemplate.detect(imageData, DetectionType.DEFECT);

        return ResponseEntity.ok(defects);
    }

    @PostMapping("/robotic-guidance")
    public ResponseEntity<List<Detection>> provideGuidance(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> guidance = visionTemplate.detect(imageData, DetectionType.ROBOTIC_GUIDANCE);

        return ResponseEntity.ok(guidance);
    }

    @PostMapping("/verify-components")
    public ResponseEntity<List<Detection>> verifyComponents(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> verifications = visionTemplate.detect(imageData, DetectionType.COMPONENT_VERIFICATION);

        return ResponseEntity.ok(verifications);
    }
}
```

## Configuration Properties

| Property                                                                 | Type    | Default | Description                                |
|--------------------------------------------------------------------------|---------|---------|--------------------------------------------|
| `spring.vision.robotics.enabled`                                         | boolean | `false` | Enable/disable the robotics backend        |
| `spring.vision.robotics.defect-detection.sensitivity`                    | double  | `0.7`   | Sensitivity for defect detection (0.0-1.0) |
| `spring.vision.robotics.defect-detection.min-defect-size`                | int     | `50`    | Minimum defect size in pixels              |
| `spring.vision.robotics.defect-detection.max-defects-per-image`          | int     | `100`   | Maximum defects to report per image        |
| `spring.vision.robotics.robotic-guidance.focal-length`                   | double  | `800.0` | Camera focal length in pixels              |
| `spring.vision.robotics.robotic-guidance.sensor-width`                   | double  | `6.0`   | Camera sensor width in millimeters         |
| `spring.vision.robotics.robotic-guidance.use-stereo-vision`              | boolean | `false` | Use stereo vision for depth estimation     |
| `spring.vision.robotics.robotic-guidance.confidence-threshold`           | double  | `0.6`   | Confidence threshold for guidance          |
| `spring.vision.robotics.component-verification.strict-mode`              | boolean | `false` | Enable strict verification mode            |
| `spring.vision.robotics.component-verification.identification-threshold` | double  | `0.7`   | Confidence threshold for component ID      |
| `spring.vision.robotics.component-verification.component-database-path`  | string  | -       | Path to component database (optional)      |

## Usage Examples

### Example 1: Basic Defect Detection

```java

@Service
public class QualityControlService {

    @Autowired
    private VisionTemplate visionTemplate;

    public List<Detection> inspectProduct(byte[] productImage) {
        ImageData imageData = ImageData.fromBytes(productImage);
        return visionTemplate.detect(imageData, DetectionType.DEFECT);
    }

    public boolean hasDefects(byte[] productImage) {
        List<Detection> defects = inspectProduct(productImage);
        return !defects.isEmpty();
    }
}
```

### Example 2: Robotic Arm Guidance for Pick-and-Place

```java

@Service
public class RobotControlService {

    @Autowired
    private VisionTemplate visionTemplate;

    public RobotCommand generatePickCommand(byte[] workspaceImage) {
        ImageData imageData = ImageData.fromBytes(workspaceImage);
        List<Detection> targets = visionTemplate.detect(imageData, DetectionType.ROBOTIC_GUIDANCE);

        if (targets.isEmpty()) {
            throw new NoTargetFoundException("No objects detected for pick operation");
        }

        Detection target = targets.get(0); // Get first/best target
        Map<String, Object> attributes = target.attributes();

        @SuppressWarnings("unchecked")
        Map<String, Double> position = (Map<String, Double>) attributes.get("targetPosition");
        @SuppressWarnings("unchecked")
        Map<String, Double> orientation = (Map<String, Double>) attributes.get("orientation");

        return new RobotCommand(
                position.get("x"),
                position.get("y"),
                position.get("z"),
                orientation.get("yaw")
        );
    }
}
```

### Example 3: Assembly Line Component Verification

```java

@Service
public class AssemblyVerificationService {

    @Autowired
    private VisionTemplate visionTemplate;

    public VerificationReport verifyAssemblyStep(byte[] assemblyImage) {
        ImageData imageData = ImageData.fromBytes(assemblyImage);
        List<Detection> components = visionTemplate.detect(imageData, DetectionType.COMPONENT_VERIFICATION);

        VerificationReport report = new VerificationReport();

        for (Detection component : components) {
            Map<String, Object> attrs = component.attributes();
            boolean verified = (boolean) attrs.get("verified");
            String componentId = (String) attrs.get("componentId");

            if (verified) {
                report.addVerified(componentId);
            } else {
                String expected = (String) attrs.get("expectedComponent");
                report.addMismatch(componentId, expected);
            }
        }

        return report;
    }
}
```

### Example 4: Complete Configuration

```properties
# Enable robotics backend
spring.vision.robotics.enabled=true
# Defect Detection Settings
spring.vision.robotics.defect-detection.sensitivity=0.8
spring.vision.robotics.defect-detection.min-defect-size=30
spring.vision.robotics.defect-detection.max-defects-per-image=50
# Robotic Guidance Settings (calibrated for your camera)
spring.vision.robotics.robotic-guidance.focal-length=850.0
spring.vision.robotics.robotic-guidance.sensor-width=6.4
spring.vision.robotics.robotic-guidance.use-stereo-vision=true
spring.vision.robotics.robotic-guidance.confidence-threshold=0.75
# Component Verification Settings
spring.vision.robotics.component-verification.strict-mode=true
spring.vision.robotics.component-verification.identification-threshold=0.8
spring.vision.robotics.component-verification.component-database-path=/opt/assembly/components.db
```

## Detection Results

### Defect Detection Response

```json
{
  "label": "defect",
  "confidence": 0.85,
  "boundingBox": {
    "x": 120,
    "y": 80,
    "width": 45,
    "height": 30
  },
  "attributes": {
    "defectType": "scratch",
    "severity": "major",
    "relativeSize": 0.015,
    "aspectRatio": 1.5,
    "area": 1350.0
  }
}
```

### Robotic Guidance Response

```json
{
  "label": "robotic_target",
  "confidence": 0.92,
  "boundingBox": {
    "x": 200,
    "y": 150,
    "width": 60,
    "height": 60
  },
  "attributes": {
    "targetPosition": {
      "x": 125.5,
      "y": 78.3,
      "z": 450.0
    },
    "orientation": {
      "roll": 0.0,
      "pitch": 0.0,
      "yaw": 15.5
    },
    "graspPoints": [
      {
        "x": 230.0,
        "y": 180.0,
        "priority": 1.0
      },
      {
        "x": 200.0,
        "y": 180.0,
        "priority": 0.7
      },
      {
        "x": 260.0,
        "y": 180.0,
        "priority": 0.7
      }
    ],
    "estimatedDistance": 450.0,
    "objectSize": {
      "width": 60,
      "height": 60
    }
  }
}
```

### Component Verification Response

```json
{
  "label": "component",
  "confidence": 0.88,
  "boundingBox": {
    "x": 100,
    "y": 100,
    "width": 50,
    "height": 25
  },
  "attributes": {
    "componentId": "COMP-001",
    "partNumber": "COMP-001",
    "componentName": "Rectangular Large",
    "verified": true,
    "verificationStatus": "PASS",
    "dimensions": {
      "width": 50,
      "height": 25
    },
    "aspectRatio": 2.0,
    "circularity": 0.65
  }
}
```

## Use Cases

### Quality Control

- Automated inspection of manufactured products
- Real-time defect detection on production lines
- Quality assurance reporting and analytics
- Defect classification and severity assessment

### Assembly Automation

- Verify correct parts are used during assembly
- Ensure proper component orientation
- Track assembly progress and completion
- Detect missing or incorrect components

### Pick-and-Place Operations

- Guide robotic arms to target objects
- Calculate optimal grasp points
- Provide real-time position feedback
- Support multiple target prioritization

## Performance

- **Defect Detection**: ~50-100ms per image (CPU), ~10-20ms (GPU)
- **Robotic Guidance**: ~80-150ms per image (includes 3D calculations)
- **Component Verification**: ~60-120ms per image (depends on database size)

*Note: Performance varies based on image size, hardware, and configuration settings*

## Architecture

The Robotics module follows the Spring Vision backend architecture:

```
RoboticsBackend (implements VisionBackend)
├── DefectDetectionCapability
├── RoboticGuidanceCapability
└── ComponentVerificationCapability
    ├── DefectDetector
    ├── RoboticArmGuidance
    └── ComponentVerifier
```

All capabilities are accessible through the unified `VisionTemplate` API.

## Requirements

- Java 21+
- Spring Boot 3.2+
- OpenCV (via JavaCV) - automatically included
- Apache Commons Math3 - automatically included

## Troubleshooting

### Issue: No detections returned

**Problem**: `visionTemplate.detect()` returns empty list

**Solution**:

1. Verify module is enabled: `spring.vision.robotics.enabled=true`
2. Check image quality and resolution
3. Adjust sensitivity settings
4. Verify the correct DetectionType is used

### Issue: Low confidence scores

**Problem**: Detections have low confidence values

**Solution**:

1. Improve lighting conditions
2. Increase image resolution
3. Calibrate camera parameters
4. Adjust detection thresholds in configuration

### Issue: Robotic guidance position inaccurate

**Problem**: 3D position estimates are incorrect

**Solution**:

1. Calibrate camera focal length: `spring.vision.robotics.robotic-guidance.focal-length`
2. Set correct sensor width: `spring.vision.robotics.robotic-guidance.sensor-width`
3. Enable stereo vision if available: `spring.vision.robotics.robotic-guidance.use-stereo-vision=true`
4. Verify camera mounting and orientation

## Best Practices

1. **Camera Calibration**: Always calibrate camera parameters for accurate robotic guidance
2. **Lighting**: Ensure consistent, adequate lighting for defect detection
3. **Image Quality**: Use high-resolution images for better detection accuracy
4. **Threshold Tuning**: Adjust sensitivity and confidence thresholds based on your use case
5. **Component Database**: Maintain an up-to-date component database for verification
6. **Testing**: Test with representative samples before production deployment
7. **Monitoring**: Log detection confidence scores and review false positives/negatives

## License

This module is part of Spring Vision and is licensed under the MIT License.
