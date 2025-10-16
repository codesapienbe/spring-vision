#!/bin/bash
# Quick download script - Downloads only the essential OpenCV models
# These models are required for the OpenCV backend to function

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
MODELS_DIR="$PROJECT_ROOT/core/src/main/resources/models"

echo -e "${GREEN}=== Spring Vision - Essential Model Downloader ===${NC}"
echo "This script downloads the essential models required by OpenCV backend"
echo "Models directory: $MODELS_DIR"
echo ""

mkdir -p "$MODELS_DIR"

# Essential detector: YuNet (Best accuracy, ready to use)
if [ ! -f "$MODELS_DIR/face_detection_yunet_2023mar.onnx" ]; then
    echo -e "${YELLOW}Downloading YuNet face detector (228KB)...${NC}"
    wget --progress=bar:force:noscroll -O "$MODELS_DIR/face_detection_yunet_2023mar.onnx" \
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"
    echo -e "${GREEN}✓ YuNet downloaded${NC}"
else
    echo -e "${GREEN}✓ YuNet already exists${NC}"
fi

# Essential recognition: SFace (Best compatibility with YuNet)
if [ ! -f "$MODELS_DIR/face_recognition_sface_2021dec.onnx" ]; then
    echo -e "${YELLOW}Downloading SFace recognition model (37MB)...${NC}"
    wget --progress=bar:force:noscroll -O "$MODELS_DIR/face_recognition_sface_2021dec.onnx" \
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"
    echo -e "${GREEN}✓ SFace downloaded${NC}"
else
    echo -e "${GREEN}✓ SFace already exists${NC}"
fi

# DNN SSD Model (5.2MB)
if [ ! -f "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" ]; then
    echo -e "${YELLOW}Downloading OpenCV DNN SSD model (5.2MB)...${NC}"
    if wget --timeout=30 --tries=2 --progress=bar:force:noscroll -O "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "https://raw.githubusercontent.com/opencv_3rdparty/dnn_samples_face_detector_20170830/master/res10_300x300_ssd_iter_140000_fp16.caffemodel" 2>&1 || \
       wget --timeout=30 --tries=2 --progress=bar:force:noscroll -O "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel" 2>&1; then
        echo -e "${GREEN}✓ DNN SSD model downloaded${NC}"
    else
        echo -e "${RED}✗ DNN SSD model download failed${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ DNN SSD model already exists${NC}"
fi

echo ""
echo -e "${GREEN}=== Download Complete ===${NC}"
echo "All essential OpenCV models are ready!"
echo ""
echo "Note: Haar cascade and other small models are included in the repository."
echo "To start the application, run: mvn spring-boot:run"
