# 🚀 Using Spring Vision with Your AI Assistant 🤖

Welcome! This guide will show you how to connect the Spring Vision MCP server to your favorite AI assistants and development frameworks. This will give your assistant the power of computer vision! 👁️

This guide is for everyone—from beginners to experienced developers! 😊

---

## 🤔 What is Spring Vision?

In simple terms, **Spring Vision gives your AI assistant eyes**.

Once connected, your AI can analyze images to detect faces, objects, and text. It's like giving your assistant a new set of superpowers! 🦸

**Spring Vision MCP** is built using the **Model Context Protocol (MCP)** standard and **Spring AI 1.0.3**, making it easy to integrate with MCP-compatible clients and AI frameworks.

---

## 🏁 Step 1: Start the Spring Vision MCP Server 🐳

First, you need to get the Spring Vision MCP server running on your computer. We'll use Docker to make this super simple.

1. **Open your terminal**:
    * On Mac: `Applications > Utilities > Terminal`
    * On Windows: `PowerShell` or `Command Prompt`
    * On Linux: Your favorite terminal emulator

2. **Run the command**:
   Copy and paste the following command into your terminal and press **Enter**.

   ```bash
   docker run -d -p 8080:8080 --name spring-vision-mcp docker.io/codesapienbe/spring-vision:latest
   ```

3. **Check if it's running** ✅
   To make sure it's working, run this command:

   ```bash
   curl http://localhost:8080/actuator/health
   ```

   You should see a response indicating the service is ready:

   ```json
   {
     "status": "UP"
   }
   ```

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

### 3. **ocr** - Text Recognition

Extracts text from an image using OCR.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data

### 4. **faces** - Face Recognition

Recognizes and analyzes faces in an image.

- **Parameters:**
    - `imageBytes` (byte[]): Raw image data

---

## 🔌 Step 2: How to Connect Spring Vision

### An Important Note: Why Aren't Claude Desktop, Cursor, or VSCode on This List?

This is the most common question we get. While **Claude Desktop**, **Cursor**, and other popular AI assistants support MCP, they typically require MCP servers to be configured via their settings files or configuration systems.

The methods below work with **open systems** and frameworks that are designed to be connected to MCP servers programmatically or through simple configuration.

Here are the top ways to connect Spring Vision MCP server to an AI model:

### Method 1: Claude Desktop (MCP Native Support) ✨

Claude Desktop has native MCP support! You can configure Spring Vision as an MCP server.

1. **Locate Claude Desktop Configuration:**
    - On Mac: `~/Library/Application Support/Claude/claude_desktop_config.json`
    - On Windows: `%APPDATA%\Claude\claude_desktop_config.json`

2. **Add Spring Vision MCP Server:**
   Edit the configuration file and add the Spring Vision server:

   ```json
   {
     "mcpServers": {
       "spring-vision": {
         "url": "http://localhost:8080",
         "transport": "sse"
       }
     }
   }
   ```

3. **Restart Claude Desktop:** Close and reopen Claude Desktop.

4. **Start Using Vision Tools:** Claude will now have access to the vision tools and can analyze images!

### Method 2: Spring AI Applications (Java/Kotlin Developers) ☕

If you're building Java or Kotlin applications with Spring AI, you can easily integrate Spring Vision MCP.

1. **Add Spring AI MCP Client Dependency:**
   Add the Spring AI MCP client starter to your `pom.xml`:

   ```xml
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-starter-mcp-client</artifactId>
       <version>1.0.3</version>
   </dependency>
   ```

2. **Configure MCP Server Connection:**
   In your `application.yml` or `application.properties`:

   ```yaml
   spring:
     ai:
       mcp:
         client:
           servers:
             spring-vision:
               url: http://localhost:8080
               transport: sse
   ```

3. **Use the Tools in Your Code:**
   Spring AI will automatically discover and make the vision tools available to your AI model.

### Method 3: Python with MCP SDK 🐍

The official MCP SDK for Python makes it easy to connect to Spring Vision.

1. **Install the MCP SDK:**
   ```bash
   pip install mcp
   ```

2. **Connect to Spring Vision:**
   ```python
   from mcp import ClientSession, StdioServerParameters
   from mcp.client.stdio import stdio_client

   # For HTTP/SSE transport
   async with ClientSession(
       server_url="http://localhost:8080",
       transport="sse"
   ) as session:
       # List available tools
       tools = await session.list_tools()
       
       # Call a tool
       result = await session.call_tool(
           "detectBase64",
           arguments={
               "base64Image": "...",
               "detectionType": "FACE"
           }
       )
   ```

