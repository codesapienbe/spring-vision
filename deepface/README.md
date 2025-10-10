# Spring Vision DeepFace Backend

This module provides DeepFace backend implementation for the Spring Vision framework, offering comprehensive face recognition and analysis through the DeepFace Python library API.

## Features

- **Face Detection**: Detect faces using multiple detector backends
- **Face Recognition**: Identify and verify faces with state-of-the-art models
- **Face Verification**: Compare two faces for similarity
- **Face Analysis**: Age, gender, emotion, and race detection
- **Multiple Models**: VGG-Face, Facenet, OpenFace, DeepFace, ArcFace
- **Multiple Detectors**: OpenCV, SSD, Dlib, MTCNN, RetinaFace
- **Distance Metrics**: Cosine, Euclidean, Euclidean L2
- **High Accuracy**: State-of-the-art face recognition performance

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
# Face recognition model (VGG-Face, Facenet, Facenet512, OpenFace, DeepFace, DeepID, ArcFace, Dlib, SFace)
spring.vision.deepface.model=VGG-Face
# Detector backend (opencv, ssd, dlib, mtcnn, retinaface)
spring.vision.deepface.detector-backend=opencv
# Distance metric (cosine, euclidean, euclidean_l2)
spring.vision.deepface.distance-metric=cosine
# Confidence threshold (0.0 - 1.0)
spring.vision.deepface.confidence-threshold=0.7
# Verification threshold (0.0 - 1.0)
spring.vision.deepface.verification-threshold=0.6
# Maximum number of faces to detect
spring.vision.deepface.max-detections=10
# Enable face analysis
spring.vision.deepface.enable-analysis=true
# Request timeout (seconds)
spring.vision.deepface.timeout-seconds=30
# Maximum retry attempts
spring.vision.deepface.max-retries=3
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/faces")
public class FaceAnalysisController {

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

        List<Detection> faces1 = visionTemplate.detectFaces(image1);
        List<Detection> faces2 = visionTemplate.detectFaces(image2);

        if (faces1.isEmpty() || faces2.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Compare faces using DeepFace verification
        double similarity = calculateSimilarity(faces1.get(0), faces2.get(0));
        boolean verified = similarity > 0.6;

        return ResponseEntity.ok(new VerificationResult(verified, similarity));
    }

