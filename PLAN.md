Plan for making all DJL-backed capabilities reliably available in `DjlVisionBackend`

Overview
--------
This repository already contains a DJL-based backend (`DjlVisionBackend`) that implements face detection, object detection, pose estimation, action recognition, semantic & instance segmentation, face embeddings (recognition), OCR, and image classification.

Goal
----
Make sure all listed capabilities "have to work" by ensuring required models are available locally (DJL model cache), verifying model loading, adding small runtime improvements (optional), and adding prefetch/test automation so CI and local dev can reproduce the setup reliably.

High-level approach
-------------------
Work in small batches. Each batch is self-contained and verifiable before moving to the next.

Batches (one at a time)
-----------------------

- Batch 1 — Ensure required models are available (two supported approaches)
    - This project supports exactly two ways to obtain DJL model artifacts; choose one and use it consistently:
        1) Runtime fetch via DJL's Model Zoo/Criteria API: let DJL load models at runtime using Criteria (djl:// or permissive criteria). This is the default runtime behavior and is appropriate when network access is available at runtime and you want DJL to manage model resolution.
        2) Build-time fetch via Maven: use the Maven-driven download/unpack flow bound to `generate-resources` (configured in `mcp/pom.xml` / `core/pom.xml`) to download model ZIPs into the build's `target/djl-cache`. This is the recommended option for CI or offline builds where you need deterministic artifacts and a failing build when downloads/extractions fail.
    - Do not use ad-hoc runtime prefetch helper classes or scripts — the above two methods are the only supported fetch mechanisms.
    - Outcome: model artifacts present in the DJL cache (either runtime-managed or pre-downloaded at build-time).

- Batch 2 — Verify cache & smoke test capabilities
    - Inspect `target/djl-cache` (and default `~/.djl.ai`) for key model tokens (retinaface, simple_pose/movenet/openpose, action_recognition, deeplabv3, yolo11n-seg, face_feature, etc.).
    - Run a small set of integration smoke tests (existing examples or lightweight JUnit tests if available) for face detection, object detection, pose estimation, segmentation, embeddings, OCR and classification.
    - Outcome: list of models that loaded successfully and any missing ones.

- Batch 3 — Download / pin missing models explicitly
    - For any missing required models, either:
        - Use a concrete DJL model URI (djl://) or permissive Criteria at runtime so DJL resolves the model, or
        - Add the model ZIP URL to the Maven download step (properties in `mcp/pom.xml`) so it is fetched during `generate-resources`.
    - Outcome: required set of models present in cache.

- Batch 4 — Improve runtime (small changes)
    - Optionally cache OCR and classification models in `DjlVisionBackend` (they are loaded per-call today). Implement and test this change if needed for performance.
    - Optionally add explicit model-name/config keys for action labels and face-embedding input size if required.
    - Outcome: safer runtime behavior and lower warm-up latency.

- Batch 5 — Documentation & CI
    - Update README/docs to explain the two supported fetch approaches and how CI should use the Maven plugin approach (recommended) for reproducible builds.
    - Wire a CI job or Makefile target to run Maven for the `mcp` module (or the root aggregator) so the `generate-resources` downloads run prior to packaging.
    - Outcome: reproducible builds and clearer developer onboarding.

Verification & quality gates
---------------------------

- After each batch:
    - Capture download/build logs (for build-time fetch) or DJL runtime logs (for runtime fetch) and verify exit codes/status.
    - Run lightweight smoke tests where possible.
    - If any model loading fails, gather the DJL `ModelNotFoundException`/logs and attempt explicit `optModelUrls` for that artifact.

Commands (copyable)
-------------------

# Build mcp (this runs generate-resources -> downloads + unpack)

mvn -pl mcp -DskipTests package

# Or let DJL fetch models at runtime (no build-time downloads required)

# Use your application normally; DJL will resolve models via Criteria/djl:// entries

***

Notes & risks
-------------

- Downloading models may be large and take time or bandwidth.
- Some models may not have a direct `djl://` model-zoo entry; using permissive Criteria or explicit model URIs/ZIP URLs may be necessary.
- GPU/native engine downloads (for CUDA-enabled PyTorch) may require matching native libraries; prefetching on CPU is the safest initial approach.

Next step (starting now)
------------------------
Run the Maven build for `mcp` to execute the download/unpack steps (or let DJL resolve models at runtime):

mvn -pl mcp -DskipTests package

The project now supports only the two fetch approaches documented above; remove references to runtime prefetch scripts or helper classes.
