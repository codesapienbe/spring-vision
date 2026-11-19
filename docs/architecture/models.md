[Docs Home](./index.md) · [Getting Started](../getting-started/quick-start.md) · [Modules](./modules.md) · [Maven Model Download](./downloads.md)

# Models Guide

This guide describes how Spring Vision handles model files today: where they live, how they are loaded, and how you can customize or download them.

## Model Locations

`ModelStore` resolves each model file in this order:

1) **Classpath** (`classpath:/models`): the library and Docker builds bundle the models with the
   `download-models` profile, and then nothing is ever downloaded at runtime.
2) **Model directory** (`spring.vision.djl.models.directory`, default `~/.springvision/models`):
   models downloaded earlier.
3) **Runtime download**, only when `spring.vision.djl.models.download=true` (the MCP server's
   `stdio` profile turns it on). Missing models are fetched in the background, smallest first,
   from the same pinned URLs as the Maven profile (`ModelCatalog`), checked against their SHA-256,
   and moved into place only once complete. An interrupted download resumes; a failed one is
   retried after 30 s, 2 min, then every 10 min.

While a model is still downloading, an operation that needs it fails with
`ModelNotReadyException` ("still setting up …"); it never runs on a missing model. Over MCP the
tool answers with an error naming the model and its progress, and `setup_status` lists every
model's state.

With `spring.vision.djl.models.lazy=true` (also on in the `stdio` profile) each model is loaded on
its first use instead of at start-up, so the server answers its client within a few seconds.

## Bundled Models (Included in JAR)

### ✅ **Core Production Models**
- **Face detection**: OpenCV Haar cascades (frontal + profile), shipped inside the OpenCV natives - no model file
- **FaceFeatureNet**, **FER+ emotion**, **age/gender**, **NSFW**, **action recognition**, **palm detection**, **deepfake detection** (see the `download-models` profile in `core/pom.xml`)

Every bundled model must carry a licence that allows commercial use. Ultralytics YOLO weights
(AGPL-3.0, including models fine-tuned with Ultralytics) are not bundled; object detection,
pose estimation and vehicle damage detection fail with `VisionUnsupportedException` until a
licence-clean model replaces them.

### 📚 **Runtime Download Models**
Advanced AI models downloaded on-demand:
- OCR models (text recognition)
- Image classification models (ResNet, Inception)
- Segmentation models (instance/semantic)
- Action recognition models
- Face embedding models

## Configuration

Spring Vision uses DJL for unified model management:

```yaml
spring:
  vision:
    djl:
      enabled: true                    # Enable DJL backend
      engine: pytorch                  # pytorch, tensorflow, onnx
      device: cpu                      # cpu or gpu
      max-concurrent-inferences: 4     # Concurrency limit
      show-progress: false             # Model loading progress

      # Model-specific settings
      object-detection:
        confidence-threshold: 0.5

      pose-estimation:
        confidence-threshold: 0.5

      face-detection:
        confidence-threshold: 0.7
        max-faces: 200
```

### Custom Model Paths

For custom models, override the default paths:

```yaml
spring:
  vision:
    djl:
      # Custom model directory (external to JAR)
      model-path: /opt/spring-vision/models

      # Or use environment variable
      # model-path: ${SPRING_VISION_MODELS:/opt/spring-vision/models}
```

**Tips:**
- **Classpath models** (bundled): Maximum portability for Docker/CI/CD
- **External paths**: For large custom models or model updates without rebuilding
- **Runtime downloads**: For advanced models not included in JAR

## Build-Time Model Download

Core models are downloaded and bundled during the Maven build:

```bash
# Download and bundle all models (run during CI/CD)
mvn clean install -Pdownload-models

# Or with Makefile
make install
```

This includes:
- The other bundled models listed above

## Adding Custom Models

Override bundled models with custom ones:

```yaml
spring:
  vision:
    djl:
      model-path: /path/to/custom/models
```

Place your models in the expected directory structure:
```
/path/to/custom/models/
└── face_feature/face_feature.pt # Custom face embeddings
```

## Docker and Cloud Deployment

### Self-Contained JARs
```dockerfile
FROM eclipse-temurin:25-jre
COPY target/spring-vision-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- No model volumes needed
- All core models bundled in JAR
- Maximum portability

### External Models
```dockerfile
FROM eclipse-temurin:25-jre
VOLUME /opt/models
ENV SPRING_VISION_MODELS=/opt/models
COPY target/spring-vision-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- Mount model volumes for updates without rebuilding
- Useful for large custom models

## GPU Acceleration

Enable GPU support for faster inference:

```yaml
spring:
  vision:
    djl:
      device: gpu
      engine: pytorch
```

**Requirements:**
- NVIDIA CUDA drivers installed
- CUDA-compatible GPU
- DJL CUDA runtime dependencies

See: [GPU Configuration Guide](../configuration/gpu.md)

---

See also: [Configuration](../configuration/config.md) · [GPU Configuration](../configuration/gpu.md) · [Runtime](../configuration/runtime.md) · [Architecture](./architecture.md)

