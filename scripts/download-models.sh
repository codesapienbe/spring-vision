#!/bin/bash
#
# Download all OpenCV and face detection models for Spring Vision
# Run this script once to download models that will be included in the JAR
#
# Usage: ./scripts/download-models.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODELS_DIR="$PROJECT_ROOT/core/src/main/resources/models"

echo "================================"
echo "Spring Vision Model Downloader"
echo "================================"
echo ""
echo "Models will be downloaded to: $MODELS_DIR"
echo ""

# Create models directory if it doesn't exist
mkdir -p "$MODELS_DIR"

# Function to download with progress
download_file() {
    local url=$1
    local output=$2
    local description=$3

    echo "📥 Downloading: $description"
    echo "   URL: $url"

    if [ -f "$output" ]; then
        echo "   ✓ Already exists, skipping"
        echo ""
        return 0
    fi

    if curl -L --progress-bar --fail "$url" -o "$output"; then
        local size=$(du -h "$output" | cut -f1)
        echo "   ✓ Downloaded successfully ($size)"
        echo ""
    else
        echo "   ✗ Failed to download"
        echo ""
        return 1
    fi
}

echo "Starting downloads..."
echo ""

# Haar Cascade Classifiers
echo "--- Haar Cascade Classifiers ---"
download_file \
    "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml" \
    "$MODELS_DIR/haarcascade_frontalface_default.xml" \
    "Haar Cascade - Frontal Face"

download_file \
    "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml" \
    "$MODELS_DIR/haarcascade_eye.xml" \
    "Haar Cascade - Eye"

download_file \
    "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_profileface.xml" \
    "$MODELS_DIR/haarcascade_profileface.xml" \
    "Haar Cascade - Profile Face"

# LBP Cascade
echo "--- LBP Cascade Classifiers ---"
download_file \
    "https://raw.githubusercontent.com/opencv/opencv/master/data/lbpcascades/lbpcascade_frontalface.xml" \
    "$MODELS_DIR/lbpcascade_frontalface.xml" \
    "LBP Cascade - Frontal Face"

# DNN Face Detector (Caffe) - Deploy prototxt only (model too large/problematic)
echo "--- DNN Face Detector (Caffe) ---"
download_file \
    "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt" \
    "$MODELS_DIR/deploy.prototxt" \
    "DNN Caffe Deploy Prototxt"

# Optional: Download the Caffe model file (large ~5MB)
echo ""
echo "The Caffe model file is optional. YuNet provides superior detection."
read -p "Download Caffe model file (res10_300x300_ssd_iter_140000_fp16.caffemodel ~5MB)? [y/N]: " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    download_file \
        "https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "DNN Caffe Model (fp16)" || \
    download_file \
        "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "DNN Caffe Model (fp16 - alternate URL)"
else
    echo "Skipping Caffe model download. YuNet and Haar cascades will be used instead."
fi
echo ""

# YuNet Face Detector (Modern ONNX-based, recommended)
echo "--- YuNet Face Detector (ONNX) ---"
download_file \
    "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" \
    "$MODELS_DIR/face_detection_yunet_2023mar.onnx" \
    "YuNet Face Detector (2023 March)"

# SFace Recognition Model (ONNX) - Using HuggingFace mirror
echo "--- SFace Face Recognition (ONNX) ---"
echo "Downloading from HuggingFace (more reliable than GitHub for large files)..."
download_file \
    "https://huggingface.co/opencv/face_recognition_sface/resolve/main/face_recognition_sface_2021dec.onnx" \
    "$MODELS_DIR/face_recognition_sface_2021dec.onnx" \
    "SFace Face Recognition Model (~37MB)"

# Optional: Download quantized version (smaller, faster, slightly less accurate)
echo ""
echo "--- Optional: Quantized SFace Models ---"
read -p "Download quantized SFace models? (int8 versions are smaller but less accurate) [y/N]: " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    download_file \
        "https://huggingface.co/opencv/face_recognition_sface/resolve/main/face_recognition_sface_2021dec_int8.onnx" \
        "$MODELS_DIR/face_recognition_sface_2021dec_int8.onnx" \
        "SFace Recognition (INT8 Quantized)"

    download_file \
        "https://huggingface.co/opencv/face_recognition_sface/resolve/main/face_recognition_sface_2021dec_int8bq.onnx" \
        "$MODELS_DIR/face_recognition_sface_2021dec_int8bq.onnx" \
        "SFace Recognition (INT8 Block Quantized)"
fi

echo ""
echo "================================"
echo "✓ Model Download Complete!"
echo "================================"
echo ""
echo "Downloaded models:"
ls -lh "$MODELS_DIR"
echo ""
echo "Total size:"
du -sh "$MODELS_DIR"
echo ""
echo "These models will be automatically included in the JAR during Maven build."
echo "You can now run: mvn clean package"
echo ""
