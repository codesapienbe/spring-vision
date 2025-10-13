[Docs Home](./index.md) · [Getting Started](./start.md) · [Modules](./modules.md) · [Maven Model Download](./downloads.md)

# Models Guide

This guide describes how Spring Vision handles model files today: where they live, how they are loaded, and how you can customize or download them.

## Model Locations

- Default path for all backends: `classpath:/models`
- You can override with an external path via properties (see Configuration below)

## Model Loading Strategy

Model resolution is unified by the ModelResourceLoader with this priority:

1) Classpath resources (bundled in the JAR)
2) Explicitly configured external path
3) User cache directory: `~/.spring-vision/models/{module}/`
4) Optional auto-download from official sources

## Bundled Models (Current)

- Core/OpenCV:
    - Haar Cascades, DNN face models, ONNX models (e.g., YuNet and SFace)
    - Total size ~40 MB
- Other modules:
    - MediaPipe and YOLO models can be added to resources, or auto-downloaded (see downloads section)

## Configure Model Paths

application.yml examples:

```yaml
spring:
  vision:
    opencv:
      enabled: true
      # Defaults to classpath:/models
      # modelPath: /custom/models/opencv
      # enableAutoDownload: false

    yolo:
      enabled: true
      model-name: yolov8n.onnx
      # modelPath: /custom/models/yolo

    mediapipe:
      enabled: true
      # modelPath: /custom/models/mediapipe
```

Tips:

- Use classpath models for maximum portability (Docker, CI/CD, read-only FS)
- Use external paths for large/custom models you don’t want bundled

## Auto-Download and Verification

- For some modules, models can be auto-downloaded during build or at runtime
- Checksum verification (SHA-256) is supported to prevent corruption
- HTTPS-only download policy recommended

See: [Maven Model Download Guide](./downloads.md)

## Adding Custom Models

- Place your ONNX/TFLite files under your chosen model path
- Update the corresponding module properties (e.g., model-name)
- For YOLO, convert from PyTorch to ONNX using Ultralytics export

## Docker and Cloud

- With classpath models, no volumes are needed; JARs are self-contained
- External paths work with mounted volumes if you prefer dynamic model swaps

## GPU Considerations

- GPU acceleration benefits larger models and batch workloads
- Build with `-P gpu` and set `spring.vision.execution-provider=gpu`
- See: [GPU Acceleration](./gpu.md)

---

See also: [Configuration](./config.md) · [Maven Model Download](./downloads.md) · [Runtime](./runtime.md) · [Architecture](./architecture.md)

