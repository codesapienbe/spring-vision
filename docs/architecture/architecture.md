# Architecture

[Docs Home](../index.md) · [Getting Started](../getting-started/start.md) · [Modules](./modules.md)

High-level view of Spring Vision's architecture.

## Layers

- **Application Layer**: REST API for computer vision operations
- **Framework Core**: VisionTemplate provides unified API for all vision tasks
- **DJL Backend**: Deep Java Library handles model management and inference
- **Vision Capabilities**: Modular detection and analysis capabilities

## Key Components

- **VisionTemplate**: Simple API for computer vision operations
- **Auto-Configuration**: Spring Boot integration with sensible defaults
- **DJL Backend**: Deep Java Library for AI model management
- **Detection Capabilities**: Face detection, object recognition, text extraction, etc.

## How It Works

1. Upload image via REST API
2. VisionTemplate processes the request
3. DJL backend runs AI models on the image
4. Results returned as structured data

## Supported Operations

- Face detection and analysis
- Object detection and classification
- Text recognition (OCR)
- Image classification
- Pose estimation
- Emotion recognition

See also: [Modules](./modules.md), [Models Guide](./models.md)
