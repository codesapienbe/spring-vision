[Docs Home](./index.md) · [Getting Started](../getting-started/quick-start.md) · [Config](./config.md) · [Runtime](./runtime.md) · [Deploying for Free](./deploy-free.md)

# GPU Acceleration

This describes the actual mechanism Spring Vision uses to run its DJL-backed models
(YOLO object detection, face detection/recognition, pose estimation, emotion,
demographics, NSFW, vehicle damage, license plate, hand detection, deepfake, and more)
on an NVIDIA GPU instead of CPU, and how to switch between the two.

There are **two independent knobs**, and both need to point the same direction for GPU
acceleration to actually happen:

1. **Build time** — which native libraries get bundled into the jar (`-P gpu` Maven
   profile).
2. **Runtime** — which device DJL is told to use (`spring.vision.djl.device`).

Building with `-P gpu` but leaving `device: cpu` at runtime just leaves the CUDA-capable
binaries unused. Setting `device: gpu` without having built with `-P gpu` fails, since
there's no CUDA-capable native library on the classpath to satisfy the request.

## Build time: one profile switches every native library

`DjlVisionBackend` loads its models through DJL's `Criteria` API, using **two** native
engines depending on the model: `PyTorch` (face recognition, one face-detection path)
and `OnnxRuntime` (everything else — emotion, demographics, NSFW, vehicle damage,
license plate, hand detection, deepfake, pose estimation, most segmentation/
classification paths, and the primary face-detection path). Both engines have separate
CPU and CUDA native artifact families on Maven Central, and both are switched by the
same flag:

```bash
# CPU-only (default)
mvn clean install -Pdownload-models

# GPU (CUDA) — every native library with a CUDA build switches at once
mvn clean install -Pdownload-models -Pgpu

# Or via the Makefile, which wraps the same flag:
make install GPU=true
make bundle GPU=true
```

Mechanically: the root `pom.xml`'s `gpu` profile overrides two properties —
`onnxruntime.artifact` (`onnxruntime` → `onnxruntime_gpu`) and `pytorch.native.artifact`
(`pytorch-native-cpu` → `pytorch-native-cu124`) — and `core/pom.xml`'s dependency
declarations for both libraries reference those properties instead of hardcoding an
artifact name, so one flag controls both. (`core/pom.xml` also excludes the
`onnxruntime-engine` wrapper's own transitive CPU `onnxruntime` dependency, so exactly
one ONNX Runtime native library — CPU or CUDA, never both — ends up on the classpath
either way.)

**Linux/Windows x86_64 only.** Neither DJL nor ONNX Runtime publish macOS or ARM64 CUDA
builds. Combining `-P gpu` with a macOS build or the `linux-aarch64` OS-detection profile
(used for Oracle Cloud's free ARM tier — see [Deploying for Free](./deploy-free.md))
fails Maven dependency resolution loudly, rather than silently falling back to CPU.

**Not covered by this switch:** bytedeco's OpenCV/OpenBLAS native libraries (used for
image I/O and drawing annotations, not model inference) stay CPU-only always — there's
no GPU-accelerated variant wired up, since this app doesn't run OpenCV as an inference
engine. The `ai.djl.tensorflow:tensorflow-engine` dependency also exists in
`core/pom.xml` but isn't actually used by any model-loading code today, so its GPU story
is out of scope.

## Runtime: one property switches every model

`spring.vision.djl.device` (`DjlProperties.device`, default `cpu`) is read once in
`DjlVisionBackend` and passed via DJL's `Criteria.optDevice(...)` to every single
model's loader, regardless of which engine backs it. There's no separate per-model or
per-engine toggle to remember:

```yaml
spring:
  vision:
    djl:
      device: cpu   # default
      # device: gpu           # first CUDA device
      # device: gpu:1         # a specific CUDA device index
```

Or via environment variable:

```bash
export SPRING_VISION_DJL_DEVICE=gpu
```

If `Device.fromName(...)` fails for any reason (e.g. built without `-P gpu`, or no GPU
present), `DjlVisionBackend` catches it and falls back to CPU with a warning in the logs
— it does not crash the application.

## Docker / Docker Compose

The jar itself must already have been built with `-P gpu` — the Dockerfile only copies
a prebuilt jar, it doesn't invoke Maven — so build first, then bring up the GPU-enabled
compose stack:

```bash
make install GPU=true

docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d --build
```

`docker-compose.gpu.yml` is an override that adds an NVIDIA device reservation to the
`mcp` service only — it doesn't change what gets built into the jar. This requires the
**NVIDIA driver** and the **NVIDIA Container Toolkit** on the host (so Docker can pass
the driver through to the container via `--gpus all` / the compose device reservation).
A plain `docker run` equivalent:

```bash
docker run --rm --gpus all -p 8080:8080 -e KEYCLOAK_ISSUER_URI=<issuer> spring-vision-mcp:<version>-gpu
```

This is my understanding of how DJL's `pytorch-native-cu124` and ONNX Runtime's
`onnxruntime_gpu` Java packages are documented to work — they bundle their own CUDA/
cuDNN runtime shared libraries inside the jar, so the container image itself doesn't
need to be an `nvidia/cuda` base image, only the host driver needs to be present. This
hasn't been verified against real GPU hardware from this repo — confirm with `nvidia-smi`
on the host and inside the running container, and watch the `mcp` service's logs for
`UnsatisfiedLinkError` before relying on it.

## Verifying it's actually running on GPU

```bash
# On the host
nvidia-smi

# Application logs on startup (DjlVisionBackend)
# "Initializing DJL Vision Backend - Engine: ..., Device: gpu, Version: ..."

# Inside a running container (if using Docker)
docker exec -it <container> nvidia-smi
```

If GPU acceleration isn't engaging, check in this order: was the jar actually built with
`-P gpu` (`unzip -l app.jar | grep -i onnxruntime_gpu` should show the CUDA jar bundled),
is `spring.vision.djl.device` actually set to `gpu` at runtime, is the NVIDIA driver
visible via `nvidia-smi`, and — for Docker — is the NVIDIA Container Toolkit installed
and `--gpus all` (or the compose device reservation) actually present.

## FAQ

**Can I use both CPU and GPU on the same running instance?** No — `device` is one value
for the whole application; run separate instances if you need both.

**Does this support AMD GPUs?** No, only NVIDIA CUDA — DJL's PyTorch and ONNX Runtime
GPU artifacts are CUDA-only.

**What if I build with `-P gpu` but run on a machine without a GPU?** `DjlVisionBackend`
catches the device-initialization failure and falls back to CPU with a logged warning.

---

See also: [Deploying for Free](./deploy-free.md) (Oracle Cloud's free ARM tier has no
GPU option — this page is for the opposite case, a machine with an NVIDIA GPU) ·
[Runtime](./runtime.md) · [Configuration](./config.md)
