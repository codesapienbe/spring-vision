# TODO: Fix startup and model issues for spring-vision-mcp

Purpose: collect the observations from the container logs and provide a prioritized, batched remediation plan that can be executed one batch at a time. Each batch is sized so a single iteration's changes + logs + tests should comfortably fit within the context of a modern small-to-medium LLM (assumption stated below).

Assumption about context length

- I assume a GPT-5 mini-like context window of roughly 64k tokens (conservative). Batches are organized to keep change sets and follow-up logs manageable in that space.

How to use this file

- Work through the batches in order (Batch 1 -> Batch 2 -> ...). Confirm each batch by running the verification steps and pasting the most relevant logs into the assistant for the next batch.
- Keep diffs small in any batch (1-5 files, small config changes) so the assistant can reason about state within the assumed context window.

Checklist (high-level)

- [ ] Batch 1: Logging, directories & permissions, and disk space checks (low risk)
- [ ] Batch 2: DJL cache and native extraction (caching, mount, offline startup)
- [ ] Batch 3: Model URIs and permissive criteria (face, pose, action) — ensure models available and correct Criteria
- [ ] Batch 4: Image size config reconciliation and client/server protocol alignment
- [ ] Batch 5: Performance tuning (DJL thread counts, concurrency) and GPU readiness
- [ ] Batch 6: Optional dependencies (OCR fallback/Tess4J) and deprecations (logback rolling policy)
- [ ] Batch 7: Tests, docs, and CI additions

Batch 1 — Logging, directories & permissions, and disk space (safe, short)

- Goal: ensure the `logs/` directory exists, has correct permissions, and logback config warnings are reduced.
- Files to check/change:
    - `core/src/main/resources/logback-spring.xml` (or `src/main/resources/logback-spring.xml` under `mcp` module if present) — search and confirm the rolling appender patterns and `SizeAndTimeBasedFNATP` usage.
    - Dockerfile(s): `mcp/Dockerfile` — ensure `logs/` directory is created and owned by the runtime user.
- Changes to make in this batch:
    1. Create `logs/` directory at image build or container start, set ownership to the app user (e.g., `djl`), and ensure it's writable by the process.
    2. In `logback-spring.xml`, replace deprecated `SizeAndTimeBasedFNATP` with `SizeAndTimeBasedRollingPolicy` (minimal config change).
    3. Validate `logback-spring.xml` is packaged in the jar (no other `logback.xml` conflicts) — keep single source of truth.
- Commands to run (local dev / container build & run):

```bash
# Build the mcp image (if using provided wrapper)
cd /home/codesapienbe/Projects/spring-vision/mcp
./mvnw -DskipTests package
# or if a Dockerfile is used
docker build -t spring-vision-mcp:local .
# Run with mounted logs dir
docker run --rm -v $PWD/logs:/app/logs:rw --name spring-vision-mcp-local spring-vision-mcp:local
```

- Verification:
    - Confirm startup logs show `Active log file name: logs/mcp.json.log` and no permission errors.
    - Confirm deprecation warnings about `SizeAndTimeBasedFNATP` are gone.
    - Run `df -h` inside container to ensure sufficient disk space for log caps.
- Estimated effort: 0.5–1 hour

Batch 2 — DJL cache and native extraction (ensure deterministic startup, offline-friendly)

- Goal: avoid long downloads/extraction at startup, and ensure cache dir is writable.
- Files to check/change:
    - Dockerfile: ensure `/opt/djl-cache` is created and owned by runtime user and optionally pre-populate cache.
    - Deployment manifest / docker-compose.yml: mount a host path to `/opt/djl-cache` to persist caches across restarts.
- Changes to make:
    1. Create `/opt/djl-cache` during image build or via `docker run -v` and set ownership.
    2. Optionally download required native libraries and model files during image build (if licenses allow) or add a startup script that waits for network and prefetches artifacts.
    3. Ensure `JAVA_TOOL_OPTIONS` or app config points `-Dai.djl.repository.cache.dir=/opt/djl-cache` (already present in logs). Confirm writable.
- Commands to run:

```bash
# Example: build image with djl-cache dir
cd mcp
# edit Dockerfile to include: RUN mkdir -p /opt/djl-cache && chown djl:djl /opt/djl-cache
./mvnw -DskipTests package && docker build -t spring-vision-mcp:local .
# Run with explicit mount
docker run --rm -v /var/lib/spring-vision-djl-cache:/opt/djl-cache:rw -v $PWD/logs:/app/logs:rw spring-vision-mcp:local
```

- Verification:
    - Startup logs should not attempt to download large JNI artifacts if they already exist in the cache.
    - No permission denied stack traces related to cache extraction.
- Estimated effort: 1–2 hours (longer if you prepare an offline cache)

Batch 3 — Models availability, Criteria, and permissive fallback behavior

- Goal: ensure primary models (`retinaface`, `simple_pose`, action recognition model) are reachable and load with expected I/O types; reduce fallback severity.
- Files to check/change:
    - `core/src/main/resources/application.yml` or module `mcp` config files where `djl` model URIs or names are configured (search for `retinaface`, `simple_pose`, `resnet18` etc.).
    - Code using DJL Criteria filters (likely `DjlVisionBackend` or similar) — confirm the Criteria accept permissive matches or explicit model URIs.
