# Spring Vision YOLO Backend

This module provides YOLO (You Only Look Once) backend implementation for the Spring Vision framework, offering real-time object detection using ONNX Runtime.

## Features

- **Real-Time Object Detection**: Detect 80+ object classes from COCO dataset
- **Multiple YOLO Versions**: Support for YOLOv8n, YOLOv8s, YOLOv8m
- **High Performance**: Optimized inference with ONNX Runtime
- **Automatic Model Download**: Download and cache models automatically
- **NMS (Non-Maximum Suppression)**: Intelligent duplicate removal
- **Bounding Boxes**: Precise object localization
- **Confidence Scores**: Detection confidence for each object
- **Batch Processing**: Process multiple images efficiently

## Getting Started

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-yolo</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the YOLO module
spring.vision.yolo.enabled=true

# Model path for YOLO models
spring.vision.yolo.model-path=~/.spring-vision/models/yolo

# Model name (yolov8n.onnx, yolov8s.onnx, yolov8m.onnx)
spring.vision.yolo.model-name=yolov8n.onnx

# Confidence threshold (0.0 - 1.0)
spring.vision.yolo.confidence-threshold=0.25

# Non-maximum suppression threshold (0.0 - 1.0)
spring.vision.yolo.nms-threshold=0.45

# Maximum number of detections
spring.vision.yolo.max-detections=100

# Enable automatic model download
spring.vision.yolo.enable-auto-download=true

# Download timeout (seconds)
spring.vision.yolo.download-timeout-seconds=300

