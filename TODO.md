# TODO – First Version Deployment

## Features to Implement

- **AnnotationCapability** – Implement annotation tools for image/video overlays.
- **BarcodeCapability** – Add barcode detection and decoding support. (Implemented: Default ZXing support in VisionBackend)
- **EmbeddingCapability** – Provide feature embedding generation for downstream tasks. (Implemented: DeepFace and CompreFace backends with verify support)
- **FaceDetectionCapability** – Integrate robust face detection across supported models. (Implemented: DeepFace and CompreFace backends)
- **HandDetectionCapability** – Enable detection of hands and gestures. (Implemented: MediaPipe backend)
- **LandmarkDetectionCapability** – Implement detection of facial and body landmarks. (Implemented: MediaPipe backend)
- **ObjectDetectionCapability** – Add generic object detection pipelines. (Implemented: YOLO backend)
- **PoseEstimationCapability** – Provide pose estimation for full‑body keypoints. (Implemented: MediaPipe backend)
- **TextOcrCapability** – Integrate OCR for extracting text from images. (Implemented: Tesseract backend)

## Deployment Checklist

- [x] Verify Maven multi‑module build (`mvn clean install`).
- [x] Containerize each module with Docker (ensure Dockerfiles exist).
- [x] Create a `docker-compose.yml` that starts all services.
- [x] Configure environment variables for model paths and API keys.
- [x] Set up CI/CD pipeline (GitHub Actions/GitLab CI) to build and push images.
- [x] Write integration tests for each capability.
- [x] Document API endpoints in `docs/API_REFERENCE.md`.
- [x] Prepare monitoring and logging (Prometheus, Grafana).

## Project Organization

- **spring-vision-core** – Core interfaces and shared utilities.
- **spring-vision-*** – Individual capability modules (e.g., `spring-vision-facebytes`, `spring-vision-deepface`).
- **spring-vision-examples** – Example applications demonstrating each capability.
- **docs/** – Documentation, deployment guides, and API specs.

*All items marked with a checkbox are part of the first release. Update this file as progress is made.*
