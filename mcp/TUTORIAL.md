<div align="center">
  <h1 align="center">MCP Server Configuration Tutorial</h1>
  <p align="center">
    <strong>A quick guide to configuring MCP server with custom tools.</strong>
  </p>
</div>

## 🎬 Introduction

This tutorial provides a script for a short, 60-second video that shows how to add a custom tool to the MCP server by adding a configuration to your GitHub MCP config file.

## 🚀 Tutorial Outline

The video will cover these steps:

1. **Locate the MCP configuration file.**
2. **Add the JSON configuration for the new tool.**
3. **Explain what the configuration does.**
4. **Show the tool being used (conceptually).**

## 📝 Script Outline

### Introduction (0-15 seconds)

* **Hook**: "Want to use your own tools with the MCP server? Here’s how to configure it in under 60 seconds."
* **Introduce MCP Configuration**: "MCP server can be extended with custom tools by simply adding a JSON configuration to your project's `.github/mcp-config.json` file."

### Implementation (15-45 seconds)

* **Show the configuration file**: "First, open your MCP configuration file in your repository."
* **Show adding the JSON config**: "Next, add the configuration for your tool. For `spring-vision`, we'll add this JSON snippet."
* **Explain the code**: "This configuration tells the MCP server to run the `spring-vision` tool using a Docker container. The `command` is `docker` and the `args` specify the container to run."
* **Show it in action**: "Now, when you use `spring-vision` through MCP, it will execute this Docker command."

### Conclusion (45-60 seconds)

* **Recap**: "And that’s it! You’ve just configured a custom tool for the MCP server."
* **Call to Action**: "Check out the documentation to learn more about configuring other tools."

## 💻 Code Example

Here is the JSON configuration to add to your MCP config file:

```json
"spring-vision": {
"command": "docker",
"args": [
"run",
"-i",
"--rm",
"codesapienbe/spring-vision:latest"
]
}
```

## 🧪 Testing with stdio

Once your Docker image is built, you can test it using stdio commands:

### Test Initialize

```bash
# Initialize an MCP session
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"clientInfo":{"name":"test","version":"1.0"}}}' | \
docker run -i --rm codesapienbe/spring-vision:latest
```

### Test List Tools

```bash
# List all available tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | \
docker run -i --rm codesapienbe/spring-vision:latest
```

### Test Call countFaces Tool

```bash
# Count faces in an image (replace with actual image URL)
echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"countFaces","arguments":{"imageUrl":"https://example.com/image.jpg"}}}' | \
docker run -i --rm codesapienbe/spring-vision:latest
```

### Interactive Testing

You can also run the container interactively and type JSON-RPC messages:

```bash
docker run -i --rm codesapienbe/spring-vision:latest
# Then paste your JSON-RPC messages and press Enter
```

### Using with MCP Clients

The most common way to use the server is through MCP clients like Claude Desktop or Cline. Add the configuration from the "Code Example" section above to your MCP client's configuration file, and the client will automatically manage the Docker container lifecycle.

## 📢 Call to Action

* **Like and Subscribe**: "If you found this helpful, please like, subscribe, and share!"
* **Documentation**: "For more detailed guides on MCP server configuration, check out the official documentation."
