# 🚀 Using Spring Vision with Your AI Assistant 🤖

Welcome! This guide will show you how to connect the Spring Vision MCP server to your favorite AI assistants and development frameworks. This will give your assistant the power of computer vision! 👁️

This guide is for everyone—from beginners to experienced developers! 😊

---

## 🤔 What is Spring Vision?

In simple terms, **Spring Vision gives your AI assistant eyes**.

Once connected, your AI can analyze images to detect faces, objects, and text. It's like giving your assistant a new set of superpowers! 🦸

**Spring Vision MCP** is built using the **Model Context Protocol (MCP)** standard and **Spring AI 1.0.3**, making it easy to integrate with MCP-compatible clients and AI frameworks.

---

## 🏁 Step 1: Configure Spring Vision MCP Server 🔧

Spring Vision MCP uses **stdio transport** for communication with MCP clients. This means it communicates via standard input/output (stdin/stdout) using JSON-RPC protocol.

### For MCP Clients (like Claude Desktop, Cline, etc.)

Add the following configuration to your MCP client settings:

**Using the JAR file:**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/spring-vision-mcp-1.0.jar"
      ]
    }
  }
}
```

**Using Docker:**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "codesapienbe/spring-vision:1.0"
      ]
    }
  }
}
```

**Note:** The `-i` flag is crucial for Docker as it enables interactive mode (stdin).

---

## 🔧 Available Vision Tools

Spring Vision MCP exposes the following tools that your AI can use:

### 1. **detect** - General Object/Face Detection

Detects objects or faces in an image from raw bytes.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data
    - `detectionType` (String, optional): "FACE" or "OBJECT" (defaults to "FACE")

### 2. **detectBase64** - Base64 Image Detection

Detects objects or faces from a base64-encoded image.

- **Parameters:**
    - `base64Image` (String): Base64-encoded image data
    - `detectionType` (String, optional): "FACE" or "OBJECT"

### 3. **detectUrl** - URL Image Detection

Detects objects or faces from an image URL.

- **Parameters:**
    - `imageUrl` (String): URL to the image
    - `detectionType` (String, optional): "FACE" or "OBJECT"

### 4. **ocr** - Text Recognition

Extracts text from an image using OCR.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data

### 5. **ocrUrl** - URL Text Recognition

Extracts text from an image URL.

- **Parameters:**
    - `imageUrl` (String): URL to the image

### 6. **faces** - Face Recognition

Recognizes and analyzes faces in an image.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data

### 7. **facesUrl** - URL Face Recognition

Recognizes and analyzes faces from an image URL.

- **Parameters:**
    - `imageUrl` (String): URL to the image

### 8. **extractEmbeddings** - Face Embedding Extraction

Extracts face embedding vectors for recognition and comparison.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data
- **Returns:**
    - `embeddings`: List of float arrays (one per detected face)
    - `count`: Number of faces detected
    - `embeddingDimension`: Size of each embedding vector

### 9. **extractEmbeddingsBase64** - Base64 Face Embedding Extraction

Extracts face embeddings from a base64-encoded image.

- **Parameters:**
    - `base64Image` (String): Base64-encoded image data

### 10. **extractEmbeddingsUrl** - URL Face Embedding Extraction

Extracts face embeddings from an image URL.

- **Parameters:**
    - `imageUrl` (String): URL to the image

---

## ⚙️ Backend Configuration

### Default Configuration

The simple configuration you mentioned **will work** for all tools:

```java

@Bean
public VisionTemplate visionTemplate() {
    return new VisionTemplate();
}
```

This uses the **OpenCV backend** by default, which supports:

- ✅ Face detection
- ✅ Object detection
- ✅ OCR (text extraction)
- ✅ Face embedding extraction (basic quality with fallback)

### Understanding Embedding Quality

For **embedding extraction tools** (`extractEmbeddings`, `extractEmbeddingsBase64`, `extractEmbeddingsUrl`):

**OpenCV Backend Behavior:**

- Uses SFace model if available (good quality embeddings)
- Falls back to default implementation if SFace not loaded
- Works out-of-the-box but may not be optimal for production face recognition

### Production-Grade Backends for Embeddings

For **high-quality face embeddings** in production, consider using specialized backends:

#### 1. **InsightFace Backend** (Recommended for Face Embeddings)

```java

@Bean
public VisionTemplate visionTemplate() {
    InsightFaceBackend backend = new InsightFaceBackend(
            "http://localhost:5001" // InsightFace service URL
    );
    return new VisionTemplate(backend);
}
```

**Docker setup:**

```bash
docker run -d -p 5001:5000 deepinsight/insightface-rest
```

**Best for:** High-quality face embeddings (512-dim), face verification, identity matching

#### 2. **DeepFace Backend**

```java

@Bean
public VisionTemplate visionTemplate() {
    DeepFaceBackend backend = new DeepFaceBackend(
            "http://localhost:5002"
    );
    return new VisionTemplate(backend);
}
```

**Docker setup:**

```bash
docker run -d -p 5002:5000 serengil/deepface
```

**Best for:** Multiple embedding models (VGG-Face, Facenet, ArcFace, etc.)

#### 3. **CompreFace Backend**

```java

@Bean
public VisionTemplate visionTemplate() {
    CompreFaceBackend backend = new CompreFaceBackend(
            "http://localhost:8000",
            "your-api-key"
    );
    return new VisionTemplate(backend);
}
```

**Docker setup:**

```bash
docker-compose -f compreface-docker-compose.yml up -d
```

**Best for:** Enterprise deployments with REST API, face collections

#### 4. **FaceBytes Backend**

```java

@Bean
public VisionTemplate visionTemplate() {
    FaceBytesBackend backend = new FaceBytesBackend();
    return new VisionTemplate(backend);
}
```

**Best for:** Lightweight, embedded face embeddings without external services

### Configuration Flexibility

The current `VisionTemplateConfiguration` uses `@ConditionalOnMissingBean`, meaning:

✅ **Default works out-of-the-box** - Uses OpenCV for development/testing
✅ **Easy to override** - Define your own `VisionTemplate` bean for production
✅ **No breaking changes** - All existing tools continue to work

### Example: Switching to InsightFace

Create an `application.yml` in your MCP server:

```yaml
spring:
  vision:
    backend: insightface
    insightface:
      url: http://localhost:5001
```

Then in your configuration:

```java

@Configuration
public class CustomVisionConfig {

    @Value("${spring.vision.insightface.url:http://localhost:5001}")
    private String insightFaceUrl;

    @Bean
    public VisionTemplate visionTemplate() {
        InsightFaceBackend backend = new InsightFaceBackend(insightFaceUrl);
        return new VisionTemplate(backend);
    }
}
```

### Recommendation Summary

| Use Case                  | Backend          | Configuration Needed      |
|---------------------------|------------------|---------------------------|
| Quick testing, demos      | OpenCV (default) | ✅ None - works out of box |
| Basic face detection      | OpenCV (default) | ✅ None                    |
| High-quality embeddings   | InsightFace      | Docker service + config   |
| Multiple embedding models | DeepFace         | Docker service + config   |
| Enterprise production     | CompreFace       | Docker compose + API key  |
| Lightweight embedded      | FaceBytes        | Additional dependency     |

---

