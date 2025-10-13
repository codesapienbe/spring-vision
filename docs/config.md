[Docs Home](./index.md) · [Getting Started](./start.md) · [Modules](./modules.md) · [GPU](./gpu.md)

# Backend Properties Guide

## Overview

Backends use dedicated `*Properties` configuration classes. Business logic reads configuration only via strongly-typed properties injected into the backend classes.

## Current Properties (examples)

### YOLO Backend (`yolo` module)

Properties include:

- `modelPath` (default: `classpath:/models`)
- `modelName` (default: `yolov8n.onnx`)
- `confidenceThreshold` (default: `0.25`)
- `nmsThreshold` (default: `0.45`)
- `maxDetections` (default: `100`)
- `enableAutoDownload` (default: `true`)
- `downloadTimeoutSeconds` (default: `300`)
- `inputSize` (default: `640`)

### MediaPipe Backend (`mediapipe` module)

Properties include:

- `modelPath` (default: `classpath:/models`)
- `confidenceThreshold` (default: `0.7`)
- `maxDetections` (default: `10`)
- `enableAutoDownload` (default: `true`)
- `downloadTimeoutSeconds` (default: `30`)
- `maxPoolSize` (default: `5`)
- `poolTimeoutSeconds` (default: `60`)

### InsightFace Backend (`insightface` module)

Properties include model metadata for model packs (`buffalo_l`, `buffalo_m`, `buffalo_s`) and standard path/threshold tuning.

## Best Practice Pattern

- Inject and read configuration via `*Properties`
- Do not embed static configuration inside service logic
- Keep model metadata in properties to enable overrides

## Configuration Example

application.yml:

```yaml
spring:
  vision:
    yolo:
      enabled: true
      model-name: yolov8m.onnx
      confidence-threshold: 0.3
      max-detections: 50
      model-info:
        custom-model.onnx:
          url: https://example.com/custom-model.onnx
          checksum: sha256:abc123...
          name: custom-model
```

---

See also: [Modules Overview](./modules.md) · [Models Guide](./models.md) · [Downloads](./downloads.md) · [Runtime](./runtime.md)
