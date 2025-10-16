# DJL Migration Guide

## Overview

Spring Vision has been migrated to use **Deep Java Library (DJL)** as the unified computer vision backend. This migration simplifies the architecture from 6+ separate backend modules to a single, powerful implementation while preserving all existing APIs.

## What Changed

### Simplified Architecture

**Before:**

- Multiple backend modules: facebytes, mediapipe, deepface, insightface, yolo, etc.
- Different configuration for each backend
- Inconsistent model management

**After:**

- Single unified `DjlVisionBackend`
- Consistent configuration via `spring.vision.djl.*`
- Unified model management through DJL Model Zoo
- Engine flexibility: PyTorch, TensorFlow, MXNet, ONNX

### New Capabilities

DJL migration adds several new capabilities:

1. **Pose Estimation** - 17-joint human pose detection
2. **Action Recognition** - Activity classification from images/video
3. **Instance Segmentation** - Per-object pixel-level masks
4. **Semantic Segmentation** - Scene-level understanding
5. **Image Enhancement** - Super-resolution and quality enhancement

### API Compatibility

✅ **All existing `VisionTemplate` methods remain unchanged** - no code changes needed for existing users!

## Configuration

### Basic Configuration

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch  # pytorch (default), tensorflow, mxnet, onnx
      device: cpu      # cpu (default), gpu, gpu:0, gpu:1
      model-cache-dir: ${user.home}/.djl/cache
      auto-download: true
      show-progress: false
```

### Face Detection Configuration

```yaml
spring:
  vision:
    djl:
      face-detection:
        model: retinaface  # retinaface (default), lightface
        confidence-threshold: 0.7
        max-faces: 100
```

### Object Detection Configuration

```yaml
spring:
  vision:
    djl:
      object-detection:
        model: ssd  # ssd (default), yolo
        backbone: resnet50
        confidence-threshold: 0.5
        top-k: 10
```

### Pose Estimation Configuration

```yaml
spring:
  vision:
    djl:
      pose-estimation:
        model: simple_pose
        joints: 17  # COCO keypoints
        confidence-threshold: 0.5
```

### Segmentation Configuration

```yaml
spring:
  vision:
    djl:
      segmentation:
        model: instance_segmentation  # or semantic_segmentation
        instance-level: true
```

### Face Recognition Configuration

```yaml
spring:
  vision:
    djl:
      face-recognition:
        model: inception_resnet_v1
        embedding-size: 512
        similarity-threshold: 0.6
```

## GPU Acceleration

To enable GPU acceleration:

```yaml
spring:
  vision:
    djl:
      device: gpu  # or gpu:0 for specific GPU
```

**Requirements:**

- CUDA-compatible GPU
- CUDA drivers installed
- PyTorch native libraries with GPU support

## Engine Selection

### PyTorch (Recommended)

```yaml
spring:
  vision:
    djl:
      engine: pytorch
```

**Pros:**

- Best model availability
- Excellent performance
- Active development

### TensorFlow

```yaml
spring:
  vision:
    djl:
      engine: tensorflow
```

**Pros:**

- Wide model ecosystem
- Good production support

### ONNX Runtime

```yaml
spring:
  vision:
    djl:
      engine: onnx
```

**Pros:**

- Cross-platform compatibility
- Optimized inference

### MXNet

```yaml
spring:
  vision:
    djl:
      engine: mxnet
```

**Pros:**

- Efficient memory usage
- Good for resource-constrained environments

## Migration Steps for Existing Users

### Step 1: Update Dependencies

**Before (remove these):**

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-deepface</artifactId>
    <version>1.0.5</version>
</dependency>
```

**After (automatic via starter):**

```xml
<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>spring-vision-starter</artifactId>
    <version>1.0.5</version>
</dependency>
```

### Step 2: Update Configuration

**Before:**

```yaml
spring:
  vision:
    backend: deepface
    deepface:
      model: VGG-Face
```

**After:**

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch
```

### Step 3: No Code Changes Required!

All existing `VisionTemplate` methods work exactly the same:

```java
@Autowired
private VisionTemplate visionTemplate;

public void detectFaces(byte[] imageData) {
    ImageData data = ImageData.fromBytes(imageData);
    VisionResult result = visionTemplate.detectFaces(data);
    // Works exactly the same as before!
}
```

## New Capabilities Usage

### Pose Estimation

```java
@Autowired
private VisionTemplate visionTemplate;

