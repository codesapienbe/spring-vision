[Docs Home](../index.md) · [Architecture](../architecture/architecture.md) · [Config](./config.md) · [GPU](./gpu.md)

# Runtime Guide

Basic runtime configuration and troubleshooting for Spring Vision 0.0.4.

## Overview

- Java 21+, Spring Boot 3.2+
- DJL backend with automatic model management
- Simple configuration for development and testing

## Basic Configuration

application.yml:

```yaml
spring:
  vision:
    djl:
      enabled: true
      engine: pytorch
      device: cpu  # or gpu for GPU acceleration
```

## Environment Variables

You can also configure using environment variables:

```bash
export SPRING_VISION_DJL_ENABLED=true
export SPRING_VISION_DJL_ENGINE=pytorch
export SPRING_VISION_DJL_DEVICE=cpu
```

## Health Check

The application includes a basic health check endpoint:

- `GET /api/vision/health` - Check if the vision backend is working

## Troubleshooting

### Common Issues

1. **Models not downloading**: Ensure internet connection for first run
2. **GPU not detected**: Install CUDA drivers and rebuild with `-P gpu` profile
3. **Out of memory**: Reduce concurrent requests or increase JVM heap size

### Logs

Check application logs for detailed error messages:

```bash
tail -f logs/spring-vision.log
```

## Development Tips

- Start with CPU configuration for development
- Use small test images to verify functionality
- Enable debug logging for troubleshooting: `logging.level.io.github.codesapienbe.springvision=DEBUG`
- Batching: Use batch processing for better throughput and GPU utilization

## Getting Help

If you encounter issues:

1. Check the application logs for error messages
2. Verify your configuration matches the examples above
3. Ensure all dependencies are properly resolved
4. Try with a simple test image first

For more detailed troubleshooting, see [Configuration Guide](./config.md) and [Models Guide](./models.md).
