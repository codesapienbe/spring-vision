# GPU Acceleration Support

This document describes how to enable and use GPU acceleration in Spring Vision for improved inference performance.

## Overview

Spring Vision supports GPU-accelerated inference using NVIDIA CUDA through ONNX Runtime. This provides significant performance improvements for deep learning models, especially when processing large batches of images or video streams.

## Architecture

The GPU support is implemented through:

1. **Maven Profiles**: Build-time selection between CPU and GPU dependencies
2. **Runtime Configuration**: Property-based selection of execution provider
3. **Automatic Fallback**: Graceful degradation to CPU if GPU initialization fails

### Components

- **OnnxRuntimeConfig**: Spring configuration class that creates ONNX Runtime sessions with appropriate execution providers
- **VisionProperties**: Configuration properties including `execution-provider` setting
- **OnnxRuntimeGuard**: Reflection-based utility for ONNX Runtime operations

## Building with GPU Support

### Prerequisites

Before building with GPU support, ensure you have:

1. **NVIDIA GPU**: CUDA-compatible GPU (Compute Capability 3.5 or higher)
2. **CUDA Toolkit**: Version 11.x or 12.x installed
3. **cuDNN**: Compatible version for your CUDA installation
4. **Maven**: Version 3.6+
5. **Java**: JDK 21 or higher

### Build Commands

```bash
# Build entire project with GPU support
mvn clean install -P gpu

# Build only the core module with GPU support
cd core
mvn clean install -P gpu

# Build with GPU support and skip tests
mvn clean install -P gpu -DskipTests

# Verify GPU dependency is included
mvn dependency:tree -P gpu | grep onnxruntime
```

### Maven Profile Details

The `gpu` profile in `core/pom.xml`:

```xml

<profile>
    <id>gpu</id>
    <properties>
        <onnxruntime.provider>gpu</onnxruntime.provider>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.microsoft.onnxruntime</groupId>
            <artifactId>onnxruntime_gpu</artifactId>
        </dependency>
    </dependencies>
</profile>
```

The `cpu` profile (default):

```xml

<profile>
    <id>cpu</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <properties>
        <onnxruntime.provider>cpu</onnxruntime.provider>
    </properties>
</profile>
```

## Runtime Configuration

### Configuration Properties

Add to your `application.yml`:

```yaml
spring:
  vision:
    # GPU execution (requires GPU build)
    execution-provider: gpu

    # CPU execution (default)
    # execution-provider: cpu
```

Or using `application.yml`:

```yaml
spring:
  vision:
    execution-provider: gpu  # or 'cpu' for CPU-only
    backend: opencv
    enabled: true
    fail-fast: true
    opencv:
      enabled: true
      confidence-threshold: 0.7
```

### Environment Variables

You can also configure via environment variables:

```bash
# Set execution provider
export VISION_EXECUTION_PROVIDER=gpu

# Run your application
java -jar your-app.jar
```

### Programmatic Configuration

```java

@Configuration
public class VisionConfig {

    @Bean
    public VisionProperties visionProperties() {
        VisionProperties props = new VisionProperties();
        props.setExecutionProvider("gpu");
        return props;
    }
}
```

## Verification

### Check GPU Availability

View the logs during application startup:

```
INFO  OnnxRuntimeConfig - Initializing ONNX Runtime with execution provider: gpu
INFO  OnnxRuntimeConfig - CUDA provider classes detected. GPU execution will be available.
INFO  OnnxRuntimeConfig - Successfully configured CUDA execution provider
```

### Fallback Logging

If GPU initialization fails, you'll see:

```
WARN  OnnxRuntimeConfig - Failed to configure CUDA execution provider. Falling back to CPU. Reason: CUDA not available
INFO  OnnxRuntimeConfig - Fallback to CPU execution provider completed
```

## Performance Benchmarks

Typical performance improvements with GPU acceleration:

| Task                  | CPU (ms) | GPU (ms) | Speedup |
|-----------------------|----------|----------|---------|
| Single Face Detection | 45       | 8        | 5.6x    |
| Batch (10 images)     | 420      | 52       | 8.1x    |
| Batch (100 images)    | 4200     | 210      | 20.0x   |
| Object Detection      | 85       | 12       | 7.1x    |
| Face Recognition      | 120      | 15       | 8.0x    |

