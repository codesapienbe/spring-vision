# FAQ

[Docs Home](./index.md) · [Getting Started](../getting-started/quick-start.md) · [GPU](./gpu.md)

## What Java and Spring versions are required?

Java 21+ and Spring Boot 3.2+.

## How do I enable GPU acceleration?

Build with `-P gpu` and set `spring.vision.execution-provider=gpu`. See [GPU Acceleration](./gpu.md).

## Do I need to download models manually?

Most are bundled or auto-downloaded during build. See the [Models Guide](./models.md) and [Maven Model Download](./downloads.md).

## Which backend should I choose?

- OpenCV: fast classical + DNN face models
- YOLO: real-time object detection
- MediaPipe: face/hand/pose
- FaceBytes/DeepFace/InsightFace: recognition and embeddings

See [Modules Overview](./modules.md).

## Is there a starter for zero config?

Yes: add the `starter` dependency and use `VisionTemplate`. See [Getting Started](../getting-started/quick-start.md).
