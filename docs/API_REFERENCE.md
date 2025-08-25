# Spring Vision API Reference

This document provides a comprehensive reference for the Spring Vision framework APIs, including core interfaces, configuration options, and usage examples.

## Table of Contents

- [Core API](#core-api)
- [VisionTemplate](#visiontemplate)
- [VisionBackend](#visionbackend)
- [Detection System](#detection-system)
- [Configuration Properties](#configuration-properties)
- [REST API Endpoints](#rest-api-endpoints)
- [Events and Metrics](#events-and-metrics)
- [Exception Handling](#exception-handling)

## Core API

### VisionTemplate

The `VisionTemplate` is the primary entry point for all computer vision operations.

#### Constructor

```java
public VisionTemplate(
    List<VisionBackend> backends,
    VisionProperties properties,
    MeterRegistry meterRegistry
)
```

#### Methods

##### detect(ImageData, DetectionQuery)

**Primary detection method (recommended)**

```java
public List<Detection> detect(ImageData imageData, DetectionQuery query)
```

**Parameters:**
- `imageData` - The input image data
- `query` - Detection query specifying what to detect and how

**Returns:** List of `Detection` objects

**Throws:**
- `VisionProcessingException` - If detection fails
- `IllegalArgumentException` - If inputs are invalid

**Example:**
```java
DetectionQuery query = DetectionQuery.builder()
    .type(DetectionType.FACE)
    .minConfidence(0.7)
    .maxDetections(10)
    .build();

List<Detection> faces = visionTemplate.detect(imageData, query);
```

##### detectFaces(byte[])

**Legacy face detection method (deprecated in 2.0)**

```java
@Deprecated
public List<Detection> detectFaces(byte[] imageData)
```

**Parameters:**
- `imageData` - Raw image bytes

**Returns:** List of face `Detection` objects

**Example:**
```java
List<Detection> faces = visionTemplate.detectFaces(imageBytes);
```

##### extractEmbeddings(ImageData)

**Extract facial embeddings for similarity comparison**

```java
public List<float[]> extractEmbeddings(ImageData imageData)
```

**Parameters:**
- `imageData` - The input image containing faces

**Returns:** List of embedding vectors (one per detected face)

**Throws:**
- `UnsupportedOperationException` - If backend doesn't support embeddings
- `VisionProcessingException` - If extraction fails

**Example:**
```java
List<float[]> embeddings = visionTemplate.extractEmbeddings(imageData);
for (float[] embedding : embeddings) {
    System.out.println("Embedding dimension: " + embedding.length);
}
```

##### verify(ImageData, ImageData, String, double)

**Verify if two images contain the same person**

```java
public boolean verify(ImageData image1, ImageData image2, String metric, double threshold)
```

**Parameters:**
- `image1` - First image
- `image2` - Second image  
- `metric` - Similarity metric ("cosine" or "euclidean")
- `threshold` - Similarity threshold for positive match

**Returns:** `true` if images likely contain the same person

**Example:**
```java
boolean isSamePerson = visionTemplate.verify(
    new ImageData(image1Bytes),
    new ImageData(image2Bytes),
    "cosine",
    0.35
);
```

##### obscureFaces(ImageData)

**Blur faces in an image for privacy protection**

```java
public ImageData obscureFaces(ImageData imageData)
```

**Parameters:**
- `imageData` - The input image

**Returns:** New `ImageData` with faces blurred

**Example:**
```java
ImageData obscuredImage = visionTemplate.obscureFaces(imageData);
byte[] obscuredBytes = obscuredImage.getData();
```

##### getActiveBackend()

**Get the currently active vision backend**

```java
public VisionBackend getActiveBackend()
```

**Returns:** The active `VisionBackend` instance

##### getAllBackends()

**Get all available vision backends**

```java
public List<VisionBackend> getAllBackends()
```

**Returns:** List of all registered backends

## VisionBackend

Interface that all vision backends must implement.

### Core Methods

#### detect(ImageData, DetectionQuery)

```java
List<Detection> detect(ImageData imageData, DetectionQuery query)
```

**Primary detection method for the new multi-category API.**

#### supports(DetectionType)

```java
boolean supports(DetectionType type)
```

**Check if backend supports a specific detection type.**

**Parameters:**
- `type` - The detection type to check

**Returns:** `true` if supported

#### getHealthInfo()

```java
BackendHealthInfo getHealthInfo()
```

**Get health and status information for the backend.**

**Returns:** `BackendHealthInfo` with status, version, capabilities

### Optional Capability Methods

These methods have default implementations that throw `UnsupportedOperationException`:

#### extractEmbeddings(ImageData)

```java
default List<float[]> extractEmbeddings(ImageData imageData)
```

#### verify(ImageData, ImageData, String, double)

```java
default boolean verify(ImageData a, ImageData b, String metric, double threshold)
```

#### obscureFaces(ImageData)

```java
default ImageData obscureFaces(ImageData imageData)
```

### Legacy Methods (Deprecated)

#### detectFaces(ImageData)

```java
@Deprecated
default List<Detection> detectFaces(ImageData imageData)
```

## Detection System

### Detection Class

Represents a single detection result.

```java
public class Detection {
    private DetectionType type;              // FACE, OBJECT, TEXT, etc.
    private DetectionCategory category;      // FACE, EYE, NOSE, MOUTH, etc.
    private BoundingBox boundingBox;        // Normalized coordinates
    private double confidence;              // [0.0, 1.0]
    private String label;                   // Human-readable label
    private Map<String, Object> attributes; // Additional metadata
}
```

#### Constructors

```java
public Detection(DetectionType type, BoundingBox boundingBox, double confidence)

public Detection(DetectionType type, DetectionCategory category, 
                BoundingBox boundingBox, double confidence, String label)
```

#### Methods

```java
// Getters
public DetectionType getType()
public DetectionCategory getCategory()
public BoundingBox getBoundingBox()
public double getConfidence()
public String getLabel()
public Map<String, Object> getAttributes()

// Setters
public void setAttributes(Map<String, Object> attributes)
public void setAttribute(String key, Object value)
```

### BoundingBox Class

Represents normalized bounding box coordinates.

```java
public class BoundingBox {
    private double x;      // Left coordinate [0,1]
    private double y;      // Top coordinate [0,1]
    private double width;  // Width [0,1]
    private double height; // Height [0,1]
}
```

#### Constructors

```java
public BoundingBox(double x, double y, double width, double height)
```

#### Methods

```java
// Coordinate access
public double getX()
public double getY() 
public double getWidth()
public double getHeight()

// Convenience methods
public double getCenterX()
public double getCenterY()
public double getArea()
public boolean contains(double x, double y)
public boolean intersects(BoundingBox other)

// Convert to pixel coordinates
public BoundingBox toPixelCoordinates(int imageWidth, int imageHeight)
```

### DetectionQuery Class

Specifies what and how to detect.

```java
public class DetectionQuery {
    private DetectionType type;
    private Set<DetectionCategory> categories;
    private Set<String> classLabels;
    private Double minConfidence;
    private Integer maxDetections;
    private BoundingBox roi;
    private Map<String, Object> options;
}
```

#### Builder Pattern

```java
DetectionQuery query = DetectionQuery.builder()
    .type(DetectionType.FACE)
    .categories(Set.of(DetectionCategory.FACE, DetectionCategory.EYE))
    .minConfidence(0.7)
    .maxDetections(50)
    .roi(new BoundingBox(0.1, 0.1, 0.8, 0.8))
    .option("scaleFactor", 1.1)
    .build();
```

#### Static Factory Methods

```java
public static DetectionQuery defaultFaceQuery()
public static DetectionQuery defaultObjectQuery()
public static DetectionQuery allCategories(DetectionType type)
```

### ImageData Class

Wrapper for image data with metadata.

```java
public class ImageData {
    private byte[] data;
    private String format;
    private Map<String, Object> metadata;
}
```

#### Constructors

```java
public ImageData(byte[] data)
public ImageData(byte[] data, String format)
public ImageData(byte[] data, String format, Map<String, Object> metadata)
```

#### Methods

```java
public byte[] getData()
public String getFormat()
public Map<String, Object> getMetadata()
public int getSize()
```

### Enums

#### DetectionType

```java
public enum DetectionType {
    FACE,        // Human faces
    OBJECT,      // General objects  
    TEXT,        // Text regions
    BARCODE,     // Barcodes/QR codes
    HAND,        // Hand detection
    POSE,        // Body pose
    LANDMARK,    // Facial landmarks
    CUSTOM       // Custom detection types
}
```

#### DetectionCategory

```java
public enum DetectionCategory {
    // Face-related
    FACE, EYE, NOSE, MOUTH, EAR, EYEBROW,
    
    // Object categories
    PERSON, VEHICLE, ANIMAL, FOOD, FURNITURE,
    
    // Text categories  
    TEXT, NUMBER, BARCODE, QR_CODE,
    
    // Body parts
    HAND, FINGER, ARM, LEG, HEAD,
    
    // Landmarks
    LANDMARK, KEYPOINT,
    
    // Custom
    CUSTOM
}
```

## Configuration Properties

### VisionProperties

Main configuration class for Spring Vision.

```java
@ConfigurationProperties(prefix = "spring.vision")
public class VisionProperties {
    private String backend;                    // Backend selection
    private OpenCvProperties opencv;           // OpenCV configuration
    private FaceBytesProperties facebytes;     // FaceBytes configuration
    private CompreFaceProperties compreface;   // CompreFace configuration
    private DeepFaceProperties deepface;       // DeepFace configuration
    private PerformanceProperties performance; // Performance settings
    private SecurityProperties security;       // Security settings
}
```

### OpenCvProperties

```java
@ConfigurationProperties(prefix = "spring.vision.opencv")
public class OpenCvProperties {
    private boolean enabled = true;
    private String faceCascadePath = "haarcascade_frontalface_alt.xml";
    private double confidenceThreshold = 0.5;
    private int minFaceSize = 30;
    private int maxFaceSize = 300;
    private double scaleFactor = 1.1;
    private int minNeighbors = 3;
}
```

### PerformanceProperties

```java
@ConfigurationProperties(prefix = "spring.vision.performance")
public class PerformanceProperties {
    private long maxImageSize = 10 * 1024 * 1024;  // 10MB
    private long processingTimeout = 30000;         // 30 seconds
    private int threadPoolSize = 10;
    private boolean enableParallelProcessing = true;
}
```

### SecurityProperties

```java
@ConfigurationProperties(prefix = "spring.vision.security")
public class SecurityProperties {
    private long maxDownloadSize = 5 * 1024 * 1024;  // 5MB
    private int connectTimeout = 5000;               // 5 seconds
    private int readTimeout = 10000;                 // 10 seconds
    private List<String> allowedHosts = new ArrayList<>();
    private boolean validateSslCertificates = true;
}
```

### Configuration Examples

#### Basic Configuration

```yaml
spring:
  vision:
    backend: opencv
    opencv:
      enabled: true
      confidence-threshold: 0.7
```

#### Advanced Configuration

```yaml
spring:
  vision:
    backend: opencv
    
    opencv:
      enabled: true
      face-cascade-path: "haarcascade_frontalface_alt.xml"
      confidence-threshold: 0.6
      min-face-size: 40
      max-face-size: 400
      scale-factor: 1.05
      min-neighbors: 4
      
    performance:
      max-image-size: 20971520          # 20MB
      processing-timeout: 45000         # 45 seconds
      thread-pool-size: 20
      enable-parallel-processing: true
      
    security:
      max-download-size: 10485760       # 10MB
      connect-timeout: 10000            # 10 seconds
      read-timeout: 20000               # 20 seconds
      validate-ssl-certificates: true
      allowed-hosts:
        - "example.com"
        - "trusted-images.com"
```

## REST API Endpoints

When using `spring-vision-starter`, the following REST endpoints are automatically available:

### POST /api/vision/detect/faces

Detect faces in uploaded image.

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Parameters:
  - `file` (required): Image file
  - `minConfidence` (optional): Minimum confidence threshold (0.0-1.0)

**Response:**
```json
{
  "message": "Success",
  "detectionCount": 2,
  "detections": [
    {
      "type": "FACE",
      "category": "FACE", 
      "boundingBox": {
        "x": 0.25,
        "y": 0.30,
        "width": 0.15,
        "height": 0.20
      },
      "confidence": 0.89,
      "label": "Face",
      "attributes": {
        "category": "FACE"
      }
    }
  ]
}
```

### POST /api/vision/detect/data

Detect faces in raw image data.

**Request:**
- Method: `POST`
- Content-Type: `application/octet-stream`
- Body: Raw image bytes
- Parameters:
  - `minConfidence` (optional): Minimum confidence threshold

**Response:** Same as `/detect/faces`

### GET /api/vision/health

Get backend health information.

**Response:**
```json
{
  "status": "UP",
  "backend": "OpenCvVisionBackend",
  "version": "1.0.0",
  "supportedTypes": ["FACE", "OBJECT"],
  "capabilities": ["EmbeddingCapability"],
  "details": {
    "cascadeLoaded": true,
    "lastCheckTime": "2024-01-15T10:30:00Z"
  }
}
```

### GET /api/vision/backends

List all available backends.

**Response:**
```json
{
  "activeBackend": "OpenCvVisionBackend",
  "availableBackends": [
    {
      "name": "OpenCvVisionBackend",
      "status": "UP",
      "supportedTypes": ["FACE", "OBJECT"]
    },
    {
      "name": "FaceBytesBackend", 
      "status": "DOWN",
      "supportedTypes": ["FACE"]
    }
  ]
}
```

## Events and Metrics

### Events

Spring Vision publishes application events for monitoring:

#### DetectionCompletedEvent

```java
public class DetectionCompletedEvent extends ApplicationEvent {
    private final String backend;
    private final DetectionType type;
    private final int detectionCount;
    private final Duration processingDuration;
    private final boolean successful;
}
```

**Usage:**
```java
@EventListener
public void handleDetectionCompleted(DetectionCompletedEvent event) {
    log.info("Detection completed: backend={}, count={}, duration={}ms",
            event.getBackend(),
            event.getDetectionCount(), 
            event.getProcessingDuration().toMillis());
}
```

#### BackendHealthChangedEvent

```java
public class BackendHealthChangedEvent extends ApplicationEvent {
    private final String backendName;
    private final HealthStatus previousStatus;
    private final HealthStatus currentStatus;
}
```

### Metrics

Spring Vision integrates with Micrometer for metrics collection:

#### Counters

- `vision.detections.total` - Total number of detections
  - Tags: `backend`, `type`, `status`
- `vision.backend.errors.total` - Total backend errors  
  - Tags: `backend`, `error_type`

#### Timers

- `vision.detection.duration` - Detection processing time
  - Tags: `backend`, `type`
- `vision.image.download.duration` - Image download time
  - Tags: `source_type`

#### Gauges

- `vision.backends.active` - Number of active backends
- `vision.image.queue.size` - Pending image processing queue size

**Accessing Metrics:**
```bash
# Prometheus format
curl http://localhost:8080/actuator/prometheus

# JSON format  
curl http://localhost:8080/actuator/metrics/vision.detections.total
```

## Exception Handling

### Exception Hierarchy

```
BaseVisionException (RuntimeException)
├── VisionConfigurationException
├── VisionProcessingException
├── VisionBackendException
└── VisionSecurityException
```

### BaseVisionException

```java
public abstract class BaseVisionException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    protected BaseVisionException(String message, String errorCode) 
    protected BaseVisionException(String message, String errorCode, Throwable cause)
}
```

### VisionProcessingException

```java
public class VisionProcessingException extends BaseVisionException {
    public VisionProcessingException(String message)
    public VisionProcessingException(String message, Throwable cause)
}
```

**Common scenarios:**
- Image decoding failures
- Backend processing errors
- Invalid image formats
- Memory allocation failures

### VisionConfigurationException

```java
public class VisionConfigurationException extends BaseVisionException {
    public VisionConfigurationException(String message)
    public VisionConfigurationException(String message, Throwable cause)
}
```

**Common scenarios:**
- Invalid backend configuration
- Missing required properties
- Backend initialization failures

### VisionBackendException

```java
public class VisionBackendException extends BaseVisionException {
    private final String backendName;
    
    public VisionBackendException(String backendName, String message)
    public VisionBackendException(String backendName, String message, Throwable cause)
}
```

**Common scenarios:**
- Backend service unavailable
- Model loading failures
- API authentication errors

### Error Handling Best Practices

#### Service Layer

```java
@Service
public class VisionService {
    
    public List<Detection> detectFaces(byte[] imageData) {
        try {
            return visionTemplate.detectFaces(imageData);
        } catch (VisionProcessingException e) {
            log.error("Face detection failed: {}", e.getMessage(), e);
            throw new ServiceException("Unable to process image", e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid input: {}", e.getMessage());
            throw new ValidationException("Invalid image data", e);
        }
    }
}
```

#### Controller Layer

```java
@RestController
public class VisionController {
    
    @ExceptionHandler(VisionProcessingException.class)
    public ResponseEntity<ErrorResponse> handleProcessingError(VisionProcessingException e) {
        ErrorResponse error = new ErrorResponse(
            "PROCESSING_FAILED",
            e.getMessage(),
            Map.of("errorCode", e.getErrorCode())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_INPUT", 
            e.getMessage(),
            Collections.emptyMap()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

## Capability Interfaces

### EmbeddingCapability

```java
public interface EmbeddingCapability extends VisionBackend {
    List<float[]> extractEmbeddings(ImageData imageData);
    boolean verify(ImageData image1, ImageData image2, String metric, double threshold);
    
    default double calculateSimilarity(float[] embedding1, float[] embedding2, String metric) {
        return switch (metric.toLowerCase()) {
            case "cosine" -> calculateCosineSimilarity(embedding1, embedding2);
            case "euclidean" -> calculateEuclideanDistance(embedding1, embedding2);
            default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
        };
    }
}
```

### AnnotationCapability

```java
public interface AnnotationCapability extends VisionBackend {
    ImageData annotateFaces(ImageData imageData, AnnotationRequest request);
    ImageData drawBoundingBoxes(ImageData imageData, List<Detection> detections);
}
```

### BarcodeCapability

```java
public interface BarcodeCapability extends VisionBackend {
    List<Detection> detectBarcodes(ImageData imageData);
    List<BarcodeResult> decodeBarcodes(ImageData imageData);
}
```

---

This API reference provides comprehensive documentation for integrating and extending the Spring Vision framework. For additional examples and tutorials, see the [Getting Started Guide](GETTING_STARTED.md) and [Architecture Documentation](ARCHITECTURE.md). 