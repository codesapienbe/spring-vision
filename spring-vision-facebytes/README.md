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

## 📦 Installation

### Maven Dependency

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-facebytes</artifactId>
    <version>1.0</version>
</dependency>
```

### Required Dependencies

- **OpenCV Java**: Face detection and image processing
- **ONNX Runtime**: Model inference
- **Apache Commons Math**: Distance calculations
- **SLF4J**: Logging framework

## 🚀 Quick Start

### Basic Face Verification

```java
import com.deepface.core.DeepFace;
import com.deepface.core.VerificationResult;

// Simple verification
VerificationResult result = DeepFace.verify("path/to/face1.jpg", "path/to/face2.jpg");

if (result.verified()) {
    System.out.println("Faces match! Distance: " + result.distance());
} else {
    System.out.println("Faces don't match. Distance: " + result.distance());
}
```

### Advanced Verification with Custom Parameters

```java
import com.deepface.enums.ModelType;
import com.deepface.enums.DistanceMetric;
import com.deepface.enums.DetectorBackend;

VerificationResult result = DeepFace.verify(
    "face1.jpg", 
    "face2.jpg",
    ModelType.VGG_FACE,           // Use VGG-Face model
    DistanceMetric.COSINE,        // Use cosine distance
    DetectorBackend.RETINAFACE    // Use RetinaFace detector
);
```

### Face Detection and Extraction

```java
import com.deepface.core.DeepFace;
import java.awt.image.BufferedImage;
import java.util.List;

// Extract all faces from an image
List<BufferedImage> faces = DeepFace.extractFaces("group_photo.jpg");

// Extract faces with specific detector
List<BufferedImage> faces = DeepFace.extractFaces(
    "group_photo.jpg", 
    DetectorBackend.MTCNN
);

System.out.println("Detected " + faces.size() + " faces");
```

### Generate Face Embeddings

```java
import com.deepface.core.DeepFace;
import com.deepface.core.EmbeddingResult;
import java.util.List;

// Generate embeddings for all faces in an image
List<EmbeddingResult> embeddings = DeepFace.represent("face.jpg");

for (EmbeddingResult embedding : embeddings) {
    float[] vector = embedding.embedding();
    System.out.println("Embedding size: " + vector.length);
    
    // Access face region information
    FaceRegion region = embedding.faceRegion();
    System.out.println("Face at: " + region.x() + "," + region.y() + 
                      " size: " + region.width() + "x" + region.height());
}
```

### Facial Analysis

```java
import com.deepface.core.DeepFace;
import com.deepface.core.AnalysisResult;
import java.util.List;

// Analyze age, gender, emotion, and race
List<AnalysisResult> analysis = DeepFace.analyze("face.jpg");

for (AnalysisResult result : analysis) {
    System.out.println("Age: " + result.age());
    System.out.println("Gender: " + result.gender());
    System.out.println("Emotion: " + result.dominantEmotion());
    
    // Access detailed emotion distribution
    Map<String, Double> emotions = result.emotionDistribution();
    emotions.forEach((emotion, confidence) -> 
        System.out.println(emotion + ": " + confidence));
}
```

### Find Best Match in Gallery

```java
import com.deepface.core.DeepFace;
import com.deepface.core.FindResult;
import java.util.List;

List<String> galleryPaths = List.of(
    "gallery/face1.jpg",
    "gallery/face2.jpg",
    "gallery/face3.jpg"
);

FindResult match = DeepFace.find("query_face.jpg", galleryPaths);

if (match.matched()) {
    System.out.println("Best match: " + match.bestMatchPath());
    System.out.println("Distance: " + match.distance());
    System.out.println("Threshold: " + match.threshold());
} else {
    System.out.println("No match found above threshold");
}
```

## 🔧 Configuration

### DeepFace Configuration

```java
import com.deepface.config.DeepFaceConfig;

DeepFaceConfig config = DeepFaceConfig.current();

// Set default distance metric
config.setDefaultDistanceMetric(DistanceMetric.COSINE);

// Set thresholds for different metrics
config.setThreshold(DistanceMetric.COSINE, 0.68);
config.setThreshold(DistanceMetric.EUCLIDEAN, 0.60);
config.setThreshold(DistanceMetric.EUCLIDEAN_L2, 1.13);

