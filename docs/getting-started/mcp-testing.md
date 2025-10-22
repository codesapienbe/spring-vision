# MCP Testing Guide

[Docs Home](../index.md) · [Quick Start](./quick-start.md) · [MCP Setup](./mcp-setup.md) · [API Usage](../development/API_USAGE.md)

This guide provides comprehensive examples for testing each Spring Vision MCP tool using real photo URLs and JSON-RPC requests. Use this guide to verify your MCP server setup and test all available computer vision capabilities.

## Prerequisites

1. Install JBang:

```bash
# Install JBang (if not already installed)
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

2. Run the MCP server:

```bash
# First, set up Spring Vision using the CLI (downloads the JAR automatically)
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar

# Then run the downloaded MCP server (it will wait for JSON-RPC requests on stdin)
jbang ~/.springvision/mcp-0.0.4.jar
```

Wait for "Spring Vision MCP Server ready", then send JSON payloads to the running server by pasting them in the terminal or using bash commands.

### Sending JSON via Bash

To send JSON programmatically via bash:

1. Find the server process PID:

```bash
PID=$(pgrep -f "jbang.*mcp-0.0.4.jar")
```

2. Send the JSON:

```bash
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"count_faces_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' > /proc/$PID/fd/0
```

Or paste the JSON directly into the terminal where the server is running.

## Face Detection & Recognition Tools

### 1. Count Faces (URL)

**Example Prompt:**

```
Count the number of faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "count_faces_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

### 2. Count Faces (Bytes)

**Example Prompt:**

```
Count the number of faces in this image (provide as base64 bytes).
```

```bash
# Download image and encode as base64 for bytes-based tools
curl -s "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg" | base64 -w 0 | \
jq -R -s '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"count_faces_b","arguments":{"imageBytes":[.]}}}' | \
# Then send the output JSON to the running MCP server
```

### 3. Extract Face Embeddings (URL)

**Example Prompt:**

```
Extract face embeddings from this image: https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "name": "extract_face_embeddings_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

### 4. Extract Face Embeddings (Bytes)

**Example Prompt:**

```
Extract face embeddings from this image (provide as base64 bytes).
```

```bash
curl -s "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg" | base64 -w 0 | \
jq -R -s '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"extract_face_embeddings_b","arguments":{"imageBytes":[.]}}}' | \
# Then send the output JSON to the running MCP server
```

### 5. Verify Faces Between URLs

**Example Prompt:**

```
Verify if the faces in these two images match: source https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg and target https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "tools/call",
  "params": {
    "name": "verify_faces_between_urls",
    "arguments": {
      "sourceImageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
      "targetImageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

### 6. Lookup Faces in Dataset (URL)

**Example Prompt:**

```
Find matching faces for this image in the dataset: source https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg, dataset [https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg, https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg]
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 8,
  "method": "tools/call",
  "params": {
    "name": "lookup_faces_in_dataset_u",
    "arguments": {
      "sourceImageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
      "datasetImageUrls": [
        "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg",
        "https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg"
      ]
    }
  }
}
```

## Object Detection & Classification

### 7. Detect Objects (URL)

**Example Prompt:**

```
Detect objects in this image: https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 9,
  "method": "tools/call",
  "params": {
    "name": "detect_objects_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
    }
  }
}
```

### 8. Classify Image (URL)

**Example Prompt:**

```
Classify this image and return top 5 categories: https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "classify_image_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg",
      "topK": 5
    }
  }
}
```

## Text & OCR Tools

### 9. Extract Text (URL)

**Example Prompt:**

```
Extract text from this image: https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 11,
  "method": "tools/call",
  "params": {
    "name": "extract_text_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"
    }
  }
}
```

### 10. Scan Barcode/QR Code (URL)

**Example Prompt:**

```
Scan for barcodes or QR codes in this image: https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 12,
  "method": "tools/call",
  "params": {
    "name": "scan_barcode_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"
    }
  }
}
```

## Emotion & Demographic Analysis

### 11. Detect Emotions (URL)

**Example Prompt:**

```
Detect emotions in faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 13,
  "method": "tools/call",
  "params": {
    "name": "detect_emotions_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

### 12. Detect Demographics (URL)

**Example Prompt:**

```
Analyze demographics (age, gender) from faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg
```

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 14,
  "method": "tools/call",
  "params": {
    "name": "detect_demographics_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

### 13. Analyze Stress (URL)

**Example Prompt:** Analyze stress levels from faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 15,
  "method": "tools/call",
  "params": {
    "name": "analyze_stress_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

## Pose & Action Recognition

### 14. Detect Poses (URL)

**Example Prompt:** Detect human poses in this image: https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 16,
  "method": "tools/call",
  "params": {
    "name": "detect_poses_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

### 15. Recognize Actions (URL)

**Example Prompt:** Recognize actions or activities in this image: https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 17,
  "method": "tools/call",
  "params": {
    "name": "recognize_actions_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

### 16. Detect Hands (URL)

**Example Prompt:** Detect hands and gestures in this image: https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 18,
  "method": "tools/call",
  "params": {
    "name": "detect_hands_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

### 17. Detect Fall (URL)

**Example Prompt:** Detect if someone is falling in this image: https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 19,
  "method": "tools/call",
  "params": {
    "name": "detect_fall_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
    }
  }
}
```

## Security & Content Analysis

### 18. Detect NSFW Content (URL)

**Example Prompt:** Check if this image contains NSFW content: https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 20,
  "method": "tools/call",
  "params": {
    "name": "detect_nsfw_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
    }
  }
}
```

### 19. Detect Deepfake (URL)

**Example Prompt:** Analyze if this image is a deepfake: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 21,
  "method": "tools/call",
  "params": {
    "name": "detect_deepfake_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

### 20. Detect Threats (URL)

**Example Prompt:** Detect potential threats or weapons in this image: https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 22,
  "method": "tools/call",
  "params": {
    "name": "detect_threats",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
    }
  }
}
```

### 21. Authenticate Access (URL)

**Example Prompt:** Authenticate access based on facial recognition in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 23,
  "method": "tools/call",
  "params": {
    "name": "authenticate_access",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    }
  }
}
```

## Health & Biometrics

### 22. Estimate Heart Rate (URL - requires multiple frames)

**Example Prompt:** Estimate heart rate from these video frames: [https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg, https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg, https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg]

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 24,
  "method": "tools/call",
  "params": {
    "name": "estimate_heart_rate_u",
    "arguments": {
      "imageUrls": [
        "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
        "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg",
        "https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg"
      ]
    }
  }
}
```

## Metadata & Utility Tools

### 23. Extract Image Metadata (URL)

**Example Prompt:** Extract metadata from this image: https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg

Send this JSON to the running MCP server:

```json
{
  "jsonrpc": "2.0",
  "id": 25,
  "method": "tools/call",
  "params": {
    "name": "extract_image_metadata_u",
    "arguments": {
      "imageUrl": "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
    }
  }
}
```