- Changes to make:
    1. Confirm model identities and URIs: either point to DJL model zoo or to packaged/custom model directories.
    2. If model I/O mismatch is the cause, adjust Criteria to accept different Input/Output types or convert models to expected signatures.
    3. Add health-check endpoints or startup checks that log missing models clearly and exit with non-zero if critical models are absent (optional but recommended for CI/CD).
- Commands to run:

```bash
# Search for model config
grep -R "retinaface\|simple_pose\|resnet18" -n
# Build and run with verbose DJL logging enabled
docker run --rm -e AI_DJL_LOG_LEVEL=DEBUG -v /var/lib/spring-vision-djl-cache:/opt/djl-cache:rw spring-vision-mcp:local
```

- Verification:
    - Startup logs should show model loads for primary models (no fallback warnings) or explicit, clear guidance in logs why a model cannot be used.
    - Unit tests (if present) that exercise model-loading Criteria should pass.
- Estimated effort: 2–6 hours (depends on whether models are remote, need conversion, or custom packaging)

Batch 4 — Image size config reconciliation & protocol version alignment

- Goal: make sure `VisionTool` and backend agree on `max_image_size_bytes` and resolve MCP protocol version mismatch between client and server.
- Files to check/change:
    - Configuration files: `application.yml`, `mcp` module properties, `VisionTool` config locations.
    - Client(s) that talk to MCP (if under your control) - update to supported protocol version or update server's supported versions.
- Changes to make:
    1. Pick one authoritative `max_image_size_bytes` (e.g., 50 MB) and update `VisionTool` or property sources so both server and tools read from the same value.
    2. Add clear validation with helpful error messages when clients submit oversized images.
    3. For protocol mismatch: update client version if possible, or add compatibility layer in server that supports the newer client protocol or responds with a clearer message.
- Commands to run:

```bash
# Grep config
grep -R "max_image_size_bytes\|max-image-size" -n
# Run client integration tests that send images at boundary sizes (10MB, 50MB)
```

- Verification:
    - Upload a 10 MB and 50 MB image, confirm the server accepts/rejects according to the configured threshold and returns intelligible error messages.
    - Verify MCP client initialize logs show the same protocol version.
- Estimated effort: 1–3 hours

Batch 5 — Performance tuning and GPU readiness

- Goal: tune DJL thread counts and concurrency to fit container CPU limits and make GPU support optional but detectable.
- Files to check/change:
    - DJL engine config and `DjlVisionBackend` code where `inter-op` / `intra-op` threads and `maxConcurrentInferences` are set.
    - Dockerfile: produce GPU-enabled image variant (CUDA-enabled) and add docs for running with NVIDIA runtime.
- Changes to make:
    1. Make thread counts/configs configurable via environment variables (e.g., `DJL_INTER_OP`, `DJL_INTRA_OP`, `MAX_CONCURRENT_INFERENCES`) and default to conservative values when CGroup limits are low.
    2. Add a `Dockerfile.gpu` (or a build arg) that uses DJL CUDA natives and document how to run with the NVIDIA container toolkit.
- Verification:
    - Run with CPU-limited container (e.g., `--cpus=2`) and ensure thread counts are adjusted.
    - On a GPU host, run GPU variant and confirm `GPU Available: true` in logs.
- Estimated effort: 2–4 hours

Batch 6 — Optional dependencies and deprecations

- Goal: add optional OCR fallback (Tess4J) as a documented opt-in and remove deprecated rolling policy usage.
- Files to check/change:
    - `pom.xml` (mcp module) to add Tess4J as optional dependency (scope optional or profile-based)
    - `logback-spring.xml` — complete deprecation fix (if not done in Batch 1)
- Changes to make:
    1. Add Tess4J to `pom.xml` under a profile or with `optional=true` and document enabling steps in `docs/`.
    2. Replace deprecated rolling policy usage (if any remained).
- Verification:
    - When Tess4J dependency is present, try OCR path and ensure it triggers fallback correctly.
- Estimated effort: 1–2 hours

Batch 7 — Tests, docs, CI

- Goal: add small unit/integration tests and document required steps for offline builds and GPU usage.
- Files to add/change:
    - `mcp/src/test/...` add model-loading unit tests (mock DJL model zoo or use small test models in test resources)
    - `docs/` add sections for `DJL cache`, `GPU setup`, `Model packaging` and `MCP protocol compatibility`
    - CI configuration: ensure caches or mount points are set for DJL cache in CI runners (if using GitHub Actions or similar)
- Verification:
    - Run `./mvnw -T1C -DskipTests=false test` and ensure tests pass.
- Estimated effort: 2–6 hours depending on test complexity

Helper notes / commands you will often need

- Grep project-wide for model names and config keys:

```bash
cd /home/codesapienbe/Projects/spring-vision
grep -R "retinaface\|simple_pose\|resnet18\|max_image_size_bytes\|djl.repository.cache.dir" -n
```

- Check logs quickly (tail):

```bash
# If running container with logs mounted
tail -n 200 mcp/logs/mcp.json.log
```

Next steps

- Execute Batch 1 changes (create logs dir and update `logback-spring.xml`), run the container, and paste the resulting startup logs here.

---

If you want, I can also:

- Prepare exact patch snippets for `mcp/Dockerfile` and `mcp/src/main/resources/logback-spring.xml` for Batch 1, and apply them directly.
- Or I can walk through any single batch and make the edits and verify them in the repo.

Generated on 2025-10-17 (UTC).