// Enable/disable strict face detection
config.setEnforceDetection(true);
```

### Model Configuration

```java
import com.deepface.models.ModelManager;

ModelManager modelManager = new ModelManager();

// Download models automatically
modelManager.downloadModel(ModelType.VGG_FACE);

// Set custom model paths
modelManager.setModelPath(ModelType.VGG_FACE, "/custom/path/vggface.onnx");
```

## 🤖 ONNX Model Configuration

FaceBytes requires ONNX models for face recognition and analysis. Models can be configured via environment variables, system properties, or auto-download.

### Environment Variables (Recommended for Production)

Set model paths using environment variables:

```bash
# Face Recognition Models
export FACEBYTES_VGGFACE_ONNX_PATH="/path/to/models/vgg_face.onnx"
export FACEBYTES_ARCFACE_ONNX_PATH="/path/to/models/arcface.onnx"
export FACEBYTES_FACENET_ONNX_PATH="/path/to/models/facenet.onnx"
export FACEBYTES_FACENET512_ONNX_PATH="/path/to/models/facenet512.onnx"
export FACEBYTES_OPENFACE_ONNX_PATH="/path/to/models/openface.onnx"
export FACEBYTES_DEEPFACE_ONNX_PATH="/path/to/models/deepface.onnx"
export FACEBYTES_SFACE_ONNX_PATH="/path/to/models/sface.onnx"
export FACEBYTES_DEEPID_ONNX_PATH="/path/to/models/deepid.onnx"

# Facial Analysis Models
export FACEBYTES_AGE_ONNX_PATH="/path/to/models/age_predictor.onnx"
export FACEBYTES_GENDER_ONNX_PATH="/path/to/models/gender_predictor.onnx"
export FACEBYTES_EMOTION_ONNX_PATH="/path/to/models/emotion_predictor.onnx"
export FACEBYTES_RACE_ONNX_PATH="/path/to/models/race_predictor.onnx"
```

### System Properties (Recommended for Development)

Set model paths using JVM system properties:

```java
// In your application startup
System.setProperty("facebytes.vggface.onnx", "/path/to/models/vgg_face.onnx");
System.setProperty("facebytes.arcface.onnx", "/path/to/models/arcface.onnx");
System.setProperty("facebytes.facenet.onnx", "/path/to/models/facenet.onnx");
System.setProperty("facebytes.facenet512.onnx", "/path/to/models/facenet512.onnx");
System.setProperty("facebytes.openface.onnx", "/path/to/models/openface.onnx");
System.setProperty("facebytes.deepface.onnx", "/path/to/models/deepface.onnx");
System.setProperty("facebytes.sface.onnx", "/path/to/models/sface.onnx");
System.setProperty("facebytes.deepid.onnx", "/path/to/models/deepid.onnx");

// Facial analysis models
System.setProperty("facebytes.age.onnx", "/path/to/models/age_predictor.onnx");
System.setProperty("facebytes.gender.onnx", "/path/to/models/gender_predictor.onnx");
System.setProperty("facebytes.emotion.onnx", "/path/to/models/emotion_predictor.onnx");
System.setProperty("facebytes.race.onnx", "/path/to/models/race_predictor.onnx");
```

Or via JVM arguments:

```bash
java -Dfacebytes.vggface.onnx=/path/to/vgg_face.onnx \
     -Dfacebytes.arcface.onnx=/path/to/arcface.onnx \
     -jar your-application.jar
```

### Spring Boot Configuration

Add model paths to your `application.yml`:

```yaml
# application.yml
facebytes:
  models:
    vggface:
      onnx: "/path/to/models/vgg_face.onnx"
    arcface:
      onnx: "/path/to/models/arcface.onnx"
    facenet:
      onnx: "/path/to/models/facenet.onnx"
    facenet512:
      onnx: "/path/to/models/facenet512.onnx"
    openface:
      onnx: "/path/to/models/openface.onnx"
    deepface:
      onnx: "/path/to/models/deepface.onnx"
    sface:
      onnx: "/path/to/models/sface.onnx"
    deepid:
      onnx: "/path/to/models/deepid.onnx"
  analysis:
    age:
      onnx: "/path/to/models/age_predictor.onnx"
    gender:
      onnx: "/path/to/models/gender_predictor.onnx"
    emotion:
      onnx: "/path/to/models/emotion_predictor.onnx"
    race:
      onnx: "/path/to/models/race_predictor.onnx"
  
  # Auto-download configuration (optional)
  auto_download:
    enabled: true
    cache_directory: "${user.home}/.facebytes/models"
    verify_checksums: true
    https_only: true
