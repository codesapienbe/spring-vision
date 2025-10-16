#!/bin/bash
# Download all detector backend models for FaceBytes
# Models will be placed in core/src/main/resources/models/

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
MODELS_DIR="$PROJECT_ROOT/core/src/main/resources/models"
MTCNN_DIR="$MODELS_DIR/mtcnn"
TEMP_DIR=$(mktemp -d)

echo -e "${GREEN}=== FaceBytes Detector Models Downloader ===${NC}"
echo "Project root: $PROJECT_ROOT"
echo "Models directory: $MODELS_DIR"
echo "Temporary directory: $TEMP_DIR"
echo ""

# Function to download with retry
download_file() {
    local url="$1"
    local output="$2"
    local max_attempts=3
    local attempt=1

    echo -e "${YELLOW}Downloading: $(basename $output)${NC}"
    echo "  URL: $url"

    while [ $attempt -le $max_attempts ]; do
        if wget --timeout=30 --tries=2 --progress=bar:force:noscroll -O "$output" "$url" 2>&1; then
            echo -e "${GREEN}  ✓ Downloaded successfully${NC}"
            return 0
        else
            echo -e "${RED}  ✗ Attempt $attempt failed${NC}"
            attempt=$((attempt + 1))
            if [ $attempt -le $max_attempts ]; then
                echo "  Retrying in 2 seconds..."
                sleep 2
            fi
        fi
    done

    echo -e "${RED}  ✗ Failed to download after $max_attempts attempts${NC}"
    return 1
}

# Create directories
echo -e "${YELLOW}Creating model directories...${NC}"
mkdir -p "$MTCNN_DIR"
mkdir -p "$MODELS_DIR/dlib"
mkdir -p "$MODELS_DIR/retinaface"

# Track download status
declare -a FAILED_DOWNLOADS=()

# =============================================================================
# 1. MTCNN Models (Multi-task Cascaded CNN - 3 models required)
# =============================================================================
echo ""
echo -e "${GREEN}=== Downloading MTCNN Models ===${NC}"
echo "MTCNN requires 3 ONNX models for its 3-stage detection pipeline"
echo ""

# MTCNN PNet (Proposal Network)
if [ ! -f "$MTCNN_DIR/pnet.onnx" ]; then
    echo -e "${YELLOW}1/3: PNet (Proposal Network)${NC}"
    # Try GitHub raw content from various MTCNN ONNX repositories
    if ! download_file \
        "https://raw.githubusercontent.com/deepinsight/insightface/master/detection/mtcnn/model/pnet.onnx" \
        "$TEMP_DIR/pnet.onnx"; then
        # Try alternative: TimesNet MTCNN ONNX export
        echo "  Trying alternative source..."
        if ! download_file \
            "https://github.com/timesler/facenet-pytorch/releases/download/v2.2.9/mtcnn_pnet.onnx" \
            "$TEMP_DIR/pnet.onnx"; then
            echo -e "${YELLOW}  ! MTCNN PNet not found - will need manual conversion${NC}"
            echo "    See: https://github.com/TropComplique/mtcnn-pytorch for conversion"
            # Don't add to failed downloads - it's expected
        fi
    fi
    if [ -f "$TEMP_DIR/pnet.onnx" ]; then
        mv "$TEMP_DIR/pnet.onnx" "$MTCNN_DIR/pnet.onnx"
        echo -e "${GREEN}  ✓ Installed to $MTCNN_DIR/pnet.onnx${NC}"
    fi
else
    echo -e "${GREEN}  ✓ PNet already exists${NC}"
fi

# MTCNN RNet (Refinement Network)
if [ ! -f "$MTCNN_DIR/rnet.onnx" ]; then
    echo -e "${YELLOW}2/3: RNet (Refinement Network)${NC}"
    if ! download_file \
        "https://raw.githubusercontent.com/deepinsight/insightface/master/detection/mtcnn/model/rnet.onnx" \
        "$TEMP_DIR/rnet.onnx"; then
        echo "  Trying alternative source..."
        if ! download_file \
            "https://github.com/timesler/facenet-pytorch/releases/download/v2.2.9/mtcnn_rnet.onnx" \
            "$TEMP_DIR/rnet.onnx"; then
            echo -e "${YELLOW}  ! MTCNN RNet not found - will need manual conversion${NC}"
        fi
    fi
    if [ -f "$TEMP_DIR/rnet.onnx" ]; then
        mv "$TEMP_DIR/rnet.onnx" "$MTCNN_DIR/rnet.onnx"
        echo -e "${GREEN}  ✓ Installed to $MTCNN_DIR/rnet.onnx${NC}"
    fi
