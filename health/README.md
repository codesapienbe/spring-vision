# Spring Vision Health Module

This module provides health-related computer vision capabilities for the Spring Vision framework. It includes APIs for heart rate monitoring, fall detection, stress analysis, and medical image classification.

## Overview

The **Spring Vision Health** module is designed around image-based APIs rather than video streams, making it easy to integrate with standard computer vision workflows. All health detectors work with sequences of `ImageData` objects and return `Detection` results compatible with the core framework.

## Key Features

- **Heart Rate Detection**: PPG-based heart rate monitoring from facial video frames
- **Fall Detection**: Real-time fall event detection from sequential frames
- **Stress Analysis**: Stress level assessment from facial expressions
- **Brain Tumor Classification**: MRI image analysis for tumor detection
- **Extensible Architecture**: Easy to add new health detection backends

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>health</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the Health module
spring.vision.health.enabled=true
# Model path for health models
spring.vision.health.model-path=~/.spring-vision/models/health
# Confidence threshold
spring.vision.health.confidence-threshold=0.7
# Enable specific features
spring.vision.health.heart-rate.enabled=true
spring.vision.health.fall-detection.enabled=true
spring.vision.health.stress-analysis.enabled=true
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/health")
public class HealthMonitoringController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/heart-rate")
    public ResponseEntity<HeartRateResult> detectHeartRate(
            @RequestParam("frames") MultipartFile[] frames) throws IOException {

        List<ImageData> imageFrames = Arrays.stream(frames)
                .map(file -> {
                    try {
                        return ImageData.fromBytes(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Detection> heartRateDetections = visionTemplate.detectHeartRate(imageFrames);

        // Aggregate results
        double avgBpm = heartRateDetections.stream()
                .mapToDouble(d -> (Double) d.attributes().get("bpm"))
                .average()
                .orElse(0.0);

        return ResponseEntity.ok(new HeartRateResult(avgBpm, heartRateDetections));
    }

    @PostMapping("/fall-detection")
    public ResponseEntity<List<Detection>> detectFalls(
            @RequestParam("frames") MultipartFile[] frames) throws IOException {

        List<ImageData> imageFrames = Arrays.stream(frames)
                .map(file -> {
                    try {
                        return ImageData.fromBytes(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Detection> fallEvents = visionTemplate.detectFall(imageFrames);

        return ResponseEntity.ok(fallEvents);
    }

    @PostMapping("/stress-analysis")
    public ResponseEntity<StressResult> analyzeStress(
            @RequestParam("frames") MultipartFile[] frames) throws IOException {

        List<ImageData> imageFrames = Arrays.stream(frames)
                .map(file -> {
                    try {
                        return ImageData.fromBytes(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        List<Detection> stressDetections = visionTemplate.detectStress(imageFrames);

        // Analyze stress level
        double avgStress = stressDetections.stream()
                .mapToDouble(d -> (Double) d.attributes().get("stress_level"))
                .average()
                .orElse(0.0);

        String level = avgStress < 0.3 ? "LOW" : avgStress < 0.7 ? "MEDIUM" : "HIGH";

        return ResponseEntity.ok(new StressResult(avgStress, level, stressDetections));
    }
}
```

## Capabilities

The module implements the following capability interfaces from `spring-vision-core`:

- **HeartRateCapability** - `detectHeartRate(List<ImageData>)`
- **FallDetectionCapability** - `detectFall(List<ImageData>)`
- **StressAnalysisCapability** - `detectStress(List<ImageData>)`
- **BrainTumorClassifier** - `classifyTumor(MRIImage)`

## Configuration Properties

| Property                                       | Type    | Default                          | Description                 |
|------------------------------------------------|---------|----------------------------------|-----------------------------|
| `spring.vision.health.enabled`                 | boolean | false                            | Enable/disable the module   |
| `spring.vision.health.model-path`              | String  | `~/.spring-vision/models/health` | Model directory path        |
| `spring.vision.health.confidence-threshold`    | double  | 0.7                              | Detection confidence        |
| `spring.vision.health.heart-rate.enabled`      | boolean | true                             | Enable heart rate detection |
| `spring.vision.health.fall-detection.enabled`  | boolean | true                             | Enable fall detection       |
| `spring.vision.health.stress-analysis.enabled` | boolean | true                             | Enable stress analysis      |

## Use Cases

### Heart Rate Monitoring

Monitor heart rate from facial video using photoplethysmography (PPG):

```java
List<ImageData> faceFrames = captureVideoFrames(30); // 30 frames
List<Detection> heartRate = visionTemplate.detectHeartRate(faceFrames);
```

### Fall Detection

Detect falls in elderly care or workplace safety:

```java
List<ImageData> activityFrames = captureVideoFrames(60); // 2 seconds @ 30fps
List<Detection> falls = visionTemplate.detectFall(activityFrames);

if(!falls.

isEmpty()){

alertEmergencyServices();
}
```

### Stress Analysis

Assess stress levels from facial expressions:

```java
List<ImageData> faceFrames = captureVideoFrames(90); // 3 seconds
List<Detection> stress = visionTemplate.detectStress(faceFrames);
```

### MRI Tumor Classification

Classify brain tumors from MRI images:

```java
MRIImage mriScan = MRIImage.fromFile("scan.dcm");
TumorClassificationResult result = brainTumorClassifier.classify(mriScan);

if(result.

getTumorType() !=TumorType.NO_TUMOR){

notifyPhysician(result);
}
```

## Architecture

This module follows a design-first approach:

1. **Core APIs**: Defined in `spring-vision-core.capabilities`
2. **Implementation**: Pluggable backends (OpenCV, ONNX, DJL, etc.)
3. **Integration**: Through `VisionTemplate` for consistent usage

## Implementation Guidelines

To create a custom health backend:

1. Implement the relevant capability interface
2. Register as a Spring component
3. Configure via properties

```java

@Component
@ConditionalOnProperty(prefix = "spring.vision.health", name = "enabled")
public class CustomHeartRateBackend implements HeartRateCapability {

    @Override
    public List<Detection> detectHeartRate(List<ImageData> frames) {
        // Your implementation
        return detections;
    }
}
```

## Requirements

- Java 21+
- Spring Boot 3.2+
- OpenCV (optional, for certain backends)
- ONNX Runtime (optional, for ML models)

## Important Notes

### Deprecated APIs

Previous versions exposed streaming/video APIs (`VideoSource`, `HeartRateListener`). These are now deprecated. New code should use:

- `List<ImageData>` for inputs
- `List<Detection>` for outputs
- `VisionTemplate` for convenience methods

### Medical Use Disclaimer

⚠️ **Important**: This module is for research and educational purposes only. It is NOT intended for medical diagnosis or clinical use. Always consult qualified healthcare professionals for medical advice.

## Examples

See the `examples/` directory for complete sample applications demonstrating health monitoring features.

## Contributing

We welcome contributions! Suggested areas:

1. OpenCV-based PPG heart rate detector
2. ONNX-based fall detection model
3. Stress analysis using facial landmarks
4. Medical image preprocessing utilities

## License

See main project LICENSE file.
