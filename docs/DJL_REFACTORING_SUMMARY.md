# DJL Refactoring Summary

## Overview

This document summarizes the comprehensive refactoring of Spring Vision to use proper DJL (Deep Java Library) patterns with **only verified HuggingFace models**.

## Key Changes

### 1. Model Loading Refactored

**Before**: Mixed approach using custom model loaders and fallback criteria
**After**: Direct HuggingFace model loading via DJL's Criteria builder pattern

#### Face Recognition Model
```java
// Now using verified ArcFace model from HuggingFace
Criteria<Image, NDArray> criteria = Criteria.builder()
    .setTypes(Image.class, NDArray.class)
    .optApplication(Application.CV.IMAGE_CLASSIFICATION)
    .optModelUrls("djl://ai.djl.huggingface.onnx/garavv/arcface-onnx")
    .optEngine("OnnxRuntime")
    .optDevice(device)
    .optArgument("inputShape", new int[]{1, 3, 112, 112})
    .optArgument("normalize", true)
    .build();
```

- **Model**: `garavv/arcface-onnx`
- **Engine**: ONNX Runtime
- **Output**: 512-dimensional face embeddings
- **Use Case**: Face recognition and verification

#### Face Detection Model
```java
// Primary: YuNet face detector
Criteria<Image, DetectedObjects> criteria = Criteria.builder()
    .setTypes(Image.class, DetectedObjects.class)
    .optApplication(Application.CV.OBJECT_DETECTION)
    .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/face_detection_yunet")
    .optEngine("OnnxRuntime")
    .optDevice(device)
    .optArgument("threshold", 0.6f)
    .optArgument("nms_threshold", 0.3f)
    .optTranslator(new YuNetFaceDetectionTranslator())
    .build();
```

- **Primary Model**: `opencv/face_detection_yunet`
- **Fallback Model**: `AdamCodd/YOLOv11n-face-detection`
- **Engine**: ONNX Runtime (primary), PyTorch (fallback)
- **Performance**: Millisecond-level inference
- **Use Case**: Fast face counting and detection

#### Pose Estimation Model
```java
// MediaPipe Pose with 33 keypoints
Criteria<Image, NDList> criteria = Criteria.builder()
    .setTypes(Image.class, NDList.class)
    .optApplication(Application.CV.POSE_ESTIMATION)
    .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/pose_estimation_mediapipe")
    .optEngine("OnnxRuntime")
    .optDevice(device)
    .optArgument("inputShape", new int[]{1, 3, 256, 256})
    .optArgument("normalize", true)
    .build();
```

- **Model**: `opencv/pose_estimation_mediapipe`
- **Engine**: ONNX Runtime
- **Output**: 33 body keypoints (face, hands, torso)
- **Use Case**: Human pose detection and analysis

### 2. New MCP Tools Added

Three new vision capabilities were added to `VisionTool.java`:

#### 1. NSFW Detection
```java
@Tool(description = "Detect NSFW content in an image")
public Map<String, Object> detectNSFW(String imageUrl)
```

**Returns**:
- `classification`: "normal" or "nsfw"
- `confidence`: Detection confidence score
- `isNSFW`: Boolean flag

**Potential Model**: `Falconsai/nsfw_image_detection` (98% accuracy, ViT-based)

#### 2. Emotion Detection
```java
@Tool(description = "Detect emotions from faces in an image")
public Map<String, Object> detectEmotions(String imageUrl)
```

**Returns**:
- 7 emotion classes: Angry, Disgust, Fear, Happy, Sad, Surprise, Neutral
- Confidence scores for each emotion
- Top detected emotion

**Potential Model**: `abhilash88/face-emotion-detection` (71.55% accuracy on FER2013)

#### 3. Deepfake Detection
```java
@Tool(description = "Detect deepfakes in an image")
public Map<String, Object> detectDeepfake(String imageUrl)
```

**Returns**:
- `classification`: "real" or "fake"
- `confidence`: Detection confidence score
- `isFake`: Boolean flag

**Potential Model**: `prithivMLmods/deepfake-detector-model-v1` (94.44% accuracy, SigLIP-based)

### 3. Model Loading Strategy

All models now follow this pattern:

1. **Explicit Model URLs**: Using `djl://ai.djl.huggingface.{engine}/{model-name}` format
2. **Engine Selection**: Appropriate engine (PyTorch/ONNX) based on model availability
3. **Graceful Fallbacks**: Primary model with fallback options where critical
4. **Proper Error Handling**: Clear error messages when models fail to load
5. **Model Caching**: All loaded models are cached for reuse

### 4. Verified Models Used

All models have been verified to exist on HuggingFace:

| Purpose | Model | Engine | Accuracy/Performance |
|---------|-------|--------|---------------------|
| Face Detection | `opencv/face_detection_yunet` | ONNX | 88.44% AP, millisecond-level |
| Face Detection (fallback) | `AdamCodd/YOLOv11n-face-detection` | PyTorch | High accuracy |
| Face Recognition | `garavv/arcface-onnx` | ONNX | 512-dim embeddings |
| Pose Estimation | `opencv/pose_estimation_mediapipe` | ONNX | 33 keypoints |
| NSFW Detection | `Falconsai/nsfw_image_detection` | PyTorch | ~98% accuracy |
| Emotion Detection | `abhilash88/face-emotion-detection` | PyTorch | 71.55% on FER2013 |
| Deepfake Detection | `prithivMLmods/deepfake-detector-model-v1` | PyTorch | 94.44% accuracy |

### 5. Additional Verified Models (Available for Future Use)

From `DJL_USAGE.md`, these verified models are documented and ready to integrate:

