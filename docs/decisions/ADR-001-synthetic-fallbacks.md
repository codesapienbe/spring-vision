# ADR-001: Synthetic fallback results for offline/test mode

## Status
Accepted

## Date
2026-05-16

## Context

`DjlVisionBackend` relies on YOLO and RetinaFace models that are large binaries (~200–500 MB each). Downloading these during CI or unit-test runs is slow, consumes bandwidth, and fails in air-gapped environments. Without a fallback, every test that touches face detection, object detection, pose estimation, or image classification fails with a `model_not_initialized` exception — making the entire test suite network-dependent.

The framework also needs to prove that application wiring (Spring context, `VisionTemplate` routing, capability interface dispatch) is correct independently of whether GPU/model inference actually works.

## Decision

When both of the following conditions are true at runtime:

1. **Models are unavailable** — either `ai.djl.offline=true` (JVM system property) **or** `spring.vision.djl.auto-download=false` (application property)
2. **Synthetic mode is opted in** — `spring.vision.djl.synthetic-fallbacks=true`

…each detection method returns a small, deterministic list of plausible-looking `Detection` objects instead of throwing an exception.

The opt-in flag (`synthetic-fallbacks`) defaults to `false` so that production deployments are never silently degraded. Test configurations explicitly set it to `true`.

The gate is encapsulated in a single helper:

```java
// DjlVisionBackend
private boolean shouldUseSynthetic() {
    return (djlOffline || !properties.isAutoDownload()) && properties.isSyntheticFallbacks();
}
```

`djlOffline` is read once at construction from `System.getProperty("ai.djl.offline", "false")` and cached as a final field.

## Alternatives Considered

### Always fail hard when models are missing
- Pros: No risk of accidentally shipping fake results.
- Cons: Every developer workstation and CI job needs the full model set (~2 GB). Test suite becomes network-dependent and slow.
- Rejected: The ops cost outweighs the benefit; the double opt-in (`auto-download=false` + `synthetic-fallbacks=true`) is sufficient guard.

### Mock the entire `DjlVisionBackend` bean in tests
- Pros: Zero risk of production code touching synthetic paths.
- Cons: Mocking the backend means tests never exercise the actual capability dispatch, error handling, or `VisionTemplate` routing — the very code paths most likely to break.
- Rejected: Integration tests need the real object graph.

### Ship a tiny "stub" model file for tests
- Pros: Tests exercise the real inference path.
- Cons: ONNX/TorchScript model files require a valid architecture header; a stub needs to be a valid model binary, which is not straightforward to maintain.
- Rejected: Synthetic results achieve the same structural validation more simply.

## Consequences

- Integration tests pass without downloading models. CI pipelines can run without network access to DJL model zoo.
- Synthetic results are clearly labeled: every synthetic `Detection` carries `attrs.get("model") == "synthetic"` so callers can detect the mode at runtime.
- Tests that assert *accuracy* of detection results (confidence thresholds, bounding-box precision) cannot be run in synthetic mode — they must download real models and are excluded from offline CI.
- Any new capability added to `DjlVisionBackend` must implement a synthetic branch in the model-null guard, or it will throw `VisionBackendException` in offline mode.
- **Do not add `Thread.sleep()` to synthetic paths.** Artificial delays were removed (2026-05-16); they add latency to tests and, if `synthetic-fallbacks` is ever enabled in a staging environment, would affect real response times.
