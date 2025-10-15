# 🚀 Spring Vision MCP Server

MCP (Model Context Protocol) server for Spring Vision - gives your AI assistant computer vision capabilities! 👁️

Built with **Spring AI 1.0.0** and the **Model Context Protocol** standard.

---

## 🔄 Architecture

**Spring Vision MCP Server uses stdio (Standard Input/Output) transport:**

- ✅ **Stdio transport** - Standard MCP communication via stdin/stdout
- ✅ **Docker-based deployment** - Containerized for easy distribution
- ✅ **Proper JSON-RPC** message handling (no log pollution)
- ✅ **Clean separation** between application logs and protocol messages

This architecture ensures:

- **Compatibility**: Works with all standard MCP clients (Claude Desktop, Cline, etc.)
- **Simplicity**: Standard stdio transport as designed by the MCP specification
- **Debugging**: Logs written to file, keeping stdio clean for MCP communication
- **Portability**: Easy to integrate with any MCP client configuration

---

## 🐳 Quick Start with Docker

**The easiest way to run Spring Vision MCP Server:**

### Prerequisites

- **Docker** installed ([https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/))

### Build the Docker Image

From the project root:

```bash
# Build everything (JAR + Docker image)
make build

# Or manually:
mvn clean package -pl mcp -am
docker build -t codesapienbe/spring-vision:latest -f mcp/Dockerfile .
```

### Test the Server

```bash
# Test with a simple initialize message
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"clientInfo":{"name":"test","version":"1.0"}}}' | \
docker run -i --rm codesapienbe/spring-vision:latest
```

### Recommended run (keep server active and suppress JVM native-access warnings)

Use this command to run the container with the recommended JVM flag so native library warnings are suppressed and the server stays active for MCP clients:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"clientInfo":{"name":"test","version":"1.0"}}}' | \
docker run -i --rm -e JAVA_OPTS='--enable-native-access=ALL-UNNAMED' codesapienbe/spring-vision:latest
```

---

## 🔧 MCP Client Configuration

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%/Claude/claude_desktop_config.json` (Windows):

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

### Cline (VS Code Extension)

Add to your Cline MCP settings:

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

### GitHub Copilot / Other MCP Clients

For any MCP client that supports stdio transport:

```json
{
  "servers": {
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

---

## 📊 Logging & Monitoring

The MCP server uses **structured JSON logging** to ensure reliable operation and easy debugging.

### Logging Architecture

- **stdout**: Reserved for MCP JSON-RPC protocol messages ONLY
- **stderr**: Application logs in JSON format
- **Files**: Persistent JSON logs for debugging and monitoring

### Log Files

- `logs/mcp.json.log` - All application logs (JSON format)
- `logs/mcp-error.json.log` - Error logs only (JSON format)

### Viewing Logs

```bash
# View real-time logs (pretty-printed JSON)
docker run -i --rm codesapienbe/spring-vision:latest 2>&1 | jq

# View log files (if mounted)
docker run -i --rm -v $(pwd)/logs:/app/logs codesapienbe/spring-vision:latest
tail -f logs/mcp.json.log | jq

# Filter by event type
jq 'select(.event == "count_faces_success")' logs/mcp.json.log

# View errors only
tail -f logs/mcp-error.json.log | jq
```

### Testing Logging

Run the included test script to verify JSON logging is working:

```bash
cd mcp
./test-logging.sh
```

### Log Format

All logs follow this JSON structure:

```json
{
  "timestamp": "2025-10-15T15:20:30.123Z",
  "level": "INFO",
  "logger": "io.github.codesapienbe.springvision.mcp.VisionTool",
  "thread": "main",
  "message": "countFaces completed successfully",
  "event": "count_faces_success",
  "face_count": 3,
  "avg_confidence": 0.9234,
  "processing_time_ms": 145
}
```


## Example Prompts

>> Count faces in https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg

### Key Features

✅ **No parsing issues**: Every log is valid JSON  
✅ **Structured data**: Easy to filter, search, and analyze  
✅ **Event-based**: All logs include an `event` field for categorization  
✅ **Production-ready**: Automatic rotation and retention policies  

For detailed logging documentation, see [LOGGING.md](LOGGING.md).

---

## 📝 Notes

- **Stdio transport** is the standard MCP communication method
- The server communicates via stdin/stdout when launched by MCP clients
- All logs use JSON format and are written to stderr/files (NOT stdout)
- The `--rm` flag ensures containers are cleaned up after use
- The `-i` flag enables interactive stdin for MCP communication

---
