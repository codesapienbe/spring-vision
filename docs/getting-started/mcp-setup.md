# MCP Server Setup Guide

[Docs Home](../index.md) · [Quick Start](./quick-start.md) · [MCP Testing](./mcp-testing.md) · [API Usage](../development/API_USAGE.md)

This guide provides comprehensive instructions for setting up and configuring the Spring Vision MCP (Model Context Protocol) server. The MCP server allows you to use Spring Vision's computer vision capabilities through AI assistants like Claude, Cursor, and other MCP-compatible tools.

## 🎯 What is MCP?

The Model Context Protocol (MCP) is a standard for connecting AI assistants to external tools and data sources. Spring Vision provides an MCP server that exposes computer vision capabilities as tools that AI assistants can use.

## 🚀 Quick Setup (Recommended)

### Option 1: CLI Tool (Easiest)

Use our CLI setup tool for the easiest installation experience:

```bash
# Run the CLI setup tool to automatically download and configure everything
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar
```

The CLI tool will:
- ✅ Check for JBang installation and guide you if needed
- 📦 Download the latest Spring Vision MCP Server JAR (~983MB)
- 💾 Store it locally in `~/.springvision/` (no re-downloads needed!)
- ℹ️ Show you the exact MCP configuration for your client
- 🚀 Set up everything automatically with no manual steps required!

### Option 2: Manual Build

If you prefer to build from source:

```bash
# Clone the repository
git clone https://github.com/codesapienbe/spring-vision.git
cd spring-vision

# Build with bundled models
make build

# Or use Maven directly
mvn clean install -Pdownload-models
```

## ⚙️ MCP Client Configuration

After installing Spring Vision, you need to configure your MCP client to use it. Here are the most common clients:

### Claude Desktop

**Configuration file location:** `~/Library/Application Support/Claude/claude_desktop_config.json`

Add this configuration:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/youruser/.springvision/mcp-0.0.4.jar"]
    }
  }
}
```

### VS Code / Cursor

**Configuration file location:** `~/.cursor/mcp.json` (or equivalent for your editor)

Add this configuration:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/youruser/.springvision/mcp-0.0.4.jar"]
    }
  }
}
```

### Other MCP Clients

For other MCP-compatible clients:

1. **Find your MCP config file** - Check the client's documentation for the configuration file location
2. **Add the server configuration** - Use the JSON format above, adjusting the path to match your system
3. **Restart the client** - Most clients require a restart to load new MCP servers

## 🧪 Testing Your Setup

After configuration, restart your MCP client and test the setup:

### Basic Test

Ask your AI assistant: *"Count the number of faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"*

You should receive a response with face detection results.

### Comprehensive Testing

For detailed testing of all MCP tools, see the **[MCP Testing Guide](./mcp-testing.md)**.

## 🛠️ Manual MCP Configuration

If you need to configure the MCP server manually, here's how:

### 1. Build the MCP Server

```bash
# Build the project
make build

# Or use Maven
mvn clean install -Pdownload-models
```

### 2. Run the Server Manually

```bash
# Run with JBang
jbang run.java

# Or run the JAR directly
java -jar mcp/target/mcp-0.0.4.jar
```

### 3. Configure Client with Custom Path

If you're running from a custom location, update your MCP configuration:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/path/to/your/spring-vision/mcp/target/mcp-0.0.4.jar"]
    }
  }
}
```

## 🔧 Advanced Configuration

### Environment Variables

You can customize the MCP server behavior with environment variables:

```bash
# Set custom model directory
export SPRING_VISION_MODELS_DIR=/custom/models/path

# Enable GPU acceleration
export SPRING_VISION_DJL_DEVICE=gpu

# Set confidence thresholds
export SPRING_VISION_DJL_CONFIDENCE_THRESHOLD=0.7

# Run with custom config
jbang run.java
```

### Custom MCP Configuration

For advanced users, you can create a custom MCP configuration file:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/user/.springvision/mcp-0.0.4.jar"],
      "env": {
        "SPRING_VISION_DJL_DEVICE": "gpu",
        "SPRING_VISION_DJL_CONFIDENCE_THRESHOLD": "0.8"
      }
    }
  }
}
```

## 🚨 Troubleshooting

### Common Issues

#### "Command not found: jbang"
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup
source ~/.bashrc  # or restart your terminal
```

#### "MCP server not responding"
1. Check that the JAR file exists: `ls -la ~/.springvision/`
2. Verify the path in your MCP config is correct
3. Restart your MCP client
4. Check logs: `jbang run.java 2>&1 | head -20`

#### "Permission denied"
```bash
# Make sure the JAR is executable
chmod +x ~/.springvision/mcp-0.0.4.jar
```

#### "Model download failed"
```bash
# Ensure you have internet access and sufficient disk space (~500MB)
# Try rebuilding with models
make clean build
```

#### "GPU not working"
```bash
# Check CUDA installation
nvidia-smi

# Set GPU explicitly
export SPRING_VISION_DJL_DEVICE=gpu
```

### Getting Help

1. **Check the logs** - Run the server manually to see error messages
2. **Verify your config** - Use a JSON validator to check your MCP configuration
3. **Test step by step** - Start with basic face detection before trying advanced features
4. **Update versions** - Ensure you're using the latest version (0.0.4)

## 📊 Available MCP Tools

Once configured, your AI assistant will have access to these Spring Vision tools:

### Core Detection Tools
- **Face Detection** - Detect and count faces in images
- **Object Detection** - Identify objects using YOLO models
- **Text Recognition** - Extract text via OCR
- **Barcode Scanning** - Read QR codes and barcodes

### Advanced Analysis Tools
- **Pose Estimation** - Detect human poses and keypoints
- **Emotion Recognition** - Analyze facial expressions
- **Image Classification** - Categorize image content
- **Metadata Extraction** - Extract EXIF/GPS data

### Security & Safety Tools
- **Threat Detection** - Identify weapons and security threats
- **NSFW Detection** - Filter inappropriate content
- **Deepfake Detection** - Identify AI-generated media
- **Biometric Authentication** - Face-based access control

### Health & Wellness Tools
- **Fall Detection** - Monitor for falls using pose analysis
- **Stress Analysis** - Assess stress levels from facial cues
- **Heart Rate Estimation** - rPPG analysis from video
- **Demographics Analysis** - Age and gender estimation

### Vehicle Analytics Tools
- **Vehicle Detection** (`detect_vehicle_u`, `detect_vehicle_b`) - Identify cars, trucks, buses, motorcycles, bicycles, trains, boats, airplanes with bounding boxes
- **Vehicle Damage Detection** (`detect_vehicle_damages_u`, `detect_vehicle_damages_b`) - Per-vehicle damage type (scratch, dent, crack, broken glass, flat tire) and severity

## 🎯 Next Steps

- **[Quick Start](./quick-start.md)** - Basic setup and usage
- **[MCP Testing Guide](./mcp-testing.md)** - Test all tools with examples
- **[API Usage](../development/API_USAGE.md)** - REST API reference
- **[Configuration](../configuration/config.md)** - Advanced configuration options
