# Architecture

[Docs Home](./index.md) · [Getting Started](./start.md) · [Modules](./modules.md)

High-level view of Spring Vision.

## Layers

- Application Layer: REST, Web UI, CLI
- Framework Core: VisionTemplate, config, utilities
- Backends: OpenCV, YOLO, MediaPipe, FaceBytes, DeepFace, InsightFace, Tesseract
- Integrations: Persistence, Health, Cyber, Robotics (planned)

## Key Components

- VisionTemplate: Unified API over multiple backends
- Auto-Configuration: Spring Boot starters and properties
- ModelResourceLoader: Unified model resolution (classpath, external path, cache, auto-download)
- ONNX Runtime Config: CPU/GPU providers with graceful fallback

## Data Flow

1. Input image/frame bytes
2. VisionTemplate orchestrates processing
3. Selected backend performs detection/recognition
4. Results returned as typed Detection objects

## Extensibility

- Pluggable backends with consistent property conventions
- Configuration via Spring Boot properties
- Optional GPU execution provider

See also: [Modules](./modules.md), [Models Guide](./models.md)
