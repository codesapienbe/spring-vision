# Spring Vision - TODO List

## HIGHEST PRIORITY TODOs

### 1. Remove All Tests and Test Configurations (IMMEDIATE)

- [x] ~~Remove all test files from all modules~~
- [x] ~~Remove test dependencies from pom.xml files~~
- [x] ~~Remove test-related build plugins (Surefire, JaCoCo)~~
- [x] ~~Remove test configurations and exclusions~~
- [x] ~~Clean up test resources and test-specific properties~~
- [x] ~~Verify all test references are completely removed from build configurations~~
- [x] ~~Ensure no test-related system properties remain in production builds~~
- [x] ~~Remove any remaining test annotations or test-specific imports from source code~~

### 2. Create Spring Vision Examples (HIGHEST PRIORITY)

#### 2.1 CLI-Based Application

- [ ] **TODO: Create CLI-based Spring Vision application**
  - [x] Created `spring-vision-examples/picocli-application/` module
  - [x] Implement command-line interface for image processing
  - [x] Support batch processing from command line
  - [x] Add interactive mode for single image processing
  - [x] Include progress bars and real-time feedback
  - [x] Support multiple output formats (JSON, CSV) [XML pending]
  - [x] Add configuration file support
  - [x] Include help and usage documentation
  - [x] Add `--embed <image>`: extract face embeddings (JSON output)
    - [x] Use FaceBytes embeddings initially (fast path), return per-face vectors and bounds
    - [x] Add optional `--truncate <n>` to shorten printed vector for readability (no effect on computation)
    - [x] Validate file path (exists, regular file, readable), secure home expansion
  - [x] Add `--verify <image1> <image2>`: verify if two photos belong to the same person
    - [x] Extract embeddings per image; select top-confidence face when multiple
    - [x] Support `--metric cosine|euclidean` (default: cosine)
    - [x] Support `--threshold <value>` (default: 0.35 for cosine; 1.24 for euclidean)
    - [x] Output JSON/text with distances and boolean `is_match`
    - [x] Secure path handling, meaningful error messages
  - [x] Add `--verify-batch <image> <directory>`: verify reference image against all images in a directory
    - [x] Options: `--metric`, `--threshold`, `--format json|csv|text`
    - [x] Show progress with `--progress`; continue on errors
    - [x] Output includes: file name, distance, is_match (and optional index)
    - [x] Secure path handling (exists, directory, readability), home expansion
    - [x] Skip unreadable/unsupported files with warning; do not abort batch
    - [x] Consider parallel processing with virtual threads (optional)
  - [x] Add `--obscure <input> <output>`: obscure faces in an image using blur
    - [x] Detect faces and apply Gaussian blur to face regions
    - [x] Preserve original image structure and quality
    - [x] Secure path handling, create output directories if needed
    - [x] Return original image if no faces detected
    - [x] Support common image formats (JPEG, PNG, etc.)
  - [ ] Future: Prefer OpenCV SFace for embeddings when `org.bytedeco.opencv.opencv_face.FaceRecognizerSF` is present
    - [ ] Fallback to FaceBytes when SFace class is unavailable
    - [ ] Keep model downloads at build (YuNet/SFace) to avoid runtime fetches

#### 2.2 GWT-Based GUI Application

- [ ] **TODO: Create GWT-based GUI application**
  - [x] Created `spring-vision-examples/gwt-application/` module
  - [ ] Implement web-based GUI using Google Web Toolkit
  - [ ] Add drag-and-drop image upload functionality
  - [ ] Create real-time image preview and detection visualization
  - [ ] Implement batch processing with progress indicators
  - [ ] Add detection result overlay on images
  - [ ] Include configuration panels for detection parameters
  - [ ] Add export functionality for results
  - [ ] Implement responsive design for different screen sizes

#### 2.3 Vaadin-Based GUI Application

