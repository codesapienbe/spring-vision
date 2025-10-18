[Docs Home](./index.md) · [Getting Started](./start.md) · [Models Guide](./models.md) · [Modules](./modules.md)

# Maven Model Download Guide

## Automatic Model Download

Models are now automatically downloaded during the `mvn install` phase if they don't exist yet.

### Build Commands

**Standard Build (downloads models automatically):**

```bash
mvn clean install
```

**Build without model downloads (uses existing models only):**

```bash
mvn clean install -DskipModelDownload=true
```

**Build specific modules with model downloads:**

```bash
# MediaPipe only
mvn clean install -pl mediapipe

# YOLO only
mvn clean install -pl yolo

# Both
mvn clean install -pl mediapipe,yolo
```

## What Gets Downloaded

### MediaPipe Module

- `face_detection_short_range.tflite` (~1 MB)
- `hand_landmarker.task` (~10 MB)
- `pose_landmarker_lite.task` (~5 MB)
- `efficientdet_lite0.tflite` (~4 MB)
  **Total: ~20 MB**

### YOLO Module

- `yolov8n.onnx` (~6 MB) - automatically converted from PyTorch
  **Total: ~6 MB**

## Requirements

### MediaPipe Models

- **wget** (usually pre-installed on Linux/macOS)
- Internet connection

### YOLO Models

- **Python 3.7+**
- **pip** (Python package manager)
- **ultralytics** package (auto-installed by script if missing)
- Internet connection

## Build Process Flow

```
mvn install
  ├─> generate-resources phase
  │   ├─> Execute download-models.sh (if exists)
  │   ├─> Check if models already exist
  │   ├─> Download missing models
  │   └─> Verify downloads
  ├─> compile phase
  ├─> test phase
  └─> package phase
      └─> Models bundled in JAR
```

## Troubleshooting

### Download Fails

If model downloads fail during build:

1. **Check internet connection**
2. **Verify wget is installed**: `which wget`
3. **For YOLO, verify Python**: `python3 --version`
4. **Manual download**: Run the script directly
   ```bash
   cd mediapipe && ./download-models.sh
   cd yolo && ./download-models.sh
   ```
5. **Skip downloads temporarily**: `mvn install -DskipModelDownload=true`

### Build on CI/CD

For CI/CD pipelines, ensure:

- wget is available
- Python 3.7+ is available (for YOLO)
- Sufficient disk space (~30 MB per module)
- Network access to model repositories

**Example GitHub Actions:**

```yaml
- name: Install dependencies
  run: |
    sudo apt-get update
    sudo apt-get install -y wget python3 python3-pip
    pip3 install ultralytics

- name: Build with model downloads
  run: mvn clean install
```

**Example GitLab CI:**

```yaml
build:
  image: maven:3.9-eclipse-temurin-21
  before_script:
    - apt-get update && apt-get install -y wget python3 python3-pip
    - pip3 install ultralytics
  script:
    - mvn clean install
```

## Offline Builds

For offline environments:

1. **Download models once on a connected machine**
   ```bash
   cd mediapipe && ./download-models.sh
   cd yolo && ./download-models.sh
   ```

2. **Commit models to repository** (optional, increases repo size)
   ```bash
   git add mediapipe/src/main/resources/models/*.tflite
   git add mediapipe/src/main/resources/models/*.task
   git add yolo/src/main/resources/models/*.onnx
   ```

3. **Or use local Maven cache**
    - Build artifacts include models once downloaded
    - Subsequent builds use cached JARs

## Model Verification

After build, verify models are bundled:

```bash
# Check JAR contents
jar tf mediapipe/target/mediapipe-1.0.jar | grep models/

# Expected output:
# models/face_detection_short_range.tflite
# models/hand_landmarker.task
# models/pose_landmarker_lite.task
# models/efficientdet_lite0.tflite
```

## Size Considerations

Total size impact:

- **Core module**: ~40 MB (already bundled)
- **MediaPipe module**: +20 MB (auto-downloaded)
- **YOLO module**: +6 MB (auto-downloaded)
- **Total**: ~66 MB for all modules

To reduce size:

- Exclude unused modules from build
- Use `-DskipModelDownload=true` and download only needed models manually
- Consider model compression (future enhancement)

# Model Download Configuration

# Set skipModelDownload=true to skip automatic model downloads during build

# Example: mvn install -DskipModelDownload=true

skipModelDownload=false

# Model download timeout (seconds)

model.download.timeout=300

# Model download retry attempts

model.download.retries=3

---

See also: [Models Guide](./models.md) · [Modules Overview](./modules.md) · [Runtime](./runtime.md) · [GPU Acceleration](./gpu.md)
