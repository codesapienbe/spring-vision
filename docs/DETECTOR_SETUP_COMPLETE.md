# Detector Backend Setup - Complete Guide

## Summary

✅ All detector backend infrastructure is now in place for FaceBytes!

## What's Available

### 🎯 Immediately Ready (No Downloads Needed)

1. **OpenCV** - Built-in, always works
   - Haar Cascade models ✅ (already in resources)
   - DNN SSD detector ✅ (already in resources)
   - LBP cascade ✅ (already in resources)
   - Eye cascade ✅ (already in resources)

2. **DLIB** - Built-in HOG detector
   - No model files needed ✅
   - Uses JavaCV's HOGDescriptor

3. **YuNet/RetinaFace** - State-of-the-art (ONNX)
   - Model ✅ (already in resources: `face_detection_yunet_2023mar.onnx`)
   - SFace embeddings ✅ (already in resources: `face_recognition_sface_2021dec.onnx`)

### ⚠️ Requires Manual Setup

4. **MTCNN** - High-accuracy 3-stage detector
   - Needs: pnet.onnx, rnet.onnx, onet.onnx
   - Directory created: `resources/models/mtcnn/`
   - Must be manually downloaded or converted from PyTorch/TensorFlow

---

## Quick Start (Recommended)

### Option 1: Use What's Already There (Easiest)

```yaml
# application.yml
spring:
  vision:
    opencv:
      enabled: true
    facebytes:
      enabled: true
      detector-backend: retinaface  # Best accuracy, already downloaded
```

**Start and test:**
```bash
cd mcp
mvn spring-boot:run
```

### Option 2: Download Additional Models

```bash
cd scripts
./download-models-quick.sh
```

This ensures all essential models are present (they already are, but this script can update them).

---

## Configuration Examples

### 1. Best Accuracy (RetinaFace)

```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: retinaface
```

### 2. Fastest (OpenCV)

```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: opencv
```

### 3. Good Balance (DLIB)

```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: dlib
```

### 4. MTCNN (After Manual Setup)

```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: mtcnn
```

---

## Files Created

### Scripts

1. **`scripts/download-detector-models.sh`** ⭐
   - Comprehensive downloader for all detector models
   - Includes MTCNN, RetinaFace, recognition models
   - Has fallback URLs and error handling

2. **`scripts/download-models-quick.sh`** ⭐⭐⭐
   - Quick downloader for essential models only
   - Downloads: YuNet, SFace, ArcFace
   - Recommended for most users

3. **`scripts/README-MODELS.md`**
   - Complete guide to model downloading
   - Manual download instructions
   - Troubleshooting tips

### Documentation

1. **`core/src/main/resources/models/DETECTOR_MODELS.md`**
   - Detailed comparison of all detectors
   - Configuration examples
   - Performance characteristics
   - Model sources and licenses

2. **`docs/EMBEDDING_FIX.md`** (Created Earlier)
   - Explains the VisionTemplate multi-backend fix
   - Shows how backends are selected

3. **`docs/DETECTOR_SETUP_COMPLETE.md`** (This File)
   - Complete setup summary
   - Quick reference guide

---

## What's Already in Resources

```
core/src/main/resources/models/
├── deploy.prototxt                           ✅ OpenCV DNN config
├── face_detection_yunet_2023mar.onnx         ✅ YuNet detector
├── face_recognition_sface_2021dec.onnx       ✅ SFace embeddings
├── haarcascade_frontalface_default.xml       ✅ Haar frontal
├── haarcascade_profileface.xml               ✅ Haar profile
├── haarcascade_eye.xml                       ✅ Haar eye
├── lbpcascade_frontalface.xml                ✅ LBP frontal
├── res10_300x300_ssd_iter_140000_fp16.caffemodel ✅ DNN SSD
├── mtcnn/                                    📁 Empty (needs models)
│   ├── pnet.onnx                             ⚠️ Not included
│   ├── rnet.onnx                             ⚠️ Not included
│   └── onet.onnx                             ⚠️ Not included
└── DETECTOR_MODELS.md                        📄 Documentation
```

---

## Backend Selection Priority

When both backends are enabled, VisionTemplate now intelligently selects:

```
1. FaceBytes (if embedding-capable) ← Selected for MCP tools
2. OpenCV (fallback)
```

See: `mcp/src/main/java/.../config/VisionTemplateConfiguration.java`

---

## Testing Your Setup

### 1. Check Available Backends

Start the application and check logs:

```bash
cd mcp
mvn spring-boot:run | grep -i "backend\|detector"
```

You should see:
```
VisionTemplateConfiguration : Found 2 registered backend(s)
VisionTemplateConfiguration :   - opencv: OpenCV Vision Backend (embeddings: false)
VisionTemplateConfiguration :   - facebytes: FaceBytes Backend (embeddings: true)
VisionTemplateConfiguration : Selected embedding-capable backend: facebytes
```

### 2. Test Face Detection

