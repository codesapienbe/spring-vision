# CompreFace Backend Integration Guide

This document describes how to integrate and use the CompreFace backend with Spring Vision, which provides enterprise-grade face recognition and analysis capabilities through the CompreFace recognition service.

## Overview

The CompreFace backend integrates with the [Exadel CompreFace](https://github.com/exadel-inc/CompreFace) service, which is a free and open-source face recognition system. This backend offers:

- **Enterprise-Grade Recognition** - Advanced face recognition with high accuracy
- **Multiple Services** - Face detection, verification, recognition, and analysis
- **RESTful API** - Production-ready HTTP API with comprehensive documentation
- **Scalable Architecture** - Microservices-based design for high-performance deployments
- **Easy Integration** - Simple setup with Docker Compose
- **No Vendor Lock-in** - Open source with commercial support available

## Quick Start

### 1. Start CompreFace Service

The easiest way to get started is using Docker Compose:

```bash
# Clone CompreFace repository
git clone https://github.com/exadel-inc/CompreFace.git
cd CompreFace

# Start CompreFace services
docker-compose up -d

# Or use our simplified configuration
docker-compose -f docker-compose.compreface.yml up -d
```

### 2. Configure Spring Vision

Add the CompreFace backend configuration to your `application.yml`:

```yaml
spring:
  vision:
    backend: compreface
    compreface:
      enabled: true
      url: "http://localhost:8000"
      api-key: "${COMPREFACE_API_KEY}"
      timeout: 30s
      
      # Service configuration
      detection:
        enabled: true
        limit: 0                       # 0 = no limit
        det-prob-threshold: 0.8        # Detection confidence threshold
        
      verification:
        enabled: true
        limit: 0
        det-prob-threshold: 0.8
        
      recognition:
        enabled: true
        subject-name: "default"        # Default subject collection
        
      analysis:
        enabled: false                 # Age, gender analysis (premium feature)
```

### 3. Use the Backend

```java
@Service
public class FaceRecognitionService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<Detection> recognizeFaces(byte[] imageData) {
        // Using new DetectionQuery API
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.8)
            .categories(Set.of(DetectionCategory.FACE))
            .option("service", "recognition")
            .build();
            
        List<Detection> detections = visionTemplate.detect(new ImageData(imageData), query);
        
        // CompreFace provides recognition results
        detections.forEach(detection -> {
            System.out.println("Face confidence: " + detection.getConfidence());
            
            // Subject recognition
            if (detection.getAttributes().containsKey("subject")) {
                System.out.println("Subject: " + detection.getAttribute("subject"));
            }
            
            // Similarity score
            if (detection.getAttributes().containsKey("similarity")) {
                System.out.println("Similarity: " + detection.getAttribute("similarity"));
            }
            
            // Bounding box
            BoundingBox box = detection.getBoundingBox();
            System.out.println("Location: " + box.getX() + "," + box.getY());
        });
        
        return detections;
    }
}
```

## Configuration Options

### Backend Properties

Configure the CompreFace backend in `application.yml`:

```yaml
spring:
  vision:
    compreface:
      enabled: true
      url: "http://localhost:8000"             # CompreFace service URL
      api-key: "${COMPREFACE_API_KEY}"         # API key from CompreFace admin
      timeout: 30s                            # Request timeout
      
      # Face detection service
      detection:
        enabled: true
        limit: 0                              # Max faces to detect (0 = unlimited)
        det-prob-threshold: 0.8               # Detection confidence threshold
        face-plugins: "landmarks,age,gender"  # Additional analysis plugins
        
      # Face verification service  
      verification:
        enabled: true
        limit: 0
        det-prob-threshold: 0.8
        face-plugins: "landmarks"
        
      # Face recognition service
      recognition:
        enabled: true
        limit: 0
        det-prob-threshold: 0.8
        prediction-count: 1                   # Max predictions per face
        subject-name: "default"               # Default subject collection
        face-plugins: "landmarks"
        
      # Face analysis service (premium)
      analysis:
        enabled: false
        face-plugins: "age,gender,landmarks"
        
      # Connection settings
      connection:
        max-connections: 50
        connection-timeout: 10s
        read-timeout: 30s
        retry-attempts: 3
```

### Environment Variables

```bash
# Backend selection
export SPRING_VISION_BACKEND=compreface
export SPRING_VISION_COMPREFACE_ENABLED=true

# Service connection
export SPRING_VISION_COMPREFACE_URL=http://compreface:8000
export COMPREFACE_API_KEY=your-api-key-here
export SPRING_VISION_COMPREFACE_TIMEOUT=45s

# Service configuration
export SPRING_VISION_COMPREFACE_DETECTION_ENABLED=true
export SPRING_VISION_COMPREFACE_RECOGNITION_ENABLED=true
```

## Docker Deployment

### Docker Compose Configuration

Create a `docker-compose.compreface.yml` file:

```yaml
version: '3.8'

services:
  compreface-postgres-db:
    image: postgres:11.5
    container_name: "compreface-postgres-db"
    restart: always
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_DB=frs
    volumes:
      - compreface-db-data:/var/lib/postgresql/data

  compreface-admin:
    image: exadel/compreface-admin:1.2.0
    container_name: "compreface-admin"
    restart: always
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_URL=jdbc:postgresql://compreface-postgres-db:5432/frs
      - SPRING_PROFILES_ACTIVE=dev
      - ENABLE_EMAIL_SERVER=false
      - EMAIL_HOST=smtp.gmail.com
      - EMAIL_USERNAME=username
      - EMAIL_FROM=username@gmail.com
      - EMAIL_PASSWORD=password
      - ADMIN_JAVA_OPTS=-Xmx8g
    depends_on:
      - compreface-postgres-db
    ports:
      - "8000:8080"

  compreface-api:
    image: exadel/compreface-api:1.2.0
    container_name: "compreface-api"
    restart: always
    depends_on:
      - compreface-postgres-db
      - compreface-admin
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
      - POSTGRES_URL=jdbc:postgresql://compreface-postgres-db:5432/frs
      - SPRING_PROFILES_ACTIVE=dev
      - API_JAVA_OPTS=-Xmx8g
    ports:
      - "8001:8080"

  compreface-fe:
    image: exadel/compreface-fe:1.2.0
    container_name: "compreface-ui"
    restart: always
    ports:
      - "8002:80"
    depends_on:
      - compreface-api
      - compreface-admin
    environment:
      - API_URL=http://compreface-api:8080

  spring-app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - compreface-admin
      - compreface-api
    environment:
      - SPRING_VISION_BACKEND=compreface
      - SPRING_VISION_COMPREFACE_URL=http://compreface-admin:8080

volumes:
  compreface-db-data:
```

### Production Deployment

For production deployments with load balancing:

```yaml
version: '3.8'

services:
  compreface-admin:
    image: exadel/compreface-admin:1.2.0
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 8G
          cpus: '4.0'
        reservations:
          memory: 4G
          cpus: '2.0'
    environment:
      - ADMIN_JAVA_OPTS=-Xmx6g
      - SPRING_PROFILES_ACTIVE=prod
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  nginx:
    image: nginx:alpine
    ports:
      - "8000:8000"
    volumes:
      - ./nginx-compreface.conf:/etc/nginx/nginx.conf
    depends_on:
      - compreface-admin
```

## API Integration

### Face Detection

The CompreFace backend implements the standard Spring Vision APIs:

```java
@RestController
@RequestMapping("/api/face")
public class FaceController {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @PostMapping("/detect")
    public ResponseEntity<List<Detection>> detectFaces(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", defaultValue = "0.8") Double threshold) 
            throws IOException {
            
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .categories(Set.of(DetectionCategory.FACE))
            .minConfidence(threshold)
            .option("service", "detection")
            .option("face_plugins", "landmarks")
            .build();
            
        List<Detection> results = visionTemplate.detect(
            new ImageData(file.getBytes()), query);
            
        return ResponseEntity.ok(results);
    }
}
```

### Face Recognition

```java
@Service
public class FaceRecognitionService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<Recognition> recognizeFaces(byte[] imageData, String subjectName) {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .minConfidence(0.8)
            .option("service", "recognition")
            .option("subject_name", subjectName)
            .option("prediction_count", 5)
            .build();
            
        List<Detection> detections = visionTemplate.detect(new ImageData(imageData), query);
        
        return detections.stream()
            .map(this::mapToRecognition)
            .collect(Collectors.toList());
    }
    
    public boolean addSubject(String subjectName, byte[] imageData) {
        // Use CompreFace-specific API for subject management
        VisionBackend backend = visionTemplate.getActiveBackend();
        if (backend instanceof CompreFaceVisionBackend compreFaceBackend) {
            return compreFaceBackend.addSubject(subjectName, new ImageData(imageData));
        }
        
        throw new UnsupportedOperationException("Subject management not supported");
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
            0.6          // Threshold
        );
    }
    
    public VerificationResult verifyWithDetails(byte[] sourceImage, byte[] targetImage) {
        // Use CompreFace-specific verification
        VisionBackend backend = visionTemplate.getActiveBackend();
        if (backend instanceof CompreFaceVisionBackend compreFaceBackend) {
            return compreFaceBackend.verifyWithDetails(
                new ImageData(sourceImage),
                new ImageData(targetImage)
            );
        }
        
        throw new UnsupportedOperationException("Detailed verification not supported");
    }
}
```

## CompreFace Services

### Available Services

| Service | Purpose | API Endpoint | Features |
|---------|---------|--------------|----------|
| **Detection** | Find faces in images | `/api/v1/detection/detect` | Face detection, landmarks |
| **Verification** | Compare two faces | `/api/v1/verification/verify` | 1:1 face comparison |
| **Recognition** | Identify faces | `/api/v1/recognition/recognize` | 1:N face identification |
| **Analysis** | Face attributes | `/api/v1/detection/detect` | Age, gender, emotions* |

*Premium features may require license

### Service Configuration

Configure which services to enable:

```yaml
spring:
  vision:
    compreface:
      services:
        detection:
          enabled: true
          endpoint: "/api/v1/detection/detect"
          default-limit: 0
          default-threshold: 0.8
          
        verification:
          enabled: true  
          endpoint: "/api/v1/verification/verify"
          default-threshold: 0.8
          
        recognition:
          enabled: true
          endpoint: "/api/v1/recognition/recognize" 
          default-subject: "employees"
          max-predictions: 1
          
        analysis:
          enabled: false
          plugins: ["age", "gender", "landmarks"]
```

## Subject Management

### Managing Face Collections

CompreFace organizes faces into subject collections:

```java
@Service
public class SubjectManagementService {
    
    @Autowired
    private CompreFaceVisionBackend compreFaceBackend;
    
    public boolean createSubject(String subjectName, byte[] faceImage) {
        return compreFaceBackend.addSubject(subjectName, new ImageData(faceImage));
    }
    
    public boolean addFaceToSubject(String subjectName, byte[] faceImage) {
        return compreFaceBackend.addFaceToSubject(subjectName, new ImageData(faceImage));
    }
    
    public List<String> listSubjects() {
        return compreFaceBackend.getSubjects();
    }
    
    public boolean deleteSubject(String subjectName) {
        return compreFaceBackend.deleteSubject(subjectName);
    }
    
    public List<String> getSubjectFaces(String subjectName) {
        return compreFaceBackend.getSubjectFaces(subjectName);
    }
}
```

### Bulk Subject Operations

```java
@Service
public class BulkSubjectService {
    
    @Autowired
    private SubjectManagementService subjectService;
    
    @Async
    public CompletableFuture<Void> importSubjects(Map<String, List<byte[]>> subjectData) {
        return CompletableFuture.runAsync(() -> {
            subjectData.forEach((subjectName, faceImages) -> {
                try {
                    // Add first image as subject
                    if (!faceImages.isEmpty()) {
                        subjectService.createSubject(subjectName, faceImages.get(0));
                        
                        // Add remaining images
                        faceImages.stream().skip(1).forEach(faceImage -> 
                            subjectService.addFaceToSubject(subjectName, faceImage)
                        );
                    }
                } catch (Exception e) {
                    log.error("Failed to import subject: " + subjectName, e);
                }
            });
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

## Performance Optimization

### Connection Configuration

Optimize HTTP connections for better performance:

```yaml
spring:
  vision:
    compreface:
      connection:
        # Connection pool settings
        max-total-connections: 200
        max-connections-per-route: 50
        connection-timeout: 10s
        socket-timeout: 30s
        
        # Keep-alive settings
        keep-alive-duration: 30s
        max-idle-connections: 10
        
        # Retry configuration
        retry-attempts: 3
        retry-delay: 1s
```

### Caching Strategy

Implement caching for repeated recognition requests:

```java
@Service
@EnableCaching
public class CachedRecognitionService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Cacheable(value = "face-recognition", key = "#imageHash + '-' + #subjectName")
    public List<Detection> recognizeFaces(String imageHash, String subjectName, byte[] imageData) {
        DetectionQuery query = DetectionQuery.builder()
            .type(DetectionType.FACE)
            .option("service", "recognition")
            .option("subject_name", subjectName)
            .build();
            
        return visionTemplate.detect(new ImageData(imageData), query);
    }
    
    @CacheEvict(value = "face-recognition", allEntries = true)
    public void clearCache() {
        // Called when subjects are updated
    }
}
```

### Async Processing

Process multiple images concurrently:

```java
@Service
public class AsyncFaceService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    @Async
    public CompletableFuture<List<Detection>> detectFacesAsync(byte[] imageData) {
        return CompletableFuture.supplyAsync(() -> {
            return visionTemplate.detectFaces(imageData);
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    @Async
    public CompletableFuture<BatchResult> processBatch(List<byte[]> images) {
        List<CompletableFuture<List<Detection>>> futures = images.stream()
            .map(this::detectFacesAsync)
            .collect(Collectors.toList());
            
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .thenApply(BatchResult::new);
    }
}
```

## Monitoring and Health Checks

### Health Check Implementation

```java
@Component
public class CompreFaceHealthIndicator implements HealthIndicator {
    
    @Autowired
    private CompreFaceVisionBackend compreFaceBackend;
    
    @Override
    public Health health() {
        try {
            BackendHealthInfo healthInfo = compreFaceBackend.getHealthInfo();
            
            if (healthInfo.getStatus() == HealthStatus.UP) {
                return Health.up()
                    .withDetail("backend", "CompreFace")
                    .withDetail("version", healthInfo.getVersion())
                    .withDetail("services", healthInfo.getDetails().get("services"))
                    .withDetail("lastCheck", Instant.now())
                    .build();
            } else {
                return Health.down()
                    .withDetail("backend", "CompreFace")
                    .withDetail("error", healthInfo.getDetails().get("error"))
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("backend", "CompreFace")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Metrics Collection

```java
@Component
public class CompreFaceMetrics {
    
    private final Counter detectionCounter;
    private final Counter recognitionCounter;
    private final Counter verificationCounter;
    private final Timer responseTimer;
    private final Gauge subjectCount;
    
    public CompreFaceMetrics(MeterRegistry registry) {
        this.detectionCounter = Counter.builder("compreface.detections.total")
            .description("Total face detections")
            .tag("backend", "compreface")
            .register(registry);
            
        this.recognitionCounter = Counter.builder("compreface.recognitions.total")
            .description("Total face recognitions")
            .register(registry);
            
        this.verificationCounter = Counter.builder("compreface.verifications.total")
            .description("Total face verifications")
            .register(registry);
            
        this.responseTimer = Timer.builder("compreface.response.duration")
            .description("CompreFace response time")
            .register(registry);
            
        this.subjectCount = Gauge.builder("compreface.subjects.count")
            .description("Number of registered subjects")
            .register(registry, this, CompreFaceMetrics::getSubjectCount);
    }
    
    private double getSubjectCount() {
        // Implementation to count subjects
        return 0.0; // Placeholder
    }
    
    public void recordDetection(Duration duration, int faceCount) {
        detectionCounter.increment();
        responseTimer.record(duration);
    }
}
```

## Security Considerations

### API Key Management

Store API keys securely:

```yaml
# Use environment variables or secrets
spring:
  vision:
    compreface:
      api-key: "${COMPREFACE_API_KEY:default-key}"
      
# Or use Spring Cloud Config
---
spring:
  profiles: prod
  vision:
    compreface:
      api-key: "${vault.secret.compreface-api-key}"
```

### Network Security

Configure secure communication:

```yaml
spring:
  vision:
    compreface:
      url: "https://compreface.company.com"
      ssl:
        verify-certificates: true
        trust-store: "/path/to/truststore.jks"
        trust-store-password: "${TRUST_STORE_PASSWORD}"
```

### Data Protection

Implement data protection measures:

```java
@Service
public class SecureFaceService {
    
    @Autowired
    private VisionTemplate visionTemplate;
    
    public List<Detection> detectFacesSecure(byte[] imageData) {
        // Validate image size and format
        if (imageData.length > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image too large");
        }
        
        try {
            return visionTemplate.detectFaces(imageData);
        } finally {
            // Clear sensitive data from memory
            Arrays.fill(imageData, (byte) 0);
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Service Connection Failed

```bash
# Check CompreFace services status
docker-compose ps

# Check admin service logs
docker logs compreface-admin

# Test API connectivity
curl -X GET "http://localhost:8000/actuator/health"
```

#### 2. API Key Authentication Failed

```bash
# Verify API key in CompreFace admin UI
# http://localhost:8000

# Test API key
curl -X POST "http://localhost:8000/api/v1/detection/detect" \
  -H "x-api-key: YOUR_API_KEY" \
  -F "file=@test-image.jpg"
```

#### 3. High Response Times

- Check CompreFace service resource allocation
- Optimize image sizes before sending
- Implement connection pooling
- Use caching for repeated requests

#### 4. Memory Issues

```bash
# Monitor CompreFace containers
docker stats compreface-admin compreface-api

# Adjust JVM memory settings
# In docker-compose.yml:
# ADMIN_JAVA_OPTS=-Xmx8g -XX:+UseG1GC
```

### Logging Configuration

Enable detailed logging:

```yaml
logging:
  level:
    com.springvision.core.backend.CompreFaceVisionBackend: DEBUG
    org.springframework.web.client.RestTemplate: DEBUG
    
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## Migration Guide

### From Other Backends

#### From OpenCV Backend

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
    backend: compreface
    compreface:
      enabled: true
      url: "http://localhost:8000"
      api-key: "${COMPREFACE_API_KEY}"
```

#### API Compatibility

The Spring Vision API remains consistent:

```java
// This code works with any backend
List<Detection> faces = visionTemplate.detectFaces(imageData);
boolean same = visionTemplate.verify(image1, image2, "cosine", 0.6);
List<float[]> embeddings = visionTemplate.extractEmbeddings(imageData);
```

## Best Practices

### 1. Environment Configuration

```yaml
# Development
spring:
  profiles: dev
  vision:
    compreface:
      url: "http://localhost:8000"
      timeout: 60s
      
---
# Production
spring:
  profiles: prod
  vision:
    compreface:
      url: "https://compreface.prod.company.com"
      timeout: 30s
      connection:
        max-total-connections: 200
```

### 2. Error Handling

```java
@Service
public class RobustCompreFaceService {
    
    @Retryable(value = {VisionProcessingException.class}, maxAttempts = 3)
    public List<Detection> detectWithRetry(byte[] imageData) {
        return visionTemplate.detectFaces(imageData);
    }
    
    @Recover
    public List<Detection> recoverFromFailure(VisionProcessingException ex, byte[] imageData) {
        log.error("CompreFace detection failed after retries: {}", ex.getMessage());
        // Return cached results or empty list
        return Collections.emptyList();
    }
}
```

### 3. Resource Management

```java
@Configuration
public class CompreFaceConfig {
    
    @Bean
    @ConditionalOnProperty(value = "spring.vision.compreface.enabled", havingValue = "true")
    public RestTemplate compreFaceRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }
}
```

## Support and Resources

- **CompreFace Documentation**: [GitHub Repository](https://github.com/exadel-inc/CompreFace)
- **Spring Vision API Reference**: [API_REFERENCE.md](API_REFERENCE.md)
- **Performance Tuning**: [ARCHITECTURE.md](ARCHITECTURE.md#performance-considerations)
- **Official Website**: [CompreFace.com](https://compreface.com)
- **Commercial Support**: Available from Exadel

For issues specific to the CompreFace backend integration, please check the troubleshooting section above or create an issue in the Spring Vision repository. 