[Docs Home](./index.md) · [Architecture](./architecture.md) · [Config](./config.md) · [GPU](./gpu.md)

# Runtime & Operations Guide

This page covers how to run Spring Vision reliably in production: configuration, health checks, metrics, threading, resource management, and troubleshooting.

## Overview

- Java 21+, Spring Boot 3.2+
- Virtual Threads for scalable concurrency
- Clean lifecycle: proper resource shutdown, health monitoring, and metrics

## Configuration Essentials

application.yml:

```yaml
spring:
  vision:
    enabled: true
    backend: opencv   # or mediapipe, yolo, facebytes, deepface, insightface
    execution-provider: cpu  # or gpu (requires GPU build)
    opencv:
      enabled: true
      confidence-threshold: 0.7
```

Environment variables:

```bash
export VISION_ENABLED=true
export VISION_BACKEND=opencv
export VISION_EXECUTION_PROVIDER=cpu
```

See also: [Config](./config.md) and [GPU](./gpu.md)

## Health & Readiness

- `/actuator/health` – overall health
- `/actuator/health/vision` – backend health and readiness

Typical readiness checks include:

- Backend initialization status
- Model availability
- Thread and queue health

## Metrics

Key metrics to watch:

- vision.detections.total – total detections
- vision.processing.time – processing time histograms
- vision.errors.total – error count
- vision.backend.health – backend status gauge

Export via Micrometer to Prometheus/Grafana for dashboards.

## Threading Model (Virtual Threads)

Spring Vision uses Java 21 Virtual Threads where applicable to improve scalability:

- Async execution: virtual-thread-per-task for high concurrency
- Background tasks (health/metrics): lightweight virtual threads

Benefits:

- Lower memory overhead vs platform threads
- Simplified concurrency (no complex pool tuning)

## Resource Management

- Models: Prefer classpath `classpath:/models` for portability; use external paths for large/custom models
- Cleanup: All native resources and thread-locals are closed on shutdown
- Timeouts: Configure sensible timeouts for long-running tasks
- Batching: Use batch processing for better throughput and GPU utilization

See: [Models Guide](./models.md)

## Docker & Kubernetes

- Use GPU-enabled images only when GPU is required
- Limit resources (CPU/memory) per environment
- Add liveness/readiness probes to use `/actuator/health` endpoints

## Troubleshooting Quick Wins

- Faces not detected: verify image size/quality, adjust thresholds
- Slow performance: downscale images, enable GPU, batch operations
- OOM: increase heap (`-Xmx`), reduce image sizes, limit concurrency
- Model not found: confirm model path or bundling; see [Downloads](./downloads.md)

## Validation Checklist

- Health is UP and vision backend is healthy
- Metrics visible and exported to your observability stack
- Graceful shutdown completes without errors
- Concurrency and timeouts validated under load

---

See also: [Config](./config.md) · [GPU](./gpu.md) · [Models](./models.md) · [Downloads](./downloads.md) · [Start](./start.md)
