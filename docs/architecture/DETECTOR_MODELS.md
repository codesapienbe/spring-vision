# Face Detection Models

Spring Vision uses RetinaFace for high-accuracy face detection, bundled directly in the JAR.

## Current Face Detection

### RetinaFace (Production Ready - Bundled in JAR)

**Model:** `retinaface.pt` (110MB PyTorch model)
**Location:** `classpath:/models/retinaface/retinaface.pt`

**Features:**
- ✅ **Bundled in JAR** - No downloads required
- ✅ **High accuracy** - State-of-the-art face detection
- ✅ **5-point landmarks** - Facial feature detection
- ✅ **Multi-scale** - Works with various face sizes
- ✅ **Robust** - Handles occlusions and angles
- ✅ **Production ready** - Optimized for performance

**Configuration:**
```yaml
spring:
  vision:
    djl:
      face-detection:
        confidence-threshold: 0.7
        max-faces: 200
```

**Automatic Fallback:**
If RetinaFace model is unavailable, Spring Vision falls back to:
1. Generic object detection (person/face class)
2. OpenCV Haar cascades (if available)

## Legacy Face Detection (Deprecated)

For historical reference, these were the previous face detection options:
**Models:**
- `haarcascade_frontalface_default.xml` - Haar cascade for frontal faces
- `lbpcascade_frontalface.xml` - LBP cascade for frontal faces  
- `haarcascade_profileface.xml` - Haar cascade for profile faces
- `haarcascade_eye.xml` - Haar cascade for eye detection
- `deploy.prototxt` - DNN model configuration
- `res10_300x300_ssd_iter_140000_fp16.caffemodel` - DNN SSD face detector

**Configuration:**
```yaml
spring.vision.facebytes.detector-backend: opencv
```

**Characteristics:**
- ✅ Fast (CPU-optimized)
- ✅ No external dependencies
- ⚠️ Moderate accuracy
- ⚠️ May have false positives on rectangular patterns
- ✅ Works in all environments

---

### 2. RetinaFace/YuNet (Recommended for Accuracy)
**Models:**
- `face_detection_yunet_2023mar.onnx` - State-of-the-art face detector from OpenCV Zoo

**Configuration:**
```yaml
spring.vision.facebytes.detector-backend: retinaface
```

**Environment Variables:**
```bash
export FACEBYTES_RETINAFACE_ONNX_PATH=/path/to/face_detection_yunet_2023mar.onnx
export FACEBYTES_RETINAFACE_SCORE_THR=0.7  # Detection threshold
export FACEBYTES_RETINAFACE_NMS_THR=0.4    # Non-maximum suppression threshold
export FACEBYTES_RETINAFACE_SIZE=640       # Input size (640x640)
```

**Characteristics:**
- ✅ Excellent accuracy (state-of-the-art)
- ✅ Works well with occluded faces
- ✅ Provides 5-point facial landmarks
- ✅ Handles multiple scales
- ⚠️ Requires ONNX Runtime
- ⚠️ Slightly slower than OpenCV
- ✅ Best for production use

**Download:**
```bash
wget https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx \
  -O core/src/main/resources/models/face_detection_yunet_2023mar.onnx
```

---

### 3. MTCNN (Multi-Task Cascaded CNN)
**Models (3-stage pipeline):**
- `mtcnn/pnet.onnx` - Proposal Network (Stage 1: candidate generation)
- `mtcnn/rnet.onnx` - Refinement Network (Stage 2: refinement)
- `mtcnn/onet.onnx` - Output Network (Stage 3: final detection + landmarks)

**Configuration:**
```yaml
spring.vision.facebytes.detector-backend: mtcnn
```

**Characteristics:**
- ✅ Very good accuracy
- ✅ Provides 5-point facial landmarks
- ✅ Good for small faces
- ✅ Robust to various poses
- ⚠️ Requires ONNX Runtime
- ⚠️ Slower (3-stage cascade)
- ⚠️ More memory intensive
- ✅ Good for challenging conditions

**Download:**
The MTCNN models need to be obtained from:
1. **Official MTCNN ONNX exports** (recommended)
2. **Convert from PyTorch/TensorFlow:**
   ```bash
   # Example: Converting from PyTorch
   # You'll need the original MTCNN implementation
   git clone https://github.com/TropComplique/mtcnn-pytorch
   # Follow their conversion instructions to ONNX
   ```