# Input size (typically 640)
spring.vision.yolo.input-size=640
```

### 3. Use VisionTemplate (Auto-Configured)

```java
@RestController
@RequestMapping("/api/objects")
public class ObjectDetectionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectObjects(
            @RequestParam("image") MultipartFile file) throws IOException {
        
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectObjects(imageData);
        
        return ResponseEntity.ok(detections);
    }
    
    @PostMapping("/detect-filtered")
    public ResponseEntity<List<Detection>> detectObjectsFiltered(
            @RequestParam("image") MultipartFile file,
            @RequestParam("classes") List<String> classes) throws IOException {
        
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> allDetections = visionTemplate.detectObjects(imageData);
        
        // Filter by specific classes
        List<Detection> filtered = allDetections.stream()
            .filter(d -> classes.contains(d.label()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(filtered);
    }
    
    @PostMapping("/count")
    public ResponseEntity<Map<String, Long>> countObjects(
            @RequestParam("image") MultipartFile file) throws IOException {
        
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectObjects(imageData);
        
        Map<String, Long> counts = detections.stream()
            .collect(Collectors.groupingBy(
                Detection::label,
                Collectors.counting()
            ));
        
        return ResponseEntity.ok(counts);
    }
}
```

## Configuration Properties

| Property                                      | Type    | Default                        | Description               |
|-----------------------------------------------|---------|--------------------------------|---------------------------|
| `spring.vision.yolo.enabled`                  | boolean | false                          | Enable/disable the module |
| `spring.vision.yolo.model-path`               | String  | `~/.spring-vision/models/yolo` | Model directory path      |
| `spring.vision.yolo.model-name`               | String  | yolov8n.onnx                   | Model file name           |
| `spring.vision.yolo.confidence-threshold`     | double  | 0.25                           | Detection confidence      |
| `spring.vision.yolo.nms-threshold`            | double  | 0.45                           | NMS threshold             |
| `spring.vision.yolo.max-detections`           | int     | 100                            | Max detections to return  |
| `spring.vision.yolo.enable-auto-download`     | boolean | true                           | Auto-download models      |
| `spring.vision.yolo.download-timeout-seconds` | int     | 300                            | Download timeout          |
| `spring.vision.yolo.input-size`               | int     | 640                            | Model input size          |

## Available Models

- **yolov8n.onnx**: Nano model, fastest (recommended for real-time)
- **yolov8s.onnx**: Small model, balanced speed/accuracy
- **yolov8m.onnx**: Medium model, higher accuracy

## Detected Object Classes (COCO)

YOLO can detect 80 object classes including:

**People & Animals:**
person, bicycle, car, motorcycle, airplane, bus, train, truck, boat, bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe

**Vehicles:**
bicycle, car, motorcycle, airplane, bus, train, truck, boat, traffic light, fire hydrant, stop sign, parking meter

**Home & Office:**
chair, couch, potted plant, bed, dining table, toilet, tv, laptop, mouse, remote, keyboard, cell phone, microwave, oven, toaster, sink, refrigerator, book, clock, vase

**Sports & Recreation:**
sports ball, kite, baseball bat, baseball glove, skateboard, surfboard, tennis racket, frisbee, skis, snowboard

**Food:**
bottle, wine glass, cup, fork, knife, spoon, bowl, banana, apple, sandwich, orange, broccoli, carrot, hot dog, pizza, donut, cake

**Accessories:**
backpack, umbrella, handbag, tie, suitcase, scissors, teddy bear, hair drier, toothbrush

## Features

### Object Detection

```java
List<Detection> objects = visionTemplate.detectObjects(imageData);
objects.forEach(obj -> {
    System.out.println("Object: " + obj.label());
    System.out.println("Confidence: " + obj.confidence());
    System.out.println("Location: " + obj.boundingBox());
});
```

### Object Counting

```java
Map<String, Long> counts = detections.stream()
    .collect(Collectors.groupingBy(
        Detection::label,
        Collectors.counting()
    ));

System.out.println("People: " + counts.get("person"));
System.out.println("Cars: " + counts.get("car"));
```

### Class Filtering

```java
// Detect only people and vehicles
List<String> classFilter = List.of("person", "car", "truck", "bus");
List<Detection> filtered = detections.stream()
    .filter(d -> classFilter.contains(d.label()))
    .collect(Collectors.toList());
```

### High-Confidence Detection

```java
DetectionQuery query = new DetectionQuery.Builder()
    .type(DetectionType.OBJECT)
    .minConfidence(0.7)  // High confidence only
    .maxDetections(50)
    .build();

List<Detection> highConfidence = visionTemplate.detect(imageData, query);
```

## Performance

| Model   | Speed (CPU) | mAP   | Parameters |
|---------|-------------|-------|------------|
| yolov8n | ~50ms       | 37.3% | 3.2M       |
| yolov8s | ~100ms      | 44.9% | 11.2M      |
| yolov8m | ~200ms      | 50.2% | 25.9M      |

*Performance measured on Intel Core i7, single image*

## Requirements

- Java 21+
- Spring Boot 3.2+
- ONNX Runtime (automatically included)

## Use Cases

- **Surveillance**: Monitor areas for specific objects or people
- **Retail**: Count customers, track inventory
- **Traffic**: Vehicle counting and classification
- **Safety**: Detect safety equipment, hazards
- **Robotics**: Object recognition for autonomous systems
- **Agriculture**: Count crops, detect pests

## Advanced Usage

### Custom Model Path

```properties
spring.vision.yolo.model-path=/custom/models/yolo
spring.vision.yolo.model-name=custom_yolov8.onnx
```

### Batch Processing

```java
@Service
public class BatchObjectDetector {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<List<Detection>> detectBatch(List<MultipartFile> files) {
        return files.stream()
            .map(file -> {
                try {
                    ImageData imageData = ImageData.fromBytes(file.getBytes());
                    return visionTemplate.detectObjects(imageData);
                } catch (IOException e) {
                    return List.<Detection>of();
                }
            })
            .collect(Collectors.toList());
    }
}
```

### Real-Time Detection

```java
@Service
public class RealtimeDetector {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public void processVideoStream(InputStream videoStream) {
        // Process video frames
        while (hasNextFrame(videoStream)) {
            byte[] frameBytes = readFrame(videoStream);
            ImageData frame = ImageData.fromBytes(frameBytes);
            
            List<Detection> objects = visionTemplate.detectObjects(frame);
            handleDetections(objects);
        }
    }
}
```

## Troubleshooting

### Out of Memory

Reduce input size or max detections:

```properties
spring.vision.yolo.input-size=416
spring.vision.yolo.max-detections=50
```

### Too Many False Positives

Increase confidence threshold:

```properties
spring.vision.yolo.confidence-threshold=0.5
```

### Overlapping Detections

Adjust NMS threshold:

```properties
spring.vision.yolo.nms-threshold=0.3
```

### Model Download Failed

Check disk space and network, or provide model manually:

```bash
mkdir -p ~/.spring-vision/models/yolo
cd ~/.spring-vision/models/yolo
wget https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.pt
```

## Example Application

```java
@SpringBootApplication
public class ObjectDetectionApp {
    public static void main(String[] args) {
        SpringApplication.run(ObjectDetectionApp.class, args);
    }
}

@Service
class SecurityMonitor {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Scheduled(fixedRate = 1000)
    public void monitorArea() {
        ImageData frame = captureFrame();
        List<Detection> objects = visionTemplate.detectObjects(frame);
        
        long people = objects.stream()
            .filter(d -> "person".equals(d.label()))
            .count();
        
        if (people > 10) {
            alertSecurity("Too many people in restricted area");
        }
    }
}
```

## License

See main project LICENSE file.

# Spring Vision DeepFace Backend

This module provides DeepFace backend implementation for the Spring Vision framework, offering face recognition and analysis through the serengil/deepface Python library API.

## Features

- **Face Detection**: Detect faces using multiple backend options (OpenCV, SSD, Dlib, MTCNN, RetinaFace, MediaPipe)
- **Face Recognition**: High-accuracy face recognition with multiple models
- **Face Verification**: Compare two faces for similarity
- **Face Analysis**: Age, gender, emotion, and race prediction
- **Multiple Models**: VGG-Face, Facenet, OpenFace, DeepFace, ArcFace, Dlib, and more
- **Distance Metrics**: Cosine, Euclidean, and Euclidean L2 distance

## Getting Started

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-deepface</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the DeepFace module
spring.vision.deepface.enabled=true

# DeepFace API server URL
spring.vision.deepface.base-url=http://localhost:5000

# Request timeout
spring.vision.deepface.timeout=30s

# Confidence threshold (0.0 - 1.0)
spring.vision.deepface.confidence-threshold=0.6

# Maximum number of faces to detect
spring.vision.deepface.max-detections=10

# Face detection backend (opencv, ssd, dlib, mtcnn, retinaface, mediapipe)
spring.vision.deepface.detector-backend=opencv

# Face recognition model (VGG-Face, Facenet, Facenet512, OpenFace, DeepFace, DeepID, ArcFace, Dlib)
spring.vision.deepface.model=VGG-Face

# Distance metric (cosine, euclidean, euclidean_l2)
spring.vision.deepface.distance-metric=cosine

# Enable face alignment
spring.vision.deepface.align=true

# Enable normalization
spring.vision.deepface.normalization=true
```

### 3. Use VisionTemplate (Auto-Configured)

```java
@RestController
@RequestMapping("/api/faces")
public class DeepFaceController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("image") MultipartFile file) throws IOException {
        
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);
        
        return ResponseEntity.ok(detections);
    }
    
    @PostMapping("/analyze")
    public ResponseEntity<FaceAnalysis> analyzeFace(
            @RequestParam("image") MultipartFile file) throws IOException {
        
        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);
        
        if (detections.isEmpty()) {
            return ResponseEntity.ok(new FaceAnalysis("No face detected", null));
        }
        
        Detection face = detections.get(0);
        Map<String, Object> attributes = face.attributes();
        
        FaceAnalysis analysis = new FaceAnalysis(
            (String) attributes.get("gender"),
            (Integer) attributes.get("age"),
            (String) attributes.get("emotion"),
            (String) attributes.get("race")
        );
        
        return ResponseEntity.ok(analysis);
    }
    
    @PostMapping("/verify")
    public ResponseEntity<VerificationResult> verifyFaces(
            @RequestParam("image1") MultipartFile file1,
            @RequestParam("image2") MultipartFile file2) throws IOException {
        
        ImageData image1 = ImageData.fromBytes(file1.getBytes());
        ImageData image2 = ImageData.fromBytes(file2.getBytes());
        
        boolean verified = visionTemplate.verify(image1, image2);
        
        return ResponseEntity.ok(new VerificationResult(verified));
    }
}
```

## Configuration Properties

| Property                                      | Type     | Default               | Description               |
|-----------------------------------------------|----------|-----------------------|---------------------------|
| `spring.vision.deepface.enabled`              | boolean  | false                 | Enable/disable the module |
| `spring.vision.deepface.base-url`             | String   | http://localhost:5000 | DeepFace API URL          |
| `spring.vision.deepface.timeout`              | Duration | 30s                   | Request timeout           |
| `spring.vision.deepface.confidence-threshold` | double   | 0.6                   | Detection confidence      |
| `spring.vision.deepface.max-detections`       | int      | 10                    | Max faces to detect       |
| `spring.vision.deepface.detector-backend`     | String   | opencv                | Detection backend         |
| `spring.vision.deepface.model`                | String   | VGG-Face              | Recognition model         |
| `spring.vision.deepface.distance-metric`      | String   | cosine                | Distance metric           |
| `spring.vision.deepface.align`                | boolean  | true                  | Enable face alignment     |
| `spring.vision.deepface.normalization`        | boolean  | true                  | Enable normalization      |

## Prerequisites

### Running DeepFace Server

You need a running DeepFace server. Use Docker:

```bash
docker run -d -p 5000:5000 serengil/deepface
```

Or use docker-compose:

```yaml
version: '3.8'
services:
  deepface:
    image: serengil/deepface
    ports:
      - "5000:5000"
```

## Detection Backends

- **OpenCV**: Fast, Haar cascade-based (default)
- **SSD**: Good balance of speed and accuracy
- **Dlib**: High accuracy, slower
- **MTCNN**: Multi-task CNN with landmarks
- **RetinaFace**: State-of-the-art accuracy
- **MediaPipe**: Google's MediaPipe face detector

## Recognition Models

- **VGG-Face**: High accuracy, 512-dimensional embeddings
- **Facenet**: Efficient, 128-dimensional embeddings
- **Facenet512**: Extended Facenet with 512 dimensions
- **OpenFace**: Lightweight face recognition
- **DeepFace**: Facebook's face recognition model
- **DeepID**: Identity-preserving embeddings
- **ArcFace**: State-of-the-art face recognition
- **Dlib**: ResNet-based face recognition

## Features

### Face Analysis

```java
// Analyze face for age, gender, emotion, race
List<Detection> faces = visionTemplate.detectFaces(imageData);
Detection face = faces.get(0);

String gender = (String) face.attributes().get("dominant_gender");
int age = (Integer) face.attributes().get("age");
String emotion = (String) face.attributes().get("dominant_emotion");
String race = (String) face.attributes().get("dominant_race");
```

### Face Verification

```java
// Compare two faces
boolean match = visionTemplate.verify(image1, image2);
if (match) {
    System.out.println("Same person!");
}
```

### Custom Detection Backend

```properties
# Use RetinaFace for higher accuracy
spring.vision.deepface.detector-backend=retinaface
```

## Performance

| Backend    | Speed  | Accuracy    |
|------------|--------|-------------|
| OpenCV     | ~50ms  | Good        |
| SSD        | ~100ms | Very Good   |
| Dlib       | ~150ms | Excellent   |
| MTCNN      | ~200ms | Excellent   |
| RetinaFace | ~250ms | Outstanding |

*Performance depends on DeepFace server hardware and network latency*

## Requirements

- Java 21+
- Spring Boot 3.2+
- Running DeepFace server (Docker recommended)
- Network access to DeepFace API

## Troubleshooting

### Connection Issues

Ensure DeepFace server is running:

```bash
curl http://localhost:5000
```

### Slow Response

Try a faster detection backend:

```properties
spring.vision.deepface.detector-backend=opencv
```

### Model Download

First request may be slow as DeepFace downloads models. Subsequent requests will be faster.

## Example Application

```java
@SpringBootApplication
public class DeepFaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeepFaceApplication.class, args);
    }
}
```

**application.properties:**

```properties
spring.vision.deepface.enabled=true
spring.vision.deepface.detector-backend=retinaface
spring.vision.deepface.model=ArcFace
```

## License

See main project LICENSE file.

