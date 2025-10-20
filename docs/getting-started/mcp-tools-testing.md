# MCP Tools Testing Guide

This guide provides comprehensive examples for testing each MCP tool in VisionTool.java using real photo URLs and JSON-RPC requests.

## Prerequisites

1. Build and run the MCP server:
```bash
./mvnw -pl mcp -am clean package
docker build -t spring-vision-mcp:latest mcp/
```

2. Test with Docker:
```bash
# Initialize
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"clientInfo":{"name":"test","version":"1.0"}}}' | docker run -i --rm spring-vision-mcp:latest

# List tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | docker run -i --rm spring-vision-mcp:latest
```

## Face Detection & Recognition Tools

### 1. Count Faces (URL)
```bash
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"count_faces_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 2. Count Faces (Bytes)
```bash
# Download image and encode as base64 for bytes-based tools
curl -s "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg" | base64 -w 0 | \
jq -R -s '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"count_faces_b","arguments":{"imageBytes":[.]} } }' | \
docker run -i --rm spring-vision-mcp:latest
```

### 3. Extract Face Embeddings (URL)
```bash
echo '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"extract_face_embeddings_u","arguments":{"imageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 4. Extract Face Embeddings (Bytes)
```bash
curl -s "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg" | base64 -w 0 | \
jq -R -s '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"extract_face_embeddings_b","arguments":{"imageBytes":[.]} } }' | \
docker run -i --rm spring-vision-mcp:latest
```

### 5. Verify Faces Between URLs
```bash
echo '{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"verify_faces_between_urls","arguments":{"sourceImageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg","targetImageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 6. Lookup Faces in Dataset (URL)
```bash
echo '{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"lookup_faces_in_dataset_u","arguments":{"sourceImageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg","datasetImageUrls":["https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg","https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg"]}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Object Detection & Classification

### 7. Detect Objects (URL)
```bash
echo '{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"detect_objects_u","arguments":{"imageUrl":"https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 8. Classify Image (URL)
```bash
echo '{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"classify_image_u","arguments":{"imageUrl":"https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg","topK":5}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Text & OCR Tools

### 9. Extract Text (URL)
```bash
echo '{"jsonrpc":"2.0","id":11,"method":"tools/call","params":{"name":"extract_text_u","arguments":{"imageUrl":"https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 10. Scan Barcode/QR Code (URL)
```bash
echo '{"jsonrpc":"2.0","id":12,"method":"tools/call","params":{"name":"scan_barcode_u","arguments":{"imageUrl":"https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Emotion & Demographic Analysis

### 11. Detect Emotions (URL)
```bash
echo '{"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"detect_emotions_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 12. Detect Demographics (URL)
```bash
echo '{"jsonrpc":"2.0","id":14,"method":"tools/call","params":{"name":"detect_demographics_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 13. Analyze Stress (URL)
```bash
echo '{"jsonrpc":"2.0","id":15,"method":"tools/call","params":{"name":"analyze_stress_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Pose & Action Recognition

### 14. Detect Poses (URL)
```bash
echo '{"jsonrpc":"2.0","id":16,"method":"tools/call","params":{"name":"detect_poses_u","arguments":{"imageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 15. Recognize Actions (URL)
```bash
echo '{"jsonrpc":"2.0","id":17,"method":"tools/call","params":{"name":"recognize_actions_u","arguments":{"imageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 16. Detect Hands (URL)
```bash
echo '{"jsonrpc":"2.0","id":18,"method":"tools/call","params":{"name":"detect_hands_u","arguments":{"imageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 17. Detect Fall (URL)
```bash
echo '{"jsonrpc":"2.0","id":19,"method":"tools/call","params":{"name":"detect_fall_u","arguments":{"imageUrl":"https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Security & Content Analysis

### 18. Detect NSFW Content (URL)
```bash
echo '{"jsonrpc":"2.0","id":20,"method":"tools/call","params":{"name":"detect_nsfw_u","arguments":{"imageUrl":"https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 19. Detect Deepfake (URL)
```bash
echo '{"jsonrpc":"2.0","id":21,"method":"tools/call","params":{"name":"detect_deepfake_u","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 20. Detect Threats (URL)
```bash
echo '{"jsonrpc":"2.0","id":22,"method":"tools/call","params":{"name":"detect_threats","arguments":{"imageUrl":"https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

### 21. Authenticate Access (URL)
```bash
echo '{"jsonrpc":"2.0","id":23,"method":"tools/call","params":{"name":"authenticate_access","arguments":{"imageUrl":"https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Health & Biometrics

### 22. Estimate Heart Rate (URL - requires multiple frames)
```bash
echo '{"jsonrpc":"2.0","id":24,"method":"tools/call","params":{"name":"estimate_heart_rate_u","arguments":{"imageUrls":["https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg","https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg","https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg"]}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Metadata & Utility Tools

### 23. Extract Image Metadata (URL)
```bash
echo '{"jsonrpc":"2.0","id":25,"method":"tools/call","params":{"name":"extract_image_metadata_u","arguments":{"imageUrl":"https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"}}}' | docker run -i --rm spring-vision-mcp:latest
```

## Batch Testing Script

Create a test script to run all tools:

```bash
#!/bin/bash
# test_all_tools.sh

TOOLS=(
    "count_faces_u"
    "extract_face_embeddings_u"
    "detect_objects_u"
    "classify_image_u"
    "extract_text_u"
    "detect_emotions_u"
    "detect_poses_u"
    "detect_nsfw_u"
    "extract_image_metadata_u"
)

URLS=(
    "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
    "https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"
    "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"
)

for tool in "${TOOLS[@]}"; do
    for url in "${URLS[@]}"; do
        echo "Testing $tool with $url"
        echo "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"$tool\",\"arguments\":{\"imageUrl\":\"$url\"}}}" | \
        docker run -i --rm spring-vision-mcp:latest
        echo -e "\n"
        sleep 1
    done
done
```

## Troubleshooting

- **Tool not found**: Ensure you're using the correct tool name from the `@Tool(name="...")` annotation
- **Image download failures**: Check that the image URLs are accessible and not behind authentication
- **Empty results**: Some tools may return empty results for certain image types (e.g., no faces in landscape photos)
- **Container issues**: Ensure the Docker image is built with `docker build -t spring-vision-mcp:latest mcp/`

## Tool Categories Summary

- **Face Analysis**: `count_faces_*`, `extract_face_embeddings_*`, `verify_faces_*`, `lookup_faces_*`
- **Object Detection**: `detect_objects_*`, `classify_image_*`
- **Text Recognition**: `extract_text_*`, `scan_barcode_*`
- **Human Analysis**: `detect_emotions_*`, `detect_demographics_*`, `analyze_stress_*`, `detect_poses_*`, `recognize_actions_*`, `detect_hands_*`
- **Safety & Security**: `detect_nsfw_*`, `detect_deepfake_*`, `detect_threats`, `authenticate_access`
- **Health**: `detect_fall_*`, `estimate_heart_rate_*`
- **Utilities**: `extract_image_metadata_*`

All URL-based tools (`*_u`) accept an `imageUrl` parameter, while byte-based tools (`*_b`) accept `imageBytes` as a base64-encoded string array.
