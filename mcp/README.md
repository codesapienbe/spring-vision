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

### ⚡ JBang MCP Server (Quick local run)

If you want a lightweight developer-friendly way to run the MCP server locally without Docker, you can use the provided JBang launcher. This is great for quick testing, CI, or when you don't want to run containers.

Prerequisites:

- Java 21+ installed (JBang runs on Java 21+ as specified in the launcher)
- jbang installed (https://www.jbang.dev/) OR a built jar from Maven

Run using JBang (from the repository root):

```bash
# Run the launcher that pulls the published MCP artifact and starts the server
jbang mcp/.jbang/mcp.java
```

You can pass JVM options via the `JAVA_OPTIONS` environment variable (the launcher already sets sensible defaults in the script):

```bash
JAVA_OPTIONS='-Xms64m -Xmx512m' jbang mcp/.jbang/mcp.java
```

Run in background (simple example):

```bash
nohup jbang mcp/.jbang/mcp.java > mcp.log 2>&1 &
```

Build and run a packaged jar (if you prefer not to use JBang):

```bash
# from the mcp module
cd mcp
mvn -DskipTests package
# then run the produced jar (check exact artifact name in target/)
java -jar target/<mcp-artifact>.jar
```

When to use JBang vs Docker:

- JBang: very fast to start, ideal for local development and CI. Requires Java/jbang on the host. Easier to attach debuggers and to iterate on local code if you build from source.
- Docker: recommended for production and reproducible deployments. Isolates dependencies and environment.

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

**Using JBang (Quick local run):**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": [
        "mcp/.jbang/mcp.java"
      ]
    }
  }
}
```

**Using a packaged jar (no JBang):**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "java",
      "args": [
        "-jar",
        "path/to/mcp.jar"
      ]
    }
  }
}
```

