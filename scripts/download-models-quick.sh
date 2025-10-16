#!/bin/bash
# Quick download script - Downloads only the essential, ready-to-use models
# For full detector backend support, use: ./download-detector-models.sh

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
MODELS_DIR="$PROJECT_ROOT/core/src/main/resources/models"

echo -e "${GREEN}=== Quick Model Downloader (Essential Models Only) ===${NC}"
echo ""

mkdir -p "$MODELS_DIR"

# Essential detector: YuNet (Best accuracy, ready to use)
if [ ! -f "$MODELS_DIR/face_detection_yunet_2023mar.onnx" ]; then
    echo -e "${YELLOW}Downloading YuNet face detector (RetinaFace)...${NC}"
    wget --progress=bar:force:noscroll -O "$MODELS_DIR/face_detection_yunet_2023mar.onnx" \
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"
    echo -e "${GREEN}✓ YuNet downloaded${NC}"
else
    echo -e "${GREEN}✓ YuNet already exists${NC}"
fi

# Essential recognition: SFace (Best compatibility with YuNet)
if [ ! -f "$MODELS_DIR/face_recognition_sface_2021dec.onnx" ]; then
    echo -e "${YELLOW}Downloading SFace recognition model...${NC}"
    wget --progress=bar:force:noscroll -O "$MODELS_DIR/face_recognition_sface_2021dec.onnx" \
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx"
    echo -e "${GREEN}✓ SFace downloaded${NC}"
else
    echo -e "${GREEN}✓ SFace already exists${NC}"
fi

# Optional: ArcFace (Higher accuracy for embeddings)
if [ ! -f "$MODELS_DIR/arcface.onnx" ]; then
    echo -e "${YELLOW}Downloading ArcFace model (optional)...${NC}"
    if wget --timeout=30 --tries=2 --progress=bar:force:noscroll -O "$MODELS_DIR/arcface.onnx" \
        "https://github.com/serengil/deepface_models/releases/download/v1.0/arcface_weights.h5" 2>&1 || \
       wget --timeout=30 --tries=2 --progress=bar:force:noscroll -O "$MODELS_DIR/arcface.onnx" \
        "https://huggingface.co/serengil/deepface/resolve/main/models/arcface.onnx" 2>&1; then
        echo -e "${GREEN}✓ ArcFace downloaded${NC}"
    else
        echo -e "${YELLOW}! ArcFace download failed (optional, skipping)${NC}"
        echo -e "${YELLOW}  Note: ArcFace is optional. SFace is already sufficient for most use cases.${NC}"
    fi
else
    echo -e "${GREEN}✓ ArcFace already exists${NC}"
fi

echo ""
echo -e "${GREEN}=== Quick Setup Complete! ===${NC}"
echo ""
echo "✓ RetinaFace (YuNet) detector ready"
echo "✓ SFace embeddings ready"
echo "✓ OpenCV/DLIB detectors ready (built-in)"
echo ""
echo -e "${GREEN}Configuration:${NC}"
cat <<EOF
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: retinaface  # Best accuracy
EOF
echo ""
echo "For more detector options, run: ./download-detector-models.sh"