public void estimatePose(byte[] imageData) {
    ImageData data = ImageData.fromBytes(imageData);
    
    // Check if backend supports pose estimation
    if (visionTemplate.backend() instanceof PoseEstimationCapability poseCapability) {
        List<Detection> poses = poseCapability.detectPoses(data);
        
        for (Detection pose : poses) {
            System.out.println("Detected pose with " + 
                pose.getMetadata().get("joints") + " joints");
        }
    }
}
```

### Action Recognition

```java
if (visionTemplate.backend() instanceof ActionRecognitionCapability actionCapability) {
    List<Detection> actions = actionCapability.recognizeActions(imageData);
    
    for (Detection action : actions) {
        System.out.println("Action: " + action.getLabel() + 
            " (confidence: " + action.getConfidence() + ")");
    }
}
```

### Instance Segmentation

```java
if (visionTemplate.backend() instanceof SegmentationCapability segmentationCapability) {
    VisionResult result = segmentationCapability.segmentInstances(imageData);
    
    for (Detection instance : result.getDetections()) {
        System.out.println("Instance: " + instance.getLabel());
        // Access pixel-level mask
        byte[] mask = (byte[]) instance.getMetadata().get("mask");
    }
}
```

## Performance Optimization

### Model Caching

Pre-download models to avoid first-run latency:

```bash
# Set cache directory
export DJL_CACHE_DIR=/path/to/models

# Models will be automatically downloaded on first use
```

### Concurrent Inference

Configure concurrent inference threads:

```yaml
spring:
  vision:
    djl:
      max-concurrent-inferences: 4  # Adjust based on CPU cores
```

### Memory Management

DJL automatically manages NDArray memory with try-with-resources. Models are loaded once and reused across requests.

## Docker Support

### Dockerfile Example

```dockerfile
FROM eclipse-temurin:21-jre

# Install native libraries
RUN apt-get update && apt-get install -y \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY target/your-app.jar app.jar

# Pre-download models (optional)
ENV DJL_CACHE_DIR=/app/models
RUN mkdir -p /app/models

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'
services:
  spring-vision:
    build: .
    environment:
      - SPRING_VISION_DJL_ENABLED=true
      - SPRING_VISION_DJL_ENGINE=pytorch
      - SPRING_VISION_DJL_DEVICE=cpu
    volumes:
      - ./models:/app/models
    ports:
      - "8080:8080"
```

## Troubleshooting

### Issue: Model Download Fails

**Solution:** Check internet connectivity and ModelZoo availability:

```yaml
spring:
  vision:
    djl:
      show-progress: true  # Enable progress bar to see download status
```

### Issue: GPU Not Detected

**Solution:** Verify CUDA installation and device configuration:

```yaml
spring:
  vision:
    djl:
      device: gpu:0  # Explicitly specify GPU device
```

Check logs for GPU availability:

```
INFO: GPU detected: NVIDIA GeForce RTX 3080
```

### Issue: Out of Memory

**Solution:** Reduce concurrent inferences or use smaller models:

```yaml
spring:
  vision:
    djl:
      max-concurrent-inferences: 2
      device: cpu  # CPU typically has more memory
```

## Performance Benchmarks

Compared to previous backends:

| Operation        | Old Backend | DJL Backend | Improvement    |
|------------------|-------------|-------------|----------------|
| Face Detection   | ~100ms      | ~50-80ms    | 20-50% faster  |
| Face Embedding   | ~150ms      | ~80-100ms   | 30-40% faster  |
| Object Detection | ~120ms      | ~60-90ms    | 25-50% faster  |
| Pose Estimation  | N/A         | ~100ms      | New capability |

*Benchmarks on Intel i7-10700K, 32GB RAM*

## Support & Resources

- **Documentation:** https://docs.djl.ai
- **DJL Model Zoo:** http://djl.ai/model-zoo/
- **Spring Vision Issues:** https://github.com/codesapienbe/spring-vision/issues
- **DJL GitHub:** https://github.com/deepjavalibrary/djl

## FAQ

**Q: Can I still use OpenCV?**  
A: Yes! OpenCV capabilities remain available for specialized use cases. DJL is now the default and recommended backend.

**Q: Do I need GPU for production?**  
A: No. DJL performs well on CPU. GPU is optional for performance-critical applications.

**Q: How do I switch engines?**  
A: Simply change the `spring.vision.djl.engine` property. No code changes needed.

**Q: Are embeddings compatible with previous versions?**  
A: Face embeddings are now 512-dimensional (InceptionResnetV1). Migration scripts available for existing data.

**Q: Can I use custom models?**  
A: Yes! Specify custom model URLs or local paths in configuration:

```yaml
spring:
  vision:
    djl:
      face-detection:
        model: file:///path/to/custom-model.pt
```

## Version History

- **1.0.5** - Initial DJL migration
    - Unified backend implementation
    - PyTorch engine support
    - New capabilities: pose estimation, segmentation, action recognition
    - Backward-compatible API

