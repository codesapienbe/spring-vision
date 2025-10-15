#!/bin/bash
set -e

MODELS_DIR="src/main/resources/models"
mkdir -p "$MODELS_DIR"

echo "Downloading OpenCV models for face detection..."

# Haar Cascade models
echo "Downloading Haar Cascade - Frontal Face..."
curl -L -o "$MODELS_DIR/haarcascade_frontalface_default.xml" \
  "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml"

echo "Downloading Haar Cascade - Eye..."
curl -L -o "$MODELS_DIR/haarcascade_eye.xml" \
  "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml"

echo "Downloading Haar Cascade - Profile Face..."
curl -L -o "$MODELS_DIR/haarcascade_profileface.xml" \
  "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_profileface.xml"

echo "Downloading LBP Cascade - Frontal Face..."
curl -L -o "$MODELS_DIR/lbpcascade_frontalface.xml" \
  "https://raw.githubusercontent.com/opencv/opencv/master/data/lbpcascades/lbpcascade_frontalface.xml"

# DNN Face Detector (ResNet-SSD)
echo "Downloading DNN Face Detector - Prototxt..."
curl -L -o "$MODELS_DIR/deploy.prototxt" \
  "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt"

echo "Downloading DNN Face Detector - Caffe Model..."
curl -L -o "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
  "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel"

# YuNet Face Detector (ONNX)
echo "Downloading YuNet Face Detector..."
curl -L -o "$MODELS_DIR/face_detection_yunet_2023mar.onnx" \
  "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"

# SFace Recognition (ONNX)
echo "Downloading SFace Recognition Model..."
curl -L -o "$MODELS_DIR/face_recognition_sface_2021dec.onnx" \
  "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"

echo "All models downloaded successfully!"
echo "Models location: $MODELS_DIR"
ls -lh "$MODELS_DIR"

