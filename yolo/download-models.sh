#!/bin/bash
# YOLO Model Downloader
# Downloads and converts YOLO models for bundling

set -e

MODELS_DIR="$(cd "$(dirname "$0")" && pwd)/src/main/resources/models"
mkdir -p "$MODELS_DIR"

echo "====================================="
echo "YOLO Model Downloader"
echo "====================================="
echo "Target directory: $MODELS_DIR"
echo ""

# Check if ultralytics is available
if ! command -v yolo &> /dev/null; then
    echo "⚠ ultralytics not found."
    echo ""
    echo "YOLO models require the ultralytics package."
    echo "To install:"
    echo "  Option 1: pip install ultralytics --break-system-packages"
    echo "  Option 2: pip install --user ultralytics"
    echo "  Option 3: Use a virtual environment"
    echo ""
    echo "Skipping YOLO model download. Build will continue with auto-download at runtime."
    echo ""
    exit 0  # Exit gracefully to allow build to continue
fi

# Function to convert and download YOLO model
convert_model() {
    local model_name="$1"
    local description="$2"
    local output_file="${model_name}.onnx"

    if [ -f "$MODELS_DIR/$output_file" ]; then
        echo "✓ $description already exists"
    else
        echo "⬇ Downloading and converting $description..."
        cd "$MODELS_DIR"
        yolo export model="${model_name}.pt" format=onnx imgsz=640 2>&1 | grep -E "(Ultralytics|Export|success)" || true
        if [ -f "$output_file" ]; then
            echo "✓ $description converted successfully"
        else
            echo "⚠ Failed to convert $description (will auto-download at runtime)"
            cd - > /dev/null
            return 0  # Don't fail the build
        fi
        cd - > /dev/null
    fi
}

# Download YOLOv8 Nano (recommended for most use cases)
convert_model "yolov8n" "YOLOv8 Nano Model"

# Optionally download other models (commented out by default to save space)
# Uncomment the lines below if you need larger models:
# convert_model "yolov8s" "YOLOv8 Small Model"
# convert_model "yolov8m" "YOLOv8 Medium Model"

echo ""
echo "====================================="
echo "✓ YOLO model check complete!"
echo "====================================="
echo ""
if [ -d "$MODELS_DIR" ]; then
    echo "Total size:"
    du -sh "$MODELS_DIR" 2>/dev/null || echo "Unable to calculate size"
fi
echo ""
echo "Note: If models were not downloaded, they will be"
echo "      auto-downloaded at runtime when first needed."
echo ""
