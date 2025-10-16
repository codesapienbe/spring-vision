# DJL Migration Guide

This guide helps you migrate from legacy OpenCV/FaceBytes backends to the new DJL-powered Spring Vision.

## What Changed

### ✅ Before (Legacy)

```yaml
spring:
  vision:
    opencv:
      enabled: true
    facebytes:
      enabled: true
```

### ✅ After (DJL)

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: PyTorch
      confidence-threshold: 0.5
```

## Migration Steps

### 1. Update Configuration

Replace your existing `application.yml`:

**CPU (Default)**:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: PyTorch
      confidence-threshold: 0.5
    opencv:
      enabled: false
    facebytes:
      enabled: false
```

**GPU Acceleration**:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: PyTorch
      confidence-threshold: 0.5
spring:
  profiles:
    active: gpu
```

### 2. No Code Changes Required

Your existing code using `VisionTemplate` will work unchanged:

```java

@Autowired
private VisionTemplate visionTemplate;

// This still works exactly the same
List<Detection> faces = visionTemplate.detectFaces(imageBytes);
```

### 3. Benefits After Migration

- ✅ **No manual model downloads** - DJL handles everything automatically
- ✅ **Faster startup** - No heavy OpenCV native libraries by default
- ✅ **Better performance** - Modern inference engines
- ✅ **Automatic model management** - Updates and caching handled by DJL
- ✅ **Smaller footprint** - Optional legacy dependencies

### 4. Rollback Option

If you need to revert to legacy backends:

```yaml
spring:
  vision:
    djl:
      enabled: false
    opencv:
      enabled: true
    facebytes:
      enabled: true
```

## FAQ

**Q: Will my existing API work?**
A: Yes, all existing `VisionTemplate` methods work unchanged.

**Q: What about performance?**
A: DJL typically provides better performance and GPU acceleration options.

**Q: Can I use custom models?**
A: Yes, DJL supports custom model URLs and the DJL model zoo.

**Q: What if I need OpenCV features?**
A: OpenCV backend is still available, just set `spring.vision.opencv.enabled=true`.