```bash
# From Cursor or your MCP client
{
  "tool": "countFaces",
  "arguments": {
    "imageUrl": "https://example.com/image.jpg"
  }
}
```

### 3. Test Embedding Extraction

```bash
{
  "tool": "extractEmbeddings",
  "arguments": {
    "imageUrl": "https://example.com/image.jpg"
  }
}
```

---

## Performance Benchmarks

| Detector   | Speed | Accuracy | Memory | Use Case                  |
|------------|-------|----------|--------|---------------------------|
| OpenCV     | 🚀🚀🚀 | ⭐⭐    | Low    | Real-time, edge devices   |
| RetinaFace | 🚀🚀  | ⭐⭐⭐⭐ | Medium | Production (best quality) |
| DLIB       | 🚀🚀  | ⭐⭐⭐   | Low    | Good balance              |
| MTCNN      | 🚀    | ⭐⭐⭐⭐ | High   | Small faces, occlusion    |

---

## MTCNN Setup (Optional)

If you need MTCNN:

### Option 1: Try Download Script

```bash
cd scripts
./download-detector-models.sh
```

It will attempt to download from multiple sources, but may fail (MTCNN ONNX models aren't widely distributed).

### Option 2: Convert from PyTorch

```bash
# Clone MTCNN PyTorch
git clone https://github.com/TropComplique/mtcnn-pytorch
cd mtcnn-pytorch

# Install dependencies
pip install torch onnx

# Convert (you'll need to write/find conversion script)
# ... conversion code ...

# Copy models
cp pnet.onnx /path/to/spring-vision/core/src/main/resources/models/mtcnn/
cp rnet.onnx /path/to/spring-vision/core/src/main/resources/models/mtcnn/
cp onet.onnx /path/to/spring-vision/core/src/main/resources/models/mtcnn/
```

### Option 3: Use RetinaFace Instead (Recommended)

RetinaFace/YuNet has similar or better accuracy and is easier to set up:

```yaml
spring.vision.facebytes.detector-backend: retinaface
```

---

## Environment Variables (Advanced)

Fine-tune detector settings:

```bash
# RetinaFace/YuNet
export FACEBYTES_RETINAFACE_ONNX_PATH=/custom/path/yunet.onnx
export FACEBYTES_RETINAFACE_SCORE_THR=0.7
export FACEBYTES_RETINAFACE_NMS_THR=0.4
export FACEBYTES_RETINAFACE_SIZE=640

# Detector selection
export FACEBYTES_DETECTOR=RETINAFACE  # or OPENCV, DLIB, MTCNN

# General settings
export FACEBYTES_ALIGN=true
export FACEBYTES_MARGIN=0
export FACEBYTES_AUTO_DOWNLOAD=true
```

---

## Troubleshooting

### Detector Not Loading

**Check:**
1. Model file exists: `ls -lh core/src/main/resources/models/`
2. ONNX Runtime available: `mvn dependency:tree | grep onnx`
3. Logs: `tail -f logs/spring-vision.log | grep -i detector`

**Common Issues:**
- Missing model file → Run download script
- ONNX Runtime missing → Should be in pom.xml (already included)
- Wrong detector name → Check configuration (opencv, retinaface, dlib, mtcnn)

### MTCNN Not Working

Expected! MTCNN needs manual setup. Use RetinaFace instead:

```yaml
spring.vision.facebytes.detector-backend: retinaface
```

### Low Accuracy

Try different detectors:

```yaml
# From lowest to highest accuracy
detector-backend: opencv     # Fastest, moderate accuracy
detector-backend: dlib       # Good balance
detector-backend: retinaface # Best accuracy (recommended)
detector-backend: mtcnn      # Very high accuracy (if set up)
```

---

## Next Steps

1. ✅ **You're ready to use:**
   - RetinaFace (YuNet) - Best accuracy
   - OpenCV - Fastest
   - DLIB - Good balance

2. **Configure your preferred detector:**
   ```yaml
   spring.vision.facebytes.detector-backend: retinaface
   ```

3. **Test the MCP tools:**
   - `countFaces` - Count faces in images
   - `extractEmbeddings` - Extract face embeddings

4. **Monitor performance:**
   ```bash
   tail -f logs/spring-vision.log
   ```

5. **Optimize if needed:**
   - Adjust thresholds
   - Try different detectors
   - Enable/disable preprocessing

---

## Support

- **Documentation:** `/docs/` directory
- **Model Guide:** `core/src/main/resources/models/DETECTOR_MODELS.md`
- **Download Scripts:** `scripts/` directory
- **Issues:** GitHub Issues

---

## Summary

✅ **3 detectors ready immediately:**
- OpenCV (Haar/DNN)
- DLIB (HOG)
- RetinaFace (YuNet)

✅ **Scripts created for easy setup**
✅ **Comprehensive documentation**
✅ **Automatic fallback to OpenCV**
✅ **Multi-backend support in VisionTemplate**

🎉 **You're all set!** Start using face detection with RetinaFace for best results.