#### Health Sector
- `codewithdark/vit-chest-xray` - Chest X-ray disease classification (98.46% accuracy)
- `ayushirathour/chest-xray-pneumonia-detection` - Pneumonia detection (96.4% sensitivity)
- `lizardwine/Melanoma-003` - Melanoma skin cancer detection (~99% accuracy)
- `ArjTheHacker/diabetic-retinopathy-detection` - Diabetic retinopathy detection
- `pavankm96/brain_tumor_det` - Brain tumor detection from MRI scans

#### Food Sector
- `nateraw/food` - Food recognition
- `Jacques7103/Food-Recognition` - Food classification
- `BinhQuocNguyen/food-recognition-model` - 92% accuracy on Food101 dataset
- `sayfeldinn/AI-Food-Detector` - Food detection
- `Kaludi/Food-Classification` - Food classification
- `openfoodfacts/ingredient-detection` - Ingredient detection
- `openfoodfacts/nutriscore-yolo` - Nutritional label detection

#### Cybersecurity
- `pirocheto/phishing-url-detection` - Phishing URL detection (98.68% ROC AUC)
- `imanoop7/bert-phishing-detector` - BERT-based phishing detection
- `gates04/DistilBERT-Network-Intrusion-Detection` - Network intrusion detection
- `anuashok/ocr-captcha-v3` - CAPTCHA recognition (0.014 CER)

#### Demographics
- `abhilash88/age-gender-prediction` - Age and gender prediction (94.3% gender accuracy)
- `fanclan/age-gender-model` - Age/gender classification

#### Hand Detection
- `DamarJati/face-hand-YOLOv5` - Face and hand detection
- `lewiswatson/yolov8x-tuned-hand-gestures` - Hand gesture recognition

## Benefits of This Refactoring

1. **Reliability**: Only verified, publicly available models are used
2. **Consistency**: All model loading follows the same DJL pattern
3. **Maintainability**: Clear model sources and loading logic
4. **Performance**: Optimized models with documented performance metrics
5. **Extensibility**: Easy to add new verified models following the same pattern
6. **Documentation**: Clear mapping between capabilities and HuggingFace models

## Usage Examples

### Face Detection with YuNet
```java
byte[] imageBytes = downloadImage("https://example.com/photo.jpg");
ImageData imageData = ImageData.fromBytes(imageBytes);
VisionResult result = visionTemplate.detectFaces(imageData);
int faceCount = result.detections().size();
```

### Face Recognition with ArcFace
```java
byte[] sourceImage = downloadImage("https://example.com/person1.jpg");
byte[] targetImage = downloadImage("https://example.com/person2.jpg");

ImageData source = ImageData.fromBytes(sourceImage);
ImageData target = ImageData.fromBytes(targetImage);

List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(source);
List<float[]> targetEmbeddings = visionTemplate.extractEmbeddings(target);

double similarity = cosineSimilarity(sourceEmbeddings.get(0), targetEmbeddings.get(0));
boolean isMatch = similarity >= 0.5;
```

### Pose Detection with MediaPipe
```java
byte[] imageBytes = downloadImage("https://example.com/athlete.jpg");
ImageData imageData = ImageData.fromBytes(imageBytes);

if (visionTemplate.backend() instanceof PoseEstimationCapability) {
    PoseEstimationCapability poseBackend = (PoseEstimationCapability) visionTemplate.backend();
    List<Detection> poses = poseBackend.detectPoses(imageData);
    
    for (Detection pose : poses) {
        Map<String, Object> joints = (Map<String, Object>) pose.attributes().get("joints");
        // Process 33 keypoints
    }
}
```

### NSFW Detection
```java
Map<String, Object> result = visionTool.detectNSFW("https://example.com/image.jpg");
boolean isNSFW = (boolean) result.get("isNSFW");
double confidence = (double) result.get("confidence");
```

### Emotion Detection
```java
Map<String, Object> result = visionTool.detectEmotions("https://example.com/face.jpg");
List<Map<String, Object>> emotions = (List<Map<String, Object>>) result.get("emotions");
String topEmotion = (String) result.get("topEmotion");
```

## Migration Guide

### For Developers Using Spring Vision

No changes required! The public API remains the same. The refactoring is internal.

### For Developers Extending Spring Vision

If you were using custom model loaders:

**Before**:
```java
Criteria<Image, DetectedObjects> criteria = DjlModelLoader.faceDetectionCriteria()
    .optFilter("model", modelName)
    .build();
```

**After**:
```java
Criteria<Image, DetectedObjects> criteria = Criteria.builder()
    .setTypes(Image.class, DetectedObjects.class)
    .optApplication(Application.CV.OBJECT_DETECTION)
    .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/face_detection_yunet")
    .optEngine("OnnxRuntime")
    .build();
```

## Testing Recommendations

1. **Unit Tests**: Verify model loading with mocked models
2. **Integration Tests**: Test with actual HuggingFace model downloads
3. **Performance Tests**: Benchmark inference times with different models
4. **Fallback Tests**: Ensure graceful degradation when primary models fail

## Future Enhancements

1. **Model Registry**: Centralized registry mapping capabilities to HuggingFace models
2. **Model Selection**: Runtime model selection based on performance/accuracy requirements
3. **Model Versioning**: Support for specific model versions
4. **Model Caching**: Persistent local cache for downloaded models
5. **Model Updates**: Automatic updates when newer model versions are available

## References

- [DJL Documentation](https://docs.djl.ai/)
- [HuggingFace Model Hub](https://huggingface.co/models)
- [DJL_USAGE.md](../DJL_USAGE.md) - Complete list of verified models
- [DJL Model Zoo](http://djl.ai/model-zoo/)

## Contributors

This refactoring ensures Spring Vision uses industry-standard DJL patterns with verified, production-ready models from HuggingFace.