- [ ] **TODO: Create Vaadin-based GUI application**
  - [x] Created `spring-vision-examples/vaadin-application/` module
  - [x] Implement modern web-based GUI using Vaadin Framework
  - [ ] Add advanced image upload with preview
  - [ ] Create interactive detection result visualization
  - [ ] Implement real-time processing with WebSocket updates
  - [ ] Add comprehensive configuration management
  - [ ] Include batch processing with detailed progress tracking
  - [ ] Add result export in multiple formats
  - [ ] Implement user authentication and session management
  - [ ] Add responsive design and mobile support

## MEDIUM PRIORITY TODOs

### 3. Framework Improvements

- [ ] **TODO: Add comprehensive logging throughout the framework**
- [ ] **TODO: Implement proper error handling and recovery mechanisms**
- [ ] **TODO: Add performance monitoring and metrics collection**
- [ ] **TODO: Create comprehensive documentation and user guides**
- [ ] **TODO: Add support for additional vision backends (MediaPipe, YOLO, etc.)**

#### 3.1 MediaPipe Backend Implementation Roadmap (`spring-vision-core/src/main/java/com/springvision/core/backend/MediaPipeVisionBackend.java`)

- [ ] **Face Detector (Tasks API) integration**
  - [ ] Implement MPImage conversion from `byte[]` safely (support JPEG/PNG/WebP; handle color space/rotation if surfaced by MediaPipe)
  - [ ] Build `BaseOptions` and `FaceDetectorOptions` via reflection; prefer `setModelAssetPath`/`setModelPath`
  - [ ] Map `FaceDetectorResult` to `Detection` with normalized `BoundingBox` and include keypoints in `attributes` (e.g., `left_eye`, `right_eye`, `nose_tip`)
  - [ ] Expose confidence threshold via internal constant; consider future property binding without breaking `VisionProperties`
  - [ ] Ensure resources are closed if the task instances implement `AutoCloseable`

- [ ] **Hand Landmarker integration**
  - [ ] Wire `HandLandmarker` and options via reflection similar to face detector
  - [ ] Map landmarks per hand to `Detection` entries (no bounding box required); include landmark list in `attributes`

- [ ] **Pose Landmarker integration**
  - [ ] Wire `PoseLandmarker` via reflection; IMAGE running mode
  - [ ] Map pose landmarks to one or more `Detection` items with skeletal keypoints in `attributes`

- [ ] **Model management & security**
  - [ ] Keep model auto-download with strict timeouts and HTTPS only; validate content length > 0 and handle redirects
  - [ ] Add checksum verification (SHA-256) for downloaded artifacts; store alongside cache
  - [ ] Provide a simple opt-out switch (environment/system property) to disable auto-download without changing public API

- [ ] **Performance & stability**
  - [ ] Add simple object pooling or memoized singletons for task instances when safe (thread-confinement documented)
  - [ ] Warm-up path to reduce first-call latency
  - [ ] Defensive OOM handling for large images; early-return with `VisionProcessingException`

- [ ] **Structured logging & observability**
  - [ ] Ensure JSON-structured logs with fields: `timestamp`, `level`, `component`, `message`, `correlation_id`
  - [ ] Add fine-grained DEBUG logs around model load, MPImage creation, detect invocation, and mapping steps
  - [ ] Add lightweight internal metrics counters (success/fail/latency) and surface via `VisionMetrics` without changing public contracts

- [ ] **Graceful shutdown**
  - [ ] Implement `shutdown()` to close/cleanup MediaPipe task instances and release native resources

- [ ] **Compatibility & fallback**
  - [ ] Keep reflection guards for missing classes; return descriptive errors without breaking `VisionTemplate`
  - [ ] Clamp and validate all normalized coordinates to [0,1]; sanitize attributes; no PII or raw image data in logs

#### 3.2 YOLO Backend Implementation Roadmap (`spring-vision-core/src/main/java/com/springvision/core/backend/YoloVisionBackend.java`)

