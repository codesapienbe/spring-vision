# Spring Vision MCP Server - Changelog

## Version 1.0.2 (October 11, 2025)

### 🎉 NEW: 5 Advanced Computer Vision Capabilities

Added 10 new MCP tools (22 total) covering the most demanding computer vision features:

#### 1. Pose Estimation 🧘

- **detectPoses** - Detect human body poses from raw bytes
- **detectPosesUrl** - Detect poses from image URL

Returns skeletal keypoints including shoulders, elbows, wrists, hips, knees, ankles, and more.
Use cases: Fitness analysis, sports coaching, ergonomics assessment, motion capture.

#### 2. Hand Detection & Gesture Recognition 👋

- **detectHands** - Detect hands and landmarks from raw bytes
- **detectHandsUrl** - Detect hands from image URL

Returns 21 landmark points per hand including finger joints, palm keypoints, and wrist position.
Use cases: Gesture recognition, sign language interpretation, VR interactions, touchless interfaces.

#### 3. Barcode & QR Code Detection 📱

- **detectBarcodes** - Detect and decode barcodes/QR codes from raw bytes
- **detectBarcodesUrl** - Detect barcodes from image URL

Supports QR, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, PDF417, DataMatrix, and more.
Returns decoded values, barcode types, and locations.
Use cases: Inventory management, product scanning, ticket validation, document tracking.

#### 4. Landmark Detection 🗼

- **detectLandmarks** - Detect geographic landmarks from raw bytes
- **detectLandmarksUrl** - Detect landmarks from image URL

Identifies famous buildings, monuments, historical sites, and natural landmarks.
Returns landmark names, confidence scores, and location information.
Use cases: Travel apps, photo organization, location-based services, educational applications.

#### 5. Image Annotation 🎨

- **annotateImage** - Annotate images with bounding boxes and labels from raw bytes
- **annotateImageUrl** - Annotate images from URL

Visualizes detection results by drawing bounding boxes, labels, and confidence scores.
Supports all detection types: FACE, OBJECT, TEXT, POSE, HAND, BARCODE.
Returns annotated image as base64 along with detection metadata.
Use cases: Visualization, debugging, creating training data, quality assurance.

### 📊 Updated Statistics

- **Total MCP Tools**: 22 (was 12)
- **New Capabilities**: 5
- **New API Endpoints**: 10
- **Supported Detection Types**: 8 (FACE, OBJECT, TEXT, POSE, HAND, BARCODE, LANDMARK, CUSTOM)

### 🔧 Technical Improvements

- All new tools follow consistent patterns with both byte array and URL variants
- Comprehensive error handling with descriptive error messages
- Integration with VisionTemplate's capability routing system
- Support for backend-specific implementations via capability interfaces
- Graceful fallback when backends don't support specific capabilities

### 📖 Documentation Updates

- Updated README.md with all 22 tools
- Added feature details for each new capability
- Included use cases and example usage
- Added backend support matrix showing which backends support which capabilities
- Updated example prompts for AI assistants

---

## Version 1.0.1 (October 10, 2025)

### 🎉 New Features

#### Face Comparison Functionality

Added two new tools for comparing faces across multiple images:

1. **compareFacesFromUrls** - Compare faces from image URLs
    - Takes a list of image URLs (minimum 2)
    - Optional similarity threshold (default: 0.6)
    - Returns pairwise comparisons with detailed similarity scores
    - Provides verdict: "same person" or "different people"

2. **compareFacesFromBase64** - Compare faces from base64 images
    - Same functionality as URLs but accepts base64-encoded images
    - Useful for uploaded images or data URIs

#### Key Capabilities

- **Cosine Similarity Calculation**: Compares face embeddings using industry-standard cosine similarity
- **Pairwise Comparison**: Compares all image pairs when more than 2 images provided
- **Confidence Levels**:
    - Very High (0.8+)
    - High (0.7-0.8)
    - Medium (0.6-0.7) - default threshold
    - Low (0.5-0.6)
    - Very Low (<0.5)
- **Smart Face Selection**: Uses first detected face if multiple faces in image
- **Detailed Results**: Returns min/max/average similarity, per-pair scores, and overall verdict

### 🔧 Stability Improvements

#### Configuration Enhancements

