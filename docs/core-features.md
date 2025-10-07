# Spring Vision Core Module — Feature Overview

This document summarizes the capabilities and features provided by the spring-vision-core module. It focuses on the abstractions, default implementations, processing modes, observability, and enterprise utilities that the core module brings to your applications.

- Module: spring-vision-core
- Primary entrypoint for application code: com.springvision.core.VisionTemplate
- Core SPI: com.springvision.core.VisionBackend

## 1) Detection Capabilities

Core defines a unified model for vision tasks via DetectionType and related capability interfaces. Backends may implement any subset.

Supported detection categories (DetectionType):
- FACE — Face detection with bounding boxes and confidence
- OBJECT — Generic object detection with labels and confidence
- TEXT — OCR text detection/recognition
- BARCODE — Barcode/QR detection and decoding
- LANDMARK — Landmark/geolocation/architectural recognition
- POSE — Human pose estimation and keypoints
- HAND — Hand detection and gesture/keypoint tracking
- CUSTOM — User-defined detection types backed by custom models

Advanced detection categories (AdvancedDetectionType):
- POSE, HAND, FACE_LANDMARKS, TEXT_OCR, BARCODE_QR, SCENE, SEGMENTATION, DEPTH, MOTION, CUSTOM

Capability interfaces (com.springvision.core.capabilities.*) enable direct routing when a backend implements them:
- FaceDetectionCapability
- ObjectDetectionCapability
- TextOcrCapability
- BarcodeCapability
- LandmarkDetectionCapability
- PoseEstimationCapability
- HandDetectionCapability
- EmbeddingCapability
- AnnotationCapability (for obscuring, tagging, drawing/marking)

How this is used:
- VisionTemplate routes requests to capability methods when present for efficient handling (see VisionTemplate.routeViaCapabilitiesIfAvailable).

## 2) Default Backend in Core

The core module ships an OpenCV-based backend implementation:
- com.springvision.core.backend.OpenCvVisionBackend
  - Implements: FaceDetectionCapability, ObjectDetectionCapability, AnnotationCapability
  - Provides: face detection (Haar/YuNet/DNN fusion), object detection, barcode detection, basic embeddings (SFace), face obscuring/blurring, tagging and marking utilities.
  - Health reporting and self-initialization with model resolution and caching.

SPI registration:
- META-INF/services/com.springvision.core.VisionBackend
- Auto-configuration import: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

## 3) High-level API (VisionTemplate)

VisionTemplate offers a simple, opinionated facade over the backend SPI:
- Detect once: detect(image, DetectionType)
- Detect convenience methods: detectFaces, detectObjects
- Detect multiple at once: detectMultiple(image, List<DetectionType>)
- Embeddings: extractEmbeddings(image)
- Face verification: verify(imageA, imageB, metric, threshold)
- Privacy/annotation: obscureFaces(image), annotate(image, request), tag(image, label, categories), mark(image, categories)
- Health: getBackendHealthInfo(), isBackendHealthy()
- Discovery: getSupportedDetectionTypes(), supportsDetectionType()

Data types:
- ImageData — unified image container (bytes, paths, etc.)
- Detection — immutable detection record with box, type, label, confidence, attributes
- BoundingBox, DetectionType, DetectionCategory

## 4) Processing Modes

Core provides several processing utilities for different workloads:
- Synchronous processing via VisionTemplate (most common)
- Async processing: com.springvision.core.async.AsyncVisionProcessor
  - Submit tasks, track TaskProgress, cancel, and observe status via callbacks
- Batch processing: com.springvision.core.batch.BatchVisionProcessor
  - Efficiently process large image lists with result aggregation and error handling

## 5) Embeddings, Similarity and Vector Ops

- extractEmbeddings(ImageData) on backends implementing EmbeddingCapability
- Face verification API in VisionTemplate.verify using selectable metrics and thresholds
- Vector service hooks for storage/lookup when provided (VisionTemplate constructor with VectorService)

## 6) Observability and Health

- BackendHealthInfo and VisionBackend.isHealthy()/getHealthInfo()
- Spring Boot Actuator health integration via VisionHealthIndicator
- Structured logging utilities: com.springvision.core.logging.VisionLogger with correlation IDs

## 7) Configuration and Auto-Configuration

- VisionAutoConfiguration — core auto-configuration for VisionTemplate and backend wiring
- FaceRecognitionAutoConfiguration — additional auto-config for face-recognition features (when available)
- Externalized configuration under spring.vision.* (see starter for properties binding)

## 8) Plugins and Extensibility

- VisionBackend is a pluggable SPI; additional backends can be discovered via ServiceLoader
- PluginContext and PluginMetadata provide metadata, requirements, and configuration hints for plugins
- DetectionCategory enables selective operations for annotation/tagging flows

## 9) Enterprise Utilities

- Caching strategies: com.springvision.core.enterprise.caching.AdvancedCachingStrategy (multi-layer with TTL and invalidation)
- Multitenancy support hooks: com.springvision.core.enterprise.multitenancy.TenantContext
- Distributed processing coordinator: com.springvision.core.enterprise.distributed.DistributedVisionProcessor
  - Register nodes with capabilities, schedule and route tasks, handle timeouts and retries

## 10) Error Handling and Safety

- Exception hierarchy: BaseVisionException, VisionBackendException, VisionProcessingException, VisionConfigurationException
- Input validation in VisionTemplate and backends
- Secure URL handling for remote resources (timeouts, SSRF-resilient patterns)

## 11) Testing Aids

- Example tests in core (e.g., OpenCvVisionBackendBarcodeTest) demonstrate barcode detection
- Integration patterns: VisionTemplateIntegrationTest illustrates end-to-end flows

## 12) Where to Start

- Use VisionTemplate from your Spring components to perform detections
- Check getSupportedDetectionTypes() for your chosen backend
- For custom backends, implement VisionBackend and optional capability interfaces, then register via ServiceLoader

References to key classes:
- com.springvision.core.VisionTemplate
- com.springvision.core.VisionBackend
- com.springvision.core.backend.OpenCvVisionBackend
- com.springvision.core.DetectionType, com.springvision.core.AdvancedDetectionType
- com.springvision.core.backend (package-info for summary)
- com.springvision.core.async.AsyncVisionProcessor
- com.springvision.core.batch.BatchVisionProcessor
- com.springvision.core.config.VisionAutoConfiguration, FaceRecognitionAutoConfiguration
- com.springvision.core.logging.VisionLogger
- com.springvision.core.enterprise.* (caching, multitenancy, distributed)
