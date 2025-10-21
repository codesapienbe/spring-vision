# 🚀 Spring Vision MCP Server

MCP (Model Context Protocol) server for Spring Vision - gives your AI assistant computer vision capabilities! 👁️

Built with **Spring AI 1.0.0** and the **Model Context Protocol** standard.

---

## 🔄 Architecture

**Spring Vision MCP Server uses stdio (Standard Input/Output) transport:**

- ✅ **Stdio transport** - Standard MCP communication via stdin/stdout
- ✅ **JAR-based deployment** - Executable JAR for easy distribution
- ✅ **Proper JSON-RPC** message handling (no log pollution)
- ✅ **Clean separation** between application logs and protocol messages

This architecture ensures:

- **Compatibility**: Works with all standard MCP clients (Claude Desktop, Cline, etc.)
- **Simplicity**: Standard stdio transport as designed by the MCP specification
- **Debugging**: Logs written to file, keeping stdio clean for MCP communication
- **Portability**: Easy to integrate with any MCP client configuration

---

## ⚡ Quick Start (Easiest Way)

**Run Spring Vision MCP Server with a single command - no installation required!**

```bash
curl -s https://raw.githubusercontent.com/codesapienbe/spring-vision/main/run.sh | bash
```

This will:

- Download the latest Spring Vision MCP Server JAR automatically
- Start the server and keep it running
- Ready for MCP client connections

**That's it!** Your MCP server is now running and can be configured in Claude Desktop, Cline, or any MCP client.

---

## 🚀 Quick Start with JBang

Run the Spring Vision MCP Server using JBang with the remote JAR:

```bash
jbang https://github.com/codesapienbe/spring-vision/releases/download/v0.0.3/mcp-0.0.3.jar
```

This downloads and runs the latest JAR directly, no local build required.

---

## 🔧 MCP Client Configuration

Configure your MCP client to use the Spring Vision server. The server uses stdio transport, so specify the command to run the JAR.

### Download the JAR

First, download the latest JAR:

```bash
curl -L https://github.com/codesapienbe/spring-vision/releases/latest/download/mcp-0.0.3.jar -o mcp-0.0.3.jar
```

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-0.0.3.jar"
      ]
    }
  }
}
```

Replace `/path/to/mcp-0.0.3.jar` with the actual path to the downloaded JAR.

### Cline (VS Code Extension)

Add to your Cline MCP settings:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-0.0.3.jar"
      ]
    }
  }
}
```

### Docker (Alternative)

If you prefer Docker, use the containerized version:

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

---

## 📊 Logging

The MCP server uses structured JSON logging for reliable debugging.

- **stdout**: MCP protocol messages only
- **stderr/files**: Application logs in JSON format

### View Logs

```bash
# Real-time logs
tail -f logs/mcp.json.log | jq

# Errors only
tail -f logs/mcp-error.json.log | jq
```

For detailed logging documentation, see [LOGGING.md](LOGGING.md).

---

## 📝 Notes

- The server communicates via stdin/stdout for MCP protocol
- Logs are in JSON format and written to stderr/files, not stdout
- Requires Java 21+ to run the JAR
- For Docker, use `--rm` to clean up containers

---
