# VisionBackend Refactor Plan: From Face-centric to Multi-category Vision

Status: Proposed (Backward-compatible)
Audience: Spring Vision maintainers and backend implementors
Versioning: Introduce deprecations in 1.x; removal no earlier than 2.0

---

## Problem Statement
`VisionBackend` currently includes face-specific operations (`detectFaces`, `obscureFaces`, `tagFaces`, `markFaces`, embeddings) alongside general detection. This leaks a specific domain (faces) into the core abstraction and hinders extensibility for hands, body, eyes, mouth, etc.

## Goals
- Generic, capability-driven core that supports faces, hands, body, eyes, mouth, objects, text, barcodes, pose, landmarks.
- Preserve existing API surface and behavior for 1.x (source/binary compatibility), with clear deprecation path.
- Provide a clean capability model so backends can implement only what they support.
- Maintain strong validation, error handling, and structured logging.

## Non-Goals (now)
- Rewrite existing backends.
- Introduce heavyweight ML frameworks beyond current guidelines.
- Break existing examples or integration contracts.

---

## High-level Design
1. Introduce capability interfaces under `com.springvision.core.capabilities`:
   - `FaceDetectionCapability` (detect faces)
   - `ObjectDetectionCapability` (detect objects)
   - `TextOcrCapability` (OCR)
   - `BarcodeCapability` (barcodes/QR)
   - `PoseEstimationCapability` (human pose)
   - `HandDetectionCapability` (hands/gestures)
   - `LandmarkDetectionCapability` (generic landmarks)
   - `AnnotationCapability` (obscure/annotate/mark by filter)
   - `EmbeddingCapability` (generic embeddings; parameterized by subject/category)

   Backends implement only the capabilities they support. The core `VisionBackend` remains the unifying SPI and advertises supported `DetectionType`s.

2. Add a generic query object for richer requests:
   - `DetectionQuery` builder with fields: `DetectionType type`, `Set<DetectionCategory> categories`, `Set<String> classLabels`, `double minConfidence`, `BoundingBox roi`, `int maxDetections`, `Map<String,Object> options`.
   - New default method: `VisionResult detect(ImageData, DetectionQuery)` in `VisionBackend` that delegates to existing `detect(ImageData, DetectionType)` when only `type` is present.

3. Add hierarchical categorization:
   - New enum `DetectionCategory`: `FACE`, `EYE`, `NOSE`, `MOUTH`, `EAR`, `HAND`, `BODY`, `PERSON`, `OBJECT`, `TEXT`, `BARCODE`, `LANDMARK`, `POSE`, `CUSTOM`.
   - Transitional use: store category in `Detection.attributes` under key `category` to avoid breaking the `Detection` record in 1.x.

4. Deprecate face-only methods on `VisionBackend` in favor of generic operations or `AnnotationCapability`:
   - `detectFaces(ImageData)` -> `detect(image, DetectionType.FACE)` or `FaceDetectionCapability.detectFaces(image)`
   - `obscureFaces(ImageData)` -> `AnnotationCapability.obscure(image, filter(category=FACE))`
   - `tagFaces(ImageData, tag)` / `markFaces(ImageData)` -> `AnnotationCapability.annotate(image, AnnotationRequest)`
   - `extractEmbeddings(ImageData)` -> `EmbeddingCapability.extract(image, subject=FACE)`

5. Keep `VisionTemplate` as the façade preserving current convenience methods while delegating to generic/capability paths. Log with the same structured fields.

---

## Step-by-step Refactor (Batches)

### Batch A: Foundations (No behavior change)
- Add `DetectionCategory` enum and `DetectionQuery` class (with builder and validation). Default everything optional except `type`.
- Add `VisionBackend.detect(ImageData, DetectionQuery)` default method that validates and delegates to `detect(ImageData, DetectionType)`.
- In `VisionTemplate`, add overloads: `detect(image, DetectionQuery)` and `detect(bytes, DetectionQuery)` preserving structured logging.

### Batch B: Capabilities API (Opt-in)
- Add capability interfaces and Javadocs in `com.springvision.core.capabilities`.
- Provide default adapters in `VisionTemplate` that detect capabilities via `instanceof` and route calls when available.
- No backend must change yet; defaults continue to work.

### Batch C: Deprecations (Backward-compatible)
- Mark on `VisionBackend` as `@Deprecated(since="1.x", forRemoval=false)`:
  - `detectFaces(ImageData)`
  - `obscureFaces(ImageData)`
  - `tagFaces(ImageData,String)`
  - `markFaces(ImageData)`
  - `extractEmbeddings(ImageData)` (superseded by `EmbeddingCapability`)
- Provide default implementations of deprecated methods that call the new generic/capability paths when present, otherwise delegate to existing behavior to avoid regressions.

### Batch D: Data Model Enrichment (Non-breaking)
- Encourage producers to include `category` in `Detection.attributes` (key: `category`, value: `DetectionCategory.name()`).
- Optionally add `parentId` semantics for part-of relationships (e.g., eyes belong to a face) via `attributes` keys: `parent_id`, `part_of`.

