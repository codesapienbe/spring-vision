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

## 📝 Notes

- **Stdio transport** is the standard MCP communication method
- The server communicates via stdin/stdout when launched by MCP clients
- Logs are written to `/app/logs/mcp.log` inside the container (separate from stdio)
- The `--rm` flag ensures containers are cleaned up after use
- The `-i` flag enables interactive stdin for MCP communication

---
