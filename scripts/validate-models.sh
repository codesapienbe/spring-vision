#!/bin/bash
#
# Validate downloaded OpenCV and face detection models
# Checks file sizes, formats, and content to ensure they're valid
#
# Usage: ./scripts/validate-models.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODELS_DIR="$PROJECT_ROOT/core/src/main/resources/models"

echo "==============================="
echo "Spring Vision Model Validator"
echo "==============================="
echo ""
echo "Validating models in: $MODELS_DIR"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL=0
VALID=0
INVALID=0
MISSING=0

# Function to check if file exists and is not empty
check_file_exists() {
    local file=$1
    local description=$2

    TOTAL=$((TOTAL + 1))

    if [ ! -f "$file" ]; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: MISSING"
        echo "   Path: $file"
        MISSING=$((MISSING + 1))
        echo ""
        return 1
    fi

    local size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
    local size_human=$(du -h "$file" | cut -f1)

    if [ "$size" -eq 0 ]; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: EMPTY FILE (0 bytes)"
        echo "   Path: $file"
        INVALID=$((INVALID + 1))
        echo ""
        return 1
    fi

    return 0
}

# Function to check if file is HTML instead of binary/XML
check_not_html() {
    local file=$1
    local description=$2
    local min_size=$3

    # Check first few bytes for HTML markers
    local header=$(head -c 200 "$file" 2>/dev/null)

    if echo "$header" | grep -qi "<!DOCTYPE\|<html\|<head\|<body\|404.*not.*found"; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: INVALID - HTML content detected (download failed)"
        echo "   Path: $file"
        local size_human=$(du -h "$file" | cut -f1)
        echo "   Size: $size_human"
        INVALID=$((INVALID + 1))
        echo ""
        return 1
    fi

    # Check minimum size
    local size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
    if [ "$size" -lt "$min_size" ]; then
        echo -e "${YELLOW}⚠${NC} $description"
        echo "   Status: WARNING - File smaller than expected"
        echo "   Path: $file"
        local size_human=$(du -h "$file" | cut -f1)
        echo "   Size: $size_human (expected at least $(numfmt --to=iec $min_size 2>/dev/null || echo "$min_size bytes"))"
        echo ""
        # Don't count as invalid, just warn
    fi

    return 0
}

# Function to validate XML file
validate_xml() {
    local file=$1
    local description=$2
    local min_size=$3

    if ! check_file_exists "$file" "$description"; then
        return 1
    fi

    if ! check_not_html "$file" "$description" "$min_size"; then
        return 1
    fi

    # Check if it's valid XML
    if ! head -n 1 "$file" | grep -q "<?xml"; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: INVALID - Not a valid XML file"
        echo "   Path: $file"
        INVALID=$((INVALID + 1))
        echo ""
        return 1
    fi

    # Check for OpenCV cascade markers
    if ! grep -q "opencv_storage\|cascade" "$file"; then
        echo -e "${YELLOW}⚠${NC} $description"
        echo "   Status: WARNING - Missing OpenCV cascade markers"
        echo "   Path: $file"
        echo ""
    fi

    local size_human=$(du -h "$file" | cut -f1)
    echo -e "${GREEN}✓${NC} $description"
    echo "   Status: VALID"
    echo "   Size: $size_human"
    VALID=$((VALID + 1))
    echo ""
    return 0
}

# Function to validate ONNX file
validate_onnx() {
    local file=$1
    local description=$2
    local min_size=$3

    if ! check_file_exists "$file" "$description"; then
        return 1
    fi

    if ! check_not_html "$file" "$description" "$min_size"; then
        return 1
    fi

    # Check ONNX magic bytes (starts with "ONNX" or protobuf marker)
    local magic=$(xxd -p -l 4 "$file" 2>/dev/null | head -1)

    # ONNX files typically start with protobuf marker (0x08) or have "ONNX" in first bytes
    if ! head -c 100 "$file" | grep -q "onnx\|pytorch" 2>/dev/null; then
        # Check if it's a valid protobuf format (common ONNX wrapper)
        if ! echo "$magic" | grep -qE "^08|^0a"; then
            echo -e "${RED}✗${NC} $description"
            echo "   Status: INVALID - Not a valid ONNX file (wrong magic bytes)"
            echo "   Path: $file"
            INVALID=$((INVALID + 1))
            echo ""
            return 1
        fi
    fi

    local size_human=$(du -h "$file" | cut -f1)
    echo -e "${GREEN}✓${NC} $description"
    echo "   Status: VALID"
    echo "   Size: $size_human"
    VALID=$((VALID + 1))
    echo ""
    return 0
}

# Function to validate Prototxt file
validate_prototxt() {
    local file=$1
    local description=$2
    local min_size=$3

    if ! check_file_exists "$file" "$description"; then
        return 1
    fi

    if ! check_not_html "$file" "$description" "$min_size"; then
        return 1
    fi

    # Check for Caffe prototxt markers
    if ! grep -q "name:\|layer\|input:" "$file"; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: INVALID - Not a valid Caffe prototxt file"
        echo "   Path: $file"
        INVALID=$((INVALID + 1))
        echo ""
        return 1
    fi

    local size_human=$(du -h "$file" | cut -f1)
    echo -e "${GREEN}✓${NC} $description"
    echo "   Status: VALID"
    echo "   Size: $size_human"
    VALID=$((VALID + 1))
    echo ""
    return 0
}

