# DeepFace Backend Integration Guide

This document describes how to integrate and use the DeepFace backend with Spring Vision, which provides advanced face detection, recognition, and analysis capabilities through the DeepFace Python library.

## Overview

The DeepFace backend integrates with the popular [serengil/deepface](https://github.com/serengil/deepface) library through its Docker container's HTTP API. This backend offers:

- **Advanced Deep Learning Models** - State-of-the-art models like VGG-Face, FaceNet, OpenFace, DeepID, ArcFace
- **Face Analysis** - Age, gender, emotion, and race detection
- **Face Recognition** - High-accuracy face verification and identification
- **Multiple Detectors** - Support for different face detection backends (OpenCV, RetinaFace, MTCNN, etc.)
- **Production Ready** - Official Docker container with built-in REST API

## Quick Start

### 1. Start DeepFace Service

The easiest way to get started is using the official Docker container:

```bash
# Using Docker directly
docker run -d -p 5000:5000 --name spring-vision-deepface serengil/deepface

# Using Docker Compose (recommended)
docker-compose up -d deepface
```

### 2. Configure Spring Vision

Add the DeepFace backend configuration to your `application.yml`:

```yaml
spring:
  vision:
    backend: deepface
    deepface:
      enabled: true
      url: "http://localhost:5000"
      timeout: 30s
      detector: "opencv"              # opencv, retinaface, mtcnn, etc.
      model: "VGG-Face"              # VGG-Face, Facenet, OpenFace, etc.
```

### 3. Use the Backend

```java
@Service
public class FaceAnalysisService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<Detection> analyzeFaces(byte[] imageData) {
        // Using new DetectionQuery API
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.7)
            .categories(Set.of(DetectionCategory.FACE))
            .build();
            
        List<Detection> detections = visionTemplate.detect(new ImageData(imageData), query);
        
        // DeepFace provides rich attributes
        detections.forEach(detection -> {
            System.out.println("Face confidence: " + detection.getConfidence());
            
            // Age analysis
            if (detection.getAttributes().containsKey("age")) {
                System.out.println("Age: " + detection.getAttribute("age"));
            }
            
            // Gender analysis  
            if (detection.getAttributes().containsKey("gender")) {
                System.out.println("Gender: " + detection.getAttribute("gender"));
            }
            
            // Emotion analysis
            if (detection.getAttributes().containsKey("emotion")) {
                System.out.println("Emotion: " + detection.getAttribute("emotion"));
            }
        });
        
        return detections;
    }
}
```

## Configuration Options

### Backend Properties

Configure the DeepFace backend in `application.yml`:

```yaml
spring:
  vision:
    deepface:
      enabled: true
      url: "http://localhost:5000"        # DeepFace service URL
      timeout: 30s                       # Request timeout
      
      # Face detection configuration
      detector: "opencv"                 # opencv, retinaface, mtcnn, ssd, dlib
      
      # Face recognition model
      model: "VGG-Face"                  # VGG-Face, Facenet, Facenet512, OpenFace, DeepFace, DeepID, ArcFace, Dlib
      
      # Analysis options
      analysis:
        enabled: true
        actions: ["age", "gender", "emotion", "race"]
        
      # Verification options  
      verification:
        enabled: true
        distance-metric: "cosine"        # cosine, euclidean, euclidean_l2
        
      # Connection settings
      connection:
        max-connections: 20
        connection-timeout: 5s
        read-timeout: 30s
```

### Environment Variables

```bash
# Backend selection
export SPRING_VISION_BACKEND=deepface
export SPRING_VISION_DEEPFACE_ENABLED=true

# Service connection
export SPRING_VISION_DEEPFACE_URL=http://deepface-service:5000
export SPRING_VISION_DEEPFACE_TIMEOUT=45s

# Model configuration
export SPRING_VISION_DEEPFACE_DETECTOR=retinaface
export SPRING_VISION_DEEPFACE_MODEL=ArcFace
```

## Docker Deployment

### Docker Compose Configuration

Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  deepface:
    image: serengil/deepface:latest
    container_name: spring-vision-deepface
    ports:
      - "5000:5000"
    environment:
      - DEEPFACE_PORT=5000
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
      
  spring-app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      deepface:
        condition: service_healthy
    environment:
      - SPRING_VISION_BACKEND=deepface
      - SPRING_VISION_DEEPFACE_URL=http://deepface:5000
```

### Production Deployment

For production deployments:

```yaml
version: '3.8'

services:
  deepface:
    image: serengil/deepface:latest
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 4G
          cpus: '2.0'
        reservations:
          memory: 2G
          cpus: '1.0'
    environment:
      - DEEPFACE_PORT=5000
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/"]
      interval: 15s
      timeout: 5s
      retries: 3
      
  nginx:
    image: nginx:alpine
    ports:
      - "5000:5000"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - deepface
```

## API Integration

### Face Detection and Analysis

The DeepFace backend implements the standard Spring Vision APIs:

```java
@RestController
@RequestMapping("/api/face")
public class FaceController {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @PostMapping("/analyze")
    public ResponseEntity<List<Detection>> analyzeFace(
            @RequestParam("file") MultipartFile file) throws IOException {
            
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .categories(Set.of(DetectionCategory.FACE))
            .minConfidence(0.6)
            .option("analysis", true)
            .option("actions", List.of("age", "gender", "emotion"))
            .build();
            
        List<Detection> results = visionTemplate.detect(
            new ImageData(file.getBytes()), query);
            
        return ResponseEntity.ok(results);
    }
}
```

### Face Verification

```java
@Service
public class FaceVerificationService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public boolean verifyFaces(byte[] image1, byte[] image2) {
        return visionTemplate.verify(
            new ImageData(image1),
            new ImageData(image2),
            "cosine",    // Distance metric
            0.68         // Threshold
        );
    }
    
    public VerificationResult verifyWithDetails(byte[] image1, byte[] image2) {
        // Use backend-specific API for detailed results
        VisionBackend backend = visionTemplate.getActiveBackend();
        if (backend instanceof DeepFaceVisionBackend deepFaceBackend) {
            return deepFaceBackend.verifyWithDetails(
                new ImageData(image1), 
                new ImageData(image2)
            );
        }
        
        throw new UnsupportedOperationException("Detailed verification not supported");
    }
}
```

### Face Embeddings

```java
@Service
public class FaceEmbeddingService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<float[]> extractEmbeddings(byte[] imageData) {
        return visionTemplate.extractEmbeddings(new ImageData(imageData));
    }
    
    public double calculateSimilarity(float[] embedding1, float[] embedding2) {
        // Cosine similarity calculation
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
```

## DeepFace Models and Detectors

### Available Models

| Model | Description | Embedding Size | Accuracy |
|-------|-------------|----------------|----------|
| VGG-Face | Original VGGFace model | 2622 | High |
| Facenet | Google's FaceNet | 128 | Very High |
| Facenet512 | Extended FaceNet | 512 | Very High |
| OpenFace | OpenFace model | 128 | Medium |
| DeepFace | Facebook's DeepFace | 4096 | High |
| ArcFace | ArcFace model | 512 | Very High |
| Dlib | Dlib face recognition | 128 | Medium |

### Available Detectors

| Detector | Speed | Accuracy | Description |
|----------|-------|----------|-------------|
| opencv | Fast | Medium | OpenCV Haar Cascades |
| retinaface | Medium | Very High | RetinaFace detector |
| mtcnn | Slow | High | Multi-task CNN |
| ssd | Fast | Medium | SSD MobileNet |
| dlib | Medium | Medium | Dlib HOG detector |

## Performance Optimization

### Model Selection

Choose models based on your requirements:

```yaml
# For speed (development/testing)
spring:
  vision:
    deepface:
      detector: "opencv"
      model: "OpenFace"
      
# For accuracy (production)
spring:
  vision:
    deepface:
      detector: "retinaface"
      model: "ArcFace"
      
# Balanced approach
spring:
  vision:
    deepface:
      detector: "ssd"
      model: "Facenet"
```

### Connection Pooling

Configure connection pooling for better performance:

```yaml
spring:
  vision:
    deepface:
      connection:
        max-connections: 50
        max-connections-per-route: 20
        connection-timeout: 5s
        read-timeout: 30s
        keep-alive: 30s
```

### Caching

Implement result caching for repeated requests:

```java
@Service
@EnableCaching
public class CachedFaceService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Cacheable(value = "face-analysis", key = "#imageHash")
    public List<Detection> analyzeFace(String imageHash, byte[] imageData) {
        return visionTemplate.detectFaces(imageData);
    }
}
```

## Monitoring and Health Checks

### Health Check

The backend provides health information:

```java
@RestController
public class HealthController {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @GetMapping("/health/deepface")
    public ResponseEntity<BackendHealthInfo> health() {
        VisionBackend backend = visionTemplate.getActiveBackend();
        BackendHealthInfo health = backend.getHealthInfo();
        
        if (health.getStatus() == HealthStatus.UP) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
```

### Metrics

Monitor DeepFace backend performance:

```java
@Component
public class DeepFaceMetrics {
    
    private final Counter requestCounter;
    private final Timer responseTimer;
    private final Gauge healthGauge;
    
    public DeepFaceMetrics(MeterRegistry registry) {
        this.requestCounter = Counter.builder("deepface.requests.total")
            .description("Total DeepFace requests")
            .tag("backend", "deepface")
            .register(registry);
            
        this.responseTimer = Timer.builder("deepface.response.duration")
            .description("DeepFace response time")
            .register(registry);
            
        this.healthGauge = Gauge.builder("deepface.health.status")
            .description("DeepFace backend health")
            .register(registry, this, DeepFaceMetrics::getHealthStatus);
    }
    
    private double getHealthStatus() {
        // Return 1 for UP, 0 for DOWN
        return 1.0; // Implementation depends on actual health check
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Service Connection Failed

```bash
# Check if DeepFace container is running
docker ps | grep deepface

# Check container logs
docker logs spring-vision-deepface

# Test direct API access
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d '{"img_path": "/path/to/test/image.jpg", "actions": ["age"]}'
```

#### 2. High Memory Usage

```bash
# Monitor container resources
docker stats spring-vision-deepface

# Adjust container memory limits
docker run -d -p 5000:5000 --memory=4g serengil/deepface
```

#### 3. Slow Response Times

- Use faster detectors (opencv, ssd)
- Choose lighter models (OpenFace, Facenet)
- Implement request caching
- Scale with multiple containers

#### 4. Model Loading Errors

```bash
# Check available disk space
df -h

# Clear model cache if needed
docker exec spring-vision-deepface rm -rf /root/.deepface/weights/

# Restart container to reload models
docker restart spring-vision-deepface
```

### Logging Configuration

Enable detailed logging for troubleshooting:

```yaml
logging:
  level:
    com.springvision.core.backend.DeepFaceVisionBackend: DEBUG
    org.springframework.web.client: DEBUG
    
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## Migration Guide

### From Other Backends

#### From OpenCV Backend

```java
// Before (OpenCV)
@Value("${spring.vision.backend:opencv}")
private String backend;

// After (DeepFace)  
@Value("${spring.vision.backend:deepface}")
private String backend;
```

Update configuration:

```yaml
# Before
spring:
  vision:
    backend: opencv
    opencv:
      enabled: true
      
# After
spring:
  vision:
    backend: deepface
    deepface:
      enabled: true
      url: "http://localhost:5000"
```

#### API Compatibility

The Spring Vision API remains the same:

```java
// This code works with any backend
List<Detection> faces = visionTemplate.detectFaces(imageData);
boolean same = visionTemplate.verify(image1, image2, "cosine", 0.6);
List<float[]> embeddings = visionTemplate.extractEmbeddings(imageData);
```

## Best Practices

### 1. Environment-Specific Configuration

```yaml
# Development
spring:
  profiles: dev
  vision:
    deepface:
      detector: "opencv"
      model: "OpenFace"
      timeout: 60s

---
# Production
spring:
  profiles: prod
  vision:
    deepface:
      detector: "retinaface"
      model: "ArcFace"
      timeout: 30s
      connection:
        max-connections: 100
```

### 2. Error Handling

```java
@Service
public class RobustFaceService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Retryable(value = {VisionProcessingException.class}, maxAttempts = 3)
    public List<Detection> detectFacesWithRetry(byte[] imageData) {
        try {
            return visionTemplate.detectFaces(imageData);
        } catch (VisionProcessingException e) {
            log.warn("Face detection attempt failed, retrying: {}", e.getMessage());
            throw e;
        }
    }
    
    @Recover
    public List<Detection> recoverFromFailure(VisionProcessingException ex, byte[] imageData) {
        log.error("All face detection attempts failed: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
```

### 3. Resource Management

```java
@Configuration
public class DeepFaceConfig {
    
    @Bean
    @ConditionalOnProperty(value = "spring.vision.deepface.enabled", havingValue = "true")
    public RestTemplate deepFaceRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }
}
```

## Support and Resources

- **DeepFace Documentation**: [GitHub Repository](https://github.com/serengil/deepface)
- **Spring Vision API Reference**: [API_REFERENCE.md](API_REFERENCE.md)
- **Performance Tuning**: [ARCHITECTURE.md](ARCHITECTURE.md#performance-considerations)
- **Docker Hub**: [serengil/deepface](https://hub.docker.com/r/serengil/deepface)

For issues specific to the DeepFace backend integration, please check the troubleshooting section above or create an issue in the Spring Vision repository.
