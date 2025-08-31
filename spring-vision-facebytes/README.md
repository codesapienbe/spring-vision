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
    <version>1.0.0-SNAPSHOT</version>
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