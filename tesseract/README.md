# Spring Vision Tesseract Backend

This module provides Tesseract OCR backend implementation for the Spring Vision framework, offering optical character recognition capabilities for extracting text from images.

## Features

- **Text Detection**: Extract text from images using Tesseract OCR
- **Multi-Language Support**: Support for 100+ languages
- **Page Segmentation**: Various page segmentation modes
- **OCR Engine Modes**: Legacy, LSTM, and hybrid modes
- **Confidence Scores**: Text detection with confidence levels
- **Layout Analysis**: Preserve text layout and structure

## Getting Started

### 1. Add Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>tesseract</artifactId>
    <version>1.0</version>
</dependency>
```

### 2. Configure via Application Properties

```properties
# Enable the Tesseract module
spring.vision.tesseract.enabled=true
# Path to tessdata directory
spring.vision.tesseract.tessdata-path=/usr/share/tesseract-ocr/4.00/tessdata
# Default OCR language (ISO 639-3 code)
spring.vision.tesseract.language=eng
# Page segmentation mode (0-13)
# 3 = Fully automatic page segmentation (default)
spring.vision.tesseract.page-seg-mode=3
# OCR Engine Mode (0-3)
# 1 = Neural nets LSTM engine only (default)
spring.vision.tesseract.ocr-engine-mode=1
# Minimum confidence threshold (0-100)
spring.vision.tesseract.min-confidence=60
# Enable automatic language data download
spring.vision.tesseract.enable-auto-download=true
# Additional languages (comma-separated)
spring.vision.tesseract.additional-languages=fra,deu,spa
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/ocr")
public class OCRController {

    @Autowired
    private VisionTemplate visionTemplate;

    @PostMapping("/extract-text")
    public ResponseEntity<TextResult> extractText(
            @RequestParam("image") MultipartFile file) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());
        List<Detection> textDetections = visionTemplate.detect(imageData, DetectionType.TEXT);

        // Aggregate all detected text
        String fullText = textDetections.stream()
                .map(Detection::label)
                .collect(Collectors.joining(" "));

        return ResponseEntity.ok(new TextResult(fullText, textDetections));
    }

    @PostMapping("/extract-with-confidence")
    public ResponseEntity<List<Detection>> extractTextWithConfidence(
            @RequestParam("image") MultipartFile file,
            @RequestParam(defaultValue = "60") int minConfidence) throws IOException {

        ImageData imageData = ImageData.fromBytes(file.getBytes());

        DetectionQuery query = new DetectionQuery.Builder()
                .type(DetectionType.TEXT)
                .minConfidence(minConfidence / 100.0)
                .build();

        List<Detection> textDetections = visionTemplate.detect(imageData, query);

        return ResponseEntity.ok(textDetections);
    }
}
```

## Configuration Properties

| Property                                       | Type    | Default          | Description                 |
|------------------------------------------------|---------|------------------|-----------------------------|
| `spring.vision.tesseract.enabled`              | boolean | false            | Enable/disable the module   |
| `spring.vision.tesseract.tessdata-path`        | String  | System dependent | Path to tessdata directory  |
| `spring.vision.tesseract.language`             | String  | eng              | Default language            |
| `spring.vision.tesseract.page-seg-mode`        | int     | 3                | Page segmentation mode      |
| `spring.vision.tesseract.ocr-engine-mode`      | int     | 1                | OCR engine mode             |
| `spring.vision.tesseract.min-confidence`       | int     | 60               | Minimum confidence (0-100)  |
| `spring.vision.tesseract.enable-auto-download` | boolean | true             | Auto-download language data |
| `spring.vision.tesseract.additional-languages` | String  | ""               | Additional languages        |

## Page Segmentation Modes

| Mode | Description                                     |
|------|-------------------------------------------------|
| 0    | Orientation and script detection only           |
| 1    | Automatic page segmentation with OSD            |
| 3    | Fully automatic page segmentation (default)     |
| 6    | Assume a single uniform block of text           |
| 11   | Sparse text. Find as much text as possible      |
| 13   | Raw line. Treat the image as a single text line |

## Supported Languages

Tesseract supports 100+ languages including:

- English (eng)
- French (fra)
- German (deu)
- Spanish (spa)
- Chinese Simplified (chi_sim)
- Arabic (ara)
- Russian (rus)
- Japanese (jpn)
- And many more...

## Prerequisites

### Install Tesseract

**Ubuntu/Debian:**

```bash
sudo apt-get install tesseract-ocr
sudo apt-get install tesseract-ocr-eng  # English
sudo apt-get install tesseract-ocr-fra  # French
```

**macOS:**

```bash
brew install tesseract
brew install tesseract-lang  # All languages
```

**Windows:**
Download from: https://github.com/UB-Mannheim/tesseract/wiki

## Usage Examples

### Basic Text Extraction

```java
ImageData document = ImageData.fromFile("document.jpg");
List<Detection> text = visionTemplate.detect(document, DetectionType.TEXT);

text.

forEach(detection ->{
        System.out.

println("Text: "+detection.label());
        System.out.

println("Confidence: "+detection.confidence());
        });
```

### Multi-Language OCR

```properties
spring.vision.tesseract.language=eng+fra+deu
```

```java
ImageData multilingualDoc = ImageData.fromFile("multilingual.jpg");
List<Detection> text = visionTemplate.detect(multilingualDoc, DetectionType.TEXT);
```

### Document Scanning

```java

@Service
public class DocumentScanner {

    @Autowired
    private VisionTemplate visionTemplate;

