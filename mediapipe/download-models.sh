#!/bin/bash
# MediaPipe Model Downloader
# Downloads required MediaPipe models for bundling

set -e

MODELS_DIR="$(cd "$(dirname "$0")" && pwd)/src/main/resources/models"
mkdir -p "$MODELS_DIR"

echo "====================================="
echo "MediaPipe Model Downloader"
echo "====================================="
echo "Target directory: $MODELS_DIR"
echo ""

# Function to download model if not exists
download_model() {
    local url="$1"
    local output_file="$2"
    local description="$3"

    if [ -f "$MODELS_DIR/$output_file" ]; then
        echo "✓ $description already exists"
    else
        echo "⬇ Downloading $description..."
        wget --timeout=60 --tries=3 -q --show-progress "$url" -O "$MODELS_DIR/$output_file"
        if [ $? -eq 0 ]; then
            echo "✓ $description downloaded successfully"
        else
            echo "✗ Failed to download $description"
            return 1
        fi
    fi
}

# Download MediaPipe models
download_model \
    "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite" \
    "face_detection_short_range.tflite" \
    "Face Detection Model"

download_model \
    "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task" \
    "hand_landmarker.task" \
    "Hand Landmarker Model"

download_model \
    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task" \
    "pose_landmarker_lite.task" \
    "Pose Landmarker Model"

download_model \
    "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/1/efficientdet_lite0.tflite" \
    "efficientdet_lite0.tflite" \
    "Object Detection Model"

echo ""
echo "====================================="
echo "✓ All MediaPipe models downloaded!"
echo "====================================="
echo ""
echo "Total size:"
du -sh "$MODELS_DIR" 2>/dev/null || echo "Unable to calculate size"
echo ""

