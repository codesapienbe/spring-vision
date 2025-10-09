# Spring Vision Computer Vision Server

This module provides a **REST API server** for Spring Vision computer vision operations, designed as a foundation for **Model Context Protocol (MCP)** support when Spring AI MCP becomes available.

## Features

- **REST API**: HTTP endpoints for computer vision operations
- **Computer Vision Tools**: Endpoints for object detection, text extraction (OCR), and face recognition
- **Docker Container**: Fully containerized with minimal environment requirements
- **Spring Boot Integration**: Built on Spring Boot for reliability and ease of deployment
- **MCP Ready**: Architecture designed to easily integrate MCP support when available

## Available Endpoints

### `GET /api/vision/health`
Health check endpoint.
- **Response**: Service status and version information

### `POST /api/vision/detect`
Detect objects in an image.
- **Parameters**:
  - `image`: Multipart file upload
  - `detectionType` (optional): Type of detection (FACE, TEXT, OBJECT)
- **Response**: Detection results with bounding boxes and confidence scores

### `POST /api/vision/ocr`
Extract text from an image using OCR.
- **Parameters**:
  - `image`: Multipart file upload
- **Response**: Extracted text and detection details

### `POST /api/vision/faces`
Recognize faces in an image.
- **Parameters**:
  - `image`: Multipart file upload
- **Response**: Face detection results with bounding boxes

### `POST /api/vision/detect/base64`
Detect objects using base64 encoded image.
- **Parameters**:
  - `detectionType` (query param, optional): Type of detection
- **Body**: JSON with `image` field containing base64 encoded image data
- **Response**: Detection results

## Building

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker (for containerized deployment)

### Build the Module
```bash
# From the project root
mvn clean install -pl spring-vision-mcp -am
```

### Build Docker Image
```bash
# From the spring-vision-mcp directory
docker build -t spring-vision-mcp:latest .
```

## Running

### Using Docker (Recommended)
```bash
# Run the container
docker run -p 8080:8080 spring-vision-mcp:latest

# With custom configuration
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e SERVER_PORT=8080 \
  spring-vision-mcp:latest
```

### Using Java directly
```bash
# From the spring-vision-mcp directory
java -jar target/spring-vision-mcp-1.0.jar
```

## Environment Variables

The application requires minimal environment configuration:

- `SERVER_PORT` (default: 8080): Port for the web server
- `SPRING_PROFILES_ACTIVE` (default: default): Spring profile to use

## API Usage Examples

### Health Check
```bash
curl http://localhost:8080/api/vision/health
```

### Object Detection with File Upload
```bash
curl -X POST -F "image=@image.jpg" \
  http://localhost:8080/api/vision/detect
```

### OCR with File Upload
```bash
curl -X POST -F "image=@document.png" \
  http://localhost:8080/api/vision/ocr
```

### Face Recognition with File Upload
```bash
curl -X POST -F "image=@photo.jpg" \
  http://localhost:8080/api/vision/faces
```

### Detection with Base64
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"image":"base64-encoded-image-data"}' \
  http://localhost:8080/api/vision/detect/base64
```

## Future MCP Integration

This server is designed to be easily extended with MCP support when Spring AI MCP becomes available. The REST endpoints provide the same functionality that will be exposed as MCP tools:

- `detect_objects` → `/api/vision/detect`
- `extract_text` → `/api/vision/ocr`
- `recognize_faces` → `/api/vision/faces`

## Architecture

The server is built using:
- **Spring Vision Core**: For computer vision capabilities
- **Spring Boot Web**: For HTTP REST API
- **Docker**: For containerization

## Security Considerations

- The server exposes computer vision capabilities via HTTP
- Ensure proper network security when deploying
- Consider authentication/authorization for production use
- Image data is processed in memory - monitor resource usage

## Contributing

When contributing to this module:
1. Follow the project's coding standards
2. Add tests for new endpoints
3. Update documentation for new features
4. Ensure Docker build works correctly
5. Design APIs to be MCP-compatible when MCP support is added

## License

This module is part of Spring Vision and follows the same Apache 2.0 license.
