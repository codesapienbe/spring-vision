# Embedding Extraction Fix - Multi-Backend Support

## Problem

When both OpenCV and FaceBytes backends were enabled in `application.yml`, the MCP `VisionTool` was still trying to use OpenCV backend for embedding extraction, which failed because OpenCV doesn't implement the `EmbeddingCapability` interface.

### Error Log
```
io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException: 
Embedding extraction is not supported by backend 'opencv'
```

## Root Cause

The `VisionTemplateConfiguration` in the MCP module was hardcoded to create a `VisionTemplate` with the OpenCV backend, ignoring other available backends that might have better capabilities (like embedding extraction).

## Solution

Modified `VisionTemplateConfiguration` to intelligently select the best available backend based on capabilities:

### Changes Made

1. **Smart Backend Selection** (`mcp/src/main/java/.../VisionTemplateConfiguration.java`)
   - Now accepts `List<VisionBackend>` to discover all registered backends
   - Prefers embedding-capable backends (FaceBytes, InsightFace, CompreFace, etc.)
   - Falls back to OpenCV if no embedding backend is available
   - Provides comprehensive logging about backend selection

2. **Configuration Update** (`mcp/src/main/resources/application.yml`)
   - Enabled both OpenCV and FaceBytes backends
   - OpenCV: Used for face detection (primary)
   - FaceBytes: Used for face detection AND embedding extraction

### Backend Selection Logic

```java
// 1. Scan all available VisionBackend beans
// 2. Filter for embedding-capable backends (FaceBytes, InsightFace, etc.)
// 3. Select first healthy embedding-capable backend
// 4. If none found, use any healthy backend
// 5. If still none, create default OpenCV backend with warning
```

### Capabilities by Backend

| Backend     | Face Detection | Embedding Extraction | Notes                          |
|-------------|----------------|---------------------|--------------------------------|
| OpenCV      | ✅ YES         | ❌ NO               | Fast, local, no embeddings     |
| FaceBytes   | ✅ YES         | ✅ YES              | Java port of DeepFace          |
| InsightFace | ✅ YES         | ✅ YES              | High accuracy, GPU support     |
| CompreFace  | ✅ YES         | ✅ YES              | Service-based, scalable        |
| DeepFace    | ✅ YES         | ✅ YES              | Python-based (if available)    |

## Configuration

### Enable Multiple Backends

```yaml
spring:
  vision:
    opencv:
      enabled: true
      confidence-threshold: 0.7
    facebytes:
      enabled: true
      detector-backend: opencv  # Options: opencv, retinaface, dlib, mtcnn
```

### Backend Selection Priority

When multiple backends are enabled:
1. **Embedding-capable backends** are preferred (FaceBytes, InsightFace, CompreFace)
2. **Healthy backends** are checked (via `isHealthy()` method)
3. **First match** is selected (deterministic, based on Spring bean ordering)
4. **OpenCV fallback** is used if no other backend is available

## Verification

### Startup Logs

After the fix, you should see logs like:

```
INFO  VisionTemplateConfiguration : Initializing VisionTemplate - scanning for available backends...
INFO  VisionTemplateConfiguration : Found 2 registered backend(s)
INFO  VisionTemplateConfiguration :   - opencv: OpenCV Vision Backend (embeddings: false)
INFO  VisionTemplateConfiguration :   - facebytes: FaceBytes Backend (embeddings: true)
INFO  VisionTemplateConfiguration : Selected embedding-capable backend: facebytes (supports full MCP tool functionality)
INFO  VisionTemplateConfiguration : === VisionTemplate Configuration Summary ===
INFO  VisionTemplateConfiguration : Backend: facebytes (FaceBytes Backend)
INFO  VisionTemplateConfiguration : Embeddings Support: YES
INFO  VisionTemplateConfiguration : Supported Detection Types: [FACE, BARCODE]
INFO  VisionTemplateConfiguration : ============================================
```

### MCP Tool Usage

Both tools should now work:

```json
// Face counting - works with both backends
{
  "name": "countFaces",
  "arguments": {
    "imageUrl": "https://example.com/image.jpg"
  }
}

// Embedding extraction - now works with FaceBytes
{
  "name": "extractEmbeddings",
  "arguments": {
    "imageUrl": "https://example.com/image.jpg"
  }
}
```

## Benefits

1. **Automatic Backend Selection**: No manual configuration needed
2. **Capability-Based Routing**: Best backend is chosen for each operation
3. **Graceful Degradation**: Falls back to OpenCV if no embedding backend available
4. **Clear Logging**: Shows which backends are available and why one was selected
5. **Zero Breaking Changes**: Existing configurations continue to work

## Testing

To test the fix:

1. Start the MCP server:
   ```bash
   cd mcp
   mvn spring-boot:run
   ```

2. Call the `extractEmbeddings` tool:
   ```bash
   # Via MCP client (Cursor, VS Code, etc.)
   # The tool should now succeed with FaceBytes backend
   ```

3. Check logs for backend selection:
   ```bash
   tail -f logs/mcp.log | grep VisionTemplateConfiguration
   ```

## Future Improvements

1. **Explicit Backend Selection**: Allow users to specify preferred backend via `spring.vision.preferred-backend=facebytes`
2. **Multi-Backend Routing**: Route different operations to different backends (e.g., OpenCV for detection, FaceBytes for embeddings)
3. **Backend Health Monitoring**: Automatically switch backends if primary becomes unhealthy
4. **Load Balancing**: Distribute requests across multiple backends for better performance

## Related Files

- `mcp/src/main/java/.../config/VisionTemplateConfiguration.java` - Smart backend selection
- `mcp/src/main/resources/application.yml` - Backend configuration
- `core/src/main/java/.../VisionTemplate.java` - Capability-based routing
- `core/src/main/java/.../FaceBytesBackend.java` - Embedding extraction implementation
- `core/src/main/java/.../OpenCvVisionBackend.java` - Face detection only

## References

- [VisionBackend Interface](../core/src/main/java/io/github/codesapienbe/springvision/core/VisionBackend.java)
- [EmbeddingCapability Interface](../core/src/main/java/io/github/codesapienbe/springvision/core/capabilities/EmbeddingCapability.java)
- [VisionTemplate](../core/src/main/java/io/github/codesapienbe/springvision/core/VisionTemplate.java)