# Function to validate Caffemodel file
validate_caffemodel() {
    local file=$1
    local description=$2
    local min_size=$3

    if ! check_file_exists "$file" "$description"; then
        return 1
    fi

    if ! check_not_html "$file" "$description" "$min_size"; then
        return 1
    fi

    # Caffemodel is protobuf binary - check for protobuf markers
    local header=$(xxd -p -l 20 "$file" 2>/dev/null)

    # Should not be ASCII text
    if file "$file" | grep -q "ASCII\|HTML"; then
        echo -e "${RED}✗${NC} $description"
        echo "   Status: INVALID - File is ASCII/HTML instead of binary"
        echo "   Path: $file"
        INVALID=$((INVALID + 1))
        echo ""
        return 1
    fi

    local size_human=$(du -h "$file" | cut -f1)
    echo -e "${GREEN}✓${NC} $description"
    echo "   Status: VALID (binary format)"
    echo "   Size: $size_human"
    VALID=$((VALID + 1))
    echo ""
    return 0
}

echo "Validating models..."
echo ""

# Validate Haar Cascade Classifiers
echo "--- Haar Cascade Classifiers ---"
validate_xml \
    "$MODELS_DIR/haarcascade_frontalface_default.xml" \
    "Haar Cascade - Frontal Face" \
    900000  # ~900KB

validate_xml \
    "$MODELS_DIR/haarcascade_eye.xml" \
    "Haar Cascade - Eye" \
    300000  # ~300KB

validate_xml \
    "$MODELS_DIR/haarcascade_profileface.xml" \
    "Haar Cascade - Profile Face" \
    800000  # ~800KB

# Validate LBP Cascade
echo "--- LBP Cascade Classifiers ---"
validate_xml \
    "$MODELS_DIR/lbpcascade_frontalface.xml" \
    "LBP Cascade - Frontal Face" \
    50000   # ~50KB

# Validate DNN Face Detector
echo "--- DNN Face Detector (Caffe) ---"
validate_prototxt \
    "$MODELS_DIR/deploy.prototxt" \
    "DNN Caffe Deploy Prototxt" \
    20000   # ~20KB

# Optional caffemodel (may not exist)
if [ -f "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" ]; then
    validate_caffemodel \
        "$MODELS_DIR/res10_300x300_ssd_iter_140000_fp16.caffemodel" \
        "DNN Caffe Model" \
        5000000  # ~5MB
else
    echo -e "${YELLOW}⊘${NC} DNN Caffe Model (Optional)"
    echo "   Status: NOT DOWNLOADED (YuNet is recommended instead)"
    echo ""
fi

# Validate YuNet Face Detector
echo "--- YuNet Face Detector (ONNX) ---"
validate_onnx \
    "$MODELS_DIR/face_detection_yunet_2023mar.onnx" \
    "YuNet Face Detector" \
    200000  # ~200KB

# Validate SFace Recognition
echo "--- SFace Face Recognition (ONNX) ---"
validate_onnx \
    "$MODELS_DIR/face_recognition_sface_2021dec.onnx" \
    "SFace Face Recognition Model" \
    35000000  # ~35MB

# Optional quantized versions
if [ -f "$MODELS_DIR/face_recognition_sface_2021dec_int8.onnx" ]; then
    validate_onnx \
        "$MODELS_DIR/face_recognition_sface_2021dec_int8.onnx" \
        "SFace Recognition (INT8 Quantized)" \
        10000000  # ~10MB
fi

if [ -f "$MODELS_DIR/face_recognition_sface_2021dec_int8bq.onnx" ]; then
    validate_onnx \
        "$MODELS_DIR/face_recognition_sface_2021dec_int8bq.onnx" \
        "SFace Recognition (INT8 Block Quantized)" \
        10000000  # ~10MB
fi

# Summary
echo "==============================="
echo "Validation Summary"
echo "==============================="
echo ""
echo "Total models checked: $TOTAL"
echo -e "${GREEN}Valid:${NC}   $VALID"
echo -e "${RED}Invalid:${NC} $INVALID"
echo -e "${YELLOW}Missing:${NC} $MISSING"
echo ""

if [ $INVALID -gt 0 ] || [ $MISSING -gt 0 ]; then
    echo -e "${RED}❌ Validation FAILED${NC}"
    echo ""
    echo "Action required:"
    if [ $INVALID -gt 0 ]; then
        echo "  - Delete invalid files (they contain HTML or corrupted data)"
        echo "  - Re-run: ./scripts/download-models.sh"
    fi
    if [ $MISSING -gt 0 ]; then
        echo "  - Run: ./scripts/download-models.sh to download missing models"
    fi
    echo ""
    exit 1
else
    echo -e "${GREEN}✓ All models are valid!${NC}"
    echo ""
    echo "You can now build the project:"
    echo "  mvn clean package"
    echo ""
    exit 0
fi