```

### Auto-Download Configuration (Advanced)

Enable automatic model downloading for development and testing:

```java
import com.deepface.config.FaceBytesConfiguration;

FaceBytesConfiguration config = new FaceBytesConfiguration();
config.setAutoDownloadEnabled(true);
config.setModelCacheDirectory("/path/to/cache/directory");
config.setVerifyChecksums(true);  // Verify model integrity
config.setHttpsOnlyDownloads(true);  // Security: HTTPS only
```

Or via environment variables:

```bash
export FACEBYTES_AUTO_DOWNLOAD_ENABLED=true
export FACEBYTES_MODEL_CACHE_DIRECTORY="/opt/facebytes/models"
export FACEBYTES_VERIFY_CHECKSUMS=true
export FACEBYTES_HTTPS_ONLY_DOWNLOADS=true
```

### Configuration Priority

FaceBytes resolves model paths in the following order (highest to lowest priority):

1. **System Properties** (`-Dfacebytes.vggface.onnx=...`)
2. **Environment Variables** (`FACEBYTES_VGGFACE_ONNX_PATH`)
3. **Spring Boot Configuration** (`application.yml`)
4. **Auto-Download** (if enabled)
5. **Default Locations** (classpath, common directories)

### Model Requirements

Each model type has specific requirements:

| Model | Input Size | Output Size | File Size | Use Case |
|-------|------------|-------------|-----------|----------|
| **VGG-Face** | 224x224x3 | 2622-d | ~500MB | High-quality recognition |
| **ArcFace** | 112x112x3 | 512-d | ~250MB | State-of-the-art accuracy |
| **FaceNet** | 160x160x3 | 128-d | ~90MB | Efficient recognition |
| **FaceNet512** | 160x160x3 | 512-d | ~90MB | Extended embeddings |
| **OpenFace** | 96x96x3 | 128-d | ~30MB | Lightweight recognition |
| **DeepFace** | 152x152x3 | 4096-d | ~250MB | Traditional approach |
| **SFace** | 112x112x3 | 128-d | ~40MB | Efficient recognition |
| **DeepID** | 55x47x3 | 160-d | ~20MB | Identity-preserving |

### Error Handling and Troubleshooting

When models are not properly configured, FaceBytes provides clear error messages:

```java
try {
    VerificationResult result = DeepFace.verify("face1.jpg", "face2.jpg");
} catch (DeepFaceException e) {
    // Example error message:
    // "VGGFace ONNX model is not available. Configure the model path via 
    //  'FACEBYTES_VGGFACE_ONNX_PATH' or system property 'facebytes.vggface.onnx', 
    //  or enable auto-download in configuration."
    
    System.err.println("Model configuration error: " + e.getMessage());
    
    // Check configuration
    String modelPath = System.getenv("FACEBYTES_VGGFACE_ONNX_PATH");
    if (modelPath == null) {
        modelPath = System.getProperty("facebytes.vggface.onnx");
    }
    
    if (modelPath == null) {
        System.err.println("No VGGFace model path configured. Please set:");
        System.err.println("  Environment: FACEBYTES_VGGFACE_ONNX_PATH=/path/to/model.onnx");
        System.err.println("  System Property: -Dfacebytes.vggface.onnx=/path/to/model.onnx");
        System.err.println("  Or enable auto-download in configuration");
    } else {
        System.err.println("Configured model path: " + modelPath);
        Path path = Paths.get(modelPath);
        if (!Files.exists(path)) {
            System.err.println("Model file does not exist: " + path);
        } else if (!Files.isReadable(path)) {
            System.err.println("Model file is not readable: " + path);
        }
    }
}
```

### Docker Configuration Example

For containerized deployments:

```dockerfile
# Dockerfile
FROM openjdk:21-jre

# Create model directory
RUN mkdir -p /opt/facebytes/models

