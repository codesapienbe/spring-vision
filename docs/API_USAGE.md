# Spring Vision API Usage Guide

This document provides comprehensive examples of how to use the Spring Vision REST API endpoints. Use this guide to build example applications that consume the Spring Vision API as a backend.

## Base URL
```
http://localhost:8080/api/vision
```

## Authentication
Currently, no authentication is required. All endpoints are publicly accessible.

## Content Types
- **Multipart Form Data**: For file uploads
- **Application JSON**: For JSON payloads with base64-encoded images

---

## Core Detection Endpoints

### 1. Face Detection

#### POST /detect/faces (File Upload)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/faces" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg" \
  -F "minConfidence=0.7"
```

**Response:**
```json
{
  "detectionType": "FACE",
  "detections": [
    {
      "label": "face",
      "confidence": 0.95,
      "boundingBox": {
        "x": 100.0,
        "y": 150.0,
        "width": 200.0,
        "height": 250.0
      },
      "attributes": {
        "age": 25,
        "gender": "male",
        "emotion": "happy"
      }
    },
    {
      "label": "face",
      "confidence": 0.87,
      "boundingBox": {
        "x": 400.0,
        "y": 200.0,
        "width": 180.0,
        "height": 220.0
      },
      "attributes": {
        "age": 30,
        "gender": "female",
        "emotion": "neutral"
      }
    }
  ],
  "averageConfidence": 0.91,
  "processingTimeMs": 150,
  "timestamp": "2025-10-18T20:49:33Z",
  "metadata": {
    "correlationId": "req_1729286973333_1",
    "filtered": true
  }
}
```

#### POST /detect/faces (JSON)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/faces" \
  -H "Content-Type: application/json" \
  -d '{
    "imageData": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
  }'
```

**Response:** Same as file upload response.

### 2. Object Detection

#### POST /detect/objects (File Upload)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/objects" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg"
```

**Response:**
```json
{
  "detectionType": "OBJECT",
  "detections": [
    {
      "label": "person",
      "confidence": 0.92,
      "boundingBox": {
        "x": 50.0,
        "y": 100.0,
        "width": 300.0,
        "height": 400.0
      },
      "attributes": {
        "class": "person",
        "supercategory": "person"
      }
    },
    {
      "label": "car",
      "confidence": 0.88,
      "boundingBox": {
        "x": 400.0,
        "y": 200.0,
        "width": 250.0,
        "height": 150.0
      },
      "attributes": {
        "class": "car",
        "supercategory": "vehicle"
      }
    },
    {
      "label": "dog",
      "confidence": 0.75,
      "boundingBox": {
        "x": 100.0,
        "y": 300.0,
        "width": 120.0,
        "height": 180.0
      },
      "attributes": {
        "class": "dog",
        "supercategory": "animal"
      }
    }
  ],
  "averageConfidence": 0.85,
  "processingTimeMs": 200,
  "timestamp": "2025-10-18T20:49:33Z",
  "metadata": {
    "correlationId": "req_1729286973333_2"
  }
}
```

### 3. Text Detection (OCR)

#### POST /detect/text (File Upload)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/text" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/document.jpg"
```

**Response:**
```json
{
  "detectionType": "TEXT",
  "detections": [
    {
      "label": "Hello World",
      "confidence": 0.98,
      "boundingBox": {
        "x": 100.0,
        "y": 50.0,
        "width": 200.0,
        "height": 30.0
      },
      "attributes": {
        "text": "Hello World",
        "language": "en",
        "font_size": 24
      }
    },
    {
      "label": "Spring Vision API",
      "confidence": 0.95,
      "boundingBox": {
        "x": 100.0,
        "y": 100.0,
        "width": 250.0,
        "height": 30.0
      },
      "attributes": {
        "text": "Spring Vision API",
        "language": "en",
        "font_size": 20
      }
    }
  ],
  "averageConfidence": 0.965,
  "processingTimeMs": 300,
  "timestamp": "2025-10-18T20:49:33Z",
  "metadata": {
    "correlationId": "req_1729286973333_3"
  }
}
```

### 4. Barcode Detection