3. **Pre-converted models:**
   ```bash
   # Example sources (verify before use)
   # https://github.com/onnx/models (if available)
   # https://huggingface.co/onnx/models
   ```

**Note:** MTCNN ONNX models may need manual conversion from PyTorch/TensorFlow implementations.

---

### 4. DLIB (HOG-based Detector)
**Models:**
- **No separate model file needed!**
- Uses built-in `org.bytedeco.opencv.opencv_objdetect.HOGDescriptor`

**Configuration:**
```yaml
spring.vision.facebytes.detector-backend: dlib
```

**Characteristics:**
- ✅ Good accuracy
- ✅ No model download needed
- ✅ Built into OpenCV/JavaCV
- ✅ Robust to lighting variations
- ⚠️ Slower than Haar cascades
- ⚠️ Requires JavaCV/OpenCV native libraries
- ✅ Works well for frontal faces

---

## Quick Setup

### Download All Models
```bash
cd scripts
./download-detector-models.sh
```

### Manual Download

#### RetinaFace/YuNet (Recommended)
```bash
cd core/src/main/resources/models
wget https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx
```

#### MTCNN Models
```bash
cd core/src/main/resources/models
mkdir -p mtcnn

# PNet
wget <PNET_URL> -O mtcnn/pnet.onnx

# RNet  
wget <RNET_URL> -O mtcnn/rnet.onnx

# ONet
wget <ONET_URL> -O mtcnn/onet.onnx
```

---

## Configuration Examples

### Using RetinaFace (Best Accuracy)
```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: retinaface
```

### Using MTCNN (Best for Small Faces)
```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: mtcnn
```

### Using DLIB (No Downloads Required)
```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: dlib
```

### Using OpenCV (Fastest)
```yaml
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: opencv
```

---

## Performance Comparison

| Detector   | Speed | Accuracy | Landmarks | Model Size | Dependencies      |
|------------|-------|----------|-----------|------------|-------------------|
| OpenCV     | ⚡⚡⚡  | ⭐⭐     | ❌        | ~6 MB      | None              |
| RetinaFace | ⚡⚡   | ⭐⭐⭐⭐  | 5-point   | ~2 MB      | ONNX Runtime      |
| MTCNN      | ⚡    | ⭐⭐⭐⭐  | 5-point   | ~5 MB      | ONNX Runtime      |
| DLIB       | ⚡⚡   | ⭐⭐⭐    | ❌        | Built-in   | JavaCV/OpenCV     |

---

## Automatic Fallback

All detectors automatically fall back to OpenCV if:
- Model files are missing
- ONNX Runtime is not available
- Model initialization fails

This ensures the application always works, even without optimal models.

---

## Troubleshooting

### ONNX Runtime Not Found
```bash
# Add ONNX Runtime dependency to pom.xml (should already be included)
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.15.1</version>
</dependency>
```

### Model Not Loading
Check logs for:
```
RetinaFaceDetector: onnx.missing
MtcnnDetector: MTCNN models not available
```

Set explicit model paths:
```bash
export FACEBYTES_RETINAFACE_ONNX_PATH=/absolute/path/to/model.onnx
```

### Memory Issues with MTCNN
Reduce input image size or use a different detector:
```yaml
spring.vision.facebytes.detector-backend: retinaface  # More memory efficient
```

---

## Additional Resources

- **OpenCV Zoo:** https://github.com/opencv/opencv_zoo
- **ONNX Model Zoo:** https://github.com/onnx/models
- **DeepFace Models:** https://github.com/serengil/deepface_models
- **MTCNN PyTorch:** https://github.com/TropComplique/mtcnn-pytorch
- **RetinaFace Paper:** https://arxiv.org/abs/1905.00641

---

## Model Licenses

- **YuNet/OpenCV Zoo:** Apache License 2.0
- **OpenCV Cascades:** BSD 3-Clause
- **MTCNN:** MIT License (original implementation)
- **DLIB/HOG:** Boost Software License

Always verify licensing before use in production.

