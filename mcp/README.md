# 🚀 Spring Vision MCP Server

MCP (Model Context Protocol) server for Spring Vision - gives your AI assistant computer vision capabilities! 👁️

Built with **Spring AI 1.0.3** and the **Model Context Protocol** standard.

---

## 🐳 Docker Deployment (Recommended)

### Building the Docker Image

The project uses Maven with Jib plugin for automated Docker builds:

```bash
# Build the entire project and create Docker image
make build

# Or manually with Maven
mvn clean install -DskipTests
```

This automatically creates the Docker image `spring-vision:1.0`.

### Pushing to Docker Hub

```bash
# Tag and push to Docker Hub
make deploy
```

This pushes the image as `codesapienbe/spring-vision:latest`.

---

## 🔧 MCP Client Configuration

### Claude Desktop / Cline / Other MCP Clients

Add this to your MCP client configuration file:

**Using Docker (Recommended):**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "codesapienbe/spring-vision:latest"
      ]
    }
  }
}
```

**Using Local Docker Image:**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "spring-vision:1.0"
      ]
    }
  }
}
```

**Important:** The `-i` flag is crucial as it enables interactive mode for stdio communication.

---

## 🔧 Available Vision Tools

Spring Vision MCP exposes **12 tools** for computer vision tasks:

### Face Detection & Recognition

1. **detect** - Detect objects/faces in images (raw bytes)
2. **detectBase64** - Detect objects/faces (base64-encoded)
3. **detectUrl** - Detect objects/faces from URL
4. **faces** - Recognize faces (raw bytes)
5. **facesUrl** - Recognize faces from URL

### OCR (Text Recognition)

6. **ocr** - Extract text from images (raw bytes)
7. **ocrUrl** - Extract text from URL

### Face Embeddings

8. **extractEmbeddings** - Extract face embeddings (raw bytes)
9. **extractEmbeddingsBase64** - Extract embeddings (base64)
10. **extractEmbeddingsUrl** - Extract embeddings from URL

### Face Comparison ✨ NEW

11. **compareFacesFromUrls** - Compare faces from multiple image URLs
    - Determines if multiple photos show the same person
    - Returns similarity scores and confidence levels
    - Example: "Compare these two photos and tell me if they're the same person: [url1], [url2]"

12. **compareFacesFromBase64** - Compare faces from base64 images
    - Same functionality for uploaded/base64-encoded images

---

## 🎯 Face Comparison Feature

The new face comparison tools use **cosine similarity** on face embeddings to verify if multiple images show the same person.

### Parameters

- **imageUrls** / **base64Images**: List of images (minimum 2)
- **threshold** (optional): Similarity threshold (0.0 to 1.0, default: 0.6)

### Similarity Thresholds

- **0.8+** = Very High confidence match
- **0.7-0.8** = High confidence match
- **0.6-0.7** = Medium confidence (default threshold)
- **0.5-0.6** = Low confidence
- **<0.5** = Different people / Very Low confidence

### Response Format

```json
{
  "comparisons": [
    {
      "pair": "Image 1 vs Image 2",
      "similarity": 0.8234,
      "match": true,
      "confidence": "Very High"
    }
  ],
  "summary": {
    "allMatch": true,
    "minSimilarity": 0.8234,
    "maxSimilarity": 0.8234,
    "avgSimilarity": 0.8234,
    "threshold": 0.6,
    "imagesCompared": 2
  },
  "verdict": "All images appear to show the same person"
}
```

### Example Usage

**With AI Assistant:**
> "Compare these two photos and tell me if they're the same person:"
> - Photo 1: https://example.com/id_photo.jpg
> - Photo 2: https://example.com/selfie.jpg

**Multi-image comparison:**
> "Are all these photos of the same person?" (provide 3+ URLs)

The tool performs pairwise comparisons and provides an overall verdict.

---

## ⚙️ Backend Configuration

### Default Configuration

By default, Spring Vision uses the **OpenCV backend**, which works out-of-the-box for:

- ✅ Face detection
- ✅ Object detection
- ✅ OCR (text extraction)
- ✅ Face embedding extraction (basic quality)

No additional configuration needed!

