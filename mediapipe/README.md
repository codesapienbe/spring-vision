# Spring Vision MediaPipe Backend

This module provides MediaPipe backend implementation for the Spring Vision framework, offering multi-purpose computer vision capabilities including face detection, hand landmarks, pose estimation, and object detection.

## Features

- **Face Detection**: Detect faces using MediaPipe Face Detector
- **Hand Landmarks**: Detect and track hand landmarks with MediaPipe Hand Landmarker
- **Pose Estimation**: Detect body pose landmarks using MediaPipe Pose Landmarker
- **Object Detection**: Detect common objects using EfficientDet
- **Automatic Model Management**: Download and cache models automatically
- **Thread-Safe**: Object pooling for MediaPipe task instances
- **High Performance**: Optimized for real-time processing

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>mediapipe</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the MediaPipe module
spring.vision.mediapipe.enabled=true
# Model path for MediaPipe models
spring.vision.mediapipe.model-path=~/.spring-vision/models/mediapipe
# Confidence threshold (0.0 - 1.0)
spring.vision.mediapipe.confidence-threshold=0.7
# Maximum number of detections
spring.vision.mediapipe.max-detections=10
# Enable automatic model download
spring.vision.mediapipe.enable-auto-download=true
# Download timeout (seconds)
spring.vision.mediapipe.download-timeout-seconds=30
# Maximum pool size for task instances
spring.vision.mediapipe.max-pool-size=5
# Pool timeout (seconds)
spring.vision.mediapipe.pool-timeout-seconds=60
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect-faces")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> faces = visionTemplate.detectFaces(imageData);

        return ResponseEntity.ok(faces);
    }

    @PostMapping("/detect-hands")
    public ResponseEntity<List<Detection>> detectHands(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> hands = visionTemplate.detect(imageData, DetectionType.HAND);

        return ResponseEntity.ok(hands);
    }

    @PostMapping("/detect-pose")
    public ResponseEntity<List<Detection>> detectPose(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> pose = visionTemplate.detect(imageData, DetectionType.POSE);

        return ResponseEntity.ok(pose);
    }

    @PostMapping("/detect-objects")
    public ResponseEntity<List<Detection>> detectObjects(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> objects = visionTemplate.detectObjects(imageData);

        return ResponseEntity.ok(objects);
    }
}
```

## Configuration Properties

| Property                                           | Type    | Default                             | Description                    |
|----------------------------------------------------|---------|-------------------------------------|--------------------------------|
| `spring.vision.mediapipe.enabled`                  | boolean | false                               | Enable/disable the module      |
| `spring.vision.mediapipe.model-path`               | String  | `~/.spring-vision/models/mediapipe` | Model directory path           |
| `spring.vision.mediapipe.confidence-threshold`     | double  | 0.7                                 | Detection confidence threshold |
| `spring.vision.mediapipe.max-detections`           | int     | 10                                  | Maximum number of detections   |
| `spring.vision.mediapipe.enable-auto-download`     | boolean | true                                | Auto-download models           |
| `spring.vision.mediapipe.download-timeout-seconds` | int     | 30                                  | Download timeout               |
| `spring.vision.mediapipe.max-pool-size`            | int     | 5                                   | Task pool size                 |
| `spring.vision.mediapipe.pool-timeout-seconds`     | int     | 60                                  | Pool timeout                   |

## Supported Detection Types

- `DetectionType.FACE` - Face detection with bounding boxes and keypoints
- `DetectionType.HAND` - Hand landmark detection (21 landmarks per hand)
- `DetectionType.POSE` - Body pose estimation (33 landmarks)
- `DetectionType.OBJECT` - Common object detection (80+ COCO classes)

## Models

The following MediaPipe models are automatically downloaded when needed:

- **face_detection_short_range.tflite** - Face detector optimized for short range
- **hand_landmarker.task** - Hand landmark detector
- **pose_landmarker_lite.task** - Lightweight pose detector
- **efficientdet_lite0.tflite** - Object detector

## Performance

- Face Detection: ~10-20ms per image
- Hand Landmarks: ~15-25ms per image
- Pose Estimation: ~20-30ms per image
- Object Detection: ~30-50ms per image

*Performance measured on mid-range CPU*

## Requirements

- Java 21+
- Spring Boot 3.2+
- MediaPipe Java library (optional - loaded via reflection)

## Example Application

```java

@SpringBootApplication
public class MediaPipeApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaPipeApplication.class, args);
    }
}
```

**application.properties:**

```properties
spring.vision.mediapipe.enabled=true
spring.vision.mediapipe.confidence-threshold=0.8
spring.vision.mediapipe.enable-auto-download=true
```

## Advanced Usage

### Manual Configuration (Optional)

```java

@Configuration
public class CustomMediaPipeConfig {

    @Bean
    public MediaPipeBackend mediaPipeBackend(MediaPipeProperties properties) {
        return new MediaPipeBackend();
    }
}
```

### Multiple Detection Types

```java
// Detect multiple features in one image
ImageData image = ImageData.fromFile("photo.jpg");

List<Detection> faces = visionTemplate.detect(image, DetectionType.FACE);
List<Detection> hands = visionTemplate.detect(image, DetectionType.HAND);
List<Detection> pose = visionTemplate.detect(image, DetectionType.POSE);
```

## Troubleshooting

### MediaPipe Library Not Found

Add the MediaPipe dependency explicitly:

```xml

<dependency>
    <groupId>com.google.mediapipe</groupId>
    <artifactId>tasks-vision</artifactId>
    <version>0.10.9</version>
</dependency>
```

### Model Download Issues

Check network connectivity and increase timeout:

```properties
spring.vision.mediapipe.download-timeout-seconds=120
```

### Performance Issues

Adjust pool size for concurrent processing:

```properties
spring.vision.mediapipe.max-pool-size=10
```

## License

See main project LICENSE file.