### Batch E: Backend Migration (Incremental)
- Update core backends to implement capability interfaces they naturally support:
  - OpenCV: `FaceDetectionCapability`, `AnnotationCapability` (blur/mark), optionally `ObjectDetectionCapability` if available.
  - MediaPipe/YOLO-like: object, hands, pose.
  - FaceBytes: `EmbeddingCapability` (subject=FACE) and `FaceDetectionCapability`.
- Replace direct face-specific logic with generic filters using `DetectionQuery.categories`.

### Batch F: Consumers & Examples (Optional)
- Update examples to demonstrate `DetectionQuery` and capability-aware usage while keeping old methods functional.
- Document migration notes in `docs/`.

### Batch G: 2.0 Cleanup (Breaking)
- Remove deprecated face-specific methods from `VisionBackend`.
- Consider promoting `category` to a first-class field by introducing `CategorizedDetection` or evolving `Detection` (major version only).

---

## API Sketches (Illustrative)

```java
// New
public enum DetectionCategory { FACE, EYE, NOSE, MOUTH, EAR, HAND, BODY, PERSON, OBJECT, TEXT, BARCODE, LANDMARK, POSE, CUSTOM }

public final class DetectionQuery {
	private final DetectionType type;
	private final java.util.Set<DetectionCategory> categories;
	private final java.util.Set<String> classLabels;
	private final double minConfidence;
	private final BoundingBox roi;
	private final int maxDetections;
	private final java.util.Map<String, Object> options;
	// builder + validation + getters
}

public interface VisionBackend {
	default VisionResult detect(ImageData image, DetectionQuery query) { /* validate + delegate */ }
}

// Capabilities
public interface FaceDetectionCapability { VisionResult detectFaces(ImageData image) throws BaseVisionException; }
public interface AnnotationCapability {
	ImageData obscure(ImageData image, java.util.function.Predicate<Detection> filter) throws BaseVisionException;
	ImageData annotate(ImageData image, AnnotationRequest request) throws BaseVisionException;
}
public interface EmbeddingCapability {
	java.util.List<float[]> extract(ImageData image, DetectionCategory subject) throws BaseVisionException;
}
```

Note: In 1.x, store `DetectionCategory` as an attribute, e.g. `detection.attributes().get("category")`.

---

## Validation, Security, and Logging
- Validate inputs in new API (non-null `type`, sane thresholds, ROI bounds) and fail-fast with descriptive messages.
- Maintain structured logging fields already used in `VisionTemplate`: `timestamp`, `level`, `component`, `message`, `correlation_id`, `backendId`, `detectionType`, add `categories`, `roi`, `minConfidence` when present.
- Sanitize free-form strings in annotations (max length, printable characters) and cap payload sizes.
- Preserve least-privilege: no additional permissions or I/O beyond existing backends.

---

## Backward Compatibility & Migration
- All new methods are additive; deprecated methods remain with defaults in 1.x.
- Existing applications and examples continue to compile and run unchanged.
- Migration path: replace face-specific calls with generic `detect(image, DetectionType.FACE)` or capability usage; use `DetectionQuery` for sub-parts (eyes, mouth) when backends support them.

---

## Acceptance Criteria
- Code compiles with deprecated APIs present.
- New types (`DetectionCategory`, `DetectionQuery`, capability interfaces) exist with Javadocs.
- `VisionTemplate` exposes `detect(image, DetectionQuery)` with structured logs and capability-aware routing when possible.
- No change in observable behavior for existing face/object flows.

---

## Rollout Plan
- Release as minor version with deprecations and documentation.
- Update backends incrementally; advertise capability coverage in release notes.
- Target removal of deprecated APIs in next major version (2.0) after examples and consumers have migrated to `DetectionQuery`/capabilities.

---

## Deprecation and Removal Schedule

Deprecated in 1.x (present, non-breaking):
- `VisionBackend.detectFaces(ImageData)`
- `VisionBackend.obscureFaces(ImageData)`
- `VisionBackend.tagFaces(ImageData, String)`
- `VisionBackend.markFaces(ImageData)`
- `VisionBackend.extractEmbeddings(ImageData)` (replaced by `EmbeddingCapability` with subject)

Migration Guidance:
- Detection: use `VisionTemplate.detect(image, DetectionType.FACE)` or `DetectionQuery`.
- Obscuring/Annotation: use `AnnotationCapability` with a face filter.
- Embeddings/Verification: prefer `EmbeddingCapability.extract(image, DetectionCategory.FACE)` and `VisionTemplate.verify(...)`.

Planned removal (2.0):
- Remove the deprecated methods listed above from `VisionBackend`.
- Keep `VisionTemplate` convenience methods that use `DetectionType`/`DetectionQuery`.
- Review whether to promote `category` from attributes to a first-class field.

Timeline:
- 1.x.Y: Deprecated APIs available with warnings in docs and Javadoc annotations.
- 2.0.0: Remove deprecated methods; update examples and docs accordingly. 