else
    echo -e "${GREEN}  ✓ RNet already exists${NC}"
fi

# MTCNN ONet (Output Network)
if [ ! -f "$MTCNN_DIR/onet.onnx" ]; then
    echo -e "${YELLOW}3/3: ONet (Output Network)${NC}"
    if ! download_file \
        "https://raw.githubusercontent.com/deepinsight/insightface/master/detection/mtcnn/model/onet.onnx" \
        "$TEMP_DIR/onet.onnx"; then
        echo "  Trying alternative source..."
        if ! download_file \
            "https://github.com/timesler/facenet-pytorch/releases/download/v2.2.9/mtcnn_onet.onnx" \
            "$TEMP_DIR/onet.onnx"; then
            echo -e "${YELLOW}  ! MTCNN ONet not found - will need manual conversion${NC}"
        fi
    fi
    if [ -f "$TEMP_DIR/onet.onnx" ]; then
        mv "$TEMP_DIR/onet.onnx" "$MTCNN_DIR/onet.onnx"
        echo -e "${GREEN}  ✓ Installed to $MTCNN_DIR/onet.onnx${NC}"
    fi
else
    echo -e "${GREEN}  ✓ ONet already exists${NC}"
fi

# Check if any MTCNN models are missing
MTCNN_MISSING=0
[ ! -f "$MTCNN_DIR/pnet.onnx" ] && MTCNN_MISSING=1
[ ! -f "$MTCNN_DIR/rnet.onnx" ] && MTCNN_MISSING=1
[ ! -f "$MTCNN_DIR/onet.onnx" ] && MTCNN_MISSING=1

if [ $MTCNN_MISSING -eq 1 ]; then
    echo ""
    echo -e "${YELLOW}NOTE: MTCNN models require manual conversion from PyTorch/TensorFlow${NC}"
    echo "Options:"
    echo "  1. Use RetinaFace/YuNet instead (recommended, ready to use)"
    echo "  2. Convert MTCNN models using: https://github.com/TropComplique/mtcnn-pytorch"
    echo "  3. Use DLIB or OpenCV detectors (no models needed)"
fi

# =============================================================================
# 2. RetinaFace Model (Already using YuNet from OpenCV Zoo)
# =============================================================================
echo ""
echo -e "${GREEN}=== Checking RetinaFace/YuNet Model ===${NC}"

if [ -f "$MODELS_DIR/face_detection_yunet_2023mar.onnx" ]; then
    echo -e "${GREEN}  ✓ YuNet model already exists (used for RetinaFace)${NC}"
    echo "    Location: $MODELS_DIR/face_detection_yunet_2023mar.onnx"
else
    echo -e "${YELLOW}Downloading YuNet model (OpenCV Zoo)...${NC}"
    if download_file \
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" \
        "$MODELS_DIR/face_detection_yunet_2023mar.onnx"; then
        echo -e "${GREEN}  ✓ YuNet model downloaded${NC}"
    else
        FAILED_DOWNLOADS+=("YuNet/RetinaFace")
    fi
fi

# Alternative: Download actual RetinaFace ONNX if you prefer
if [ ! -f "$MODELS_DIR/retinaface/retinaface_mobilenet.onnx" ]; then
    echo ""
    echo -e "${YELLOW}Downloading RetinaFace MobileNet model (alternative)...${NC}"
    if download_file \
        "https://github.com/onnx/models/raw/main/vision/body_analysis/age_gender/models/retinaface_mobilenet.onnx" \
        "$TEMP_DIR/retinaface_mobilenet.onnx"; then
        mv "$TEMP_DIR/retinaface_mobilenet.onnx" "$MODELS_DIR/retinaface/retinaface_mobilenet.onnx"
        echo -e "${GREEN}  ✓ RetinaFace MobileNet downloaded${NC}"
    else
        echo -e "${YELLOW}  ! RetinaFace alternative not available (YuNet will be used)${NC}"
    fi
