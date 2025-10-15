# FaceBytes Models

This directory contains face detection and recognition models for the FaceBytes module.

## Bundled Models

### Haar Cascade Face Detector

- **File**: `haarcascades/haarcascade_frontalface_default.xml`
- **Size**: ~909 KB
- **Description**: OpenCV Haar Cascade classifier for frontal face detection
- **Status**: ✅ Already bundled

## Additional Models (Auto-downloaded)

The following models are downloaded automatically when needed:

### RetinaFace (Face Detection)

- **File**: `retinaface_r50.onnx`
- **Size**: ~109 MB
- **Description**: RetinaFace detector with ResNet-50 backbone
- **Download URL**: Configured in ModelUrls class
- **Accuracy**: High precision face detection with landmarks

### ArcFace (Face Recognition)

- **File**: `arcface_r100.onnx`
- **Size**: ~250 MB
- **Description**: ArcFace embedding model with ResNet-100 backbone
- **Download URL**: Configured in ModelUrls class
- **Features**: 512-dimensional face embeddings

### SFace (Face Recognition - Lightweight)

- **File**: `sface.onnx`
- **Size**: ~37 MB
- **Description**: Lightweight face recognition model from OpenCV
- **Download URL**: OpenCV Zoo
- **Features**: Fast 128-dimensional embeddings

## Model Loading Priority

The FaceBytes module uses the following priority when loading models:

1. **Classpath resources** (this directory) - highest priority
2. **Configured external path** - if specified in configuration
3. **User cache** (~/.spring-vision/facebytes/models/) - for backwards compatibility
4. **Auto-download** - downloads from configured URLs if not found

## Configuration

### Using Bundled Models (Default)

```yaml
facebytes:
  auto_download: true  # Enable auto-download for ONNX models
```

### Disable Auto-download

```yaml
facebytes:
  auto_download: false  # Disable auto-download, use only bundled/cached models
```

### Custom Model Paths

```yaml
facebytes:
  detector:
    retinaface:
      onnx_path: /path/to/custom/retinaface.onnx
  recognizer:
    arcface:
      onnx_path: /path/to/custom/arcface.onnx
```

## Adding Models Manually

To manually download and bundle models:

```bash
cd facebytes/src/main/resources/models/

# Create directories
mkdir -p detection recognition

# Download models (URLs from ModelUrls.java)
# Note: Check the actual URLs in the source code
wget <MODEL_URL> -O detection/retinaface_r50.onnx
wget <MODEL_URL> -O recognition/arcface_r100.onnx
```

## Model Checksums

The module verifies model integrity using SHA-256 checksums. Checksums are defined in `ModelUrls.checksums()` method.

## License

- **Haar Cascades**: BSD License (OpenCV)
- **RetinaFace**: MIT License
- **ArcFace**: Apache 2.0 License
- **SFace**: Apache 2.0 License (OpenCV)

Please verify licenses before redistribution.

# MediaPipe Models

This directory contains MediaPipe models bundled with the application for offline use.

## Bundled Models

Currently, this directory is prepared to contain the following models:

### Face Detection

- **File**: `face_detection_short_range.tflite`
- **URL**: https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite
- **Size**: ~1 MB
- **Description**: BlazeFace short-range face detector (optimized for faces close to camera)

### Hand Landmarks

- **File**: `hand_landmarker.task`
- **URL**: https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
- **Size**: ~10 MB
- **Description**: Hand landmark detection with 21 keypoints per hand

### Pose Landmarks

- **File**: `pose_landmarker_lite.task`
- **URL**: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task
- **Size**: ~5 MB
- **Description**: Lightweight pose landmark detection with 33 body keypoints

### Object Detection

- **File**: `efficientdet_lite0.tflite`
- **URL**: https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/1/efficientdet_lite0.tflite
- **Size**: ~4 MB
- **Description**: EfficientDet Lite0 for general object detection (COCO classes)

## Downloading Models

To download all models for bundling:

```bash
cd mediapipe/src/main/resources/models/

# Face detection
wget https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite \
  -O face_detection_short_range.tflite

# Hand landmarks
wget https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task

# Pose landmarks
wget https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task

# Object detection
wget https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/1/efficientdet_lite0.tflite
```

## Model Loading Priority

The MediaPipe backend uses the following priority when loading models:

1. **Classpath resources** (this directory) - highest priority
2. **Configured external path** - if specified in configuration
3. **User cache** (~/.spring-vision/models/mediapipe/) - for backwards compatibility
4. **Auto-download** - downloads from Google's CDN if not found

## Usage

Models in this directory are automatically loaded by the MediaPipe backend. No configuration is needed:

```yaml
spring:
  vision:
    mediapipe:
      enabled: true
      # modelPath: classpath:/models (default)
```

## License

MediaPipe models are provided by Google under the Apache 2.0 License.
See: https://github.com/google/mediapipe/blob/master/LICENSE

