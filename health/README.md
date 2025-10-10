# spring-vision-health (design-only)

This module contains design, API interfaces and DTOs for health-related computer vision tasks.
It intentionally provides no implementations. The goal is to allow contributors to implement
pluggable backends (ONNX, DJL, OpenCV, DeepFace, DeepLearning4J, etc.) while keeping a
stable, well-documented API.

## IMPORTANT: Image-list based APIs (core integration)

All new health APIs are intentionally based on sequences of images rather than video streams.
This simplifies the design and aligns with the rest of the project which uses `ImageData` and
`Detection` as core data shapes. Implementations and client code should use `io.github.codesapienbe.springvision.core.ImageData`
for inputs and `io.github.codesapienbe.springvision.core.Detection` for outputs.

The canonical way to call health detectors is via the core `VisionTemplate` convenience methods or the
core capability SPI (see `io.github.codesapienbe.springvision.core.capabilities.*`). Example usages:

- Heart rate (sequence of frames -> aggregated detections):
    - `visionTemplate.detectHeartRate(List<ImageData> frames)`
    - or implement `io.github.codesapienbe.springvision.core.capabilities.HeartRateCapability`

- Fall detection (sequence of frames -> event detections):
    - `visionTemplate.detectFall(List<ImageData> frames)`
    - or implement `io.github.codesapienbe.springvision.core.capabilities.FallDetectionCapability`

- Stress analysis (sequence of frames -> aggregated stress detections):
    - `visionTemplate.detectStress(List<ImageData> frames)`
    - or implement `io.github.codesapienbe.springvision.core.capabilities.StressAnalysisCapability`

- Brain tumor classification (MRI images use core health DTOs):
    - Implement `io.github.codesapienbe.springvision.health.api.BrainTumorClassifier` as an adapter that delegates to
      `io.github.codesapienbe.springvision.core.health.MRIImage` and returns `io.github.codesapienbe.springvision.core.health.TumorClassificationResult`.

## Backwards compatibility / deprecated types

This module previously exposed streaming/video and listener-based APIs (e.g. `VideoSource`, `HeartRateListener`).
Those types are now deprecated in this module and retained only for backwards compatibility. New code should not use
these streaming types.

## Design principles / contributor guidance

- Implementations must depend on `spring-vision-core` and register capability implementations if they provide
  heart/fall/stress functionality (via `io.github.codesapienbe.springvision.core.capabilities.*`).

- Keep implementations in separate modules (e.g. `spring-vision-health-impl-opencv`, `-impl-onnx`).

- Prefer pure, stateless functions that accept `List<ImageData>` and return `List<Detection>` so they are easy to test.

## Suggested first contributions

1. Add a simple OpenCV-based `HeartRateCapability` prototype that computes a naive green-channel PPG and returns
   min/max/avg BPM as Detection metadata.
2. Add a fall-detection capability that uses pose estimation outputs (MediaPipe/OpenPose) to heuristically emit falls.
3. Implement a DJL/ONNX-backed `BrainTumorClassifier` adapter that consumes preprocessed `MRIImage` objects.

## Testing & CI

- Keep this module interface-only. Implementations can add native dependencies and heavier CI tasks in their own modules.
- Unit-test implementations with synthetic image sequences to avoid shipping large datasets in CI.

## Licensing

Follow the project-wide Apache 2.0 license.