    @PostMapping("/analyze")
    public ResponseEntity<FaceAnalysis> analyzeFace(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> detections = visionTemplate.detectFaces(imageData);

        if (detections.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Detection face = detections.get(0);
        Map<String, Object> attrs = face.attributes();

        FaceAnalysis analysis = new FaceAnalysis(
                (Integer) attrs.get("age"),
                (String) attrs.get("gender"),
                (String) attrs.get("emotion"),
                (String) attrs.get("race"),
                face.confidence()
        );

        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/batch-analyze")
    public ResponseEntity<List<FaceAnalysis>> analyzeBatch(
            @RequestParam("images") MultipartFile[] files) throws IOException {

        List<FaceAnalysis> results = new ArrayList<>();

        for (MultipartFile file : files) {
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            List<Detection> detections = visionTemplate.detectFaces(imageData);

            if (!detections.isEmpty()) {
                Detection face = detections.get(0);
                Map<String, Object> attrs = face.attributes();

                results.add(new FaceAnalysis(
                        (Integer) attrs.get("age"),
                        (String) attrs.get("gender"),
                        (String) attrs.get("emotion"),
                        (String) attrs.get("race"),
                        face.confidence()
                ));
            }
        }

        return ResponseEntity.ok(results);
    }
}
```

## Configuration Properties

| Property                                        | Type    | Default               | Description                  |
|-------------------------------------------------|---------|-----------------------|------------------------------|
| `spring.vision.deepface.enabled`                | boolean | false                 | Enable/disable the module    |
| `spring.vision.deepface.base-url`               | String  | http://localhost:5000 | DeepFace API URL             |
| `spring.vision.deepface.model`                  | String  | VGG-Face              | Face recognition model       |
| `spring.vision.deepface.detector-backend`       | String  | opencv                | Face detector backend        |
| `spring.vision.deepface.distance-metric`        | String  | cosine                | Distance calculation metric  |
| `spring.vision.deepface.confidence-threshold`   | double  | 0.7                   | Minimum detection confidence |
| `spring.vision.deepface.verification-threshold` | double  | 0.6                   | Face verification threshold  |
| `spring.vision.deepface.max-detections`         | int     | 10                    | Maximum faces to detect      |
| `spring.vision.deepface.enable-analysis`        | boolean | true                  | Enable demographic analysis  |
| `spring.vision.deepface.timeout-seconds`        | int     | 30                    | Request timeout              |
| `spring.vision.deepface.max-retries`            | int     | 3                     | Maximum retry attempts       |

## Running DeepFace Server

You need a running DeepFace API server. Use Docker:

```bash
docker run -d -p 5000:5000 serengil/deepface:latest
```

Or use Docker Compose:

```yaml
services:
  deepface:
    image: serengil/deepface:latest
    ports:
      - "5000:5000"
    environment:
      - DEEPFACE_HOME=/root/.deepface
    volumes:
      - deepface-models:/root/.deepface
    restart: unless-stopped

volumes:
  deepface-models:
```

## Usage Examples

### Basic Face Detection

```java
ImageData image = ImageData.fromFile("photo.jpg");
List<Detection> faces = visionTemplate.detectFaces(image);

faces.

forEach(face ->{
        System.out.

println("Face detected at: "+face.boundingBox());
        System.out.

println("Confidence: "+face.confidence());
        });
```

### Face Verification

```java
@Service
public class FaceVerificationService {

    @Autowired
    private VisionTemplate visionTemplate;

    public boolean verifyIdentity(MultipartFile photo1, MultipartFile photo2) 
            throws IOException {
        ImageData img1 = ImageData.fromBytes(photo1.getBytes());
        ImageData img2 = ImageData.fromBytes(photo2.getBytes());
        
        List<Detection> faces1 = visionTemplate.detectFaces(img1);
        List<Detection> faces2 = visionTemplate.detectFaces(img2);
        
        if (faces1.isEmpty() || faces2.isEmpty()) {
            return false;
        }
        
        // Use embeddings or verification API
        return compareFaceEmbeddings(faces1.get(0), faces2.get(0));
    }
}
```

### Demographic Analysis

```java
ImageData portrait = ImageData.fromFile("portrait.jpg");
List<Detection> faces = visionTemplate.detectFaces(portrait);

if(!faces.

isEmpty()){
Detection face = faces.get(0);
Map<String, Object> attrs = face.attributes();
    
    System.out.

println("Age: "+attrs.get("age"));
        System.out.

println("Gender: "+attrs.get("gender"));
        System.out.

println("Emotion: "+attrs.get("emotion"));
        System.out.

println("Race: "+attrs.get("race"));
        }
```

### Custom Model Selection

```properties
# Use different models for different accuracy/speed tradeoffs
spring.vision.deepface.model=Facenet512  # High accuracy
spring.vision.deepface.detector-backend=retinaface  # High accuracy detection
```

## Model Comparison

| Model      | Accuracy  | Speed  | Embedding Size |
|------------|-----------|--------|----------------|
| VGG-Face   | High      | Slow   | 2622-D         |
| Facenet    | High      | Fast   | 128-D          |
| Facenet512 | Very High | Medium | 512-D          |
| ArcFace    | Very High | Medium | 512-D          |
| OpenFace   | Medium    | Fast   | 128-D          |
| DeepFace   | Medium    | Medium | 4096-D         |
| DeepID     | Medium    | Fast   | 160-D          |
| Dlib       | Medium    | Medium | 128-D          |
| SFace      | High      | Fast   | 128-D          |

## Detector Comparison

| Detector   | Accuracy  | Speed  | Notes                  |
|------------|-----------|--------|------------------------|
| OpenCV     | Medium    | Fast   | Haar Cascade (default) |
| SSD        | High      | Fast   | Single Shot Detector   |
| Dlib       | High      | Medium | HOG-based              |
| MTCNN      | High      | Slow   | Multi-task CNN         |
| RetinaFace | Very High | Slow   | Best accuracy          |

## Performance

- Face Detection: ~100-300ms per image
- Face Recognition: ~200-500ms per image
- Face Verification: ~300-600ms per comparison
- Face Analysis: ~500-1000ms per image

*Performance depends on model selection, detector backend, and server hardware*

## Requirements

- Java 21+
- Spring Boot 3.2+
- Running DeepFace server (Docker recommended)
- Network access to DeepFace API
- Sufficient memory for model loading (2-4GB recommended)

## Troubleshooting

### Connection Refused

Ensure DeepFace server is running:

```bash
curl http://localhost:5000/
```

### Slow Performance

Try using faster models and detectors:

```properties
spring.vision.deepface.model=Facenet
spring.vision.deepface.detector-backend=opencv
```

### Out of Memory

Reduce max detections or use lighter models:

```properties
spring.vision.deepface.max-detections=5
spring.vision.deepface.model=OpenFace
```

### Model Download Issues

DeepFace downloads models on first use. Ensure:

- Internet connectivity
- Sufficient disk space
- Write permissions in model directory

## Best Practices

1. **Choose Right Model**: Balance accuracy vs. speed for your use case
2. **Tune Thresholds**: Adjust verification thresholds based on requirements
3. **Handle Errors**: Implement proper error handling and retries
4. **Monitor Performance**: Track API response times and model performance
5. **Cache Results**: Cache embeddings for frequently compared faces
6. **Use HTTPS**: Always use HTTPS in production environments

## Advanced Features

### Custom Distance Thresholds

Different models have different optimal thresholds:

```properties
# VGG-Face optimal: 0.68
# Facenet optimal: 0.40
# ArcFace optimal: 0.68
spring.vision.deepface.verification-threshold=0.68
```

### Multi-Face Detection

```java
List<Detection> allFaces = visionTemplate.detectFaces(groupPhoto);
System.out.

println("Detected "+allFaces.size() +" faces");
```

## License

See main project LICENSE file.

