# Spring Vision InsightFace Backend

This module provides InsightFace backend implementation for the Spring Vision framework, offering state-of-the-art face recognition using ArcFace models.

## Features

- **High-Accuracy Face Detection**: Advanced face detection with InsightFace
- **ArcFace Recognition**: State-of-the-art face recognition using ArcFace
- **Face Verification**: Compare faces with high precision
- **Face Identification**: Search faces in a database
- **Demographic Analysis**: Age and gender prediction
- **Emotion Detection**: Facial emotion recognition
- **Landmark Detection**: 5-point and 106-point facial landmarks
- **Face Database**: Built-in face database for recognition

## Getting Started

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>insightface</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the InsightFace module
spring.vision.insightface.enabled=true

# InsightFace API URL
spring.vision.insightface.api-url=http://localhost:8000

# API key for authentication
spring.vision.insightface.api-key=your-api-key-here

# Model name (buffalo_l, buffalo_m, buffalo_s)
spring.vision.insightface.model-name=buffalo_l

# Confidence threshold (0.0 - 1.0)
spring.vision.insightface.confidence-threshold=0.5

# Verification threshold (0.0 - 1.0)
spring.vision.insightface.verification-threshold=0.6

# Maximum number of faces to detect
spring.vision.insightface.max-detections=10

# Enable age and gender analysis
spring.vision.insightface.enable-age-gender=true

# Enable emotion detection
spring.vision.insightface.enable-emotion=true

# Enable landmark detection
spring.vision.insightface.enable-landmarks=true

# Request timeout (seconds)
spring.vision.insightface.timeout-seconds=30

# Maximum retry attempts
spring.vision.insightface.max-retries=3
```

### 3. Use VisionTemplate (Auto-Configured)

```java
@RestController
@RequestMapping("/api/faces")
public class InsightFaceController {

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
            return ResponseEntity.notFound().build();
        }
        
        Detection face = detections.get(0);
        Map<String, Object> attrs = face.attributes();
        
        return ResponseEntity.ok(new FaceAnalysis(
            (Integer) attrs.get("age"),
            (String) attrs.get("gender"),
            (String) attrs.get("emotion"),
            (List<Map<String, Object>>) attrs.get("landmarks")
        ));
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

| Property                                           | Type    | Default               | Description               |
|----------------------------------------------------|---------|-----------------------|---------------------------|
| `spring.vision.insightface.enabled`                | boolean | false                 | Enable/disable the module |
| `spring.vision.insightface.api-url`                | String  | http://localhost:8000 | InsightFace API URL       |
| `spring.vision.insightface.api-key`                | String  | ""                    | API authentication key    |
| `spring.vision.insightface.model-name`             | String  | buffalo_l             | Model variant             |
| `spring.vision.insightface.confidence-threshold`   | double  | 0.5                   | Detection confidence      |
| `spring.vision.insightface.verification-threshold` | double  | 0.6                   | Verification threshold    |
| `spring.vision.insightface.max-detections`         | int     | 10                    | Max faces to detect       |
| `spring.vision.insightface.enable-age-gender`      | boolean | true                  | Enable age/gender         |
| `spring.vision.insightface.enable-emotion`         | boolean | true                  | Enable emotion            |
| `spring.vision.insightface.enable-landmarks`       | boolean | true                  | Enable landmarks          |
| `spring.vision.insightface.timeout-seconds`        | int     | 30                    | Request timeout           |
| `spring.vision.insightface.max-retries`            | int     | 3                     | Retry attempts            |

## Available Models

- **buffalo_l**: Large model, highest accuracy (recommended)
- **buffalo_m**: Medium model, balanced speed/accuracy
- **buffalo_s**: Small model, fastest inference

## Features

### Face Detection with Analysis

```java
List<Detection> faces = visionTemplate.detectFaces(imageData);
for (Detection face : faces) {
    Map<String, Object> attrs = face.attributes();
    
    System.out.println("Age: " + attrs.get("age"));
    System.out.println("Gender: " + attrs.get("gender"));
    System.out.println("Emotion: " + attrs.get("emotion"));
    System.out.println("Landmarks: " + attrs.get("landmarks"));
}
```

### Face Verification

```java
boolean match = visionTemplate.verify(image1, image2);
System.out.println("Faces match: " + match);
```

### Face Recognition Database

```java
// Add face to database
insightFaceBackend.addFace("john_doe", imageBytes, correlationId);

// Recognize faces
List<RecognitionResult> results = insightFaceBackend.recognizeFaces(imageBytes, correlationId);
results.forEach(result -> {
    System.out.println("Identity: " + result.getIdentity());
    System.out.println("Confidence: " + result.getConfidence());
});

// Remove face from database
insightFaceBackend.removeFace("john_doe");
```

### Embedding Extraction

```java
List<double[]> embeddings = insightFaceBackend.extractEmbeddings(imageBytes, correlationId);
embeddings.forEach(embedding -> {
    System.out.println("Embedding dimension: " + embedding.length);
});
```

## Performance

| Model     | Speed  | Accuracy  | Embedding Size |
|-----------|--------|-----------|----------------|
| buffalo_s | ~50ms  | Good      | 512            |
| buffalo_m | ~100ms | Very Good | 512            |
| buffalo_l | ~150ms | Excellent | 512            |

*Performance depends on hardware and image resolution*

## Prerequisites

### Running InsightFace Server

Deploy your own InsightFace API server or use a cloud service. Example using Python:

```python
from insightface.app import FaceAnalysis
from flask import Flask, request, jsonify

app = Flask(__name__)
face_app = FaceAnalysis(name='buffalo_l')
face_app.prepare(ctx_id=0)

@app.route('/detect', methods=['POST'])
def detect():
    # Your implementation
    pass

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

## Requirements

- Java 21+
- Spring Boot 3.2+
- Running InsightFace API server
- Network access to API

## Use Cases

- **Access Control**: High-security face authentication
- **Surveillance**: Real-time face recognition in video streams
- **Social Media**: Tag friends in photos automatically
- **Attendance Systems**: Automated attendance tracking
- **Customer Recognition**: Identify VIP customers

## Troubleshooting

### API Connection Failed

Check server status:

```bash
curl http://localhost:8000/health
```

### Low Recognition Accuracy

- Use buffalo_l model for best accuracy
- Ensure good image quality (resolution, lighting)
- Adjust verification threshold
- Use face alignment preprocessing

### Performance Issues

- Use buffalo_s for faster inference
- Implement caching for embeddings
- Batch process multiple faces
- Use connection pooling

## Example Application

```java
@SpringBootApplication
public class FaceRecognitionApp {
    public static void main(String[] args) {
        SpringApplication.run(FaceRecognitionApp.class, args);
    }
}

@Service
class FaceRecognitionService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public boolean authenticateUser(byte[] faceImage, String userId) throws IOException {
        ImageData imageData = ImageData.fromBytes(faceImage);
        List<float[]> embeddings = visionTemplate.extractEmbeddings(
            imageData, 
            DetectionCategory.FACE
        );
        
        // Compare with stored embedding
        return compareWithDatabase(embeddings.get(0), userId);
    }
}
```

## License

See main project LICENSE file.