- **Added application.properties**: Proper Spring Boot configuration for MCP stdio mode
    - Disabled web application mode (no web server needed)
    - Disabled banner for cleaner stdio output
    - Configured proper logging levels
    - Set graceful shutdown timeout

#### Logging Improvements

- **Added logback-spring.xml**: Professional logging configuration
    - Logs to stderr to avoid interfering with stdio protocol
    - Rolling file appender with 7-day retention
    - Reduced verbosity of framework logs
    - Separate log file (mcp.log) for persistent logging

#### Docker Optimization

- **Simplified Dockerfile**: Optimized for MCP stdio mode
    - Removed unnecessary port exposures
    - Uses eclipse-temurin:21-jdk-slim base image
    - Streamlined for container deployment

### 🐛 Bug Fixes

1. **Fixed stdio communication issues**
    - Proper configuration for stdin/stdout protocol
    - Logs directed to stderr instead of stdout

2. **Fixed embedding type mismatch**
    - Changed from double[] to float[] to match VisionTemplate API
    - Updated cosine similarity calculation for float arrays

3. **Improved error handling**
    - Better validation of input parameters
    - Clearer error messages
    - Graceful handling of missing faces in images

### 📚 Documentation Updates

#### Streamlined for Docker Deployment

- **README.md**: Completely rewritten for Docker-first approach
    - Removed bash script references
    - Focus on Makefile automation
    - Clear Docker Hub deployment instructions
    - MCP client configuration examples

#### Updated Guides

- **TROUBLESHOOTING.md**: Docker-specific troubleshooting
- **CHANGELOG.md**: Version history (this file)
- **IMPLEMENTATION_SUMMARY.md**: Technical details

### 🚀 Deployment

#### Docker-Only Approach

- Removed bash scripts (start-mcp.sh, test-face-comparison.sh)
- Leveraging existing Makefile automation:
    - `make build` - Build project and create Docker image
    - `make deploy` - Push to Docker Hub
- Simplified deployment workflow

### 🔒 Stability Guarantees

The MCP server is production-ready with:

- ✅ Proper stdio protocol handling
- ✅ Clean log separation (stderr for logs, stdout for MCP)
- ✅ Graceful error handling
- ✅ Docker-based consistent deployment
- ✅ Automated build and deploy via Makefile

### 🚀 Usage

#### Deploy to Docker Hub

```bash
make build   # Build and create image
make deploy  # Push to Docker Hub
```

#### MCP Client Configuration

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

#### Using Face Comparison

**With AI Assistant:**

```
"Compare these two photos and tell me if they're the same person:
 - Photo 1: https://example.com/id_photo.jpg
 - Photo 2: https://example.com/selfie.jpg"
```

**Response includes:**

- Similarity score (0.0 - 1.0)
- Match verdict (true/false)
- Confidence level (Very High, High, Medium, Low, Very Low)
- Overall verdict with explanation

### 🔄 Migration Notes

**Simplified Deployment:**

- No more bash scripts needed
- Docker is the only deployment method
- Existing Makefile handles all automation
- No breaking changes to MCP tools

### 📦 Dependencies

No new dependencies added. Uses existing VisionTemplate API.

### 🎯 Next Steps

Recommended improvements for future versions:

1. Add batch comparison with caching for performance
2. Support for face clustering (grouping similar faces)
3. Add face verification with confidence intervals
4. Support for storing and retrieving known face embeddings
5. Add metrics and monitoring endpoints

---

## Total Tools Available: 22

1. detect
2. detectBase64
3. detectUrl
4. ocr
5. ocrUrl
6. faces
7. facesUrl
8. extractEmbeddings
9. extractEmbeddingsBase64
10. extractEmbeddingsUrl
11. compareFacesFromUrls ✨ NEW
12. compareFacesFromBase64 ✨ NEW
13. detectPoses ✨ NEW
14. detectPosesUrl ✨ NEW
15. detectHands ✨ NEW
16. detectHandsUrl ✨ NEW
17. detectBarcodes ✨ NEW
18. detectBarcodesUrl ✨ NEW
19. detectLandmarks ✨ NEW
20. detectLandmarksUrl ✨ NEW
21. annotateImage ✨ NEW
22. annotateImageUrl ✨ NEW
