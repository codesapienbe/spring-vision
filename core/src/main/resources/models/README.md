# OpenCV Models for Spring Vision

This directory contains pre-trained models for face detection and recognition.

## 🚀 Quick Start

**Important:** Large model files are NOT stored in Git. Download them using:

```bash
cd scripts
./download-models-quick.sh
```

This will download the essential models (~42MB total):

- YuNet face detector (228KB)
- SFace recognition model (37MB)
- DNN-SSD face detector (5.2MB)

## Model Files

### ✅ Included in Repository (Small Files)

These configuration files are committed to Git:

- `haarcascade_frontalface_default.xml` - Haar cascade for frontal faces
- `haarcascade_eye.xml` - Eye detection for face validation
- `haarcascade_profileface.xml` - Profile face detection
- `lbpcascade_frontalface.xml` - LBP cascade (better for diverse skin tones)
- `deploy.prototxt` - DNN-SSD configuration

### 📥 Downloaded via Script (Large Files)

These binary model files must be downloaded:

- `face_detection_yunet_2023mar.onnx` - YuNet face detector (228KB)
- `face_recognition_sface_2021dec.onnx` - Face recognition/embeddings (37MB)
- `res10_300x300_ssd_iter_140000_fp16.caffemodel` - DNN-SSD face detector (5.2MB)

## Why Not Commit Models to Git?

Large binary files (especially ONNX and Caffe models) cause problems:

- Push/pull operations become very slow
- GitHub has file size limits (100MB max, warnings at 50MB)
- Repository size grows unnecessarily
- Harder to update models independently

Instead, we use download scripts that fetch models on-demand.

## Model Loading Strategy

Spring Vision uses a multi-detector fusion approach with the following priority:

1. **YuNet (Primary)** - Modern ONNX-based detector, highest accuracy
2. **DNN-SSD (Secondary)** - ResNet-based detector, provides additional coverage
3. **Haar Cascades (Fallback)** - Classic OpenCV detectors, always available

## Advanced: Download All Models

For additional models and backends, see:

```bash
./scripts/download-detector-models.sh  # All detector models
./scripts/download-models.sh           # Complete model set
```

## Model Fallback Behavior

If optional models are missing, the system will:

1. **No YuNet**: Use DNN-SSD + Haar cascades
2. **No DNN-SSD**: Use YuNet + Haar cascades (recommended default)
3. **No SFace**: Face recognition features disabled, detection still works
4. **Only Haar cascades**: Basic detection works, but lower accuracy