    public String scanDocument(MultipartFile file) throws IOException {
        ImageData imageData = ImageData.fromBytes(file.getBytes());

        DetectionQuery query = new DetectionQuery.Builder()
                .type(DetectionType.TEXT)
                .minConfidence(0.7)
                .build();

        List<Detection> detections = visionTemplate.detect(imageData, query);

        return detections.stream()
                .sorted((a, b) -> {
                    // Sort by y-position (top to bottom)
                    double y1 = a.boundingBox().y();
                    double y2 = b.boundingBox().y();
                    return Double.compare(y1, y2);
                })
                .map(Detection::label)
                .collect(Collectors.joining("\n"));
    }
}
```

## Performance

- Simple text: ~100-200ms per image
- Complex documents: ~500ms-1s per image
- Multi-language: +20-50% processing time

*Performance depends on image size, complexity, and system resources*

## Requirements

- Java 21+
- Spring Boot 3.2+
- Tesseract OCR 4.0+ installed on system
- Language data files (tessdata)

## Troubleshooting

### Tesseract Not Found

Ensure Tesseract is installed and in PATH:

```bash
tesseract --version
```

### Language Data Missing

Install required language packs:

```bash
sudo apt-get install tesseract-ocr-{language_code}
```

Or set custom tessdata path:

```properties
spring.vision.tesseract.tessdata-path=/custom/path/tessdata
```

### Low Accuracy

Try different page segmentation modes or pre-process images:

- Increase image contrast
- Remove noise
- Straighten text alignment
- Use mode 6 for single-column text

## Best Practices

1. **Pre-process Images**: Improve OCR accuracy with image enhancement
2. **Choose Right PSM**: Select appropriate page segmentation mode
3. **Language Selection**: Use correct language for better results
4. **Confidence Filtering**: Filter low-confidence results
5. **Post-processing**: Clean and validate extracted text

## License

See main project LICENSE file.

# Spring Vision CompreFace Backend

This module provides CompreFace backend implementation for the Spring Vision framework, offering face recognition and analysis through the Exadel CompreFace API.

## Features

- **Face Detection**: Detect faces using CompreFace Face Detector
- **Face Recognition**: Recognize and verify faces with high accuracy
- **Face Analysis**: Age, gender, and demographic analysis
- **Face Verification**: Compare two faces for similarity
- **Embedding Extraction**: Generate face embeddings for custom applications
- **Landmark Detection**: Facial landmark identification

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
# API key for authentication (if required)
spring.vision.compreface.api-key=your-api-key-here
# Request timeout
spring.vision.compreface.timeout=30s
# Confidence threshold for face detection (0.0 - 1.0)
spring.vision.compreface.confidence-threshold=0.7
# Maximum number of faces to detect
spring.vision.compreface.max-detections=10
# Face detection model
spring.vision.compreface.detection-model=blazeface
# Enable face verification
spring.vision.compreface.enable-verification=true
# Enable demographic analysis
spring.vision.compreface.enable-demographics=false
# Enable landmark detection
spring.vision.compreface.enable-landmarks=false
```

### 3. Use VisionTemplate (Auto-Configured)

```java

@RestController
@RequestMapping("/api/faces")
public class FaceController {

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

        boolean verified = visionTemplate.verify(image1, image2);

        return ResponseEntity.ok(new VerificationResult(verified));
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

| Property                                        | Type     | Default               | Description               |
|-------------------------------------------------|----------|-----------------------|---------------------------|
| `spring.vision.compreface.enabled`              | boolean  | false                 | Enable/disable the module |
| `spring.vision.compreface.base-url`             | String   | http://localhost:8000 | CompreFace API URL        |
| `spring.vision.compreface.api-key`              | String   | ""                    | API authentication key    |
| `spring.vision.compreface.timeout`              | Duration | 30s                   | Request timeout           |
| `spring.vision.compreface.confidence-threshold` | double   | 0.7                   | Detection confidence      |
| `spring.vision.compreface.max-detections`       | int      | 10                    | Max faces to detect       |
| `spring.vision.compreface.detection-model`      | String   | blazeface             | Detection model name      |
| `spring.vision.compreface.enable-verification`  | boolean  | true                  | Enable verification       |
| `spring.vision.compreface.enable-demographics`  | boolean  | false                 | Enable demographics       |
| `spring.vision.compreface.enable-landmarks`     | boolean  | false                 | Enable landmarks          |

## Prerequisites

### Running CompreFace Server

You need a running CompreFace server. Use Docker:

```bash
docker run -d -p 8000:8000 exadel/compreface:latest
```

Or use docker-compose:

```yaml
version: '3.8'
services:
  compreface:
    image: exadel/compreface:latest
    ports:
      - "8000:8000"
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
```

## Features

### Face Detection

Detect faces with bounding boxes and confidence scores:

```java
List<Detection> faces = visionTemplate.detectFaces(imageData);
faces.

forEach(face ->{
        System.out.

println("Face detected with confidence: "+face.confidence());
        System.out.

println("Bounding box: "+face.boundingBox());
        });
```

### Face Verification

Compare two faces for similarity:

```java
boolean match = visionTemplate.verify(image1, image2);
if(match){
        System.out.

println("Faces match!");
}
```

### Embedding Extraction

Extract face embeddings for custom applications:

```java
List<float[]> embeddings = visionTemplate.extractEmbeddings(imageData, DetectionCategory.FACE);
embeddings.

forEach(embedding ->{
        System.out.

println("Embedding dimension: "+embedding.length);
});
```

## Performance

- Face Detection: ~50-100ms per image
- Face Verification: ~100-150ms per pair
- Embedding Extraction: ~100ms per face

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
docker ps | grep compreface
```

### Authentication Failed

Set the correct API key in properties:

```properties
spring.vision.compreface.api-key=your-actual-api-key
```

### Timeout Issues

Increase the timeout for slower networks:

```properties
spring.vision.compreface.timeout=60s
```

## License

See main project LICENSE file.

