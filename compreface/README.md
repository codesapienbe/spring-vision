# Spring Vision CompreFace Backend

This module provides CompreFace backend implementation for the Spring Vision framework, offering face recognition and analysis through the Exadel CompreFace API.

## Features

- **Face Detection**: Detect faces using CompreFace Face Detector
- **Face Recognition**: Identify and verify faces with high accuracy
- **Face Verification**: Compare two faces for similarity
- **Face Analysis**: Age, gender, and emotion detection
- **Face Database**: Manage face collections and subjects
- **REST API Integration**: Easy integration with CompreFace server
- **High Performance**: Optimized for production workloads
- **Multi-Subject Support**: Recognize multiple people

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>compreface</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the CompreFace module
spring.vision.compreface.enabled=true
# CompreFace API server URL
spring.vision.compreface.base-url=http://localhost:8000
# API key for authentication
spring.vision.compreface.api-key=your-api-key-here
# Confidence threshold (0.0 - 1.0)
spring.vision.compreface.confidence-threshold=0.7
# Recognition threshold (0.0 - 1.0)
spring.vision.compreface.recognition-threshold=0.8
# Maximum number of faces to detect
spring.vision.compreface.max-detections=10
# Enable face analysis (age, gender, emotion)
spring.vision.compreface.enable-analysis=true
# Request timeout (seconds)
spring.vision.compreface.timeout-seconds=30
# Maximum retry attempts
spring.vision.compreface.max-retries=3
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

        List<Detection> faces1 = visionTemplate.detectFaces(image1);
        List<Detection> faces2 = visionTemplate.detectFaces(image2);

        if (faces1.isEmpty() || faces2.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Compare embeddings or use verification capability
        boolean match = compareFaces(faces1.get(0), faces2.get(0));

        return ResponseEntity.ok(new VerificationResult(match));
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
                face.confidence()
        );

        return ResponseEntity.ok(analysis);
    }
}
```

## Configuration Properties

| Property                                         | Type    | Default               | Description                  |
|--------------------------------------------------|---------|-----------------------|------------------------------|
| `spring.vision.compreface.enabled`               | boolean | false                 | Enable/disable the module    |
| `spring.vision.compreface.base-url`              | String  | http://localhost:8000 | CompreFace API URL           |
| `spring.vision.compreface.api-key`               | String  | -                     | API key for authentication   |
| `spring.vision.compreface.confidence-threshold`  | double  | 0.7                   | Minimum detection confidence |
| `spring.vision.compreface.recognition-threshold` | double  | 0.8                   | Face recognition threshold   |
| `spring.vision.compreface.max-detections`        | int     | 10                    | Maximum faces to detect      |
| `spring.vision.compreface.enable-analysis`       | boolean | true                  | Enable age/gender/emotion    |
| `spring.vision.compreface.timeout-seconds`       | int     | 30                    | Request timeout              |
| `spring.vision.compreface.max-retries`           | int     | 3                     | Maximum retry attempts       |

## Running CompreFace Server

You need a running CompreFace server. Use Docker:

```bash
docker run -d -p 8000:80 exadel/compreface:latest
```

Or use Docker Compose (recommended for development):

```bash
cd compreface
docker compose up -d
```

For integration tests, Spring Boot automatically manages docker-compose. Just add the dependency and run tests:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>test</scope>
</dependency>
```

Then run:

```bash
mvn verify  # Automatically starts/stops CompreFace service
```

See [Testing with Docker Compose](../docs/testing-with-docker-compose.md) for more details.

## Usage Examples

### Basic Face Detection

```java
ImageData image = ImageData.fromFile("group_photo.jpg");
List<Detection> faces = visionTemplate.detectFaces(image);

faces.

forEach(face ->{
        System.out.

println("Face detected at: "+face.boundingBox());
        System.out.

println("Confidence: "+face.confidence());
        });
```

### Face Recognition

```java

@Service
public class FaceRecognitionService {

    @Autowired
    private VisionTemplate visionTemplate;

    public String recognizePerson(MultipartFile photo) throws IOException {
        ImageData imageData = ImageData.fromBytes(photo.getBytes());
        List<Detection> faces = visionTemplate.detectFaces(imageData);

        if (faces.isEmpty()) {
            return "No face detected";
        }

        Detection face = faces.get(0);
        String subjectId = (String) face.attributes().get("subject");

        return subjectId != null ? subjectId : "Unknown person";
    }
}
```

### Face Analysis

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
        }
```

## Performance

- Face Detection: ~50-100ms per image
- Face Recognition: ~100-200ms per image
- Face Verification: ~150-300ms per comparison

*Performance depends on CompreFace server hardware and network latency*

## Requirements

- Java 21+
- Spring Boot 3.2+
- Running CompreFace server (Docker recommended)
- Network access to CompreFace API

## Troubleshooting

### Connection Refused

Ensure CompreFace server is running:

```bash
curl http://localhost:8000/status
```

### Invalid API Key

Check your API key configuration and ensure it matches the CompreFace server settings.

### Low Recognition Accuracy

Try adjusting the recognition threshold:

```properties
spring.vision.compreface.recognition-threshold=0.7
```

## Best Practices

1. **Use HTTPS**: Always use HTTPS in production
2. **Secure API Keys**: Store API keys in environment variables
3. **Tune Thresholds**: Adjust confidence thresholds for your use case
4. **Handle Errors**: Implement proper error handling and retries
5. **Monitor Performance**: Track API response times and errors

## License

See main project LICENSE file.
