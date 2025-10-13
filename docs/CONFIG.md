# Backend Properties Refactoring Summary

## Overview

Refactored all Vision backend modules to follow the principle of separation of concerns by moving static model metadata and configuration values from backend service classes into dedicated `*Properties` configuration classes.

## Changes Made

### 1. YOLO Backend (`yolo` module)

#### YoloProperties (`yolo/src/main/java/.../config/YoloProperties.java`)

**Added:**

- `modelInfo` map containing static model metadata for YOLO models (yolov8n, yolov8s, yolov8m)
- `ModelInfo` inner class with fields: `url`, `checksum`, `name`
- Configuration fields moved from backend:
    - `modelPath` (default: "classpath:/models")
    - `modelName` (default: "yolov8n.onnx")
    - `confidenceThreshold` (default: 0.25)
    - `nmsThreshold` (default: 0.45)
    - `maxDetections` (default: 100)
    - `enableAutoDownload` (default: true)
    - `downloadTimeoutSeconds` (default: 300)
    - `inputSize` (default: 640)

#### YoloBackend (`yolo/src/main/java/.../YoloBackend.java`)

**Changed:**

- Added constructor dependency injection for `YoloProperties`
- Removed static `MODEL_INFO` map
- Removed static `ModelInfo` inner class
- Removed all local configuration fields (enabled, modelPath, modelName, etc.)
- Updated all references to use `properties.getXxx()` methods instead of local fields
- Updated `getModelFilePath()` to use `properties.getModelInfo()`

**Benefits:**

- Better testability (can inject mock properties)
- Configuration externalized and overridable via application.properties/yml
- Clear separation between business logic and configuration

---

### 2. MediaPipe Backend (`mediapipe` module)

#### MediaPipeProperties (`mediapipe/src/main/java/.../config/MediaPipeProperties.java`)

**Added:**

- `modelInfo` map containing static model metadata for MediaPipe models:
    - `face_detection_short_range.tflite`
    - `hand_landmarker.task`
    - `pose_landmarker_lite.task`
    - `efficientdet_lite0.tflite`
- `ModelInfo` inner class with fields: `url`, `checksum`
- Additional configuration fields:
    - `modelPath` (default: "classpath:/models")
    - `confidenceThreshold` (default: 0.7)
    - `maxDetections` (default: 10)
    - `enableAutoDownload` (default: true)
    - `downloadTimeoutSeconds` (default: 30)
    - `maxPoolSize` (default: 5)
    - `poolTimeoutSeconds` (default: 60)

#### MediaPipeBackend (Future Update Required)

**To Do:**

- Add constructor dependency injection for `MediaPipeProperties`
- Remove static `MODEL_INFO` map
- Remove static `ModelInfo` inner class
- Remove local configuration fields
- Update all references to use properties

---

### 3. InsightFace Backend (`insightface` module)

#### InsightFaceProperties (`insightface/src/main/java/.../config/InsightFaceProperties.java`)

**Added:**

- `modelInfo` map containing static model metadata for InsightFace models:
    - `buffalo_l`
    - `buffalo_m`
    - `buffalo_s`
- `ModelInfo` inner class with fields: `url`, `checksum`, `name`

#### InsightFaceBackend (Future Update Required)

**To Do:**

- Add constructor dependency injection for `InsightFaceProperties`
- Remove static `MODEL_INFO` map
- Remove static `ModelInfo` inner class
- Update references to use properties where applicable

---

## Design Pattern

### Before (Anti-pattern):

```java

@Component
public class YoloBackend {
    // Static model metadata embedded in service
    private static final Map<String, ModelInfo> MODEL_INFO = Map.of(...);

    // Configuration as instance fields
    private String modelPath = "classpath:/models";
    private double confidenceThreshold = 0.25;

    // Business logic mixed with config
}
```

### After (Best Practice):

```java

@Component
public class YoloBackend {
    private final YoloProperties properties;

    public YoloBackend(YoloProperties properties) {
        this.properties = properties;
    }

    // Business logic uses properties.getXxx()
    private void someMethod() {
        double threshold = properties.getConfidenceThreshold();
        Map<String, ModelInfo> models = properties.getModelInfo();
    }
}
```

---

## Benefits of This Refactoring

1. **Separation of Concerns**: Configuration is separated from business logic
2. **Testability**: Easy to inject mock/test configurations
3. **Externalization**: All configuration can be overridden via application.properties/yml
4. **Maintainability**: Single place to manage model URLs, checksums, and defaults
5. **Spring Boot Best Practices**: Follows @ConfigurationProperties pattern
6. **Type Safety**: Configuration is strongly typed with validation
7. **Documentation**: Configuration properties are self-documenting via JavaDoc

---

## Configuration Example

Users can now override defaults in `application.yml`:

```yaml
spring:
  vision:
    yolo:
      enabled: true
      model-name: yolov8m.onnx
      confidence-threshold: 0.3
      max-detections: 50
      model-info:
        custom-model.onnx:
          url: https://example.com/custom-model.onnx
          checksum: sha256:abc123...
          name: custom-model
```

---

## Files Modified

### Completed:

1. `/yolo/src/main/java/.../config/YoloProperties.java` ✅
2. `/yolo/src/main/java/.../YoloBackend.java` ✅
3. `/mediapipe/src/main/java/.../config/MediaPipeProperties.java` ✅
4. `/insightface/src/main/java/.../config/InsightFaceProperties.java` ✅

### Pending (Backend Classes):

1. `/mediapipe/src/main/java/.../MediaPipeBackend.java` ⏳
2. `/insightface/src/main/java/.../InsightFaceBackend.java` ⏳

---

## Next Steps

1. **Update MediaPipeBackend**: Inject `MediaPipeProperties` and remove static MODEL_INFO
2. **Update InsightFaceBackend**: Inject `InsightFaceProperties` and remove static MODEL_INFO
3. **Testing**: Verify all backends work correctly with new properties
4. **Documentation**: Update module READMEs with new configuration options
5. **Migration Guide**: Document for existing users how to migrate configurations

---

## Compilation Status

✅ YoloBackend compiles successfully with new properties structure
✅ All Properties classes compile without errors (only unused constructor warnings for Spring binding)

---

Date: 2025-10-13
Author: Spring Vision Team

