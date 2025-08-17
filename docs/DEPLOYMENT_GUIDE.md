# Spring Vision Deployment Guide

Comprehensive guide for deploying Spring Vision applications in various environments.

## Table of Contents

1. [Overview](#overview)
2. [Development Environment](#development-environment)
3. [Production Deployment](#production-deployment)
4. [Docker Deployment](#docker-deployment)
5. [Kubernetes Deployment](#kubernetes-deployment)
6. [Cloud Deployment](#cloud-deployment)
7. [Performance Optimization](#performance-optimization)
8. [Security Hardening](#security-hardening)
9. [Monitoring and Observability](#monitoring-and-observability)
10. [Troubleshooting](#troubleshooting)

## Overview

This guide covers deploying Spring Vision applications across different environments, from development to production, including containerized and cloud deployments.

### Deployment Options

- **Development**: Local development with hot reload
- **Production**: Traditional server deployment
- **Docker**: Containerized deployment
- **Kubernetes**: Orchestrated container deployment
- **Cloud**: AWS, Azure, GCP deployment

## Development Environment

### Local Development Setup

1. **Clone the Repository**

   ```bash
   git clone https://github.com/spring-vision/spring-vision.git
   cd spring-vision
   ```

2. **Build the Project**

   ```bash
   mvn clean install
   ```

3. **Run the Application**

   ```bash
   mvn spring-boot:run -pl spring-vision-starter
   ```

4. **Access the Application**
   - Application: <http://localhost:8080>
   - Health Check: <http://localhost:8080/actuator/health>
   - API Documentation: <http://localhost:8080/api/vision/health>

### Container-First Configuration

The application uses environment variables for configuration. No separate profile files are needed:

```yaml
# application.yml (single configuration file)
spring:
  application:
    name: spring-vision-application

vision:
  enabled: true
  backend: ${VISION_BACKEND:opencv}
  opencv:
    confidence-threshold: ${VISION_CONFIDENCE_THRESHOLD:0.7}
    gpu-acceleration: ${VISION_GPU_ACCELERATION:false}

logging:
  level:
    com.springvision: ${LOG_LEVEL:INFO}
    org.springframework.web: ${WEB_LOG_LEVEL:INFO}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: ${HEALTH_SHOW_DETAILS:when-authorized}
```

**Environment Variables for Development:**

```bash
LOG_LEVEL=DEBUG
WEB_LOG_LEVEL=DEBUG
HEALTH_SHOW_DETAILS=always
VISION_CONFIDENCE_THRESHOLD=0.7
```

### IDE Configuration

#### IntelliJ IDEA

1. **Import Project**
   - File → Open → Select `pom.xml`
   - Import as Maven project

2. **Run Configuration**
   - Run → Edit Configurations
   - Add Spring Boot configuration
   - Main class: `com.springvision.starter.SampleVisionApplication`
   - VM options: `-Xmx2g -Xms1g`

3. **Debug Configuration**
   - Set breakpoints in controller methods
   - Use debug mode for step-by-step execution

#### Eclipse

1. **Import Project**
   - File → Import → Maven → Existing Maven Projects
   - Select project root directory

2. **Run Configuration**
   - Run → Run Configurations
   - Spring Boot App
   - Main class: `com.springvision.starter.SampleVisionApplication`

### Development Tools

#### Hot Reload

Add Spring Boot DevTools for hot reload:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

#### Database (Optional)

For persistent storage, add a database:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:visiondb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  h2:
    console:
      enabled: true
```

## Production Deployment

### Traditional Server Deployment

#### 1. Build the Application

```bash
mvn clean package -DskipTests
```

#### 2. Prepare the Server

**System Requirements**:

- Java 21+
- 4GB+ RAM
- Multi-core CPU
- 10GB+ storage

**Install Java**:

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# CentOS/RHEL
sudo yum install java-21-openjdk-devel

# macOS
brew install openjdk@21
```

#### 3. Deploy the Application

```bash
# Copy JAR file
scp target/spring-vision-starter-1.0.0-SNAPSHOT.jar user@server:/opt/spring-vision/

# Create systemd service
sudo nano /etc/systemd/system/spring-vision.service
```

**Systemd Service Configuration**:

```ini
[Unit]
Description=Spring Vision Application
After=network.target

[Service]
Type=simple
User=spring-vision
ExecStart=/usr/bin/java -Xmx4g -Xms2g -jar /opt/spring-vision/spring-vision-starter-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom"

[Install]
WantedBy=multi-user.target
```

**Start the Service**:

```bash
sudo systemctl daemon-reload
sudo systemctl enable spring-vision
sudo systemctl start spring-vision
sudo systemctl status spring-vision
```

#### 4. Production Configuration

Use environment variables for production configuration. No separate profile files needed:

**Production Environment Variables:**
```bash
# Vision Configuration
VISION_CONFIDENCE_THRESHOLD=0.8
VISION_GPU_ACCELERATION=true
VISION_MAX_IMAGE_SIZE=10485760

# Logging Configuration
LOG_LEVEL=INFO
WEB_LOG_LEVEL=WARN
LOG_FILE=/var/log/spring-vision/application.log
LOG_MAX_SIZE=100MB
LOG_MAX_HISTORY=30

# Server Configuration
SERVER_PORT=8080
HEALTH_SHOW_DETAILS=when-authorized

# Security
ENVIRONMENT=production
```

**Systemd Service File** (add environment variables):
```ini
[Service]
Environment="VISION_CONFIDENCE_THRESHOLD=0.8"
Environment="VISION_GPU_ACCELERATION=true"
Environment="LOG_LEVEL=INFO"
Environment="WEB_LOG_LEVEL=WARN"
Environment="ENVIRONMENT=production"
```

### Reverse Proxy Configuration

#### Nginx Configuration

```nginx
upstream spring-vision {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name your-domain.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL Configuration
    ssl_certificate /path/to/certificate.crt;
    ssl_certificate_key /path/to/private.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    # Security Headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    # File Upload Limits
    client_max_body_size 10M;
    
    location / {
        proxy_pass http://spring-vision;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    location /actuator/ {
        proxy_pass http://spring-vision;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Basic Auth for actuator endpoints
        auth_basic "Restricted Access";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }
}
```

## Docker Deployment

### Dockerfile

Create `Dockerfile`:

```dockerfile
# Multi-stage build for smaller image
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY spring-vision-core/pom.xml spring-vision-core/
COPY spring-vision-autoconfigure/pom.xml spring-vision-autoconfigure/
COPY spring-vision-starter/pom.xml spring-vision-starter/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

# Install system dependencies for OpenCV
RUN apt-get update && apt-get install -y \
    libopencv-core4.8 \
    libopencv-imgproc4.8 \
    libopencv-objdetect4.8 \
    libopencv-highgui4.8 \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r spring-vision && useradd -r -g spring-vision spring-vision

# Create application directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/spring-vision-starter/target/spring-vision-starter-1.0.0-SNAPSHOT.jar app.jar

# Create log directory
RUN mkdir -p /var/log/spring-vision && \
    chown -R spring-vision:spring-vision /app /var/log/spring-vision

# Switch to application user
USER spring-vision

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  spring-vision:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
    volumes:
      - ./logs:/var/log/spring-vision
      - ./config:/app/config
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - vision-network

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - spring-vision
    restart: unless-stopped
    networks:
      - vision-network

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=200h'
      - '--web.enable-lifecycle'
    restart: unless-stopped
    networks:
      - vision-network

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    restart: unless-stopped
    networks:
      - vision-network

volumes:
  prometheus-data:
  grafana-data:

networks:
  vision-network:
    driver: bridge
```

### Docker Configuration

**Docker Environment Variables:**

```yaml
# docker-compose.yml
environment:
  - VISION_CONFIDENCE_THRESHOLD=0.8
  - VISION_GPU_ACCELERATION=false
  - VISION_MAX_IMAGE_SIZE=10485760
  - LOG_LEVEL=INFO
  - WEB_LOG_LEVEL=WARN
  - SERVER_PORT=8080
  - HEALTH_SHOW_DETAILS=when-authorized
  - ENVIRONMENT=docker
```

### Build and Run

```bash
# Build image
docker build -t spring-vision:latest .

# Run with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f spring-vision

# Scale application
docker-compose up -d --scale spring-vision=3
```

## Kubernetes Deployment

### Namespace

Create `namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: spring-vision
  labels:
    name: spring-vision
```

### ConfigMap

Create `configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spring-vision-config
  namespace: spring-vision
data:
  VISION_CONFIDENCE_THRESHOLD: "0.8"
  VISION_GPU_ACCELERATION: "true"
  VISION_MAX_IMAGE_SIZE: "10485760"
  LOG_LEVEL: "INFO"
  WEB_LOG_LEVEL: "WARN"
  SERVER_PORT: "8080"
  HEALTH_SHOW_DETAILS: "when-authorized"
  ENVIRONMENT: "production"
```
      backend: opencv
      opencv:
        confidence-threshold: 0.8
        gpu-acceleration: false
        max-image-size: 10485760
      health:
        enabled: true
        check-interval: 30000
        max-response-time: 5000
      metrics:
        enabled: true
        collection-interval: 60000
    
    logging:
      level:
        com.springvision: INFO
        org.springframework.web: WARN
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: when-authorized
      metrics:
        export:
          prometheus:
            enabled: true
    
    server:
      port: 8080
      compression:
        enabled: true
```

### Secret

Create `secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: spring-vision-secret
  namespace: spring-vision
type: Opaque
data:
  # Base64 encoded values
  actuator-password: YWRtaW4=  # admin
  api-key: c2VjcmV0LWtleQ==   # secret-key
```

### Deployment

Create `deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-vision
  namespace: spring-vision
  labels:
    app: spring-vision
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
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: JAVA_OPTS
          value: "-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport"
        resources:
          requests:
            memory: "2Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /app/config
        - name: logs
          mountPath: /var/log/spring-vision
      volumes:
      - name: config
        configMap:
          name: spring-vision-config
      - name: logs
        emptyDir: {}
      imagePullSecrets:
      - name: registry-secret
```

### Service

Create `service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-vision-service
  namespace: spring-vision
  labels:
    app: spring-vision
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: spring-vision
```

### Ingress

Create `ingress.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: spring-vision-ingress
  namespace: spring-vision
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/client-max-body-size: "10m"
spec:
  tls:
  - hosts:
    - your-domain.com
    secretName: spring-vision-tls
  rules:
  - host: your-domain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: spring-vision-service
            port:
              number: 80
      - path: /actuator
        pathType: Prefix
        backend:
          service:
            name: spring-vision-service
            port:
              number: 80
```

### Horizontal Pod Autoscaler

Create `hpa.yaml`:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: spring-vision-hpa
  namespace: spring-vision
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
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f namespace.yaml

# Apply configurations
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# Deploy application
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml
kubectl apply -f hpa.yaml

# Check deployment
kubectl get pods -n spring-vision
kubectl get services -n spring-vision
kubectl get ingress -n spring-vision

# Check logs
kubectl logs -f deployment/spring-vision -n spring-vision

# Scale deployment
kubectl scale deployment spring-vision --replicas=5 -n spring-vision
```

## Cloud Deployment

### AWS Deployment

#### ECS Deployment

Create `task-definition.json`:

```json
{
  "family": "spring-vision",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/spring-vision-task-role",
  "containerDefinitions": [
    {
      "name": "spring-vision",
      "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/spring-vision:latest",
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
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### EKS Deployment

Use the Kubernetes deployment files with EKS:

```bash
# Create EKS cluster
eksctl create cluster --name spring-vision-cluster --region us-east-1 --nodes 3

# Deploy application
kubectl apply -f k8s/

# Create load balancer
kubectl apply -f k8s/ingress.yaml
```

### Azure Deployment

#### Azure Container Instances

```bash
# Deploy to ACI
az container create \
  --resource-group myResourceGroup \
  --name spring-vision \
  --image spring-vision:latest \
  --dns-name-label spring-vision \
  --ports 8080 \
  --environment-variables SPRING_PROFILES_ACTIVE=azure
```

#### Azure Kubernetes Service

```bash
# Create AKS cluster
az aks create \
  --resource-group myResourceGroup \
  --name spring-vision-cluster \
  --node-count 3 \
  --enable-addons monitoring

# Deploy application
kubectl apply -f k8s/
```

### Google Cloud Deployment

#### Google Kubernetes Engine

```bash
# Create GKE cluster
gcloud container clusters create spring-vision-cluster \
  --num-nodes=3 \
  --zone=us-central1-a

# Deploy application
kubectl apply -f k8s/
```

## Performance Optimization

### JVM Tuning

```bash
# Production JVM options
JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
```

### Application Tuning

```yaml
# Optimized configuration
vision:
  opencv:
    gpu-acceleration: true
    max-image-size: 5242880  # 5MB
    confidence-threshold: 0.7

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
      file-size-threshold: 2KB
```

### Database Optimization

```yaml
# If using database
spring:
  jpa:
    hibernate:
      jdbc:
        batch_size: 20
      order_inserts: true
      order_updates: true
    properties:
      hibernate:
        jdbc:
          batch_versioned_data: true
```

## Security Hardening

### Network Security

```yaml
# Security configuration
server:
  port: 8080
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12

spring:
  security:
    user:
      name: admin
      password: ${ACTUATOR_PASSWORD}
```

### Application Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/vision/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            );
        return http.build();
    }
}
```

### Container Security

```dockerfile
# Security-focused Dockerfile
FROM openjdk:21-jre-slim

# Create non-root user
RUN groupadd -r spring-vision && useradd -r -g spring-vision spring-vision

# Install security updates
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    libopencv-core4.8 \
    libopencv-imgproc4.8 \
    libopencv-objdetect4.8 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application
COPY --chown=spring-vision:spring-vision app.jar app.jar

# Switch to non-root user
USER spring-vision

# Security headers
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Monitoring and Observability

### Prometheus Configuration

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "vision_rules.yml"

scrape_configs:
  - job_name: 'spring-vision'
    static_configs:
      - targets: ['spring-vision:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
```

### Grafana Dashboard

Create dashboard configuration for monitoring:

- Application metrics
- JVM metrics
- System metrics
- Custom business metrics

### Alerting

Configure alerts for:

- High CPU/memory usage
- Slow response times
- Error rates
- Health check failures

## Troubleshooting

### Common Issues

1. **Memory Issues**
   - Increase heap size
   - Monitor garbage collection
   - Check for memory leaks

2. **Performance Issues**
   - Optimize image processing
   - Use GPU acceleration
   - Scale horizontally

3. **Network Issues**
   - Check firewall rules
   - Verify DNS resolution
   - Test connectivity

4. **Storage Issues**
   - Monitor disk space
   - Check file permissions
   - Verify backup procedures

### Debug Commands

```bash
# Check application status
curl -f http://localhost:8080/actuator/health

# Check metrics
curl http://localhost:8080/actuator/metrics

# Check logs
tail -f /var/log/spring-vision/application.log

# Check system resources
top
free -h
df -h

# Check network connectivity
netstat -tulpn | grep 8080
```

### Support

For deployment issues:

1. Check application logs
2. Verify configuration
3. Test connectivity
4. Review system resources
5. Contact support with correlation IDs
