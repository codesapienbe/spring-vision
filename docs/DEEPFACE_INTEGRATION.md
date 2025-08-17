# DeepFace Integration

This document describes how to use the DeepFace backend with Spring Vision, which connects to the serengil/deepface Docker container's built-in HTTP API.

## Overview

The DeepFace backend provides advanced face detection, recognition, and analysis capabilities by leveraging the DeepFace library through the official Docker container's HTTP API. This approach offers several advantages:

- **Advanced ML Models**: DeepFace uses state-of-the-art deep learning models for face detection and analysis
- **Multiple Analysis Types**: Age, gender, emotion, and race detection
- **Face Recognition**: Face verification and similarity matching
- **Built-in HTTP API**: The official Docker image provides a complete REST API
- **Spring WebClient**: Proper HTTP communication with timeout and error handling

## Quick Start

### 1. Start the DeepFace Container

```bash
# Start the DeepFace container with built-in API
docker run -d -p 5000:5000 --name spring-vision-deepface serengil/deepface

# Or use Docker Compose
docker-compose -f docker-compose.yml up -d

# Check if the container is running
docker ps | grep deepface
```

### 2. Configure Spring Vision

Add the DeepFace backend configuration to your `application.yml`:

```yaml
vision:
  backend: deepface
  deepface:
    enabled: true
    api-url: http://localhost:5000
    timeout: 30s
```

### 3. Use the DeepFace Backend

```java
@Autowired
private VisionTemplate visionTemplate;

public void detectFaces(byte[] imageData) {
    VisionResult result = visionTemplate.detectFaces(imageData);
    
    if (result.hasDetections()) {
        result.detections().forEach(detection -> {
            System.out.println("Face detected with confidence: " + detection.confidence());
            System.out.println("Bounding box: " + detection.boundingBox());
            
            // Access DeepFace-specific attributes
            if (detection.hasAttribute("age")) {
                System.out.println("Age: " + detection.getAttribute("age"));
            }
            if (detection.hasAttribute("gender")) {
                System.out.println("Gender: " + detection.getAttribute("gender"));
            }
        });
    }
}
```

## API Endpoints

The DeepFace container provides the following built-in endpoints:

### Face Analysis

- **POST** `/analyze` - Analyze face attributes (age, gender, emotion, race)
  - Request: `{"img_path": "base64_image", "actions": ["age", "gender", "emotion", "race"]}`
  - Response: `{"results": [{"age": 25, "dominant_gender": "Man", "dominant_emotion": "happy", ...}]}`

### Face Verification

- **POST** `/verify` - Verify if two faces belong to the same person
  - Request: `{"img1_path": "base64_image1", "img2_path": "base64_image2"}`
  - Response: `{"verified": true, "distance": 0.42, "threshold": 0.68, ...}`

### Face Representation

- **POST** `/represent` - Generate face embeddings
  - Request: `{"model_name": "VGG-Face", "img": "base64_image"}`
  - Response: `{"results": [{"embedding": [...], "face_confidence": 0.92, ...}]}`

## Docker Configuration

The `docker-compose.deepface.yml` file uses the official serengil/deepface image:

```yaml
version: '3.8'

services:
  deepface:
    image: serengil/deepface:latest
    container_name: spring-vision-deepface
    ports:
      - "5000:5000"
    restart: unless-stopped
```

### Key Benefits

1. **Official API**: Uses the built-in HTTP API provided by the DeepFace container
2. **Spring WebClient**: Proper HTTP communication with timeout and error handling
3. **No Custom Code**: No need for custom API implementation
4. **Production Ready**: Built-in API with proper error handling and responses

## Configuration Options

### Spring Vision Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `vision.deepface.enabled` | `true` | Enable/disable DeepFace backend |
| `vision.deepface.api-url` | `http://localhost:5000` | DeepFace API server URL |
| `vision.deepface.timeout` | `30s` | Request timeout |

### Environment Variables

```bash
export VISION_BACKEND=deepface
export VISION_DEEPFACE_ENABLED=true
export VISION_DEEPFACE_API_URL=http://localhost:5000
export VISION_DEEPFACE_TIMEOUT=30s
```

## Usage Examples

### Basic Face Detection

```java
DeepFaceVisionBackend backend = new DeepFaceVisionBackend("http://localhost:5000");
backend.initialize();

ImageData imageData = ImageData.fromBytes(imageBytes);
VisionResult result = backend.detectFaces(imageData);
```

### Face Analysis

```java
// Analyze face attributes
Map<String, Object> analysis = backend.analyzeFace(imageData);
System.out.println("Age: " + analysis.get("age"));
System.out.println("Gender: " + analysis.get("gender"));
System.out.println("Emotion: " + analysis.get("emotion"));
System.out.println("Race: " + analysis.get("race"));
```

### Face Verification

```java
// Verify if two faces are the same person
Map<String, Object> verification = backend.verifyFaces(image1Data, image2Data);
boolean isSamePerson = (Boolean) verification.get("verified");
double similarity = (Double) verification.get("distance");
double threshold = (Double) verification.get("threshold");
```

## Troubleshooting

### Common Issues

1. **Container won't start**

   ```bash
   # Check logs
   docker logs spring-vision-deepface
   
   # Check if port 5000 is available
   netstat -tulpn | grep 5000
   ```

2. **API connection timeout**

   ```bash
   # Test API directly
   curl -X POST http://localhost:5000/analyze \
     -H "Content-Type: application/json" \
     -d '{"img_path": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==", "actions": ["age"]}'
   ```

3. **WebClient issues**

   ```bash
   # Check if the API is accessible
   curl http://localhost:5000/analyze
   ```

### Logs

Check DeepFace container logs:

```bash
docker logs -f spring-vision-deepface
```

Check Spring Vision logs:

```bash
# Look for DeepFace backend messages
tail -f application.log | grep -i deepface
```

## Performance Considerations

### Memory Usage

- DeepFace models are loaded into memory when the container starts
- Each API call processes images in memory
- Consider container memory limits based on expected load

### Processing Time

- Face detection: ~100-500ms per image
- Face analysis: ~200-1000ms per face
- Face verification: ~300-1500ms per pair

### Scaling

- Run multiple DeepFace containers behind a load balancer
- Use container orchestration (Kubernetes, Docker Swarm)
- Implement connection pooling in the Java backend

## Migration from Other Backends

### From OpenCV Backend

```java
// Before
OpenCvVisionBackend backend = new OpenCvVisionBackend();

// After
DeepFaceVisionBackend backend = new DeepFaceVisionBackend("http://localhost:5000");
```

### From FaceBytes Backend

```java
// Before
FaceBytesBackend backend = new FaceBytesBackend();

// After
DeepFaceVisionBackend backend = new DeepFaceVisionBackend("http://localhost:5000");
```

The API remains the same, so minimal code changes are required.

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting)
2. Review [DeepFace documentation](https://github.com/serengil/deepface)
3. Check Spring Vision logs for backend-specific errors
4. Open an issue on GitHub