### Method 4: LangChain with MCP Integration 🐍

LangChain now supports MCP servers! You can easily add Spring Vision as a tool.

1. **Install Required Packages:**
   ```bash
   pip install langchain langchain-openai mcp
   ```

2. **Create an MCP Tool:**
   ```python
   from langchain.agents import create_openai_functions_agent, AgentExecutor
   from langchain_openai import ChatOpenAI
   from langchain.tools import Tool
   import requests

   def detect_objects(image_b64: str, detection_type: str = "FACE") -> dict:
       """Detect faces or objects in an image."""
       response = requests.post(
           "http://localhost:8080/api/mcp/tools/detectBase64",
           json={
               "base64Image": image_b64,
               "detectionType": detection_type
           }
       )
       return response.json()

   vision_tool = Tool(
       name="vision_detect",
       func=detect_objects,
       description="Detect faces or objects in images"
   )

   # Create agent with the tool
   llm = ChatOpenAI(model="gpt-4")
   agent = create_openai_functions_agent(llm, [vision_tool])
   agent_executor = AgentExecutor(agent=agent, tools=[vision_tool])
   ```

### Method 5: Custom MCP Client (Any Language) 🛠️

You can build a custom MCP client in any language that supports HTTP/SSE.

1. **MCP Tool Discovery Endpoint:**
   ```
   GET http://localhost:8080/mcp/tools
   ```

2. **Call a Tool:**
   ```
   POST http://localhost:8080/mcp/tools/{toolName}
   Content-Type: application/json

   {
     "arguments": {
       "base64Image": "...",
       "detectionType": "FACE"
     }
   }
   ```

3. **SSE for Real-time Updates:**
   ```
   GET http://localhost:8080/mcp/sse
   ```

---

## 🏗️ Building from Source

Want to build and run Spring Vision MCP from source?

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/codesapienbe/spring-vision.git
   cd spring-vision
   ```

2. **Build with Maven:**
   ```bash
   mvn clean install -DskipTests
   ```

3. **Run the MCP Server:**
   ```bash
   cd spring-vision-mcp
   mvn spring-boot:run
   ```

The MCP server will start on `http://localhost:8080`.

---

## 🔍 Technical Details

### Architecture

Spring Vision MCP is built with:

- **Spring Boot 3.1.3**: Modern Java framework
- **Spring AI 1.0.3**: Official Spring AI integration with MCP support
- **OpenCV**: Computer vision processing
- **Model Context Protocol (MCP)**: Standard protocol for AI tool integration

### Tool Annotations

Tools are defined using Spring AI's `@Tool` annotation:

```java

@Tool(description = "Detect objects in an image")
public Map<String, Object> detect(byte[] imageBytes, String detectionType) {
    // Implementation
}
```

Spring AI automatically:

- Discovers all `@Tool` annotated methods
- Generates JSON schemas for tool parameters
- Exposes them via MCP endpoints
- Handles serialization/deserialization

---

## 🎉 Step 3: Time to Play!

Now that you've connected Spring Vision MCP using your chosen method, it's time for the fun part! Ask your AI to analyze an image:

> "How many faces are in this image?"

> "What objects do you see in this picture?"

> "Can you read the text in this image?"

Enjoy your new, super-powered AI assistant with vision capabilities! ✨

---

## 📚 Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Spring Vision GitHub Repository](https://github.com/codesapienbe/spring-vision)
- [OpenCV Documentation](https://docs.opencv.org/)

## 🐛 Troubleshooting

### Server Won't Start

- Check if port 8080 is already in use: `lsof -i :8080` (Mac/Linux) or `netstat -ano | findstr :8080` (Windows)
- Check Docker logs: `docker logs spring-vision-mcp`

### Tools Not Available

- Verify the server is running: `curl http://localhost:8080/actuator/health`
- Check MCP tools endpoint: `curl http://localhost:8080/mcp/tools`

### Detection Not Working

- Ensure images are properly encoded (base64 for `detectBase64`)
- Check image format is supported (JPEG, PNG, BMP, etc.)
- Review server logs for error messages

---

## 🤝 Contributing

We welcome contributions! If you'd like to improve Spring Vision MCP:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

See our [Contributing Guidelines](CONTRIBUTING.md) for more details.