- [ ] **Backend scaffolding**
  - [ ] Create `YoloVisionBackend` implementing `VisionBackend`; initially support `DetectionType.OBJECT`
  - [ ] Maintain strict backward compatibility; do not change public APIs or `VisionProperties`

- [ ] **Inference engine integration (reflection-based, optional deps)**
  - [ ] Primary: ONNX Runtime (`ai.onnxruntime.*`) via reflection; load `.onnx` models without hard dependency
  - [ ] Alternative (future): TensorFlow Lite (`org.tensorflow.lite.*`) and OpenVINO, behind reflection guards
  - [ ] Select first available engine at runtime; expose engine name/version in `BackendHealthInfo` metrics

- [ ] **Pre/Post-processing pipeline**
  - [ ] Implement letterbox resize with aspect-ratio preservation and configurable stride
  - [ ] Normalize inputs (scale/mean/std) per model requirements; support RGB/BGR switches
  - [ ] Decode YOLO outputs (v5/v8/v9) with anchors/grids as needed; compute boxes in original image space
  - [ ] Implement class score filtering and NMS (Greedy/Soft-NMS configurable internally)
  - [ ] Map results to `Detection` with normalized `BoundingBox`, `label`, `confidence`, and optional attributes (classId)

- [ ] **Model management & security**
  - [ ] Support model loading from local path and secure HTTPS auto-download (timeouts, redirects off by default)
  - [ ] Add SHA-256 checksum verification and optional signature verification
  - [ ] Environment/system property to disable auto-download (no public API change)
  - [ ] Allow optional labels file; map class indices to human-readable labels

- [ ] **Configuration (non-breaking)**
  - [ ] Internal constants for `CONFIDENCE_THRESHOLD`, `IOU_THRESHOLD`, `INPUT_SIZE` (e.g., 640)
  - [ ] Consider reading overrides from env/system properties without modifying `VisionProperties`

- [ ] **Performance & stability**
  - [ ] Warm-up inference and model caching; reuse session/execution providers
  - [ ] Support batched inference path when feasible
  - [ ] Prefer direct buffers (NDArray/FloatBuffer) to reduce copies; guard memory usage

- [ ] **Structured logging & observability**
  - [ ] Log inference timings, model/engine selection, NMS stats, and detection counts (JSON structured)
  - [ ] Expose lightweight counters via `VisionMetrics` (no API change)

- [ ] **Graceful shutdown**
  - [ ] Close inference sessions/resources in `shutdown()`; handle engine-specific cleanup

- [ ] **Compatibility & fallback**
  - [ ] If engines are unavailable, return descriptive errors while keeping the app stable
  - [ ] Clamp all normalized coordinates to [0,1]; sanitize labels; never log image data or PII

#### 3.4 InsightFace Backend Implementation Roadmap (`spring-vision-core/src/main/java/com/springvision/core/backend/InsightFaceVisionBackend.java`)

- [ ] **Backend scaffolding**
  - [ ] Create `InsightFaceVisionBackend` implementing `VisionBackend`; support `DetectionType.FACE` and `DetectionType.CUSTOM` for embeddings/verification
  - [ ] Keep public APIs/config intact; no changes to `VisionProperties`

- [ ] **ONNX-based integration (reflection and optional deps)**
  - [ ] Use ONNX Runtime via reflection to load standard InsightFace models: `RetinaFace` (detection), `ArcFace`/`Glint360K` (embedding)
  - [ ] Allow GPU providers if present (CUDA/DirectML) with safe fallbacks to CPU
  - [ ] Surface engine/provider info in `BackendHealthInfo` metrics

