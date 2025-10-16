#!/bin/bash
#
# Download all FaceBytes ONNX models for Spring Vision
# This script downloads face recognition embedding models required by the FaceBytes backend
#
# Usage: ./scripts/download-facebytes-models.sh
#

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default to user cache directory (same as ModelDownloader.java)
CACHE_DIR="${HOME}/.spring-vision/facebytes/models"

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}FaceBytes Model Downloader${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Models will be downloaded to: $CACHE_DIR"
echo ""

# Create cache directory if it doesn't exist
mkdir -p "$CACHE_DIR"

# Function to download with progress and checksum validation
download_model() {
    local url=$1
    local output=$2
    local description=$3
    local expected_sha256=$4

    echo -e "${YELLOW}📥 Downloading: $description${NC}"
    echo "   URL: $url"

    if [ -f "$output" ]; then
        echo -e "${GREEN}   ✓ File exists, validating...${NC}"

        # Validate existing file if checksum provided
        if [ -n "$expected_sha256" ] && [ "$expected_sha256" != "SKIP" ]; then
            actual_sha256=$(sha256sum "$output" | cut -d' ' -f1)
            if [ "$actual_sha256" = "$expected_sha256" ]; then
                echo -e "${GREEN}   ✓ Checksum valid, skipping download${NC}"
                echo ""
                return 0
            else
                echo -e "${YELLOW}   ⚠ Checksum mismatch, re-downloading...${NC}"
                rm -f "$output"
            fi
        else
            local size=$(du -h "$output" | cut -f1)
            echo -e "${GREEN}   ✓ Already exists ($size), skipping${NC}"
            echo ""
            return 0
        fi
    fi

    # Download with retries
    local max_attempts=3
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        echo "   Attempt $attempt of $max_attempts..."

        if wget --progress=bar:force:noscroll --timeout=60 --tries=2 -O "$output.tmp" "$url" 2>&1; then
            mv "$output.tmp" "$output"
            local size=$(du -h "$output" | cut -f1)

            # Validate checksum if provided
            if [ -n "$expected_sha256" ] && [ "$expected_sha256" != "SKIP" ]; then
                actual_sha256=$(sha256sum "$output" | cut -d' ' -f1)
                if [ "$actual_sha256" = "$expected_sha256" ]; then
                    echo -e "${GREEN}   ✓ Downloaded and verified successfully ($size)${NC}"
                    echo ""
                    return 0
                else
                    echo -e "${RED}   ✗ Checksum mismatch!${NC}"
                    echo "     Expected: $expected_sha256"
                    echo "     Got:      $actual_sha256"
                    rm -f "$output"
                    attempt=$((attempt + 1))
                    continue
                fi
            else
                echo -e "${GREEN}   ✓ Downloaded successfully ($size)${NC}"
                echo ""
                return 0
            fi
        else
            echo -e "${RED}   ✗ Download failed${NC}"
            rm -f "$output.tmp"
            attempt=$((attempt + 1))
        fi

        if [ $attempt -le $max_attempts ]; then
            echo "   Retrying in 2 seconds..."
            sleep 2
        fi
    done

    echo -e "${RED}   ✗ Failed after $max_attempts attempts${NC}"
    echo ""
    return 1
}

echo "Starting downloads..."
echo ""

# Track failures
FAILED_MODELS=()

# ArcFace - High accuracy face recognition (512-d embeddings)
echo -e "${GREEN}--- ArcFace Model ---${NC}"
if ! download_model \
    "https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx" \
    "$CACHE_DIR/arcface.onnx" \
    "ArcFace ResNet100 (512-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("arcface.onnx")
fi

# Facenet - 128-dimensional embeddings
echo -e "${GREEN}--- Facenet Model ---${NC}"
if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/facenet_weights.h5" \
    "$CACHE_DIR/facenet128.onnx" \
    "Facenet (128-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("facenet128.onnx")
fi

# Facenet512 - 512-dimensional embeddings
echo -e "${GREEN}--- Facenet512 Model ---${NC}"
if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/facenet512_weights.h5" \
    "$CACHE_DIR/facenet512.onnx" \
    "Facenet512 (512-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("facenet512.onnx")
fi

# VGGFace - Classic face recognition model
echo -e "${GREEN}--- VGGFace Model ---${NC}"
if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/vgg_face_weights.h5" \
    "$CACHE_DIR/vggface.onnx" \
    "VGGFace (2622-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("vggface.onnx")
fi

# OpenFace - Lightweight face recognition
echo -e "${GREEN}--- OpenFace Model ---${NC}"
if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/openface_weights.h5" \
    "$CACHE_DIR/openface.onnx" \
    "OpenFace (128-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("openface.onnx")
fi

# SFace - Compact and efficient
echo -e "${GREEN}--- SFace Model ---${NC}"
if ! download_model \
    "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx" \
    "$CACHE_DIR/sface.onnx" \
    "SFace (128-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("sface.onnx")
fi

# DeepFace - Original DeepFace model
echo -e "${GREEN}--- DeepFace Model ---${NC}"
if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/deepface_weights.h5" \
    "$CACHE_DIR/deepface.onnx" \
    "DeepFace (4096-d embeddings)" \
    "SKIP"; then
    FAILED_MODELS+=("deepface.onnx")
fi

# Analysis models (age, gender, emotion, race)
echo ""
echo -e "${GREEN}--- Facial Analysis Models ---${NC}"

if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/age_model_weights.h5" \
    "$CACHE_DIR/age_model.h5" \
    "Age Prediction Model" \
    "SKIP"; then
    FAILED_MODELS+=("age_model.h5")
fi

if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/gender_model_weights.h5" \
    "$CACHE_DIR/gender_model.h5" \
    "Gender Prediction Model" \
    "SKIP"; then
    FAILED_MODELS+=("gender_model.h5")
fi

if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/facial_expression_model_weights.h5" \
    "$CACHE_DIR/emotion_model.h5" \
    "Emotion Prediction Model" \
    "SKIP"; then
    FAILED_MODELS+=("emotion_model.h5")
fi

if ! download_model \
    "https://github.com/serengil/deepface_models/releases/download/v1.0/race_model_single_batch.h5" \
    "$CACHE_DIR/race_model.h5" \
    "Race Prediction Model" \
    "SKIP"; then
    FAILED_MODELS+=("race_model.h5")
fi

# RetinaFace detector
echo ""
echo -e "${GREEN}--- Face Detector Models ---${NC}"

if ! download_model \
    "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx" \
    "$CACHE_DIR/retinaface.onnx" \
    "RetinaFace/YuNet Detector" \
    "SKIP"; then
    FAILED_MODELS+=("retinaface.onnx")
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Download Summary${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# List downloaded files
echo "Downloaded models:"
ls -lh "$CACHE_DIR" | grep -v "^total" | awk '{print "  " $9 " (" $5 ")"}'

echo ""

if [ ${#FAILED_MODELS[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All models downloaded successfully!${NC}"
    echo ""
    echo -e "${GREEN}Configuration:${NC}"
    echo "Set the following environment variables or system properties:"
    echo ""
    echo "  export FACEBYTES_ARCFACE_ONNX_PATH=\"$CACHE_DIR/arcface.onnx\""
    echo "  export FACEBYTES_FACENET_ONNX_PATH=\"$CACHE_DIR/facenet128.onnx\""
    echo "  export FACEBYTES_FACENET512_ONNX_PATH=\"$CACHE_DIR/facenet512.onnx\""
    echo "  export FACEBYTES_VGGFACE_ONNX_PATH=\"$CACHE_DIR/vggface.onnx\""
    echo "  export FACEBYTES_OPENFACE_ONNX_PATH=\"$CACHE_DIR/openface.onnx\""
    echo "  export FACEBYTES_SFACE_ONNX_PATH=\"$CACHE_DIR/sface.onnx\""
    echo "  export FACEBYTES_DEEPFACE_ONNX_PATH=\"$CACHE_DIR/deepface.onnx\""
    echo "  export FACEBYTES_RETINAFACE_ONNX_PATH=\"$CACHE_DIR/retinaface.onnx\""
    echo ""
    echo "Or update application.yml with model paths."
    echo ""
else
    echo -e "${RED}✗ Some models failed to download:${NC}"
    for model in "${FAILED_MODELS[@]}"; do
        echo -e "${RED}  - $model${NC}"
    done
    echo ""
    echo "The system will still work with available models."
    exit 1
fi

