# YOLO Models

This directory contains YOLO models bundled with the application for offline use.

## Bundled Models

Currently, this directory is prepared to contain the following models:

### YOLOv8 Nano (Recommended for most use cases)

- **File**: `yolov8n.onnx`
- **Size**: ~6 MB
- **Description**: Ultra-fast, lightweight model suitable for real-time detection
- **Accuracy**: Good for general object detection
- **Speed**: ~150 FPS on modern CPU

### YOLOv8 Small (Optional)

- **File**: `yolov8s.onnx`
- **Size**: ~22 MB
- **Description**: Balanced speed and accuracy
- **Accuracy**: Better than Nano
- **Speed**: ~100 FPS on modern CPU

### YOLOv8 Medium (Optional)

- **File**: `yolov8m.onnx`
- **Size**: ~50 MB
- **Description**: Higher accuracy for demanding applications
- **Accuracy**: High precision
- **Speed**: ~50 FPS on modern CPU

## Downloading/Converting Models

YOLO models need to be converted from PyTorch format to ONNX:

### Option 1: Convert from PyTorch (Recommended)

```bash
# Install ultralytics
pip install ultralytics

# Convert models to ONNX
yolo export model=yolov8n.pt format=onnx
yolo export model=yolov8s.pt format=onnx
yolo export model=yolov8m.pt format=onnx

# Move to resources directory
mv yolov8n.onnx yolo/src/main/resources/models/
mv yolov8s.onnx yolo/src/main/resources/models/
mv yolov8m.onnx yolo/src/main/resources/models/
```

### Option 2: Download Pre-converted ONNX Models

Check Ultralytics releases for pre-converted ONNX models:
https://github.com/ultralytics/assets/releases

## Model Loading Priority

The YOLO backend uses the following priority when loading models:

1. **Classpath resources** (this directory) - highest priority
2. **Configured external path** - if specified in configuration
3. **User cache** (~/.spring-vision/models/yolo/) - for backwards compatibility
4. **Auto-download** - downloads from Ultralytics if not found

## Usage

Configure which model to use:

```yaml
spring:
  vision:
    yolo:
      enabled: true
      modelName: yolov8n.onnx  # or yolov8s.onnx, yolov8m.onnx
      # modelPath: classpath:/models (default)
```

## Supported Classes

YOLOv8 models are trained on COCO dataset with 80 classes:

- Person, bicycle, car, motorcycle, airplane, bus, train, truck, boat
- Traffic light, fire hydrant, stop sign, parking meter, bench
- Bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe
- And 55 more common objects...

Full list: https://github.com/ultralytics/ultralytics/blob/main/ultralytics/cfg/datasets/coco.yaml

## License

YOLOv8 models are provided by Ultralytics under AGPL-3.0 License.
See: https://github.com/ultralytics/ultralytics/blob/main/LICENSE

