# CompreFace Example Application

This example demonstrates how to use the Spring Vision framework with the CompreFace backend for face detection and recognition.

## Prerequisites

1. **Docker and Docker Compose**: Ensure you have Docker and Docker Compose installed
2. **Java 21+**: The application requires Java 21 or later
3. **Maven**: For building the application

## Setup

### 1. Start the CompreFace Service

The CompreFace service is configured in the main `docker-compose.yml` file. Start it with:

```bash
# From the project root
docker-compose up -d compreface postgres

# Or start all services
docker-compose up -d
```

This will start:
- **CompreFace API**: Available at `http://localhost:8000`
- **PostgreSQL**: Required by CompreFace for data storage

**Important Notes:**
- **Startup Time**: CompreFace takes approximately 45 seconds to start due to service initialization timing
- **Database Dependencies**: If PostgreSQL starts slowly, CompreFace may fail initially but will restart automatically
- **Health Monitoring**: The container includes health checks to ensure proper startup

### 2. Build the Application

```bash
mvn clean package
```

### 3. Run the Application

```bash
# From the compreface-example directory
cd spring-vision-examples/compreface-example

# Run with a test image
java -jar target/compreface-example.jar /path/to/face.jpg

# Or run without arguments to see usage
java -jar target/compreface-example.jar

# Or using Maven
mvn spring-boot:run -- /path/to/face.jpg

# Or from project root with module specification
mvn spring-boot:run -pl spring-vision-examples/compreface-example -- /path/to/face.jpg
```

## Configuration

The application is configured via `application.yml`:

```yaml
vision:
  backend: compreface
  timeout: 30s
  health-check:
    enabled: true
    interval: 30s
```

## Features

- **Face Detection**: Detects faces in images with bounding boxes and confidence scores
- **Face Recognition**: Identifies known faces from a database
- **Face Verification**: Compares two faces for similarity
- **Health Monitoring**: Monitors the CompreFace API health status

## API Endpoints

CompreFace provides several REST API endpoints:

- `POST /api/v1/recognition/detect` - Face detection
- `POST /api/v1/recognition/recognize` - Face recognition
- `POST /api/v1/verification/verify` - Face verification

## Troubleshooting

### CompreFace Service Not Starting

1. **Check startup status**:
   ```bash
   # Check if container is running
   docker ps | grep compreface
   
   # Monitor startup logs (wait for "exited: startup (exit status 0; expected)")
   docker logs spring-vision-compreface -f
   ```

2. **Check if PostgreSQL is running**:
   ```bash
   docker-compose ps postgres
   ```

3. **Check CompreFace logs**:
   ```bash
   docker-compose logs compreface
   ```

4. **Ensure the database connection is working**:
   ```bash
   docker-compose exec postgres psql -U springvision -d springvision
   ```

5. **Wait for full startup**:
   - CompreFace takes ~45 seconds to start
   - Look for "exited: startup (exit status 0; expected)" in logs
   - Service will restart automatically if database is slow

### API Connection Issues

1. Verify CompreFace is accessible:
   ```bash
   curl http://localhost:8000/api/v1/recognition/detect
   ```

2. Check the application logs for connection errors

## Example Output

```
=== CompreFace Example Application ===
Using CompreFace backend: CompreFace Vision Backend
CompreFace backend is healthy and ready
Processing image: /path/to/face.jpg
Image loaded: 245760 bytes, format: JPEG
Detecting faces...
Found 1 faces:
  - Face at BoundingBox{x=150, y=100, width=200, height=250} with confidence: 0.95
    Age: 25
    Gender: male
    Mask: no_mask
Processing completed in 1250ms
```

## CompreFace Maintenance

### Startup Monitoring

CompreFace has a complex startup process that takes approximately 45 seconds:

1. **Monitor startup progress**:
   ```bash
   docker logs spring-vision-compreface -f
   ```

2. **Look for completion message**:
   ```
   exited: startup (exit status 0; expected)
   ```

3. **Verify service is running**:
   ```bash
   docker ps | grep compreface
   ```

### Performance Configuration

You can configure CompreFace performance using environment variables:

```bash
# Increase Java heap memory (default: 2GB)
export COMPREFACE_API_JAVA_OPTS="-Xmx4g"

# Start with custom memory settings
docker-compose up -d compreface postgres
```

### Data Persistence

- CompreFace data is stored in PostgreSQL volume
- Data persists across container restarts
- Backup the PostgreSQL volume for data safety

### Manual Docker Run (Alternative)

If you prefer manual Docker commands:

```bash
# Basic setup with data persistence
docker run -d \
  --name=CompreFace \
  -e "POSTGRES_URL=jdbc:postgresql://postgres:5432/springvision" \
  -e "POSTGRES_USER=springvision" \
  -e "POSTGRES_PASSWORD=springvision" \
  -e "EXTERNAL_DB=true" \
  -e "API_JAVA_OPTS=-Xmx4g" \
  -v compreface-db:/var/lib/postgresql/data \
  -p 8000:80 \
  --restart=always \
  exadel/compreface
```

## Next Steps

- Configure face recognition by adding known faces to the CompreFace database
- Implement face verification between multiple images
- Add custom attributes and metadata to detected faces
- Integrate with your application's user management system 