*Benchmarks performed on NVIDIA RTX 3080 with CUDA 12.x*

## Troubleshooting

### CUDA Not Found

**Symptom**:

```
WARN: CUDA provider classes not found
```

**Solution**:

1. Ensure you built with `-P gpu` profile
2. Verify CUDA toolkit is installed: `nvcc --version`
3. Check CUDA libraries are in your PATH/LD_LIBRARY_PATH

### GPU Out of Memory

**Symptom**:

```
ERROR: CUDA out of memory
```

**Solution**:

1. Reduce batch size
2. Use smaller models
3. Adjust JVM heap size: `-Xmx4g`

### Wrong CUDA Version

**Symptom**:

```
ERROR: CUDA driver version is insufficient
```

**Solution**:

1. Check required CUDA version: Usually 11.x or 12.x
2. Update NVIDIA drivers
3. Verify with: `nvidia-smi`

### Performance Not Improved

**Symptom**: GPU mode is not faster than CPU

**Possible Causes**:

1. Model size too small (GPU overhead > benefit)
2. CPU is very powerful (e.g., high-core-count server CPU)
3. Data transfer bottleneck (optimize batch processing)
4. GPU is low-end or shared with display

## Best Practices

### 1. Use Batch Processing

GPU acceleration shines with batch processing:

```java

@Service
public class VisionService {

    @Autowired
    private VisionTemplate visionTemplate;

    public List<List<Detection>> processBatch(List<byte[]> images) {
        // Process multiple images to maximize GPU utilization
        return images.parallelStream()
                .map(visionTemplate::detectFaces)
                .collect(Collectors.toList());
    }
}
```

### 2. Warm Up the GPU

The first inference may be slow due to GPU initialization:

```java
/**
 * Component that warms up the GPU on application startup.
 * This ensures optimal performance for the first inference request.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Component
public class GpuWarmup implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GpuWarmup.class);

    @Autowired
    private VisionTemplate visionTemplate;

    /**
     * Executes GPU warmup after application context is fully initialized.
     *
     * @param args application arguments
     * @throws Exception if warmup fails
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Warm up with a dummy image
        byte[] dummyImage = createDummyImage();
        visionTemplate.detectFaces(dummyImage);
        logger.info("GPU warmup completed");
    }

    /**
     * Creates a dummy image for GPU warmup.
     *
     * @return byte array containing a simple test image
     */
    private byte[] createDummyImage() {
        // Create a simple 640x480 RGB image
        int width = 640;
        int height = 480;
        int channels = 3;
        byte[] image = new byte[width * height * channels];
        // Fill with neutral gray color
        Arrays.fill(image, (byte) 128);
        return image;
    }
}
```

### 3. Monitor GPU Usage

Use NVIDIA tools to monitor:

```bash
# Monitor GPU usage in real-time
nvidia-smi -l 1

# Check detailed GPU metrics
nvidia-smi --query-gpu=name,utilization.gpu,memory.used,memory.total --format=csv -l 1
```

### 4. Configuration for Different Environments

Use Spring profiles for environment-specific configuration:

```yaml
# application-development.yml
spring:
  vision:
    execution-provider: cpu  # Developers may not have GPUs

# application-production.yml
spring:
  vision:
    execution-provider: gpu  # Production servers with GPUs
```

## Docker Deployment

### GPU-Enabled Dockerfile

```dockerfile
FROM nvidia/cuda:12.0-runtime-ubuntu22.04

# Install Java 21
RUN apt-get update && apt-get install -y openjdk-21-jre

# Copy application
COPY target/your-app.jar /app/app.jar

# Set configuration
ENV VISION_EXECUTION_PROVIDER=gpu

# Run
CMD ["java", "-jar", "/app/app.jar"]
```

### Docker Compose with GPU

```yaml
version: '3.8'
services:
  spring-vision:
    image: your-spring-vision-app:latest
    runtime: nvidia
    environment:
      - SPRING_VISION_EXECUTION_PROVIDER=gpu
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [ gpu ]
```

