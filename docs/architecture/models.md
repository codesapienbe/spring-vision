[Docs Home](./index.md) · [Getting Started](../getting-started/quick-start.md) · [Modules](./modules.md) · [Maven Model Download](./downloads.md)

# Models Guide

This guide describes how Spring Vision handles model files today: where they live, how they are loaded, and how you can customize or download them.

## Model Locations

- **Primary**: `classpath:/models` (bundled in JAR for portability)
- **Fallback**: DJL Model Zoo (runtime download for advanced models)
- **Override**: External paths via configuration (for custom models)

## Model Loading Strategy

Models are loaded with this priority:

1) **Classpath resources** (bundled YOLO, RetinaFace models)
2) **DJL Model Zoo** (runtime download for advanced AI models)
3) **External paths** (configured via properties)
4) **User cache** (`~/.spring-vision/models/`)

## Bundled Models (Included in JAR)

### ✅ **Core Production Models**
- **YOLOv8 Object Detection**: `yolov8n.pt`, `yolov8s.pt`, `yolov8m.pt`, `yolov8l.pt`, `yolov8x.pt`
- **YOLOv8 Pose Estimation**: `yolov8n-pose.pt`, `yolov8s-pose.pt`, `yolov8m-pose.pt`
- **YOLOv8 Segmentation**: `yolov8n-seg.pt`, `yolov8s-seg.pt`, `yolov8m-seg.pt`
- **RetinaFace**: `retinaface.pt` (high-accuracy face detection)
- **Classification**: `yolov8n-cls.pt` (image classification)
- **OBB Detection**: `yolov8n-obb.pt` (oriented bounding boxes)

**Total bundled size**: ~500MB (all variants included)

### 📚 **Runtime Download Models**
Advanced AI models downloaded on-demand:
- OCR models (text recognition)
- Image classification models (ResNet, Inception)
- Segmentation models (instance/semantic)
- Action recognition models
- Face embedding models

## Configuration

Spring Vision uses DJL for unified model management:

```yaml
spring:
  vision:
    djl:
      enabled: true                    # Enable DJL backend
      engine: pytorch                  # pytorch, tensorflow, onnx
      device: cpu                      # cpu or gpu
      max-concurrent-inferences: 4     # Concurrency limit
      show-progress: false             # Model loading progress

      # Model-specific settings
      object-detection:
        model: yolo                   # yolo (bundled) or ssd (runtime)
        confidence-threshold: 0.5

      pose-estimation:
        model: yolo                   # yolo (bundled) or simple_pose (runtime)
        confidence-threshold: 0.5

      face-detection:
        confidence-threshold: 0.7     # RetinaFace bundled
        max-faces: 200
```

### Custom Model Paths

For custom models, override the default paths:

```yaml
spring:
  vision:
    djl:
      # Custom model directory (external to JAR)
      model-path: /opt/spring-vision/models

      # Or use environment variable
      # model-path: ${SPRING_VISION_MODELS:/opt/spring-vision/models}
```

**Tips:**
- **Classpath models** (bundled): Maximum portability for Docker/CI/CD
- **External paths**: For large custom models or model updates without rebuilding
- **Runtime downloads**: For advanced models not included in JAR

## Build-Time Model Download

Core models are downloaded and bundled during the Maven build:

```bash
# Download and bundle all models (run during CI/CD)
mvn clean install -Pdownload-models

# Or with Makefile
make build
```

This includes:
- YOLOv8 models (object detection, pose, segmentation)
- RetinaFace model (face detection)
- All model variants (n, s, m, l, x sizes)

## Adding Custom Models

Override bundled models with custom ones:

```yaml
spring:
  vision:
    djl:
      model-path: /path/to/custom/models
```

Place your models in the expected directory structure:
```
/path/to/custom/models/
├── yolov8/yolov8n.pt          # Custom object detection
├── yolov8-pose/yolov8n-pose.pt # Custom pose estimation
└── retinaface/retinaface.pt    # Custom face detection
```

## Docker and Cloud Deployment

### Self-Contained JARs
```dockerfile
FROM openjdk:21-jre-slim
COPY target/spring-vision-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- No model volumes needed
- All core models bundled in JAR
- Maximum portability

### External Models
```dockerfile
FROM openjdk:21-jre-slim
VOLUME /opt/models
ENV SPRING_VISION_MODELS=/opt/models
COPY target/spring-vision-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- Mount model volumes for updates without rebuilding
- Useful for large custom models

## GPU Acceleration

Enable GPU support for faster inference:

```yaml
spring:
  vision:
    djl:
      device: gpu
      engine: pytorch
```

**Requirements:**
- NVIDIA CUDA drivers installed
- CUDA-compatible GPU
- DJL CUDA runtime dependencies

See: [GPU Configuration Guide](../configuration/gpu.md)

---

See also: [Configuration](../configuration/config.md) · [GPU Configuration](../configuration/gpu.md) · [Runtime](../configuration/runtime.md) · [Architecture](./architecture.md)