### Production Backends (Optional)

For **higher quality face embeddings** and better comparison accuracy, consider specialized backends:

#### 1. InsightFace (Recommended)

```yaml
# application.yml
spring:
  vision:
    backend: insightface
    insightface:
      url: http://insightface-service:5001
```

**Docker Compose Example:**

```yaml
services:
  insightface:
    image: deepinsight/insightface-rest
    ports:
      - "5001:5000"

  spring-vision:
    image: codesapienbe/spring-vision:latest
    environment:
      - SPRING_VISION_BACKEND=insightface
      - SPRING_VISION_INSIGHTFACE_URL=http://insightface:5000
```

#### 2. DeepFace

```yaml
spring:
  vision:
    backend: deepface
    deepface:
      url: http://deepface-service:5002
```

#### 3. CompreFace

```yaml
spring:
  vision:
    backend: compreface
    compreface:
      url: http://compreface:8000
      apiKey: your-api-key
```

### Backend Comparison

| Backend     | Quality   | Speed  | Setup Complexity |
|-------------|-----------|--------|------------------|
| OpenCV      | Good      | Fast   | ✅ None           |
| InsightFace | Excellent | Fast   | Docker service   |
| DeepFace    | Excellent | Medium | Docker service   |
| CompreFace  | Excellent | Medium | Docker Compose   |
| FaceBytes   | Good      | Fast   | Additional deps  |

---

## 🔨 Development

### Building Locally

```bash
# Build with Maven
mvn clean install -DskipTests

# Or use Makefile
make build
```

### Running Tests

```bash
mvn test

# Or use Makefile
make verify
```

### Releasing to Maven Central

```bash
make release
```

---

## 📊 Tool Registration

When the MCP server starts, you should see:

```
INFO --- Registered tools: 12
```

This confirms all vision tools are available to your AI assistant.

---

## 🐛 Troubleshooting

### Container Exits Immediately

Ensure the `-i` flag is present in your MCP client config:

```json
"args": ["run", "-i", "--rm", "codesapienbe/spring-vision:latest"]
```

### Face Comparison Returns "No face detected"

- Ensure images are at least 640x480 pixels
- Face should be clearly visible and front-facing
- Supported formats: JPG, PNG, BMP, TIFF
- For better accuracy, use specialized backends (InsightFace recommended)

### Low Similarity Scores for Same Person

- Try lowering the threshold (default 0.6 → 0.5)
- Use specialized backend for better embeddings
- Ensure consistent lighting and angles in photos

### Image Download Fails

- Check URL is publicly accessible
- Verify image format is supported
- Check firewall/network settings

---

## 📚 Additional Documentation

- **CHANGELOG.md** - Version history and feature updates
- **TROUBLESHOOTING.md** - Detailed troubleshooting guide
- **IMPLEMENTATION_SUMMARY.md** - Technical implementation details

---

## 🎯 Use Cases

- **Identity Verification**: Compare ID photos with selfies
- **Duplicate Detection**: Find duplicate faces across image sets
- **Face Clustering**: Group photos by person
- **Access Control**: Verify authorized personnel
- **Content Moderation**: Detect specific individuals
- **Photo Organization**: Auto-tag photos by person

---

## 🔒 Features

✅ **12 Computer Vision Tools** - Face detection, OCR, embeddings, comparison
✅ **Face Comparison** - Verify if photos show the same person
✅ **Docker Deployment** - Simple, consistent deployment
✅ **MCP Protocol** - Works with Claude, Cline, and other MCP clients
✅ **Multiple Backends** - OpenCV, InsightFace, DeepFace, CompreFace
✅ **Production Ready** - Stable stdio communication, proper logging
✅ **Zero Configuration** - Works out-of-the-box with OpenCV

---

## 📝 License

See LICENSE file for details.

---

## 🌟 Quick Start

1. Build the project: `make build`
2. (Optional) Push to Docker Hub: `make deploy`
3. Add to your MCP client config (see above)
4. Restart your AI assistant
5. Ask: "Compare these two photos: [url1], [url2]"

**That's it!** Your AI now has computer vision superpowers! 🦸‍♂️