#### POST /detect/barcodes (File Upload)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/barcodes" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/barcode.jpg"
```

**Response:**
```json
{
  "detectionType": "BARCODE",
  "detections": [
    {
      "label": "123456789012",
      "confidence": 0.99,
      "boundingBox": {
        "x": 150.0,
        "y": 200.0,
        "width": 300.0,
        "height": 100.0
      },
      "attributes": {
        "format": "EAN_13",
        "data": "123456789012",
        "type": "PRODUCT"
      }
    }
  ],
  "averageConfidence": 0.99,
  "processingTimeMs": 80,
  "timestamp": "2025-10-18T20:49:33Z",
  "metadata": {
    "correlationId": "req_1729286973333_4"
  }
}
```

---

## Advanced Vision Endpoints

### 5. Face Counting

#### POST /faces/count
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/faces/count" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/group_photo.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_5",
  "status": "success",
  "count": 5,
  "averageConfidence": 0.89,
  "processingTimeMs": 120,
  "message": "Detected 5 faces"
}
```

### 6. Face Embeddings

#### POST /faces/embeddings
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/faces/embeddings" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_6",
  "status": "success",
  "count": 1,
  "embeddings": [
    {
      "id": "face-0",
      "embedding_base64": "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl9gYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXp7fH1+f4CBgoOEhYaHiImKi4yNjo+QkZKTlJWWl5iZmpucnZ6foKGio6SlpqeoqaqrrK2ur7CxsrO0tba3uLm6u7y9vr/AwcLDxMXGx8jJysvMzc7P0NHS09TV1tfY2drb3N3e3+Dh4uPk5ebn6Onq6+zt7u/w8fLz9PX29/j5+vv8/f7/",
      "length": 512
    }
  ],
  "processingTimeMs": 180
}
```

### 7. Image Classification

#### POST /classify
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/classify" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg" \
  -F "topK=3"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_7",
  "status": "success",
  "classifications": [
    {
      "label": "golden retriever",
      "confidence": 0.95
    },
    {
      "label": "labrador retriever",
      "confidence": 0.03
    },
    {
      "label": "cocker spaniel",
      "confidence": 0.02
    }
  ],
  "topPrediction": "golden retriever",
  "count": 3,
  "processingTimeMs": 250
}
```

### 8. Pose Detection

#### POST /detect/poses
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/poses" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/person.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_8",
  "status": "success",
  "poses": [
    {
      "label": "standing",
      "confidence": 0.92,
      "attributes": {
        "keypoints": [
          {"name": "nose", "x": 200, "y": 100, "confidence": 0.99},
          {"name": "left_eye", "x": 190, "y": 95, "confidence": 0.98},
          {"name": "right_eye", "x": 210, "y": 95, "confidence": 0.97},
          {"name": "left_shoulder", "x": 180, "y": 150, "confidence": 0.95},
          {"name": "right_shoulder", "x": 220, "y": 150, "confidence": 0.94}
        ],
        "pose_type": "standing",
        "body_orientation": "front"
      }
    }
  ],
  "count": 1,
  "processingTimeMs": 300,
  "message": "Detected 1 poses"
}
```

### 9. Action Recognition

#### POST /recognize/actions
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/recognize/actions" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/action.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_9",
  "status": "success",
  "actions": [
    {
      "action": "running",
      "confidence": 0.88
    },
    {
      "action": "walking",
      "confidence": 0.10
    },
    {
      "action": "jumping",
      "confidence": 0.02
    }
  ],
  "topAction": "running",
  "count": 3,
  "processingTimeMs": 400
}
```

### 10. NSFW Detection

#### POST /detect/nsfw
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/nsfw" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_10",
  "status": "success",
  "classification": "safe",
  "confidence": 0.95,
  "isNSFW": false,
  "processingTimeMs": 150
}
```

### 11. Emotion Detection

#### POST /detect/emotions
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/emotions" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_11",
  "status": "success",
  "emotions": [
    {
      "emotion": "happy",
      "confidence": 0.85,
      "faceIndex": 0,
      "boundingBox": {
        "x": 100,
        "y": 150,
        "width": 200,
        "height": 250
      }
    }
  ],
  "topEmotion": "happy",
  "count": 1,
  "processingTimeMs": 200
}
```

### 12. Deepfake Detection

#### POST /detect/deepfake
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/deepfake" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_12",
  "status": "success",
  "classification": "real",
  "confidence": 0.92,
  "isFake": false,
  "manipulationType": "none",
  "processingTimeMs": 500
}
```

### 13. Hand Detection

#### POST /detect/hands
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/hands" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/hands.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_13",
  "status": "success",
  "hands": [
    {
      "label": "left_hand",
      "confidence": 0.90,
      "boundingBox": {
        "x": 150,
        "y": 200,
        "width": 100,
        "height": 120
      }
    },
    {
      "label": "right_hand",
      "confidence": 0.88,
      "boundingBox": {
        "x": 300,
        "y": 200,
        "width": 100,
        "height": 120
      }
    }
  ],
  "count": 2,
  "processingTimeMs": 180
}
```

