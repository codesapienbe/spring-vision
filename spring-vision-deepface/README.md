# Spring Vision DeepFace Pipeline

A scalable face embedding pipeline using Spring Boot, Kafka, and Python DeepFace.

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │  Spring Boot    │    │     Kafka       │
│                 │    │   Producer      │    │                 │
│ POST /extract   │───▶│   API Server    │───▶│  face-tasks     │
│   - userId      │    │   Port: 8092    │    │   Topic         │
│   - image file  │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │   Python        │
                                              │   Consumer      │
                                              │                 │
                                              │ DeepFace.represent()
                                              │ - Face detection
                                              │ - Embedding gen │
                                              │ - Log results   │
                                              └─────────────────┘
```

## 🚀 Quick Start

### Full Docker Stack (Recommended)

```bash
# Build and start all services with a single command
docker-compose up -d --build

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Manual Setup (Alternative)

#### 1. Start Infrastructure Only

```bash
docker-compose up -d zookeeper kafka
```

#### 2. Start Spring Boot Producer

```bash
cd spring-producer
mvn clean install
mvn spring-boot:run
```

#### 3. Start Python Consumer

```bash
cd python-consumer
uv sync
uv run deepface_consumer
```

### 4. Test the Pipeline

```bash
# Send a test image
curl -X POST http://localhost:8092/api/extract-face \
  -F "userId=test-user" \
  -F "file=@/path/to/face.jpg"
```

## 📁 Project Structure

```
spring-vision-deepface/
├── docker-compose.yml          # Kafka/Zookeeper infrastructure
├── spring-producer/            # Spring Boot API server
│   ├── src/main/java/
│   │   └── com/springvision/deepface/
│   │       ├── DeepFaceProducerApplication.java
│   │       ├── config/
│   │       │   └── KafkaConfig.java
│   │       └── controllers/
│   │           └── FaceExtractionController.java
│   └── src/main/resources/
│       └── application.yml
├── python-consumer/            # Python DeepFace worker
│   ├── pyproject.toml
│   └── src/deepface_consumer/
│       └── __init__.py
└── README.md
```

## ⚙️ Configuration

### Kafka Settings

- **Bootstrap Servers**: `localhost:9092`
- **Topic**: `face-tasks`
- **Max Message Size**: 5MB
- **Key Format**: `userId:filename`
- **Value Format**: Raw image bytes

### Spring Boot Producer

- **Port**: 8092
- **Endpoint**: `POST /api/extract-face`
- **Max File Size**: 50MB
- **Multipart Support**: Enabled

### Python Consumer

- **Group ID**: `face-worker-group`
- **Model**: Facenet
- **Auto Offset Reset**: earliest
- **Auto Commit**: enabled

## 🔧 Development

### Adding New Endpoints

1. Add controller methods in `FaceExtractionController`
2. Configure Kafka topics in `application.yml`
3. Update consumer logic in Python

### Scaling

- **Multiple Consumers**: Run multiple Python processes with same `group_id`
- **Multiple Producers**: Deploy multiple Spring Boot instances behind load balancer
- **Kafka Partitions**: Increase topic partitions for parallel processing

## 📊 Monitoring

### Logs

- **Spring Boot**: Structured JSON logs to `application.log`
- **Python**: Console output with embedding previews
- **Kafka**: Docker logs via `docker-compose logs kafka`

### Health Checks

- **Producer**: `GET http://localhost:8092/actuator/health`
- **Kafka**: `docker-compose exec kafka kafka-topics --list`

## 🚨 Troubleshooting

### Common Issues

1. **Kafka Connection Failed**: Ensure `docker-compose up -d` is running
2. **Image Too Large**: Check `max-file-size` in `application.yml`
3. **DeepFace Import Error**: Install dependencies with `pip install -e .`
4. **Port Conflicts**: Change ports in `docker-compose.yml` or `application.yml`

### Debug Commands

```bash
# Check Kafka topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Monitor consumer group
docker-compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group face-worker-group --describe

# View producer logs
tail -f spring-producer/application.log
```

## 📈 Performance Tuning

### Recommended Settings

- **Kafka Partitions**: 10-50 for high throughput
- **Consumer Instances**: 1 per CPU core
- **Producer Batch Size**: 16KB-1MB
- **Consumer Fetch Size**: 1MB

### Memory Optimization

- **JVM Heap**: 2-4GB for Spring Boot
- **Python Workers**: Monitor with `htop` or `ps`
- **Kafka**: Default 1GB heap, increase for high load
