# OpenCV Models for Spring Vision

This directory contains pre-trained models for face detection and recognition.

## Model Loading Strategy

Spring Vision uses a multi-detector fusion approach with the following priority:

1. **YuNet (Primary)** - Modern ONNX-based detector, highest accuracy
2. **Haar Cascades (Fallback)** - Classic OpenCV detectors, always available
3. **DNN-SSD (Optional)** - ResNet-based detector, provides additional coverage

## Required Models

These models are essential and should always be present:

- `haarcascade_frontalface_default.xml` - Haar cascade for frontal faces
- `haarcascade_eye.xml` - Eye detection for face validation
- `haarcascade_profileface.xml` - Profile face detection
- `lbpcascade_frontalface.xml` - LBP cascade (better for diverse skin tones)
- `face_detection_yunet_2023mar.onnx` - YuNet face detector (recommended)

## Optional Models

These models provide enhanced functionality but are not required:

- `res10_300x300_ssd_iter_140000_fp16.caffemodel` - DNN-SSD face detector (~5MB)
- `deploy.prototxt` - DNN-SSD configuration
- `face_recognition_sface_2021dec.onnx` - Face recognition/embeddings (~37MB)

## Downloading Models

Run the download script to fetch all models:

```bash
./scripts/download-models.sh
```

The script will:

1. Download all required models automatically
2. Prompt for optional models (DNN-SSD caffemodel, quantized SFace)
3. Report download status and total size

## Model Fallback Behavior

If optional models are missing, the system will:

1. **No DNN-SSD caffemodel**: Use YuNet + Haar cascades (recommended default)
2. **No YuNet**: Use DNN-SSD + Haar cascades
3. **No SFace**: Face recognition features disabled, detection still works
4. **Only Haar cascades**: Basic detection works but with lower accuracy

## Current Status

The system is designed to work optimally with:

- **YuNet + Haar Cascades** (default configuration)
- Total size: ~10MB (without DNN-SSD caffemodel)

Adding the DNN-SSD caffemodel increases size by ~5MB but provides minimal benefit over YuNet.

## Troubleshooting

### Models not loading from classpath

If you see warnings about models not loading:

1. Verify models exist in this directory
2. Run `mvn clean install` to rebuild the JAR with models included
3. Check logs for "Model loaded from classpath" messages

### Native library issues

In headless environments, you may see warnings about `jniopencv_highgui` - this is expected and normal. The system will skip GUI-dependent features and work correctly.

### YuNet initialization failures

If YuNet fails with `opencv_highgui` errors, the system will fall back to Haar cascades automatically.

## Model Sources

- **Haar/LBP Cascades**: https://github.com/opencv/opencv
- **YuNet**: https://github.com/opencv/opencv_zoo
- **DNN-SSD**: https://github.com/opencv/opencv_3rdparty
- **SFace**: https://huggingface.co/opencv/face_recognition_sface