# Copy pre-downloaded models
COPY models/ /opt/facebytes/models/

# Set environment variables
ENV FACEBYTES_VGGFACE_ONNX_PATH=/opt/facebytes/models/vgg_face.onnx
ENV FACEBYTES_ARCFACE_ONNX_PATH=/opt/facebytes/models/arcface.onnx
ENV FACEBYTES_AGE_ONNX_PATH=/opt/facebytes/models/age_predictor.onnx
ENV FACEBYTES_GENDER_ONNX_PATH=/opt/facebytes/models/gender_predictor.onnx

COPY app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  facebytes-app:
    build: .
    environment:
      - FACEBYTES_VGGFACE_ONNX_PATH=/opt/models/vgg_face.onnx
      - FACEBYTES_ARCFACE_ONNX_PATH=/opt/models/arcface.onnx
      - FACEBYTES_AUTO_DOWNLOAD_ENABLED=false
    volumes:
      - ./models:/opt/models:ro
    ports:
      - "8080:8080"
```

### Kubernetes Configuration Example

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: facebytes-config
data:
  application.yml: |
    facebytes:
      models:
        vggface:
          onnx: "/opt/models/vgg_face.onnx"
        arcface:
          onnx: "/opt/models/arcface.onnx"
      auto_download:
        enabled: false

---
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: facebytes-app
spec:
  template:
    spec:
      containers:
      - name: app
        image: facebytes-app:latest
        env:
        - name: FACEBYTES_VGGFACE_ONNX_PATH
          value: "/opt/models/vgg_face.onnx"
        - name: FACEBYTES_ARCFACE_ONNX_PATH
          value: "/opt/models/arcface.onnx"
        volumeMounts:
        - name: config
          mountPath: /config
        - name: models
          mountPath: /opt/models
      volumes:
      - name: config
        configMap:
          name: facebytes-config
      - name: models
        persistentVolumeClaim:
          claimName: facebytes-models-pvc
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Categories

```bash
# Core functionality tests
mvn test -Dtest=DeepFaceIntegrationTest

# Detector tests
mvn test -Dtest=DetectorFactoryTest

# Advanced functionality tests
mvn test -Dtest=DeepFaceAdvancedTest
```

## 📊 Performance

### Detection Performance (200x200 image)

| Backend | Speed | Accuracy | Memory |
|---------|-------|----------|---------|
| OpenCV | ~50ms | Good | Low |
| DLIB | ~100ms | Good | Low |
| MTCNN | ~200ms | Excellent | Medium |
| RetinaFace | ~150ms | Excellent | Medium |

### Model Performance

| Model | Embedding Size | Speed | Accuracy |
|-------|----------------|-------|----------|
| VGG-Face | 512 | ~100ms | Excellent |
| FaceNet | 128 | ~50ms | Very Good |
| ArcFace | 512 | ~120ms | Excellent |
| OpenFace | 128 | ~40ms | Good |

## 🚨 Error Handling

FaceBytes provides comprehensive error handling with custom exceptions:

```java
import com.deepface.exceptions.DeepFaceException;

try {
    VerificationResult result = DeepFace.verify("face1.jpg", "face2.jpg");
} catch (DeepFaceException e) {
    // Handle face recognition errors
    log.error("Face verification failed: {}", e.getMessage());
} catch (IllegalArgumentException e) {
    // Handle invalid input parameters
    log.error("Invalid parameters: {}", e.getMessage());
}
```

## 🔒 Security Features

- **Input Validation**: Comprehensive validation of all inputs
- **File Size Limits**: Configurable maximum file size limits
- **Image Dimension Limits**: Protection against memory exhaustion
- **Secure Downloads**: HTTPS model downloads with integrity checks
- **Resource Cleanup**: Automatic cleanup of native resources

## 📈 Monitoring and Logging

### Structured Logging

```java
import com.deepface.utils.Logs;

// Log business events
Logs.info("FaceRecognition", "verification.completed", Map.of(
    "model", "VGG_FACE",
    "distance", 0.45,
    "verified", true
));

// Log errors with context
Logs.error("FaceRecognition", "verification.failed", exception, Map.of(
    "image1", "path1.jpg",
    "image2", "path2.jpg"
));
```

### Performance Metrics

```java
import com.deepface.metrics.VisionMetrics;

