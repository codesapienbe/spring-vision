# Spring Vision vLLM Backend

vLLM backend implementation for Spring Vision with Granite 3.2 Vision model support and optimized ONNX Runtime integration.

## Features

### Server Mode (via Docker/vLLM)

- Connect to external vLLM server via REST API
- Support for Granite 3.2 Vision and other vision language models
- Object detection and text extraction using natural language
- Scene analysis and visual question answering
- Configurable sampling parameters (temperature, top-p, max tokens)

### Embedded Mode (ONNX Runtime with DJL)

- Run optimized ONNX models directly in JVM without Docker
- Support for highly compressed models (SqueezeNet ~0.5MB, MobileNetV3 ~6MB)
- Hardware acceleration via Execution Providers (OpenVINO for Intel CPUs, CUDA/TensorRT for GPUs)
- Production-grade inference with DJL Translator pattern
- Automatic model downloading using core ModelResourceLoader

## Quick Start

### Maven Dependency

```xml

<dependency>
    <groupId>io.github.codesapienbe.springvision</groupId>
    <artifactId>vllm</artifactId>
    <version>1.0.3</version>
</dependency>
```

### Server Mode Configuration

```properties
spring.vision.vllm.enabled=true
spring.vision.vllm.base-url=http://localhost:8000
spring.vision.vllm.model=ibm-granite/granite-3.2-8b-instruct
```

Start vLLM server:

```bash
docker-compose up -d
```

### Embedded Mode Configuration (DJL + ONNX Runtime)

```properties
spring.vision.vllm.embedded.djl.enabled=true
spring.vision.vllm.embedded.djl.model-path=models/squeezenet_int8.onnx
spring.vision.vllm.embedded.djl.execution-provider=OpenVINO
```

## Usage Examples

### Server Mode

```java

@Autowired
private GraniteVisionBackend backend;

public List<Detection> analyzeImage(byte[] imageData) {
    ImageData image = ImageData.fromBytes(imageData);
    return backend.detectObjects(image);
}
```

### Embedded Mode (ONNX)

```java

@Autowired
private OptimizedDjlVisionBackend backend;

public List<Detection> classifyImage(byte[] imageData) {
    ImageData image = ImageData.fromBytes(imageData);
    return backend.detectObjects(image);
}
```

## Supported Models

**Server Mode:** Granite 3.2 Vision (8B/2B), LLaVA, Qwen-VL, and other vLLM-compatible vision models

**Embedded Mode:**

- SqueezeNet v1.1 (INT8 quantized ~0.5MB) - Absolute minimum size
- MobileNetV3-Large (W8A16 ~6.35MB) - Balanced efficiency
- EfficientNet-LITE (~12.46MB) - Superior accuracy

## Configuration Properties

### Server Mode

| Property                         | Default                               | Description                |
|----------------------------------|---------------------------------------|----------------------------|
| `spring.vision.vllm.enabled`     | `false`                               | Enable vLLM server backend |
| `spring.vision.vllm.base-url`    | `http://localhost:8000`               | vLLM API URL               |
| `spring.vision.vllm.model`       | `ibm-granite/granite-3.2-8b-instruct` | Model identifier           |
| `spring.vision.vllm.max-tokens`  | `512`                                 | Maximum tokens to generate |
| `spring.vision.vllm.temperature` | `0.7`                                 | Sampling temperature       |

### Embedded Mode (DJL)

| Property                                             | Default                       | Description             |
|------------------------------------------------------|-------------------------------|-------------------------|
| `spring.vision.vllm.embedded.djl.enabled`            | `false`                       | Enable DJL ONNX backend |
| `spring.vision.vllm.embedded.djl.model-path`         | `models/squeezenet_int8.onnx` | Path to ONNX model      |
| `spring.vision.vllm.embedded.djl.execution-provider` | `OpenVINO`                    | EP for acceleration     |
| `spring.vision.vllm.embedded.djl.input-size`         | `224`                         | Input image size        |

## CHANGELOG

### Version 1.0.3 (October 2025)

- ✅ Added Granite 3.2 Vision backend with vLLM server support
- ✅ Added optimized ONNX Runtime backend with DJL integration
- ✅ Support for INT8 quantized models with hardware acceleration
- ✅ Execution Provider strategy (OpenVINO, CUDA, TensorRT) for optimal performance
- ✅ Production-grade Translator pattern with strict pre-processing contracts
- ✅ Model downloading support via core ModelResourceLoader utility
- ✅ Docker Compose configuration for easy vLLM server deployment
- ✅ Comprehensive examples and configuration templates

## Requirements

**Server Mode:** Java 21+, Spring Boot 3.2.8+, Docker (for vLLM server), GPU recommended

**Embedded Mode:** Java 21+, Spring Boot 3.2.8+, DJL, ONNX Runtime, ONNX models

## License

MIT License - Part of Spring Vision framework
