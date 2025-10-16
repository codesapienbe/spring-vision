# Model Download Scripts

This directory contains scripts to download face detection and recognition models for Spring Vision's FaceBytes backend.

## Quick Start (Recommended)

For most users, the quick download script is sufficient:

```bash
cd scripts
./download-models-quick.sh
```

**This downloads:**
- ✅ YuNet face detector (RetinaFace - best accuracy)
- ✅ SFace face recognition model
- ✅ ArcFace model (optional, high accuracy embeddings)

**Already included (no download needed):**
- OpenCV Haar cascades
- OpenCV DNN SSD detector  
- DLIB HOG detector (built-in)

---

## Full Download (All Backends)

For advanced users who want all detector options:

```bash
cd scripts
./download-detector-models.sh
```

**This attempts to download:**
- All Quick Start models (above)
- MTCNN models (pnet, rnet, onet)
- Additional recognition models
- RetinaFace variants

**Note:** MTCNN models may require manual conversion from PyTorch/TensorFlow.

---

## Detector Backend Comparison

| Backend    | Model Download | Status              | Use Case                    |
|------------|----------------|---------------------|-----------------------------|
| **RetinaFace** | ✅ Auto      | **Ready**           | **Best accuracy**           |
| **OpenCV**     | ✅ Included  | **Ready**           | **Fastest, always works**   |
| **DLIB**       | ✅ Built-in  | **Ready**           | Good balance                |
| **MTCNN**      | ⚠️ Manual    | Needs conversion    | Small faces, high accuracy  |

---

## Configuration

### After Running Quick Download

```yaml
# application.yml or application-facebytes.properties
spring:
  vision:
    opencv:
      enabled: true
    facebytes:
      enabled: true
      detector-backend: retinaface  # Recommended
```

### Alternative Configurations

```yaml
# Use built-in OpenCV (no models needed, always works)
spring.vision.facebytes.detector-backend: opencv

# Use DLIB HOG (built-in, no models needed)
spring.vision.facebytes.detector-backend: dlib

# Use MTCNN (requires manual model download)
spring.vision.facebytes.detector-backend: mtcnn
```

---

## Manual Model Downloads

### RetinaFace/YuNet (Recommended)

```bash
cd core/src/main/resources/models
wget https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx
```

### SFace Recognition

```bash
cd core/src/main/resources/models
wget https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx
```

### ArcFace (High Accuracy Embeddings)

```bash
cd core/src/main/resources/models
wget https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/ArcFace.onnx -O arcface.onnx
```

### VGGFace

```bash
cd core/src/main/resources/models
wget https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/VGGFace.onnx -O vggface.onnx
```

### Facenet128

```bash
cd core/src/main/resources/models
wget https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/Facenet128.onnx -O facenet128.onnx
```

---

## MTCNN Models (Advanced)

MTCNN requires manual conversion since ONNX versions aren't publicly distributed.

### Option 1: Use RetinaFace Instead (Recommended)

RetinaFace/YuNet is easier to set up and has better accuracy.

### Option 2: Convert from PyTorch

```bash
# Clone MTCNN PyTorch implementation
git clone https://github.com/TropComplique/mtcnn-pytorch
cd mtcnn-pytorch

# Install dependencies
pip install torch onnx

# Convert to ONNX (example - actual code depends on implementation)
python convert_to_onnx.py

# Copy to resources
cp pnet.onnx core/src/main/resources/models/mtcnn/
cp rnet.onnx core/src/main/resources/models/mtcnn/
cp onet.onnx core/src/main/resources/models/mtcnn/
```

### Option 3: Use Existing PyTorch Models

Some MTCNN implementations include pre-trained weights:
- https://github.com/timesler/facenet-pytorch
- https://github.com/deepinsight/insightface

---

## Verifying Models

After downloading, verify models are in place:

```bash
ls -lh core/src/main/resources/models/

# You should see:
# face_detection_yunet_2023mar.onnx       (~2 MB)
# face_recognition_sface_2021dec.onnx    (~11 MB)
# arcface.onnx                           (~132 MB - optional)
# haarcascade_frontalface_default.xml    (~1 MB - included)
# ... other models
```

---

## Troubleshooting

### Download Fails

```bash
# Check internet connection
ping github.com

# Try manual download with curl
curl -L -o model.onnx "https://..."

# Check disk space
df -h
```

### Model Not Loading

Check logs:
```
tail -f logs/spring-vision.log | grep -i "detector\|model\|onnx"
```

Common issues:
- Missing ONNX Runtime dependency (should be in pom.xml)
- Incorrect file path in configuration
- Corrupted download (re-download)

### ONNX Runtime Issues

```bash
# Verify ONNX Runtime is available
mvn dependency:tree | grep onnx

# Should show:
# com.microsoft.onnxruntime:onnxruntime:jar:1.15.1
```

---

## Storage Requirements

| Component            | Size       | Required? |
|---------------------|------------|-----------|
| OpenCV cascades     | ~6 MB      | ✅ Yes    |
| YuNet detector      | ~2 MB      | ⭐ Recommended |
| SFace recognition   | ~11 MB     | ⭐ Recommended |
| ArcFace recognition | ~132 MB    | Optional  |
| VGGFace recognition | ~574 MB    | Optional  |
| MTCNN (all 3)       | ~5 MB      | Optional  |
| **Total Recommended** | **~20 MB** | -         |

---

## Model Sources

All models are downloaded from official sources:
- **OpenCV Zoo:** https://github.com/opencv/opencv_zoo (Apache 2.0)
- **DeepFace Models:** https://github.com/serengil/deepface_models (Various)
- **OpenCV Cascades:** Included with OpenCV (BSD)

---

## License Compliance

Models have different licenses:
- **YuNet/SFace:** Apache License 2.0 ✅
- **OpenCV Cascades:** BSD 3-Clause ✅
- **ArcFace:** Research-only (check before commercial use) ⚠️
- **VGGFace:** Research-only (check before commercial use) ⚠️

**For commercial use:** Stick with YuNet/SFace (Apache 2.0)

---

## Getting Help

- Documentation: `/docs/models.md`
- Detector comparison: `/docs/DETECTOR_MODELS.md`
- Issues: https://github.com/codesapienbe/spring-vision/issues

---

## Next Steps

After downloading models:

1. **Configure backend:**
   ```yaml
   spring.vision.facebytes.detector-backend: retinaface
   ```

2. **Test detection:**
   ```bash
   cd mcp
   mvn spring-boot:run
   # Call extractEmbeddings or countFaces tools
   ```

3. **Monitor performance:**
   ```bash
   tail -f logs/spring-vision.log
   ```

4. **Optimize if needed:**
   - Adjust score thresholds
   - Try different detector backends
   - Enable/disable preprocessing

