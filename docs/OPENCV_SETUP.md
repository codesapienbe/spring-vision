# OpenCV Setup Guide

This guide explains how to set up OpenCV dependencies for the Spring Vision framework.

## Overview

Spring Vision uses OpenCV through JavaCV bindings to provide computer vision capabilities. The OpenCV backend supports face detection, object detection, and basic image processing operations.

## Dependencies

### Maven Dependencies

The following dependencies are automatically included when using the Spring Vision starter:

```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv</artifactId>
    <version>1.5.9</version>
</dependency>
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.9</version>
</dependency>
```

### System Requirements

- **Java**: JDK 21 or later
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 2GB RAM (4GB recommended)
- **Storage**: 500MB free space for OpenCV libraries

## Installation

### Automatic Installation (Recommended)

The easiest way to get started is to use the Spring Vision starter, which automatically includes all necessary OpenCV dependencies:

```xml
<dependency>
    <groupId>com.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Manual Installation

If you need to install OpenCV manually:

#### Windows

1. **Download OpenCV**: Visit [opencv.org](https://opencv.org/releases/) and download OpenCV 4.8.1
2. **Extract**: Extract the downloaded archive to a directory (e.g., `C:\opencv`)
3. **Set Environment Variables**:
   ```cmd
   set OPENCV_DIR=C:\opencv\build
   set PATH=%PATH%;%OPENCV_DIR%\x64\vc16\bin
   ```
4. **Verify Installation**:
   ```cmd
   java -cp "target/classes;target/dependency/*" com.springvision.core.backend.OpenCvDemo test-image.jpg
   ```

#### macOS

1. **Using Homebrew** (Recommended):
   ```bash
   brew install opencv
   ```

2. **Using MacPorts**:
   ```bash
   sudo port install opencv
   ```

3. **Set Environment Variables**:
   ```bash
   export OPENCV_DIR=/usr/local
   export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:/usr/local/lib
   ```

#### Linux (Ubuntu/Debian)

1. **Install Dependencies**:
   ```bash
   sudo apt update
   sudo apt install build-essential cmake pkg-config
   sudo apt install libjpeg-dev libpng-dev libtiff-dev
   sudo apt install libavcodec-dev libavformat-dev libswscale-dev libv4l-dev
   sudo apt install libxvidcore-dev libx264-dev
   sudo apt install libgtk-3-dev
   sudo apt install libatlas-base-dev gfortran
   sudo apt install python3-dev
   ```

2. **Install OpenCV**:
   ```bash
   sudo apt install libopencv-dev python3-opencv
   ```

3. **Verify Installation**:
   ```bash
   pkg-config --modversion opencv4
   ```

## Configuration

### Application Properties

Configure the OpenCV backend in your `application.yml`:

```yaml
vision:
  backend: opencv
  opencv:
    enabled: true
    face-cascade-path: /haarcascade_frontalface_default.xml
    confidence-threshold: 0.8
    min-face-size-ratio: 0.1
    max-face-size-ratio: 0.8
```

### Environment Variables

You can also configure OpenCV using environment variables:

```bash
export VISION_BACKEND=opencv
export VISION_OPENCV_ENABLED=true
export VISION_OPENCV_CONFIDENCE_THRESHOLD=0.8
```

## Usage

### Basic Usage

```java
@Autowired
private VisionTemplate visionTemplate;

public void detectFaces(byte[] imageData) {
    VisionResult result = visionTemplate.detectFaces(imageData);
    
    if (result.hasDetections()) {
        result.detections().forEach(detection -> {
            System.out.println("Face detected with confidence: " + detection.confidence());
            System.out.println("Bounding box: " + detection.boundingBox());
        });
    }
}
```

### Direct Backend Usage

```java
OpenCvVisionBackend backend = new OpenCvVisionBackend();
backend.initialize();

ImageData imageData = ImageData.fromBytes(imageBytes);
VisionResult result = backend.detectFaces(imageData);

backend.shutdown();
```

## Troubleshooting

### Common Issues

#### 1. OpenCV Not Found

**Error**: `OpenCV is not available on this system`

**Solution**: Ensure OpenCV is properly installed and the native libraries are in your system PATH.

#### 2. Face Cascade Not Found

**Error**: `Failed to load face detection cascade classifier`

**Solution**: The face cascade file is included in the JAR. If you're using a custom path, ensure the file exists and is readable.

#### 3. Memory Issues

**Error**: `OutOfMemoryError` during image processing

**Solution**: Increase JVM heap size:
```bash
java -Xmx4g -jar your-application.jar
```

#### 4. Performance Issues

**Symptoms**: Slow face detection

**Solutions**:
- Reduce image size before processing
- Adjust confidence thresholds
- Use a more powerful machine
- Consider using GPU acceleration (requires additional setup)

### Debug Mode

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    com.springvision.core.backend: DEBUG
    org.bytedeco: DEBUG
```

### Health Checks

Monitor the OpenCV backend health:

```java
@Autowired
private VisionTemplate visionTemplate;

public void checkHealth() {
    if (visionTemplate.isBackendHealthy()) {
        BackendHealthInfo healthInfo = visionTemplate.getBackendHealthInfo();
        System.out.println("Backend is healthy: " + healthInfo.statusMessage());
    } else {
        System.out.println("Backend is unhealthy: " + healthInfo.errorMessage());
    }
}
```

## Performance Optimization

### Image Preprocessing

For better performance, preprocess images before detection:

```java
// Resize large images
if (imageData.getSizeInMB() > 1.0) {
    // Resize to reasonable dimensions
    imageData = resizeImage(imageData, 1024, 768);
}
```

### Batch Processing

Process multiple images efficiently:

```java
List<ImageData> images = // ... load images
List<VisionResult> results = new ArrayList<>();

for (ImageData image : images) {
    VisionResult result = visionTemplate.detectFaces(image);
    results.add(result);
}
```

### Memory Management

OpenCV automatically manages memory, but you can help by:

```java
// Explicitly release resources when done
backend.shutdown();

// Use try-with-resources for temporary backends
try (OpenCvVisionBackend tempBackend = new OpenCvVisionBackend()) {
    tempBackend.initialize();
    // ... use backend
} // automatically calls shutdown()
```

## Advanced Configuration

### Custom Cascade Files

Use custom cascade files for specialized detection:

```java
OpenCvVisionBackend backend = new OpenCvVisionBackend();
backend.setFaceCascadePath("/path/to/custom/cascade.xml");
backend.initialize();
```

### GPU Acceleration

For GPU acceleration (requires CUDA):

1. Install CUDA toolkit
2. Install OpenCV with CUDA support
3. Set environment variables:
   ```bash
   export CUDA_VISIBLE_DEVICES=0
   export OPENCV_CUDA_ENABLED=true
   ```

## Testing

### Unit Tests

Run OpenCV tests:

```bash
mvn test -Dopencv.available=true
```

### Integration Tests

Test with real images:

```bash
# Create test image
java -cp target/classes com.springvision.core.backend.OpenCvDemo test-image.jpg
```

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting)
2. Review [Spring Vision documentation](../README.md)
3. Open an issue on GitHub
4. Check OpenCV documentation at [opencv.org](https://docs.opencv.org/)

## Version Compatibility

| Spring Vision | OpenCV | JavaCV | Java |
|---------------|--------|--------|------|
| 1.0.0-SNAPSHOT | 4.8.1 | 1.5.9 | 21+ |

## License

OpenCV is released under the Apache 2.0 license. See [opencv.org](https://opencv.org/license/) for details. 
