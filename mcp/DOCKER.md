# 🐳 Docker Deployment Guide for Spring Vision MCP Server

This guide covers Docker-based deployment options for the Spring Vision MCP Server with SSE support.

---

## 🚀 Quick Start

### Option 1: Docker Run

```bash
# Build the Docker image
mvn clean install -pl mcp -am

# Run the container
docker run -d \
  --name spring-vision-mcp \
  -p 8081:8081 \
  spring-vision:1.0.4

# Check logs
docker logs -f spring-vision-mcp

# Stop the container
docker stop spring-vision-mcp

# Remove the container
docker rm spring-vision-mcp
```

### Option 2: Docker Compose (Recommended)

```bash
# Navigate to mcp directory
cd mcp

# Start the service
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the service
docker-compose down
```

---

## 🔧 Configuration

### Environment Variables

You can customize the server using environment variables:

```bash
docker run -d \
  --name spring-vision-mcp \
  -p 8081:8081 \
  -e SERVER_PORT=8081 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e LOGGING_LEVEL_IO_GITHUB_CODESAPIENBE_SPRINGVISION=DEBUG \
  spring-vision:1.0.4
```

### Custom Configuration File

Mount a custom `application.yml`:

```bash
docker run -d \
  --name spring-vision-mcp \
  -p 8081:8081 \
  -v /path/to/custom/application.yml:/app/resources/application.yml \
  spring-vision:1.0.4
```

---

## 🌐 MCP Client Configuration

With Docker deployment using SSE, MCP clients simply connect to the HTTP endpoint:

### GitHub Copilot

`~/.config/github-copilot/intellij/mcp.json`:

```json
{
  "servers": {
    "spring-vision": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:8081/mcp"
      }
    }
  }
}
```

### Claude Desktop

`~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "spring-vision": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:8081/mcp"
      }
    }
  }
}
```

---

## 🔍 Troubleshooting

### Check Container Status

```bash
docker ps -a | grep spring-vision-mcp
```

### View Logs

```bash
docker logs spring-vision-mcp
```

### Check Health

```bash
curl http://localhost:8081/actuator/health
```

### Restart Container

```bash
docker restart spring-vision-mcp
```

### Rebuild Image

```bash
# Clean old images
docker rmi spring-vision:1.0.4

# Rebuild
mvn clean install -pl mcp -am
```

---

## 🔒 Production Deployment

### With Docker Compose

Update `docker-compose.yml` for production:

```yaml
version: '3.8'

services:
  spring-vision-mcp:
    image: spring-vision:1.0.4
    container_name: spring-vision-mcp
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SERVER_PORT=8081
      - LOGGING_LEVEL_ROOT=WARN
      - LOGGING_LEVEL_IO_GITHUB_CODESAPIENBE_SPRINGVISION=INFO
    restart: unless-stopped
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8081/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - spring-vision-network
    volumes:
      - ./logs:/app/logs

networks:
  spring-vision-network:
    driver: bridge
```

---

## 📊 Monitoring

### View Real-time Logs

```bash
docker logs -f --tail 100 spring-vision-mcp
```

### Check Resource Usage

```bash
docker stats spring-vision-mcp
```

---

## 🚀 Advanced: Docker Swarm / Kubernetes

### Docker Swarm

```bash
docker service create \
  --name spring-vision-mcp \
  --publish 8081:8081 \
  --replicas 3 \
  spring-vision:1.0.4
```

### Kubernetes Deployment

Create `k8s-deployment.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-vision-mcp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-vision-mcp
  template:
    metadata:
      labels:
        app: spring-vision-mcp
    spec:
      containers:
        - name: spring-vision-mcp
          image: spring-vision:1.0.4
          ports:
            - containerPort: 8081
          env:
            - name: SERVER_PORT
              value: "8081"
---
apiVersion: v1
kind: Service
metadata:
  name: spring-vision-mcp-service
spec:
  selector:
    app: spring-vision-mcp
  ports:
    - protocol: TCP
      port: 8081
      targetPort: 8081
  type: LoadBalancer
```

Deploy:

```bash
kubectl apply -f k8s-deployment.yml
```

---

## 💡 Benefits of Docker Deployment

1. **Isolation**: Runs in its own container with controlled resources
2. **Easy updates**: Simply pull new image and restart
3. **Portability**: Run anywhere Docker is available
4. **Consistency**: Same environment in dev and production
5. **SSE Support**: Native HTTP server, no stdio complexity
6. **Multiple clients**: Multiple MCP clients can connect simultaneously
7. **Production-ready**: Built-in health checks and restart policies