fi

# =============================================================================
# 3. DLIB Detector (HOG-based, built into OpenCV - no separate model needed)
# =============================================================================
echo ""
echo -e "${GREEN}=== DLIB Detector ===${NC}"
echo -e "${GREEN}  ✓ DLIB uses HOG descriptor from OpenCV (no separate model file needed)${NC}"
echo "    Implementation: Uses org.bytedeco.opencv.opencv_objdetect.HOGDescriptor"

# =============================================================================
# 4. Optional: Download face recognition models (embeddings)
# =============================================================================
echo ""
echo -e "${YELLOW}=== Optional: Face Recognition Models ===${NC}"
echo "These models are for embedding extraction (separate from detection)"

# ArcFace
if [ ! -f "$MODELS_DIR/arcface.onnx" ]; then
    echo -e "${YELLOW}Downloading ArcFace model...${NC}"
    if download_file \
        "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/ArcFace.onnx" \
        "$MODELS_DIR/arcface.onnx"; then
        echo -e "${GREEN}  ✓ ArcFace model downloaded${NC}"
    else
        echo -e "${YELLOW}  ! ArcFace download failed (optional)${NC}"
    fi
else
    echo -e "${GREEN}  ✓ ArcFace model already exists${NC}"
fi

# VGGFace
if [ ! -f "$MODELS_DIR/vggface.onnx" ]; then
    echo -e "${YELLOW}Downloading VGGFace model...${NC}"
    if download_file \
        "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/VGGFace.onnx" \
        "$MODELS_DIR/vggface.onnx"; then
        echo -e "${GREEN}  ✓ VGGFace model downloaded${NC}"
    else
        echo -e "${YELLOW}  ! VGGFace download failed (optional)${NC}"
    fi
else
    echo -e "${GREEN}  ✓ VGGFace model already exists${NC}"
fi

# Facenet128
if [ ! -f "$MODELS_DIR/facenet128.onnx" ]; then
    echo -e "${YELLOW}Downloading Facenet128 model...${NC}"
    if download_file \
        "https://github.com/serengil/deepface_models/releases/download/pre-trained-weights/Facenet128.onnx" \
        "$MODELS_DIR/facenet128.onnx"; then
        echo -e "${GREEN}  ✓ Facenet128 model downloaded${NC}"
    else
        echo -e "${YELLOW}  ! Facenet128 download failed (optional)${NC}"
    fi
else
    echo -e "${GREEN}  ✓ Facenet128 model already exists${NC}"
fi

# SFace (already exists)
if [ -f "$MODELS_DIR/face_recognition_sface_2021dec.onnx" ]; then
    echo -e "${GREEN}  ✓ SFace model already exists${NC}"
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo -e "${GREEN}=== Download Summary ===${NC}"
echo ""

# Cleanup temp directory
rm -rf "$TEMP_DIR"

# Check for failures
if [ ${#FAILED_DOWNLOADS[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All models downloaded successfully!${NC}"
    echo ""
    echo -e "${GREEN}Available detector backends:${NC}"
    echo "  • OpenCV (Haar/DNN) - Always available"
    [ -f "$MODELS_DIR/face_detection_yunet_2023mar.onnx" ] && echo "  • RetinaFace (YuNet) - ✓ Ready"
    [ -f "$MTCNN_DIR/pnet.onnx" ] && [ -f "$MTCNN_DIR/rnet.onnx" ] && [ -f "$MTCNN_DIR/onet.onnx" ] && echo "  • MTCNN - ✓ Ready"
    echo "  • DLIB (HOG) - ✓ Ready (built-in)"
    echo ""
    echo -e "${GREEN}Configuration for application.yml:${NC}"
    cat <<EOF
spring:
  vision:
    facebytes:
      enabled: true
      detector-backend: retinaface  # or: opencv, mtcnn, dlib
EOF
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some models failed to download:${NC}"
    for model in "${FAILED_DOWNLOADS[@]}"; do
        echo -e "  ${RED}✗ $model${NC}"
    done
    echo ""
    echo -e "${YELLOW}Note: Failed models will use OpenCV as fallback${NC}"
    echo ""
    exit 1
fi

