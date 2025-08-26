# Spring Vision Deployment Guide

## Overview

This guide provides comprehensive deployment instructions for the Spring Vision framework across various cloud platforms and environments.

## Quick Start

### Docker Deployment

```bash
# Build the application
docker build -t spring-vision .

# Run with default configuration
docker run -p 8080:8080 spring-vision

# Run with custom configuration
docker run -p 8080:8080 \
  -e SPRING_VISION_OPENCV_ENABLED=true \
  -e SPRING_VISION_MEDIAPIPE_ENABLED=true \
  spring-vision
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-vision
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-vision
  template:
    metadata:
      labels:
        app: spring-vision
    spec:
      containers:
      - name: spring-vision
        image: spring-vision:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

## Cloud Platform Deployments

### AWS Deployment

#### ECS/Fargate
```yaml
# task-definition.json
{
  "family": "spring-vision",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "spring-vision",
      "image": "spring-vision:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/spring-vision",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### AWS Lambda
```yaml
# serverless.yml
service: spring-vision

provider:
  name: aws
  runtime: java11
  memorySize: 2048
  timeout: 30

functions:
  vision:
    handler: com.springvision.lambda.VisionHandler::handleRequest
    events:
      - http:
          path: /detect
          method: post
    environment:
      SPRING_PROFILES_ACTIVE: lambda
```

### Google Cloud Platform

#### Cloud Run
```bash
# Deploy to Cloud Run
gcloud run deploy spring-vision \
  --image gcr.io/PROJECT_ID/spring-vision \
  --platform managed \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --concurrency 80 \
  --max-instances 10
```

#### GKE (Kubernetes)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-vision-service
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
  selector:
    app: spring-vision
```

### Azure

#### Azure Container Instances
```bash
az container create \
  --resource-group myResourceGroup \
  --name spring-vision \
  --image spring-vision:latest \
  --dns-name-label spring-vision \
  --ports 8080 \
  --memory 2 \
  --cpu 2
```

#### Azure Kubernetes Service
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-vision
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-vision
  template:
    metadata:
      labels:
        app: spring-vision
    spec:
      containers:
      - name: spring-vision
        image: spring-vision:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "azure"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

## Environment-Specific Configurations

### Production Configuration

```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
  
  vision:
    opencv:
      enabled: true
      cascade-path: /opt/opencv/cascades
    mediapipe:
      enabled: true
      model-path: /opt/mediapipe/models
    yolo:
      enabled: true
      model-path: /opt/yolo/models
    insightface:
      enabled: true
      api-url: https://insightface-service.example.com

logging:
  level:
    com.springvision: INFO
    root: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### Development Configuration

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  
  vision:
    opencv:
      enabled: true
    mediapipe:
      enabled: false
    yolo:
      enabled: false
    insightface:
      enabled: false

logging:
  level:
    com.springvision: DEBUG
    root: INFO

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

## Monitoring and Observability

### Prometheus Metrics

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-vision'
    static_configs:
      - targets: ['spring-vision:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Spring Vision Metrics",
    "panels": [
      {
        "title": "Detection Requests",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(vision_requests_total[5m])",
            "legendFormat": "{{backend}}"
          }
        ]
      },
      {
        "title": "Processing Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(vision_processing_time_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      }
    ]
  }
}
```

## Security Configuration

### SSL/TLS Configuration

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: spring-vision
```

### Security Headers

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers()
                .frameOptions().deny()
                .contentTypeOptions()
                .and()
                .httpStrictTransportSecurity()
                .and()
                .xssProtection()
                .and()
                .contentSecurityPolicy("default-src 'self'");
        
        return http.build();
    }
}
```

## Performance Optimization

### JVM Tuning

```bash
# Production JVM options
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

### Resource Limits

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

## Troubleshooting

### Common Issues

1. **Memory Issues**
   ```bash
   # Check memory usage
   docker stats spring-vision
   
   # Increase memory limit
   docker run -m 4g spring-vision
   ```

2. **Model Loading Issues**
   ```bash
   # Check model paths
   docker exec spring-vision ls -la /opt/models
   
   # Verify model downloads
   docker logs spring-vision | grep "Model downloaded"
   ```

3. **Performance Issues**
   ```bash
   # Check CPU usage
   docker stats spring-vision
   
   # Monitor JVM metrics
   curl http://localhost:8080/actuator/metrics/jvm.memory.used
   ```

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Backend health
curl http://localhost:8080/actuator/health/vision-backend

# Custom health check
curl http://localhost:8080/actuator/health/vision
```

## Backup and Recovery

### Configuration Backup

```bash
# Backup configuration
kubectl get configmap spring-vision-config -o yaml > config-backup.yaml

# Restore configuration
kubectl apply -f config-backup.yaml
```

### Data Backup

```bash
# Backup model files
tar -czf models-backup.tar.gz /opt/models/

# Backup logs
tar -czf logs-backup.tar.gz /var/log/spring-vision/
```

## Scaling Strategies

### Horizontal Scaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: spring-vision-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: spring-vision
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Vertical Scaling

```yaml
resources:
  requests:
    memory: "4Gi"
    cpu: "2000m"
  limits:
    memory: "8Gi"
    cpu: "4000m"
```

This deployment guide provides comprehensive instructions for deploying Spring Vision across various cloud platforms with proper monitoring, security, and performance optimization.