### 14. Demographics Detection

#### POST /detect/demographics
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/demographics" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_14",
  "status": "success",
  "demographics": [
    {
      "gender": "male",
      "confidence": 0.85,
      "age": 28,
      "ageRange": "25-30",
      "genderConfidence": 0.85,
      "ageError": 2.5,
      "faceIndex": 0,
      "boundingBox": {
        "x": 100,
        "y": 150,
        "width": 200,
        "height": 250
      }
    }
  ],
  "facesAnalyzed": 1,
  "processingTimeMs": 250
}
```

### 15. Fall Detection

#### POST /detect/fall
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/detect/fall" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/person.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_15",
  "status": "success",
  "fallDetected": false,
  "bodyOrientation": "upright",
  "riskLevel": "low",
  "confidence": 0.92,
  "aspectRatio": 0.4,
  "headHeight": 0.8,
  "analysisDetails": "Person appears to be standing normally",
  "boundingBox": {
    "x": 100,
    "y": 50,
    "width": 200,
    "height": 400
  },
  "processingTimeMs": 300
}
```

### 16. Stress Analysis

#### POST /analyze/stress
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/analyze/stress" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_16",
  "status": "success",
  "stressLevel": "low",
  "confidence": 0.78,
  "stressScore": 0.25,
  "dominantEmotion": "calm",
  "emotionIntensity": 0.3,
  "indicators": ["relaxed_eyebrows", "neutral_mouth", "steady_gaze"],
  "boundingBox": {
    "x": 100,
    "y": 150,
    "width": 200,
    "height": 250
  },
  "disclaimer": "Not for medical diagnosis - research and wellness monitoring only",
  "processingTimeMs": 400
}
```

### 17. Metadata Extraction

#### POST /metadata/extract
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/metadata/extract" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/image.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_17",
  "status": "success",
  "metadata": {
    "exif": {
      "camera_make": "Canon",
      "camera_model": "EOS R5",
      "focal_length": "50mm",
      "aperture": "f/2.8",
      "shutter_speed": "1/125",
      "iso": 400,
      "date_taken": "2025-10-18T15:30:00Z"
    },
    "gps": {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "altitude": 10.5,
      "location": "New York, NY"
    },
    "metadata": {
      "width": 1920,
      "height": 1080,
      "format": "JPEG",
      "file_size": 2048576,
      "color_space": "sRGB"
    }
  },
  "groupCount": 3,
  "processingTimeMs": 100,
  "backend": "opencv"
}
```

### 18. Security Threat Detection

#### POST /security/threats
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/security/threats" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/security_image.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_18",
  "status": "success",
  "threats": [
    {
      "label": "knife",
      "confidence": 0.88,
      "boundingBox": {
        "x": 200,
        "y": 300,
        "width": 50,
        "height": 200
      },
      "threatType": "weapon",
      "severity": "HIGH",
      "weaponClass": "blade",
      "description": "Sharp blade weapon detected"
    }
  ],
  "threatCount": 1,
  "highSeverityCount": 1,
  "processingTimeMs": 350,
  "disclaimer": "For legitimate security and safety use only. Comply with local surveillance laws and privacy regulations.",
  "warning": "False positives may occur. Human verification recommended for critical decisions."
}
```

### 19. Biometric Authentication

#### POST /security/authenticate
**Request:**
```bash
curl -X POST "http://localhost:8080/api/vision/security/authenticate" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/face.jpg"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_19",
  "status": "success",
  "authorized": true,
  "label": "authenticated",
  "confidence": 0.95,
  "matchScore": 0.92,
  "timestamp": "2025-10-18T20:49:33Z",
  "userId": "user123",
  "userName": "John Doe",
  "message": "Access granted for user: John Doe",
  "securityNote": "This is a demonstration. Production systems should implement liveness detection and multi-factor authentication.",
  "privacyNote": "Ensure compliance with biometric privacy laws (GDPR, BIPA, etc.)",
  "processingTimeMs": 250
}
```

---

## System Endpoints

### 20. Health Check

#### GET /health
**Request:**
```bash
curl -X GET "http://localhost:8080/api/vision/health"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_20",
  "backendId": "opencv",
  "backendName": "Vision Backend",
  "backendVersion": "1.0.8",
  "status": "HEALTHY",
  "statusMessage": "Backend is operating normally",
  "responseTimeMs": 45,
  "supportedDetectionTypes": ["face", "object", "text", "barcode"],
  "error": null
}
```

### 21. System Information

#### GET /info
**Request:**
```bash
curl -X GET "http://localhost:8080/api/vision/info"
```

**Response:**
```json
{
  "correlationId": "req_1729286973333_21",
  "status": "success",
  "backend": {
    "id": "opencv",
    "status": "HEALTHY",
    "responseTimeMs": 45,
    "supportedDetectionTypes": ["face", "object", "text", "barcode"]
  },
  "version": "1.0.8",
  "message": "Info retrieved successfully"
}
```

---

## Error Responses

### Common Error Response Format
```json
{
  "detectionType": "FACE",
  "detections": [],
  "averageConfidence": 0.0,
  "processingTimeMs": 0,
  "timestamp": "2025-10-18T20:49:33Z",
  "metadata": {
    "correlationId": "req_1729286973333_error",
    "error": "File size exceeds maximum allowed size of 10485760 bytes"
  }
}
```

### HTTP Status Codes
- **200 OK**: Successful request
- **400 Bad Request**: Invalid request parameters
- **413 Payload Too Large**: File size exceeds limit (10MB)
- **415 Unsupported Media Type**: Invalid file format
- **500 Internal Server Error**: Processing error

---

## File Upload Constraints

### Supported File Types
- `image/jpeg`
- `image/jpg`
- `image/png`
- `image/gif`
- `image/bmp`
- `image/webp`

### File Size Limits
- **Maximum file size**: 10MB (10,485,760 bytes)

### Base64 Encoding (for JSON requests)
When sending images via JSON, encode them as base64 strings:
```javascript
// JavaScript example
const fileInput = document.getElementById('fileInput');
const file = fileInput.files[0];
const reader = new FileReader();
reader.onload = function(e) {
  const base64 = e.target.result.split(',')[1]; // Remove data:image/jpeg;base64, prefix
  // Send base64 string in JSON payload
};
reader.readAsDataURL(file);
```

---

## Example Applications

### 1. Face Detection Web App
```html
<!DOCTYPE html>
<html>
<head>
    <title>Face Detection App</title>