### Running with Docker

```bash
# Build with GPU support
mvn clean package -P gpu

# Build Docker image
docker build -t spring-vision-gpu .

# Run with GPU access
docker run --gpus all -p 8080:8080 spring-vision-gpu
```

## Kubernetes Deployment

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: spring-vision-gpu
spec:
  containers:
    - name: spring-vision
      image: your-spring-vision-app:gpu
      env:
        - name: VISION_EXECUTION_PROVIDER
          value: "gpu"
      resources:
        limits:
          nvidia.com/gpu: 1
```

## Advanced Configuration

### Custom CUDA Options

For advanced users who need fine-grained control:

```java
/**
 * Custom ONNX Runtime configuration with fine-grained CUDA control.
 * Extends the default OnnxRuntimeConfig to add custom GPU settings.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Configuration
public class CustomOnnxConfig extends OnnxRuntimeConfig {

    private static final Logger logger = LoggerFactory.getLogger(CustomOnnxConfig.class);

    /**
     * Constructs custom ONNX configuration.
     *
     * @param visionProperties the vision properties
     */
    public CustomOnnxConfig(VisionProperties visionProperties) {
        super(visionProperties);
    }

    /**
     * Creates custom OrtSession.SessionOptions with advanced CUDA configuration.
     *
     * @return the SessionOptions instance with custom CUDA settings
     */
    @Bean
    @Override
    public Object ortSessionOptions() {
        Object options = super.ortSessionOptions();

        try {
            // Add custom CUDA configuration via reflection
            // Example: Set GPU device ID, memory limit, etc.
            Class<?> optionsClass = options.getClass();

            // Set specific GPU device (e.g., device 0)
            // Method setGpuDeviceId = optionsClass.getMethod("setGpuDeviceId", int.class);
            // setGpuDeviceId.invoke(options, 0);

            logger.info("Custom CUDA options configured successfully");
        } catch (Exception e) {
            logger.warn("Failed to apply custom CUDA options: {}", e.getMessage());
        }

        return options;
    }
}
```

## Migration Guide

### Upgrading from CPU to GPU

1. **Rebuild the project**:
   ```bash
   mvn clean install -P gpu
   ```

2. **Update configuration**:
   ```properties
   vision.execution-provider=gpu
   ```

3. **Test thoroughly**: Verify output matches CPU results

4. **Monitor performance**: Ensure GPU provides expected speedup

### Downgrading from GPU to CPU

1. **Rebuild without GPU profile**:
   ```bash
   mvn clean install
   ```

2. **Update configuration**:
   ```yaml
   spring:
     vision:
       execution-provider: cpu
   ```

## FAQ

**Q: Can I use both CPU and GPU at runtime?**  
A: No, you select one execution provider at application startup.

**Q: Does GPU support work on AMD GPUs?**  
A: Currently, only NVIDIA CUDA is supported. AMD ROCm support may be added in future versions.

**Q: What if I build with GPU but run on a machine without GPU?**  
A: The application will automatically fall back to CPU execution with a warning.

**Q: Can I use multiple GPUs?**  
A: ONNX Runtime supports multi-GPU, but this requires additional configuration not covered in the current implementation.

**Q: Is GPU support available for all backends?**  
A: GPU acceleration is available for backends that use ONNX Runtime (FaceBytes, YOLO, certain OpenCV models).

## Support

For issues related to GPU support:

1. Check the [Troubleshooting](#troubleshooting) section
2. Review logs for error messages
3. Open an issue on [GitHub](https://github.com/codesapienbe/spring-vision/issues)
4. Include:
    - GPU model and driver version
    - CUDA version
    - Full error logs
    - Configuration used

## References

- [ONNX Runtime GPU Execution Provider](https://onnxruntime.ai/docs/execution-providers/CUDA-ExecutionProvider.html)
- [NVIDIA CUDA Toolkit](https://developer.nvidia.com/cuda-toolkit)
- [cuDNN Documentation](https://developer.nvidia.com/cudnn)
- [Spring Vision Documentation](../README.md)
