# OpenCV Models

This directory contains pre-bundled OpenCV models for face detection and recognition.

## Models Included

### Haar Cascade Models (Face Detection)

- `haarcascade_frontalface_default.xml` (909 KB) - Default frontal face detector
- `haarcascade_eye.xml` (334 KB) - Eye detector for validation
- `haarcascade_profileface.xml` (810 KB) - Profile face detector
- `lbpcascade_frontalface.xml` (51 KB) - LBP-based frontal face detector (better for diverse skin tones)

### DNN Models (Caffe - ResNet-SSD)

- `deploy.prototxt` (28 KB) - Network architecture definition
- `res10_300x300_ssd_iter_140000_fp16.caffemodel` (288 KB) - Trained model weights (FP16)

### ONNX Models (OpenCV Zoo)

- `face_detection_yunet_2023mar.onnx` (228 KB) - YuNet face detector
- `face_recognition_sface_2021dec.onnx` (37 MB) - SFace face recognition model

## Source

These models are automatically downloaded from:

- OpenCV GitHub repository (haarcascades, lbpcascades)
- OpenCV 3rd party repository (DNN models)
- OpenCV Zoo (ONNX models)

## License

These models are distributed under the same license as OpenCV (Apache 2.0).

