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
docker compose up -d --build

# Check service status
docker compose ps

# View logs
docker compose logs -f

# Test the API
curl http://localhost:8092/actuator/health

# Stop all services
docker compose down
```

### Manual Setup (Alternative)

#### 1. Start Infrastructure Only

```bash
docker compose up -d zookeeper kafka
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

#### 4. Test the Pipeline

```bash
# Send a test image
curl -X POST http://localhost:8092/api/extract-face \
  -F "userId=test-user" \
  -F "file=@/path/to/face.jpg"
```

## 📁 Project Structure

```
spring-vision-deepface/
├── docker-compose.yml          # Container orchestration
├── spring-producer/            # Spring Boot API server
│   ├── src/main/java/
│   │   └── com/springvision/deepface/
│   │       ├── DeepFaceProducerApplication.java
│   │       ├── config/
│   │       │   ├── KafkaConfig.java
│   │       │   ├── HealthCheckConfig.java
│   │       │   └── StartupConfig.java
│   │       └── controllers/
│   │           └── FaceExtractionController.java
│   └── src/main/resources/
│       └── application.yml     # Single configuration file
├── python-consumer/            # Python DeepFace worker
│   ├── pyproject.toml
│   └── src/deepface_consumer/
│       └── __init__.py
└── README.md
```

## ⚙️ Configuration

### Environment Variables

The application uses environment variables for container-first configuration:

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka connection (default: `localhost:9092`)
- `SERVER_PORT`: Application port (default: `8092`)
- `LOG_LEVEL`: Logging level (default: `INFO`)

### Kafka Settings

- **Bootstrap Servers**: Configurable via `KAFKA_BOOTSTRAP_SERVERS`
- **Topic**: `face-tasks`
- **Max Message Size**: 5MB
- **Key Format**: `userId:filename`
- **Value Format**: Raw image bytes
- **Producer Retries**: 3 with exponential backoff
- **Consumer Group**: `face-extraction-producer`

### Spring Boot Producer

- **Port**: Configurable via `SERVER_PORT`
- **Endpoint**: `POST /api/extract-face`
- **Max File Size**: 50MB
- **Multipart Support**: Enabled
- **Health Check**: `/actuator/health`

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

1. **Kafka Connection Failed**: 
   - Ensure Zookeeper is healthy before Kafka starts
   - Check logs: `docker compose logs kafka`
   - Verify Kafka configuration in docker-compose.yml

2. **Image Too Large**: Check `max-file-size` in `application.yml`

3. **DeepFace Import Error**: Install dependencies with `pip install -e .`

4. **Port Conflicts**: Change ports in `docker-compose.yml` or `application.yml`

5. **Spring Boot Startup Issues**: 
   - Ensure Docker Compose services are running before starting Spring Boot
   - Application automatically detects container environment via environment variables

### Debug Commands

```bash
# Check Kafka topics
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Monitor consumer group
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group face-worker-group --describe

# View producer logs
tail -f spring-producer/application.log

# Check service health
docker compose ps

# View specific service logs
docker compose logs -f kafka
docker compose logs -f spring-producer
docker compose logs -f python-consumer
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
