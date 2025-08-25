# Spring Vision API Documentation

Comprehensive documentation for the Spring Vision REST API, including endpoints, request/response formats, examples, and best practices.

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Base URL](#base-url)
4. [Endpoints](#endpoints)
5. [Request/Response Formats](#requestresponse-formats)
6. [Error Handling](#error-handling)
7. [Rate Limiting](#rate-limiting)
8. [Examples](#examples)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)
11. [Advanced Usage: DetectionQuery and Capabilities](#advanced-usage-detectionquery-and-capabilities)

## Overview

The Spring Vision API provides computer vision capabilities through REST endpoints. It supports face detection, object detection, and health monitoring with both file uploads and JSON payloads.

### Features

- **Face Detection**: Detect human faces in images
- **Object Detection**: Detect various objects using OpenCV algorithms
- **Multiple Detection Types**: Process multiple detection types in a single request
- **Health Monitoring**: Check backend status and capabilities
- **File Upload Support**: Direct image file uploads
- **JSON Payload Support**: Base64 encoded image data
- **Correlation IDs**: Request tracking and debugging
- **Performance Metrics**: Processing time and confidence scores

### Supported Image Formats

- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- BMP (.bmp)

### File Size Limits

- **Maximum file size**: 10MB
- **Recommended size**: < 5MB for optimal performance

## Authentication

Currently, the API does not require authentication. All endpoints are publicly accessible.

**Note**: For production deployments, consider implementing authentication and authorization.

## Base URL

```
http://localhost:8080/api/vision
```

Replace `localhost:8080` with your server's hostname and port.

## Endpoints

### Face Detection

#### POST /api/vision/detect/faces

Detect human faces in an image.

**Content-Type**: `multipart/form-data` or `application/json`

**Parameters**:
- `file` (multipart): Image file to process
- `imageData` (JSON): Base64 encoded image data

**Response**: `DetectionResponse`

**Example (File Upload)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/faces \
  -F "file=@image.jpg"
```

**Example (JSON)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/faces \
  -H "Content-Type: application/json" \
  -d '{
    "imageData": "base64-encoded-image-data"
  }'
```

**Response Example**:
```json
{
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "detectionType": "face",
  "detectionCount": 2,
  "averageConfidence": 0.85,
  "processingTimeMs": 150,
  "detections": [
    { "label": "face", "confidence": 0.92, "boundingBox": { "x": 0.1, "y": 0.2, "width": 0.3, "height": 0.4 } },
    { "label": "face", "confidence": 0.78, "boundingBox": { "x": 0.6, "y": 0.3, "width": 0.25, "height": 0.35 } }
  ]
}
```

### Object Detection

#### POST /api/vision/detect/objects

Detect objects in an image.

**Content-Type**: `multipart/form-data` or `application/json`

**Parameters**:
- `file` (multipart): Image file to process
- `imageData` (JSON): Base64 encoded image data

**Response**: `DetectionResponse`

**Example (File Upload)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/objects \
  -F "file=@image.jpg"
```

**Example (JSON)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/objects \
  -H "Content-Type: application/json" \
  -d '{
    "imageData": "base64-encoded-image-data"
  }'
```

**Response Example**:
```json
{
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "detectionType": "object",
  "detectionCount": 3,
  "averageConfidence": 0.78,
  "processingTimeMs": 200,
  "detections": [
    { "label": "person", "confidence": 0.85, "boundingBox": { "x": 0.1, "y": 0.2, "width": 0.3, "height": 0.6 } },
    { "label": "car", "confidence": 0.72, "boundingBox": { "x": 0.5, "y": 0.4, "width": 0.4, "height": 0.3 } },
    { "label": "building", "confidence": 0.77, "boundingBox": { "x": 0.0, "y": 0.0, "width": 1.0, "height": 0.8 } }
  ]
}
```

### Multiple Detection Types

#### POST /api/vision/detect/multiple

Perform multiple detection types on a single image.

**Content-Type**: `multipart/form-data` or `application/json`

**Parameters**:
- `file` (multipart): Image file to process
- `detectionTypes` (multipart): Comma-separated list of detection types
- `imageData` (JSON): Base64 encoded image data
- `detectionTypes` (JSON): Array of detection type strings

**Supported Detection Types**:
- `face`: Face detection
- `object`: Object detection

**Response**: `MultipleDetectionResponse`

**Example (File Upload)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/multiple \
  -F "file=@image.jpg" \
  -F "detectionTypes=face,object"
```

**Example (JSON)**:
```bash
curl -X POST http://localhost:8080/api/vision/detect/multiple \
  -H "Content-Type: application/json" \
  -d '{
    "imageData": "base64-encoded-image-data",
    "detectionTypes": ["face", "object"]
  }'
```

**Response Example**:
```json
{
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "detectionTypes": ["face", "object"],
  "results": [
    { "correlationId": "123e4567-e89b-12d3-a456-426614174000", "detectionType": "face", "detectionCount": 2, "averageConfidence": 0.85, "processingTimeMs": 150, "detections": [...] },
    { "correlationId": "123e4567-e89b-12d3-a456-426614174000", "detectionType": "object", "detectionCount": 3, "averageConfidence": 0.78, "processingTimeMs": 200, "detections": [...] }
  ]
}
```

### Health Check

#### GET /api/vision/health

Get the health status of the vision backend.

**Response**: `HealthResponse`

**Example**:
```bash
curl http://localhost:8080/api/vision/health
```

**Response Example**:
```json
{
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "backendId": "opencv",
  "backendName": "OpenCV Vision Backend",
  "backendVersion": "4.8.1",
  "status": "HEALTHY",
  "statusMessage": "Backend is operating normally",
  "responseTimeMs": 45,
  "supportedDetectionTypes": ["face", "object"]
}
```

### Backend Information

#### GET /api/vision/info

Get information about the vision backend.

**Response**: JSON object

**Example**:
```bash
curl http://localhost:8080/api/vision/info
```

**Response Example**:
```json
{
  "correlationId": "123e4567-e89b-12d3-a456-426614174000",
  "backendId": "opencv",
  "backendName": "OpenCV Vision Backend",
  "backendVersion": "4.8.1",
  "supportedDetectionTypes": ["face", "object"],
  "isHealthy": true
}
```

## Request/Response Formats

### DetectionRequest

```json
{
  "imageData": "base64-encoded-image-data"
}
```

### MultipleDetectionRequest

```json
{
  "imageData": "base64-encoded-image-data",
  "detectionTypes": ["face", "object"]
}
```

### DetectionResponse

```json
{
  "correlationId": "uuid",
  "detectionType": "face|object",
  "detectionCount": 0,
  "averageConfidence": 0.0,
  "processingTimeMs": 0,
  "detections": [
    {
      "label": "string",
      "confidence": 0.0,
      "boundingBox": {
        "x": 0.0,
        "y": 0.0,
        "width": 0.0,
        "height": 0.0
      }
    }
  ],
  "error": "string (optional)"
}
```

### MultipleDetectionResponse

```json
{
  "correlationId": "uuid",
  "detectionTypes": ["string"],
  "results": [
    {
      "correlationId": "uuid",
      "detectionType": "string",
      "detectionCount": 0,
      "averageConfidence": 0.0,
      "processingTimeMs": 0,
      "detections": [...],
      "error": "string (optional)"
    }
  ],
  "error": "string (optional)"
}
```

### HealthResponse

```json
{
  "correlationId": "uuid",
  "backendId": "string",
  "backendName": "string",
  "backendVersion": "string",
  "status": "HEALTHY|UNHEALTHY|UNKNOWN",
  "statusMessage": "string",
  "responseTimeMs": 0,
  "supportedDetectionTypes": ["string"],
  "error": "string (optional)"
}
```

## Error Handling

### HTTP Status Codes

- **200 OK**: Request successful
- **400 Bad Request**: Invalid request format or validation errors
- **413 Payload Too Large**: File size exceeds limits
- **415 Unsupported Media Type**: Unsupported image format
- **500 Internal Server Error**: Backend processing errors

### Error Response Format

```json
{
  "correlationId": "uuid",
  "detectionType": "string",
  "error": "Error message describing the issue"
}
```

### Common Error Messages

- `"File is empty"`: Uploaded file contains no data
- `"File size exceeds maximum limit of 10485760 bytes"`: File too large
- `"Unsupported content type: text/plain"`: Invalid file format
- `"Image data is required"`: Missing image data in JSON request
- `"Detection types are required"`: Missing detection types
- `"Invalid detection type: invalid_type"`: Unsupported detection type
- `"Backend error"`: Internal processing error

## Rate Limiting

Currently, no rate limiting is implemented. Consider implementing rate limiting for production deployments.

## Examples

### JavaScript/Node.js

```javascript
// Face detection with file upload
const FormData = require('form-data');
const fs = require('fs');

const form = new FormData();
form.append('file', fs.createReadStream('image.jpg'));

fetch('http://localhost:8080/api/vision/detect/faces', {
  method: 'POST',
  body: form
})
.then(response => response.json())
.then(data => {
  console.log('Detections:', data.detections);
  console.log('Processing time:', data.processingTimeMs);
});

// Object detection with base64
const imageBuffer = fs.readFileSync('image.jpg');
const base64Image = imageBuffer.toString('base64');

fetch('http://localhost:8080/api/vision/detect/objects', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    imageData: base64Image
  })
})
.then(response => response.json())
.then(data => {
  console.log('Objects detected:', data.detectionCount);
});
```

### Python

```python
import requests
import base64

# Face detection with file upload
with open('image.jpg', 'rb') as f:
    files = {'file': f}
    response = requests.post(
        'http://localhost:8080/api/vision/detect/faces',
        files=files
    )
    data = response.json()
    print(f"Faces detected: {data['detectionCount']}")

# Object detection with base64
with open('image.jpg', 'rb') as f:
    image_data = base64.b64encode(f.read()).decode('utf-8')
    
response = requests.post(
    'http://localhost:8080/api/vision/detect/objects',
    json={'imageData': image_data}
)
data = response.json()
print(f"Objects detected: {data['detectionCount']}")

# Multiple detection types
response = requests.post(
    'http://localhost:8080/api/vision/detect/multiple',
    files={'file': open('image.jpg', 'rb')},
    data={'detectionTypes': 'face,object'}
)
data = response.json()
for result in data['results']:
    print(f"{result['detectionType']}: {result['detectionCount']} detections")
```

### Java

```java
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

// Face detection with file upload
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);

MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new File("image.jpg"));

HttpEntity<MultiValueMap<String, Object>> requestEntity = 
    new HttpEntity<>(body, headers);

ResponseEntity<DetectionResponse> response = restTemplate.postForEntity(
    "http://localhost:8080/api/vision/detect/faces",
    requestEntity,
    DetectionResponse.class
);

DetectionResponse result = response.getBody();
System.out.println("Faces detected: " + result.getDetectionCount());

// Object detection with base64
byte[] imageBytes = Files.readAllBytes(Paths.get("image.jpg"));
String base64Image = Base64.getEncoder().encodeToString(imageBytes);

DetectionRequest request = DetectionRequest.builder()
    .imageData(imageBytes)
    .build();

HttpHeaders jsonHeaders = new HttpHeaders();
jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

HttpEntity<DetectionRequest> jsonRequest = 
    new HttpEntity<>(request, jsonHeaders);

ResponseEntity<DetectionResponse> jsonResponse = restTemplate.postForEntity(
    "http://localhost:8080/api/vision/detect/objects",
    jsonRequest,
    DetectionResponse.class
);
```

### cURL

```bash
# Face detection
curl -X POST http://localhost:8080/api/vision/detect/faces \
  -F "file=@image.jpg"

# Object detection
curl -X POST http://localhost:8080/api/vision/detect/objects \
  -F "file=@image.jpg"

# Multiple detection types
curl -X POST http://localhost:8080/api/vision/detect/multiple \
  -F "file=@image.jpg" \
  -F "detectionTypes=face,object"

# Health check
curl http://localhost:8080/api/vision/health

# Backend info
curl http://localhost:8080/api/vision/info
```

## Best Practices

### Performance

1. **Image Size**: Keep images under 5MB for optimal performance
2. **Image Format**: Use JPEG for photos, PNG for graphics with transparency
3. **Resolution**: Higher resolution doesn't always improve detection accuracy
4. **Batch Processing**: Use multiple detection endpoint for multiple types

### Error Handling

1. **Always check HTTP status codes**
2. **Handle correlation IDs for debugging**
3. **Implement retry logic for transient errors**
4. **Log error messages for troubleshooting**

### Security

1. **Validate image files before upload**
2. **Sanitize file names and paths**
3. **Implement authentication for production**
4. **Use HTTPS in production environments**

### Monitoring

1. **Track processing times**
2. **Monitor detection accuracy**
3. **Log correlation IDs for request tracing**
4. **Set up health check monitoring**

## Troubleshooting

### Common Issues

1. **File Upload Fails**
   - Check file size (max 10MB)
   - Verify supported format (JPEG, PNG, GIF, BMP)
   - Ensure proper multipart/form-data encoding

2. **JSON Request Fails**
   - Verify base64 encoding of image data
   - Check Content-Type header
   - Validate JSON structure

3. **No Detections Found**
   - Check image quality and lighting
   - Verify image contains detectable objects
   - Try different detection types

4. **Slow Performance**
   - Reduce image size
   - Use appropriate image format
   - Check server resources

### Debugging

1. **Use correlation IDs** to track requests
2. **Check server logs** for detailed error messages
3. **Verify backend health** with health endpoint
4. **Test with known good images** to isolate issues

### Support

For additional support:
1. Check the [GitHub repository](https://github.com/spring-vision/spring-vision)
2. Review the [OpenCV documentation](https://docs.opencv.org/)
3. Open an issue with correlation ID and error details 

## Advanced Usage: DetectionQuery and Capabilities

Spring Vision now supports a richer detection API via `DetectionQuery` and optional capability routing:

- Use `VisionTemplate.detect(ImageData, DetectionQuery)` to provide:
  - `type` (required): e.g., `DetectionType.FACE`
  - `categories` (optional): e.g., `[FACE, EYE]`
  - `minConfidence`, `maxDetections`, `roi` (optional)
- The system validates inputs and logs structured metadata.
- When a backend implements capability interfaces (e.g., `FaceDetectionCapability`), calls are routed to those implementations automatically, otherwise they fall back to the backend’s generic `detect`.

Example (Java):
```java
DetectionQuery query = new DetectionQuery.Builder()
    .type(DetectionType.FACE)
    .categories(java.util.Set.of(DetectionCategory.FACE))
    .minConfidence(0.6)
    .build();

VisionResult result = visionTemplate.detect(imageData, query);
```

Backward compatibility:
- Existing endpoints and facade methods (e.g., `detectFaces`) continue to work unchanged in 1.x.
- Deprecated methods will be removed in a future major (2.0). 
