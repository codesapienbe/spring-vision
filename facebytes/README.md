# FaceBytes - Java Implementation of DeepFace

**FaceBytes** is a comprehensive Java port of the DeepFace library, providing state-of-the-art face recognition, detection, and analysis capabilities. Built with Spring Boot integration and enterprise-grade architecture.

## 🚀 Features

### ✅ **Core Face Recognition**

- **Face Verification**: Compare two faces and determine if they belong to the same person
- **Face Embedding**: Generate high-dimensional face representations for similarity matching
- **Face Detection**: Locate faces in images using multiple detection backends
- **Face Analysis**: Analyze age, gender, emotion, and race attributes

### ✅ **Multiple Detection Backends**

- **OpenCV**: Fast Haar cascade-based detection (default)
- **RetinaFace**: High-accuracy deep learning detection
- **DLIB**: HOG-based detection with good accuracy
- **MTCNN**: Multi-task cascaded CNN detection with landmarks

### ✅ **Advanced Models**

- **VGG-Face**: High-quality 512-dimensional embeddings
- **FaceNet**: Efficient 128-dimensional embeddings
- **FaceNet512**: Extended 512-dimensional embeddings
- **ArcFace**: State-of-the-art face recognition
- **OpenFace**: Lightweight face recognition
- **DeepFace**: Traditional deep learning approach
- **SFace**: Efficient face recognition
- **DeepID**: Identity-preserving embeddings

### ✅ **Distance Metrics**

- **Cosine Distance**: Angular similarity (recommended)
- **Euclidean Distance**: L2 norm distance
- **Euclidean L2**: Normalized L2 distance

## 🏗️ Architecture

```
FaceBytes
├── Core API (DeepFace)
├── Detectors (Face Detection)
├── Models (Embedding Generation)
├── Utils (Image Processing, Metrics)
├── Config (Configuration Management)
└── Exceptions (Error Handling)
```

## 📦 Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-facebytes</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the FaceBytes module
spring.vision.facebytes.enabled=true
# Model path for FaceBytes models
spring.vision.facebytes.model-path=~/.spring-vision/models/facebytes
# Face detection backend (opencv, dlib, retinaface)
spring.vision.facebytes.detector-backend=opencv
# Face recognition model (VGG-Face, Facenet, OpenFace, DeepFace)
spring.vision.facebytes.recognition-model=VGG-Face
# Distance metric (cosine, euclidean, euclidean_l2)
spring.vision.facebytes.distance-metric=cosine
# Confidence threshold (0.0 - 1.0)
spring.vision.facebytes.confidence-threshold=0.7
# Maximum number of faces to detect
spring.vision.facebytes.max-detections=10
# Enable automatic model download
spring.vision.facebytes.enable-auto-download=true
# Enable face alignment
spring.vision.facebytes.enable-alignment=true
# Enable face quality validation
spring.vision.facebytes.enable-quality-check=false
# Minimum face size (pixels)
spring.vision.facebytes.min-face-size=20
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/faces")
public class FaceRecognitionController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);

        return ResponseEntity.ok(detections);
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

    @PostMapping("/embeddings")
    public ResponseEntity<List<float[]>> extractEmbeddings(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<float[]> embeddings = visionTemplate.extractEmbeddings(
                imageData,
                DetectionCategory.FACE
        );

        return ResponseEntity.ok(embeddings);
    }
}
```

## Configuration Properties

| Property                                       | Type    | Default                             | Description                |
|------------------------------------------------|---------|-------------------------------------|----------------------------|
| `spring.vision.facebytes.enabled`              | boolean | false                               | Enable/disable the module  |
| `spring.vision.facebytes.model-path`           | String  | `~/.spring-vision/models/facebytes` | Model directory path       |
| `spring.vision.facebytes.detector-backend`     | String  | opencv                              | Detection backend          |
| `spring.vision.facebytes.recognition-model`    | String  | VGG-Face                            | Recognition model          |
| `spring.vision.facebytes.distance-metric`      | String  | cosine                              | Distance metric            |
| `spring.vision.facebytes.confidence-threshold` | double  | 0.7                                 | Detection confidence       |
| `spring.vision.facebytes.max-detections`       | int     | 10                                  | Max faces to detect        |
| `spring.vision.facebytes.enable-auto-download` | boolean | true                                | Auto-download models       |
| `spring.vision.facebytes.enable-alignment`     | boolean | true                                | Enable face alignment      |
| `spring.vision.facebytes.enable-quality-check` | boolean | false                               | Enable quality validation  |
| `spring.vision.facebytes.min-face-size`        | int     | 20                                  | Minimum face size (pixels) |

## Advanced Usage

### Custom Configuration

```java

@Configuration
public class CustomFaceBytesConfig {

    @Bean
    public FaceBytesBackend faceBytesBackend(FaceBytesProperties properties) {
        return new FaceBytesBackend();
    }
}
```

### Direct API Access

```java
import io.github.codesapienbe.springvision.facebytes.core.DeepFace;

// Direct DeepFace API usage
BufferedImage image = ImageIO.read(new File("face.jpg"));
        List<EmbeddingResult> embeddings = DeepFace.represent(image);

        // Face verification
        boolean match = DeepFace.verify(image1, image2);
```

## Performance

- **OpenCV Detector**: ~50ms per image
- **DLIB Detector**: ~100ms per image
- **RetinaFace**: ~150ms per image (higher accuracy)
- **Embedding Extraction**: ~200ms per face

## Requirements

- Java 21+
- Spring Boot 3.2+
- OpenCV (optional, for detection)
- ONNX Runtime (for model inference)

## Examples

See the `examples/` directory for complete sample applications:

- Basic face detection
- Face verification system
- Face recognition database
- Real-time face tracking

## License

See main project LICENSE file.