// Record processing time
VisionMetrics.recordProcessingTime("face_verification", durationMs);

// Record detection accuracy
VisionMetrics.recordDetectionAccuracy("opencv", detectedFaces, actualFaces);
```

## 🌟 Advanced Features

### Custom Detector Implementation

```java
import com.deepface.detectors.FaceDetector;
import com.deepface.core.FaceRegion;
import java.awt.image.BufferedImage;
import java.util.List;

public class CustomDetector implements FaceDetector {
    @Override
    public List<FaceRegion> detectFaces(BufferedImage image) {
        // Implement custom detection logic
        return List.of(new FaceRegion(0, 0, 100, 100, 0.9, null));
    }
}
```

### Batch Processing

```java
import com.deepface.core.DeepFace;
import java.util.List;

List<String> imagePaths = List.of("face1.jpg", "face2.jpg", "face3.jpg");

// Process multiple images efficiently
for (String path : imagePaths) {
    List<EmbeddingResult> embeddings = DeepFace.represent(path);
    // Process embeddings...
}
```

### Face Quality Assessment

```java
import com.deepface.utils.FaceQualityValidator;

FaceQualityValidator validator = new FaceQualityValidator();
boolean isHighQuality = validator.validateFaceQuality(faceImage);

if (isHighQuality) {
    // Process high-quality face
} else {
    // Request better quality image
}
```

## 🔄 Migration from Python DeepFace

### Python to Java API Mapping

| Python DeepFace | Java FaceBytes |
|-----------------|----------------|
| `verify()` | `DeepFace.verify()` |
| `represent()` | `DeepFace.represent()` |
| `extract_faces()` | `DeepFace.extractFaces()` |
| `analyze()` | `DeepFace.analyze()` |
| `find()` | `DeepFace.find()` |

### Parameter Mapping

```python
# Python
result = DeepFace.verify(img1_path, img2_path, 
                        model_name="VGG-Face", 
                        distance_metric="cosine",
                        detector_backend="opencv")
```

```java
// Java
VerificationResult result = DeepFace.verify(
    img1Path, img2Path,
    ModelType.VGG_FACE,
    DistanceMetric.COSINE,
    DetectorBackend.OPENCV
);
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Install dependencies: `mvn install`
3. Run tests: `mvn test`
4. Build: `mvn clean package`

### Code Style

- Follow Java coding conventions
- Use comprehensive Javadoc
- Include unit tests for new features
- Follow SOLID principles

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **DeepFace Team**: Original Python implementation
- **OpenCV**: Computer vision library
- **ONNX Runtime**: Model inference engine
- **Spring Framework**: Application framework

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/spring-vision/facebytes/issues)
- **Discussions**: [GitHub Discussions](https://github.com/spring-vision/facebytes/discussions)
- **Documentation**: [API Reference](docs/API_REFERENCE.md)

---

**FaceBytes** - Bringing DeepFace to the Java ecosystem with enterprise-grade quality and performance. 

## 📣 Actuator & Prometheus Integration Example

To export FaceBytes model load and inference metrics to Prometheus via Spring Boot Actuator + Micrometer, add the following dependencies to your application's `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

The library auto-configuration provided by FaceBytes (`ModelMetricsAutoConfiguration`) will automatically bind the application's `MeterRegistry` into the FaceBytes `ModelManager` so model metrics are emitted through your application's registry.

Add Prometheus endpoint exposure in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "prometheus,health,info"
```

Run your Spring Boot application and navigate to `http://localhost:8080/actuator/prometheus`. You should see metrics such as:

- `facebytes_model_load_time_seconds` (timer) — model load durations
- `facebytes_model_inference_time_seconds{model="arcface"}` — inference durations per model

Example curl to verify:

```bash
curl -s http://localhost:8080/actuator/prometheus | grep facebytes
```

Structured logs from model loading and inference will appear via the `Logs` utility (JSON-style) and include `component`, `model`, and `correlation_id` when the latter is set in MDC. Use `ModelManager.setMeterRegistry(yourRegistry)` if you need to bind a custom registry programmatically.

If you'd like, I can also add a short example application project demonstrating full Actuator + FaceBytes integration and a sample Prometheus scrape configuration. 