**Important:** The `-i` flag is crucial for Docker as it enables interactive mode for stdio communication. For JBang/Java runs, the process must remain attached to stdio (don't run one-off background shells that immediately exit) so the client can communicate over stdio.

---

## 🔧 Available Vision Tools

Spring Vision MCP now exposes **24 tools** for advanced computer vision tasks:

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

### Face Comparison

11. **compareFacesFromUrls** - Compare faces from multiple image URLs
12. **compareFacesFromBase64** - Compare faces from base64 images

### 🆕 Metadata Extraction (NEW)

13. **extractMetaData** - Extract EXIF, GPS, and camera metadata (raw bytes)
14. **extractMetaDataUrl** - Extract metadata from URL
    - Extracts GPS coordinates (latitude, longitude, altitude)
    - Camera information (make, model, settings)
    - Timestamps and capture information
    - Copyright and author information
    - Image properties (dimensions, orientation)
    - Useful for photo organization, forensics, location tracking

### 🆕 Pose Estimation

15. **detectPoses** - Detect human body poses and skeletal keypoints (raw bytes)
16. **detectPosesUrl** - Detect poses from URL
    - Returns skeletal keypoints (shoulders, elbows, knees, hips, etc.)
    - Useful for fitness apps, sports analysis, ergonomics

### 🆕 Hand Detection & Gesture Recognition

17. **detectHands** - Detect hands and hand landmarks (raw bytes)
18. **detectHandsUrl** - Detect hands from URL
    - Returns hand landmarks including finger positions and palm keypoints
    - Useful for gesture recognition, sign language, HCI applications

### 🆕 Barcode & QR Code Detection

19. **detectBarcodes** - Detect and decode barcodes/QR codes (raw bytes)
20. **detectBarcodesUrl** - Detect barcodes from URL
    - Supports QR codes, EAN, UPC, Code 128, and more
    - Returns decoded values, barcode types, and locations
    - Useful for inventory management, product scanning, ticketing

### 🆕 Landmark Detection

21. **detectLandmarks** - Detect geographic landmarks and famous places (raw bytes)
22. **detectLandmarksUrl** - Detect landmarks from URL
    - Identifies famous buildings, monuments, and geographic features
    - Returns landmark names, confidence scores, and location information
    - Useful for travel apps, photo organization, location tagging

### 🆕 Image Annotation

23. **annotateImage** - Annotate images with bounding boxes and labels (raw bytes)
24. **annotateImageUrl** - Annotate images from URL
    - Supports multiple detection types (FACE, OBJECT, TEXT, POSE, HAND, BARCODE)
    - Draws bounding boxes and labels on detected objects
    - Returns annotated image as base64 along with detection details
    - Useful for visualization, debugging, creating training data

---

## 🎯 Feature Details

### Face Comparison

The face comparison tools use **cosine similarity** on face embeddings to verify if multiple images show the same person.

**Parameters:**

- **imageUrls** / **base64Images**: List of images (minimum 2)
- **threshold** (optional): Similarity threshold (0.0 to 1.0, default: 0.6)

**Similarity Thresholds:**

- **0.8+** = Very High confidence match
- **0.7-0.8** = High confidence match
- **0.6-0.7** = Medium confidence (default threshold)
- **0.5-0.6** = Low confidence
- **<0.5** = Different people / Very Low confidence

**Example Response:**

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

### Metadata Extraction

Extracts comprehensive metadata embedded in images including:

**GPS Metadata:**

- Latitude and longitude coordinates
- Altitude information
- GPS timestamp

**EXIF Metadata:**

- Camera make and model
- Image dimensions and orientation
- Capture datetime
- Software used
- Copyright and artist information

**Camera Settings:**

- Exposure time and ISO
- F-number (aperture)
- Flash status
- Focal length
- White balance

**Use Cases:**

- Photo organization by location and date
- Copyright and forensic analysis
- Travel photography cataloging
- Image authentication
- Location-based services

**Example Response:**

```json
{
  "metadata": [
    {
      "label": "gps",
      "type": "gps",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "altitude": "15m"
    },
    {
      "label": "exif",
      "type": "exif",
      "camera_make": "Canon",
      "camera_model": "EOS 5D Mark IV",
      "datetime": "2025:10:11 14:30:00"
    },
    {
      "label": "camera_settings",
      "type": "camera_settings",
      "iso": "400",
      "exposure_time": "1/250s",
      "f_number": "f/2.8"
    }
  ],
  "count": 3
}
```

### Barcode Detection

Supports multiple barcode formats:

- QR Code
- EAN-13, EAN-8
- UPC-A, UPC-E
- Code 128, Code 39
- PDF417, DataMatrix

**Use Cases:**

- Product scanning
- Inventory management
- Ticket validation
- Document tracking

### Image Annotation

Visualizes detection results by drawing:

- Bounding boxes around detected objects
- Labels with object names
- Confidence scores

**Supported Detection Types:**

- FACE - Annotate detected faces
- OBJECT - Annotate detected objects
- TEXT - Annotate detected text regions
- POSE - Annotate body poses
- HAND - Annotate hands
- BARCODE - Annotate barcodes/QR codes

**Parameters:**

- **imageBytes** / **imageUrl**: Input image
- **detectionType**: Type of detection (default: "OBJECT")
- **drawBoxes**: Draw bounding boxes (default: true)
- **drawLabels**: Draw labels (default: true)

---

## 📊 Example Usage

### Pose Estimation

```
"Analyze the yoga pose in this image: https://example.com/yoga.jpg"
```

### Hand Gesture Recognition

```
"What hand gesture is shown in this image?"
```

### Barcode Scanning

```
"Scan the QR code in this image and tell me what it says"
```

### Landmark Identification

```
"What landmark is shown in this photo?"
```

### Image Annotation

```
"Annotate all the people in this image with bounding boxes"
```

---

## 🔍 Backend Support

Different vision backends support different capabilities:

| Capability        | MediaPipe | OpenCV | YOLO | Tesseract |
|-------------------|-----------|--------|------|-----------|
| Face Detection    | ✅         | ✅      | ✅    | ❌         |
| Object Detection  | ✅         | ✅      | ✅    | ❌         |
| Text/OCR          | ❌         | ❌      | ❌    | ✅         |
| Pose Estimation   | ✅         | ❌      | ✅    | ❌         |
| Hand Detection    | ✅         | ❌      | ❌    | ❌         |
| Barcode Detection | ❌         | ✅      | ❌    | ❌         |
| Face Embeddings   | ✅         | ✅      | ❌    | ❌         |
| Annotation        | ✅         | ✅      | ✅    | ❌         |

---

## 🚀 Getting Started

1. Build the project: `make build`
2. (Optional) Push to Docker Hub: `make deploy`
3. Add to your MCP client config (see above)
4. Restart your AI assistant
5. Explore new capabilities:
    - "Detect faces in this image: [image_url]"
    - "Recognize this face: [image_url]"
    - "Extract text from this image: [image_url]"
    - "Compare these two faces: [image_url1], [image_url2]"
    - "Analyze the pose in this image: [image_url]"
    - "What gesture is shown in this image: [image_url]?"
    - "Scan the barcode in this image: [image_url]"
    - "What landmark is this: [image_url]?"
    - "Annotate objects in this image: [image_url]"

**That's it!** Your AI now has advanced computer vision superpowers! 🦸‍♂️
