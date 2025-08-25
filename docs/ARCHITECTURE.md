# Spring Vision Architecture

This document provides a comprehensive overview of the Spring Vision framework architecture, design patterns, and extensibility mechanisms.

## Table of Contents

- [System Overview](#system-overview)
- [Core Architecture](#core-architecture)
- [Module Structure](#module-structure)
- [Backend System](#backend-system)
- [Detection Pipeline](#detection-pipeline)
- [Configuration System](#configuration-system)
- [Security Model](#security-model)
- [Performance Considerations](#performance-considerations)
- [Extension Points](#extension-points)

## System Overview

Spring Vision is a modular computer vision framework designed for Spring Boot applications. It provides a unified API for multiple vision backends while maintaining high performance, security, and extensibility.

### Design Principles

1. **Backend Agnostic**: Unified API across different vision engines
2. **Spring Native**: Full Spring Boot integration with auto-configuration
3. **Performance First**: Optimized for production workloads
4. **Security by Design**: Input validation, SSRF protection, resource limits
5. **Extensible**: Plugin architecture for custom backends
6. **Observable**: Comprehensive logging and metrics

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Vision Framework                    │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │   Web UI    │ │  REST API   │ │      CLI/Desktop       │ │
│  │  (Vaadin/   │ │ (Spring MVC)│ │    (JavaFX/PicoCLI)    │ │
│  │   GWT)      │ │             │ │                        │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Framework Core                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              VisionTemplate                            │ │
│  │         (Main API Entry Point)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │               VisionBackend                            │ │
│  │            (Pluggable Backends)                       │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Backend Implementations                                    │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ ┌─────────────┐ │
│  │  OpenCV  │ │FaceBytes │ │ CompreFace │ │  DeepFace   │ │
│  │ (Native) │ │(Embedded)│ │ (External)  │ │ (External)  │ │
│  └──────────┘ └──────────┘ └─────────────┘ └─────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐                │
│  │MediaPipe │ │   YOLO   │ │ InsightFace │  (Planned)     │
│  │(Google)  │ │ (ONNX)   │ │   (ONNX)    │                │
│  └──────────┘ └──────────┘ └─────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

## Core Architecture

### VisionTemplate - Central Orchestrator

The `VisionTemplate` serves as the main entry point and orchestrates all vision operations:

```java
@Component
public class VisionTemplate {
    private final List<VisionBackend> backends;
    private final VisionProperties properties;
    private final MeterRegistry meterRegistry;
    
    // Primary API methods
    public List<Detection> detect(ImageData imageData, DetectionQuery query) { }
    public List<Detection> detectFaces(byte[] imageData) { }
    public List<float[]> extractEmbeddings(ImageData imageData) { }
    public boolean verify(ImageData a, ImageData b, String metric, double threshold) { }
    public ImageData obscureFaces(ImageData imageData) { }
}
```

**Key Responsibilities:**
- Backend selection and routing
- Request validation and sanitization
- Performance monitoring and metrics
- Error handling and logging
- Capability-based routing

### VisionBackend Interface

The `VisionBackend` interface defines the contract for all vision implementations:

```java
public interface VisionBackend {
    // Core detection method (new API)
    List<Detection> detect(ImageData imageData, DetectionQuery query);
    
    // Legacy methods (deprecated in 2.0)
    @Deprecated List<Detection> detectFaces(ImageData imageData);
    
    // Optional capabilities
    default List<float[]> extractEmbeddings(ImageData imageData) { 
        throw new UnsupportedOperationException("Embeddings not supported");
    }
    
    default boolean verify(ImageData a, ImageData b, String metric, double threshold) {
        throw new UnsupportedOperationException("Verification not supported");
    }
    
    default ImageData obscureFaces(ImageData imageData) {
        throw new UnsupportedOperationException("Face obscuring not supported");
    }
    
    // Health and metadata
    BackendHealthInfo getHealthInfo();
    boolean supports(DetectionType type);
}
```

### Detection Query System

The `DetectionQuery` provides a flexible way to specify detection requirements:

```java
public class DetectionQuery {
    private DetectionType type;                    // FACE, OBJECT, TEXT, etc.
    private Set<DetectionCategory> categories;     // FACE, EYE, NOSE, MOUTH, etc.
    private Set<String> classLabels;              // Specific object classes
    private Double minConfidence;                  // Confidence threshold
    private Integer maxDetections;                 // Limit results
    private BoundingBox roi;                       // Region of interest
    private Map<String, Object> options;          // Backend-specific options
    
    public static DetectionQuery defaultFaceQuery() {
        return builder()
            .type(DetectionType.FACE)
            .categories(Set.of(DetectionCategory.FACE))
            .minConfidence(0.5)
            .build();
    }
}
```

### Capability System

Backends can implement capability interfaces for optional features:

```java
// Embedding extraction capability
public interface EmbeddingCapability extends VisionBackend {
    List<float[]> extractEmbeddings(ImageData imageData);
    boolean verify(ImageData a, ImageData b, String metric, double threshold);
}

// Face annotation capability  
public interface AnnotationCapability extends VisionBackend {
    ImageData annotateFaces(ImageData imageData, AnnotationRequest request);
}

// Barcode detection capability
public interface BarcodeCapability extends VisionBackend {
    List<Detection> detectBarcodes(ImageData imageData);
}
```

## Module Structure

### spring-vision-core

**Core framework components and interfaces**

```
src/main/java/com/springvision/core/
├── VisionTemplate.java              # Main API entry point
├── VisionBackend.java               # Backend interface
├── ImageData.java                   # Image data wrapper
├── Detection.java                   # Detection result
├── DetectionQuery.java              # Query specification
├── DetectionType.java               # FACE, OBJECT, TEXT, etc.
├── DetectionCategory.java           # FACE, EYE, NOSE, etc.
├── BoundingBox.java                 # Normalized coordinates
├── backend/                         # Backend implementations
│   ├── OpenCvVisionBackend.java     # OpenCV integration
│   ├── FaceBytesBackend.java        # FaceBytes integration
│   ├── CompreFaceVisionBackend.java # CompreFace REST API
│   ├── DeepFaceVisionBackend.java   # DeepFace REST API
│   └── MediaPipeVisionBackend.java  # MediaPipe (in progress)
├── capabilities/                    # Optional capability interfaces
│   ├── EmbeddingCapability.java     # Face embeddings
│   ├── AnnotationCapability.java    # Image annotation
│   ├── BarcodeCapability.java       # Barcode detection
│   └── ...
├── exception/                       # Exception hierarchy
│   ├── BaseVisionException.java     
│   ├── VisionProcessingException.java
│   └── VisionConfigurationException.java
└── util/                           # Utility classes
    ├── EmbeddingSupport.java        # Embedding utilities
    └── ImageUtils.java              # Image processing utilities
```

### spring-vision-autoconfigure

**Spring Boot auto-configuration**

```
src/main/java/com/springvision/autoconfigure/
├── VisionAutoConfiguration.java     # Main auto-configuration
├── VisionProperties.java            # Configuration properties
├── VisionHealthIndicator.java       # Health check integration
└── VisionMetrics.java               # Micrometer metrics

src/main/resources/META-INF/
├── spring.factories                 # Auto-configuration registration
├── additional-spring-configuration-metadata.json
└── spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### spring-vision-starter

**Spring Boot starter with REST API**

```
src/main/java/com/springvision/starter/web/
├── VisionController.java            # REST API endpoints
├── dto/                            # Data transfer objects
│   ├── DetectionRequest.java        
│   ├── DetectionResponse.java       
│   ├── HealthResponse.java          
│   └── ...
└── config/
    └── AsyncConfig.java             # Async configuration
```

### spring-vision-facebytes

**FaceBytes integration module**

```
src/main/java/com/deepface/
├── core/                           # Core FaceBytes API
│   ├── DeepFace.java               # Main DeepFace interface
│   ├── AnalysisResult.java         # Analysis results
│   ├── EmbeddingResult.java        # Embedding results
│   └── ...
├── detectors/                      # Face detectors
│   ├── DetectorFactory.java        
│   ├── OpenCVDetector.java         
│   └── ...
├── models/                         # AI models
│   ├── ArcFaceModel.java           
│   ├── VGGFaceModel.java           
│   └── ...
└── utils/                          # Utilities
    ├── DistanceMetrics.java         
    ├── FacePreprocessor.java        
    └── ...
```

## Backend System

### Backend Registration

Backends are automatically discovered via Spring's component scanning:

```java
@Component
@ConditionalOnProperty(value = "spring.vision.opencv.enabled", havingValue = "true")
public class OpenCvVisionBackend implements VisionBackend, EmbeddingCapability {
    
    @Override
    public List<Detection> detect(ImageData imageData, DetectionQuery query) {
        // Implementation
    }
}
```

### Backend Selection Logic

The `VisionTemplate` selects backends based on:

1. **Configuration Priority**: Explicitly configured backend
2. **Capability Matching**: Backend supports required capabilities
3. **Health Status**: Backend is healthy and available
4. **Performance**: Fastest backend for the operation type

```java
public class VisionTemplate {
    
    private VisionBackend selectBackend(DetectionQuery query) {
        // 1. Check explicit backend configuration
        if (properties.getBackend() != null) {
            return getBackendByName(properties.getBackend());
        }
        
        // 2. Find backends supporting the detection type
        List<VisionBackend> candidates = backends.stream()
            .filter(backend -> backend.supports(query.getType()))
            .filter(backend -> backend.getHealthInfo().getStatus() == HealthStatus.UP)
            .toList();
            
        // 3. Select based on capabilities and performance
        return selectOptimalBackend(candidates, query);
    }
}
```

### Backend Health Monitoring

Each backend provides health information:

```java
public class BackendHealthInfo {
    private String backendName;
    private HealthStatus status;        // UP, DOWN, DEGRADED
    private String version;
    private Map<String, Object> details;
    private List<DetectionType> supportedTypes;
    private List<String> capabilities;
    private PerformanceMetrics metrics;
}
```

## Detection Pipeline

### Image Processing Pipeline

```
Input Image (bytes/ImageData)
         │
         ▼
   Input Validation
    │  (size, format, etc.)
    │
    ▼
  Image Decoding
    │  (OpenCV Mat, BufferedImage, etc.)
    │
    ▼
  Preprocessing
    │  (resize, normalize, color conversion)
    │
    ▼
  Backend Detection
    │  (face detection, object detection, etc.)
    │
    ▼
  Postprocessing
    │  (NMS, filtering, coordinate normalization)
    │
    ▼
  Result Mapping
    │  (Detection objects with metadata)
    │
    ▼
  Response Formatting
         │
         ▼
    Detection Results
```

### Detection Result Structure

```java
public class Detection {
    private DetectionType type;                    // FACE, OBJECT, TEXT
    private DetectionCategory category;            // FACE, EYE, NOSE, MOUTH
    private BoundingBox boundingBox;              // Normalized coordinates [0,1]
    private double confidence;                     // [0.0, 1.0]
    private String label;                         // Human-readable label
    private Map<String, Object> attributes;       // Additional metadata
    
    // For face detections
    // attributes.put("age", 25);
    // attributes.put("gender", "male");
    // attributes.put("emotion", "happy");
    // attributes.put("landmarks", List.of(leftEye, rightEye, nose, ...));
}

public class BoundingBox {
    private double x;      // Left coordinate [0,1]
    private double y;      // Top coordinate [0,1] 
    private double width;  // Width [0,1]
    private double height; // Height [0,1]
}
```

### Coordinate System

All coordinates are normalized to [0,1] range:

```
(0,0) ┌─────────────────────┐ (1,0)
      │                     │
      │    ┌─────────┐      │
      │    │  Face   │      │  
      │    └─────────┘      │
      │                     │
(0,1) └─────────────────────┘ (1,1)

Face BoundingBox:
- x: 0.25 (25% from left edge)
- y: 0.20 (20% from top edge)  
- width: 0.30 (30% of image width)
- height: 0.40 (40% of image height)
```

## Configuration System

### Application Properties

```yaml
spring:
  vision:
    # Backend selection
    backend: opencv                    # opencv, facebytes, compreface, deepface
    
    # OpenCV backend configuration
    opencv:
      enabled: true
      face-cascade-path: "haarcascade_frontalface_alt.xml"
      confidence-threshold: 0.5
      min-face-size: 30
      max-face-size: 300
      scale-factor: 1.1
      min-neighbors: 3
      
    # FaceBytes backend configuration  
    facebytes:
      enabled: false
      model: "ArcFace"                 # ArcFace, VGG-Face, Facenet, etc.
      detector: "opencv"               # opencv, retinaface, mtcnn, etc.
      
    # External service backends
    compreface:
      enabled: false
      url: "http://localhost:8000"
      api-key: "${COMPREFACE_API_KEY}"
      
    deepface:
      enabled: false  
      url: "http://localhost:5000"
      
    # Performance settings
    performance:
      max-image-size: 10485760         # 10MB
      processing-timeout: 30000        # 30 seconds
      thread-pool-size: 10
      
    # Security settings
    security:
      max-download-size: 5242880       # 5MB for URL downloads
      connect-timeout: 5000            # 5 seconds
      read-timeout: 10000              # 10 seconds
      allowed-hosts: []                # Empty = all public hosts allowed
```

### Environment Variables

```bash
# Backend selection
SPRING_VISION_BACKEND=opencv

# OpenCV configuration
SPRING_VISION_OPENCV_ENABLED=true
SPRING_VISION_OPENCV_CONFIDENCE_THRESHOLD=0.7

# External service credentials
COMPREFACE_API_KEY=your-api-key-here
DEEPFACE_URL=http://localhost:5000

# Performance tuning
SPRING_VISION_PERFORMANCE_MAX_IMAGE_SIZE=20971520  # 20MB
SPRING_VISION_PERFORMANCE_THREAD_POOL_SIZE=20
```

### Configuration Precedence

1. **Command line arguments**: `--spring.vision.backend=opencv`
2. **Environment variables**: `SPRING_VISION_BACKEND=opencv`
3. **Application properties**: `spring.vision.backend=opencv`
4. **Default values**: Built-in framework defaults

## Security Model

### Input Validation

All inputs are validated at multiple layers:

```java
public class VisionTemplate {
    
    public List<Detection> detectFaces(byte[] imageData) {
        // Layer 1: Null and basic validation
        Objects.requireNonNull(imageData, "Image data cannot be null");
        
        if (imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }
        
        // Layer 2: Size limits
        if (imageData.length > properties.getPerformance().getMaxImageSize()) {
            throw new IllegalArgumentException("Image size exceeds maximum limit");
        }
        
        // Layer 3: Format validation
        ImageData validated = validateImageFormat(imageData);
        
        return detect(validated, DetectionQuery.defaultFaceQuery());
    }
}
```

### SSRF Protection

URL-based image loading includes comprehensive SSRF protection:

```java
private void validatePublicHost(URL url) throws IOException {
    String host = url.getHost();
    
    // Block localhost and private networks
    if ("localhost".equalsIgnoreCase(host) || 
        host.startsWith("127.") || 
        host.startsWith("10.") ||
        host.startsWith("192.168.") ||
        (host.startsWith("172.") && isPrivateClassB(host))) {
        throw new IOException("Access to private/local addresses is not allowed");
    }
    
    // Block reserved IP ranges
    InetAddress address = InetAddress.getByName(host);
    if (address.isLoopbackAddress() || 
        address.isLinkLocalAddress() || 
        address.isSiteLocalAddress()) {
        throw new IOException("Access to reserved IP addresses is not allowed");
    }
}
```

### Resource Limits

```java
public class SecurityConfig {
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;     // 10MB
    public static final long MAX_DOWNLOAD_SIZE = 5 * 1024 * 1024;   // 5MB
    public static final int CONNECT_TIMEOUT_MS = 5000;              // 5 seconds
    public static final int READ_TIMEOUT_MS = 10000;                // 10 seconds
    public static final int MAX_DETECTIONS = 1000;                  // Per request
}
```

### Data Sanitization

Sensitive data is never logged:

```java
// ✅ Safe logging
logger.info("Face detection completed: detectionCount={}, processingTimeMs={}", 
           results.size(), processingTime);

// ❌ Never log image data or embeddings
// logger.debug("Image bytes: {}", Arrays.toString(imageData));
// logger.debug("Embedding vector: {}", Arrays.toString(embedding));
```

## Performance Considerations

### Optimization Strategies

#### 1. Image Processing Optimization

```java
// Reuse OpenCV Mat objects
private final ThreadLocal<Mat> matBuffer = ThreadLocal.withInitial(() -> new Mat());

public List<Detection> detectFaces(ImageData imageData) {
    Mat image = matBuffer.get();
    try {
        // Decode directly into reused Mat
        Imgcodecs.imdecode(new MatOfByte(imageData.getData()), 
                          Imgcodecs.IMREAD_COLOR, image);
        return performDetection(image);
    } finally {
        // Clear but don't deallocate Mat
        image.setTo(Scalar.all(0));
    }
}
```

#### 2. Backend Warm-up

```java
@EventListener(ApplicationReadyEvent.class)
public void warmUpBackends() {
    byte[] dummyImage = createDummyImage(100, 100);
    
    for (VisionBackend backend : backends) {
        try {
            backend.detectFaces(new ImageData(dummyImage));
            logger.info("Warmed up backend: {}", backend.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to warm up backend: {}", backend.getClass().getSimpleName());
        }
    }
}
```

#### 3. Async Processing

```java
@Async
public CompletableFuture<List<Detection>> detectFacesAsync(ImageData imageData) {
    return CompletableFuture.supplyAsync(() -> {
        return detectFaces(imageData);
    }, Executors.newVirtualThreadPerTaskExecutor());
}
```

### Memory Management

#### Native Resource Cleanup

```java
public class OpenCvVisionBackend implements VisionBackend, DisposableBean {
    private CascadeClassifier faceCascade;
    
    @Override
    public void destroy() throws Exception {
        if (faceCascade != null) {
            faceCascade.close();
        }
    }
}
```

#### Image Size Limits

```java
private ImageData validateImageSize(byte[] imageData) {
    if (imageData.length > MAX_IMAGE_SIZE) {
        // Resize large images instead of rejecting
        return resizeImage(imageData, MAX_DIMENSION);
    }
    return new ImageData(imageData);
}
```

### Performance Monitoring

```java
@Component
public class VisionMetrics {
    private final Counter detectionCounter;
    private final Timer detectionTimer;
    private final Gauge activeBackends;
    
    public VisionMetrics(MeterRegistry registry) {
        this.detectionCounter = Counter.builder("vision.detections.total")
            .description("Total number of detections performed")
            .tag("backend", "unknown")
            .register(registry);
            
        this.detectionTimer = Timer.builder("vision.detection.duration")
            .description("Time spent performing detections")
            .register(registry);
    }
    
    public void recordDetection(String backend, Duration duration, int detectionCount) {
        detectionCounter.increment(Tags.of("backend", backend), detectionCount);
        detectionTimer.record(duration);
    }
}
```

## Extension Points

### Custom Backend Development

#### 1. Implement VisionBackend Interface

```java
@Component
@ConditionalOnProperty("spring.vision.mybackend.enabled")
public class MyVisionBackend implements VisionBackend, EmbeddingCapability {
    
    @Override
    public List<Detection> detect(ImageData imageData, DetectionQuery query) {
        // Custom detection logic
        return performCustomDetection(imageData, query);
    }
    
    @Override
    public boolean supports(DetectionType type) {
        return type == DetectionType.FACE || type == DetectionType.OBJECT;
    }
    
    @Override
    public BackendHealthInfo getHealthInfo() {
        return BackendHealthInfo.builder()
            .backendName("MyBackend")
            .status(HealthStatus.UP)
            .version("1.0.0")
            .supportedTypes(List.of(DetectionType.FACE, DetectionType.OBJECT))
            .build();
    }
}
```

#### 2. Add Configuration Properties

```java
@ConfigurationProperties(prefix = "spring.vision.mybackend")
@Data
public class MyBackendProperties {
    private boolean enabled = false;
    private String modelPath;
    private double confidenceThreshold = 0.5;
    private int maxDetections = 100;
}
```

#### 3. Create Auto-Configuration

```java
@Configuration
@EnableConfigurationProperties(MyBackendProperties.class)
@ConditionalOnProperty(value = "spring.vision.mybackend.enabled", havingValue = "true")
public class MyBackendAutoConfiguration {
    
    @Bean
    public MyVisionBackend myVisionBackend(MyBackendProperties properties) {
        return new MyVisionBackend(properties);
    }
}
```

### Custom Capabilities

```java
public interface CustomCapability extends VisionBackend {
    
    /**
     * Extract custom features from image.
     */
    Map<String, Object> extractFeatures(ImageData imageData);
    
    /**
     * Perform custom analysis.
     */
    AnalysisResult analyze(ImageData imageData, AnalysisOptions options);
}

@Component
public class MyAdvancedBackend implements VisionBackend, CustomCapability {
    
    @Override
    public Map<String, Object> extractFeatures(ImageData imageData) {
        // Custom feature extraction
        return Map.of(
            "color_histogram", extractColorHistogram(imageData),
            "texture_features", extractTextureFeatures(imageData),
            "shape_descriptors", extractShapeDescriptors(imageData)
        );
    }
}
```

### Plugin Architecture

The framework supports runtime plugin loading:

```java
@Component
public class PluginManager {
    
    public void loadPlugin(Path pluginJar) {
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{pluginJar.toUri().toURL()})) {
                
            // Load plugin configuration
            PluginMetadata metadata = loadPluginMetadata(classLoader);
            
            // Instantiate backend
            Class<?> backendClass = classLoader.loadClass(metadata.getBackendClass());
            VisionBackend backend = (VisionBackend) backendClass.getDeclaredConstructor()
                .newInstance();
                
            // Register with Spring context
            registerBackend(backend, metadata);
            
        } catch (Exception e) {
            logger.error("Failed to load plugin: {}", pluginJar, e);
        }
    }
}
```

---

This architecture provides a solid foundation for building scalable, secure, and extensible computer vision applications with Spring Boot. The modular design allows for easy integration of new backends while maintaining consistency and performance across the entire framework. 