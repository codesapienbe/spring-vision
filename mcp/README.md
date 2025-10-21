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
jbang https://github.com/codesapienbe/spring-vision/releases/download/v0.0.3/mcp-0.0.3.jar
```

This downloads and runs the latest JAR directly, no local build required. Ready for MCP client connections!

**That's it!** Your MCP server is now running and can be configured in Claude Desktop, Cline, or any MCP client.

---

## 🔧 MCP Client Configuration

Configure your MCP client to use the Spring Vision server. The server uses stdio transport, so specify the command to run the JAR remotely.

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": [
        "https://github.com/codesapienbe/spring-vision/releases/download/v0.0.3/mcp-0.0.3.jar"
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
      "command": "jbang",
      "args": [
        "https://github.com/codesapienbe/spring-vision/releases/download/v0.0.1/mcp-0.0.1.jar"
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

## 🚀 For Developers

If you're contributing to the project, you can build and run locally:

### Prerequisites

- **JBang** installed ([https://jbang.dev/](https://jbang.dev/))
- **Java 21+** installed

### Build and Run

From the project root:

```bash
# Build the project
make build

# Run the MCP server
jbang run.java
```
