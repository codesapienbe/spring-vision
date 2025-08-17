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
docker-compose up -d compreface postgres
```

This will start:
- **CompreFace API**: Available at `http://localhost:8000`
- **PostgreSQL**: Required by CompreFace for data storage

### 2. Build the Application

```bash
mvn clean package
```

### 3. Run the Application

```bash
# Run with a test image
java -jar target/compreface-example.jar /path/to/face.jpg

# Or run without arguments to see usage
java -jar target/compreface-example.jar
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

1. Check if PostgreSQL is running:
   ```bash
   docker-compose ps postgres
   ```

2. Check CompreFace logs:
   ```bash
   docker-compose logs compreface
   ```

3. Ensure the database connection is working:
   ```bash
   docker-compose exec postgres psql -U springvision -d springvision
   ```

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

## Next Steps

- Configure face recognition by adding known faces to the CompreFace database
- Implement face verification between multiple images
- Add custom attributes and metadata to detected faces
- Integrate with your application's user management system 