- [ ] **Pre/Post-processing**
  - [ ] Implement image normalization (RGB, mean/std), alignment (five-point landmark alignment) for embeddings
  - [ ] Decode `RetinaFace` outputs, apply NMS, and return normalized `BoundingBox` with confidence
  - [ ] Generate 512-D (or model-specific) embeddings; L2-normalize; map to `Detection` attributes (e.g., `embedding_checksum`, avoid raw vectors in logs)
  - [ ] Provide cosine similarity utilities under `CUSTOM` detection flow for verification

- [ ] **Model management & security**
  - [ ] Load models from local path or secure HTTPS with timeouts; verify SHA-256 checksums
  - [ ] Optionally support model zoo identifiers; cache under `~/.spring-vision/insightface-models`
  - [ ] Add env/system property to disable auto-download

- [ ] **Configuration (non-breaking)**
  - [ ] Internal constants for thresholds: `FACE_CONFIDENCE`, `NMS_IOU`, `VERIFY_THRESHOLD`
  - [ ] Optional env/system overrides; no public property changes

- [ ] **Performance & stability**
  - [ ] Reuse ONNX sessions; warm-up paths; prefer direct buffers for inputs/outputs
  - [ ] Avoid large intermediate allocations; clamp image sizes; return `VisionProcessingException` on resource pressure

- [ ] **Structured logging & observability**
  - [ ] Log engine/provider, load/detect/embedding timings, detection counts (JSON structured)
  - [ ] Expose counters/latency via `VisionMetrics` without public API changes

- [ ] **Graceful shutdown**
  - [ ] Close ONNX sessions/resources; update health state

- [ ] **Compatibility & fallback**
  - [ ] If ONNX Runtime missing or models unavailable, return descriptive errors; do not crash the app
  - [ ] Clamp normalized coordinates to [0,1]; never log raw image bytes or full embeddings; redact PII

#### 3.5 Embedding APIs (non-breaking additions)

- [x] Add optional embedding/verification APIs to `VisionBackend` and `VisionTemplate` (default provided safely)
  - [x] `List<float[]> extractEmbeddings(ImageData)` and `boolean verify(ImageData a, ImageData b, String metric, double threshold)`
  - [ ] Implement in `OpenCvVisionBackend` using SFace when available; fallback to FaceBytes when not
  - [x] Do not break existing public contracts; defaults provided safely

#### 3.6 Face Obscuring APIs (non-breaking additions)

- [x] Add face obscuring API to `VisionBackend` and `VisionTemplate` (default: unsupported)
  - [x] `ImageData obscureFaces(ImageData)` - detects faces and applies blur/obfuscation
  - [x] Implement in `OpenCvVisionBackend` using OpenCV Gaussian blur
  - [x] Preserve image quality and structure while protecting privacy
  - [x] Return original image if no faces detected
  - [x] Support common image formats and coordinate normalization

### 4. Testing Strategy (Future)

- [ ] **TODO: Design and implement advanced testing strategy using modern testing libraries**
- [ ] **TODO: Add integration tests with proper test containers**
- [ ] **TODO: Implement performance and load testing**
- [ ] **TODO: Add security testing and vulnerability scanning**

## COMPLETED TASKS

### ✅ Build Issue Fixes

- [x] Fixed OpenCV native library loading issues
- [x] Resolved compilation errors in starter module
- [x] Fixed test configuration issues
- [x] Removed all test files and configurations
- [x] Cleaned up Maven build configurations
- [x] Removed all test dependencies and properties from parent pom.xml
- [x] Verified no test references remain in any module

### ✅ Framework Core

- [x] Implemented core vision framework
- [x] Created OpenCV backend integration
- [x] Added autoconfiguration support
- [x] Implemented Spring Boot starter
- [x] Added health monitoring and metrics

## NOTES

- **Focus on manual testing first**: The framework should be thoroughly tested manually before adding automated tests
- **Examples are critical**: The three example applications will serve as both documentation and validation of the framework
- **CLI first**: Start with the CLI application as it's the simplest to implement and test
- **GUI applications**: The GWT and Vaadin applications will demonstrate the framework's versatility in different UI paradigms