</head>
<body>
    <input type="file" id="fileInput" accept="image/*">
    <button onclick="detectFaces()">Detect Faces</button>
    <div id="results"></div>

    <script>
    async function detectFaces() {
        const fileInput = document.getElementById('fileInput');
        const file = fileInput.files[0];
        
        const formData = new FormData();
        formData.append('file', file);
        
        try {
            const response = await fetch('/api/vision/detect/faces', {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            document.getElementById('results').innerHTML = 
                `Found ${result.detections.length} faces with average confidence ${result.averageConfidence}`;
        } catch (error) {
            console.error('Error:', error);
        }
    }
    </script>
</body>
</html>
```

### 2. Object Detection with Python
```python
import requests
import json

def detect_objects(image_path):
    url = "http://localhost:8080/api/vision/detect/objects"
    
    with open(image_path, 'rb') as f:
        files = {'file': f}
        response = requests.post(url, files=files)
    
    if response.status_code == 200:
        result = response.json()
        print(f"Found {len(result['detections'])} objects:")
        for detection in result['detections']:
            print(f"- {detection['label']} (confidence: {detection['confidence']:.2f})")
    else:
        print(f"Error: {response.status_code}")

# Usage
detect_objects("image.jpg")
```

### 3. OCR with Node.js
```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

async function extractText(imagePath) {
    const form = new FormData();
    form.append('file', fs.createReadStream(imagePath));
    
    try {
        const response = await axios.post(
            'http://localhost:8080/api/vision/detect/text',
            form,
            { headers: form.getHeaders() }
        );
        
        const result = response.data;
        console.log('Extracted text:');
        result.detections.forEach(detection => {
            console.log(`- "${detection.label}" (confidence: ${detection.confidence})`);
        });
    } catch (error) {
        console.error('Error:', error.message);
    }
}

// Usage
extractText('document.jpg');
```

---

## Integration Tips

### 1. Correlation IDs
All responses include correlation IDs for request tracking. Use these for debugging and logging.

### 2. Processing Time
Monitor `processingTimeMs` to understand performance characteristics of different operations.

### 3. Confidence Thresholds
Use `minConfidence` parameter for face detection to filter out low-confidence results.

### 4. Error Handling
Always check HTTP status codes and handle errors gracefully. The API returns empty results for errors rather than throwing exceptions.

### 5. Batch Processing
For multiple images, make parallel requests to improve throughput.

### 6. Caching
Consider caching results for identical images to reduce API calls and improve performance.

---

This API usage guide provides everything needed to build comprehensive example applications using the Spring Vision API as a backend. The examples cover all major use cases from simple face detection to advanced security applications.
