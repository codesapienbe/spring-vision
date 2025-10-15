# 🚀 Spring Vision MCP Server

MCP (Model Context Protocol) server for Spring Vision - gives your AI assistant computer vision capabilities! 👁️

Built with **Spring AI 1.0.3** and the **Model Context Protocol** standard.

---

## 🔄 Architecture

**Spring Vision MCP Server uses HTTP/SSE (Server-Sent Events) transport:**

- ✅ **Runs as a persistent HTTP server** on port 8081
- ✅ **SSE transport** for reliable client-server communication
- ✅ **Proper JSON-RPC** message handling (no log pollution)
- ✅ **Clean separation** between application logs and protocol messages

This architecture ensures:

- **Stability**: HTTP transport is more reliable than stdio
- **Debugging**: Logs don't interfere with MCP protocol messages
- **Flexibility**: Multiple clients can connect simultaneously
- **Production-ready**: Standard web server deployment

---

## 🐳 Quick Start with Docker Compose (Recommended)

**The easiest way to run Spring Vision MCP Server with SSE support:**

### Prerequisites

- **Docker** and **Docker Compose** installed ([https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/))

### Build the Docker Image

From the project root:

```bash
mvn clean install -pl mcp -am
```

This will create the `spring-vision:1.0.4` Docker image using Jib.

### Start the Server

```bash
cd mcp
docker-compose up -d
```

The server will start on **http://localhost:8081** with the MCP endpoint at **http://localhost:8081/mcp**

### Manage the Server

```bash
# View logs
docker-compose logs -f

# Stop the server
docker-compose down

# Restart the server
docker-compose restart

# Check status
docker-compose ps
```

---

## ⚡ Alternative: Quick Start with JBang

If you prefer not to use Docker:

### Prerequisites

- **Java 21+** installed
- **JBang** installed ([https://www.jbang.dev/](https://www.jbang.dev/))

### Installation

```bash
# Install JBang (if not already installed)
curl -Ls https://sh.jbang.dev | bash -s - app setup
```

### Running the Server

JBang will automatically download and run the published artifact from Maven Central:

```bash
jbang io.github.codesapienbe.springvision:mcp:1.0.4
```

Note: When using JBang, the server runs on port 8080 instead of 8081.

### JVM Options (Optional)

You can pass JVM options:

```bash
jbang -Xmx512m -Xms64m io.github.codesapienbe.springvision:mcp:1.0.4
```

---

## 🔧 MCP Client Configuration

### GitHub Copilot (IntelliJ)

**For Docker Compose deployment (Recommended):**

Add to `~/.config/github-copilot/intellij/mcp.json`:

```json
{
  "servers": {
    "spring-vision": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:8081/mcp"
      }
    }
  }
}
```

**For JBang deployment:**

```json
{
  "servers": {
    "spring-vision": {
      "command": "jbang",
      "args": [
        "io.github.codesapienbe.springvision:mcp:1.0.4"
      ],
      "transport": {
        "type": "sse",
        "url": "http://localhost:8080/mcp"
      }
    }
  }
}
```

### Claude Desktop

**For Docker Compose deployment (Recommended):**

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "spring-vision": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:8081/mcp"
      }
    }
  }
}
```

**For JBang deployment:**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": [
        "io.github.codesapienbe.springvision:mcp:1.0.4"
      ],
      "transport": {
        "type": "sse",
        "url": "http://localhost:8080/mcp"
      }
    }
  }
}
```

### Cline / Other MCP Clients

**For Docker Compose deployment (Recommended):**

Add to your MCP client's configuration file:

```json
{
  "mcpServers": {
    "spring-vision": {
      "transport": {
        "type": "sse",
        "url": "http://localhost:8081/mcp"
      }
    }
  }
}
```

**For JBang deployment:**

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": [
        "io.github.codesapienbe.springvision:mcp:1.0.4"
      ],
      "transport": {
        "type": "sse",
        "url": "http://localhost:8080/mcp"
      }
    }
  }
}
```

---

## 📝 Notes

- **Docker Compose deployment** runs on port **8081** (to avoid conflicts with other services)
- **JBang deployment** runs on port **8080**
- With SSE transport, the MCP client simply connects to the HTTP endpoint
- The server must be running before the MCP client connects
- No need for `command` and `args` in MCP config when using Docker Compose (server runs independently)
- Example configurations are available in the `examples/` directory

---
