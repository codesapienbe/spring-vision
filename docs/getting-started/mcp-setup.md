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
make install

# Or use Maven directly
mvn clean install -Pdownload-models
```

## 🔑 Authentication (Keycloak)

The MCP server runs over Streamable-HTTP and requires a Keycloak-issued bearer token on
every request to `/mcp` — there's no unauthenticated access, by design (see
`keycloak/README.md`). For local dev:

```bash
# 1. Start local Keycloak (imports the spring-vision realm automatically)
make keycloak-up   # done for you by `make run` too

# 2. Fetch a token for your client (client_credentials grant, no user password involved)
curl -s -X POST http://localhost:8180/realms/spring-vision/protocol/openid-connect/token \
  -d grant_type=client_credentials \
  -d client_id=spring-vision-mcp-desktop \
  -d client_secret=desktop-dev-secret-change-me \
  | jq -r .access_token
```

The desktop client's tokens expire in 1 hour — re-run the curl command to get a fresh one
when your MCP client reports authentication failures. See `keycloak/README.md` for the
mobile client (longer-lived tokens) and how to rotate the dev secrets.

## ⚙️ MCP Client Configuration

The server must already be running (`make run`) and reachable at its `/mcp` endpoint —
unlike the old stdio setup, MCP clients connect to it over HTTP rather than launching it
themselves.

### Claude Code

```bash
claude mcp add --scope user --transport http \
  --header "Authorization: Bearer <token-from-above>" \
  spring-vision http://localhost:8080/mcp
```

### Claude Desktop

**Configuration file location:** `~/Library/Application Support/Claude/claude_desktop_config.json`

Whether Claude Desktop's config file supports a plain bearer-token header on a custom
remote server isn't clearly documented — test it directly. If it doesn't work, bridge via
the community `mcp-remote` stdio proxy:

```json
{
  "mcpServers": {
    "spring-vision": {
      "command": "npx",
      "args": ["mcp-remote", "http://localhost:8080/mcp", "--header", "Authorization: Bearer <token-from-above>"]
    }
  }
}
```

### Other MCP Clients

For other MCP-compatible clients:

1. **Find your MCP config file** - Check the client's documentation for the configuration file location
2. **Point it at `http://localhost:8080/mcp`** (or your deployed server's URL) with an `Authorization: Bearer <token>` header
3. **Restart the client** - Most clients require a restart to load new MCP servers

## 🧪 Testing Your Setup

After configuration, restart your MCP client and test the setup:

### Basic Test

Ask your AI assistant: *"Count the number of faces in this image: https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"*

You should receive a response with face detection results.

### Comprehensive Testing

For detailed testing of all MCP tools, see the **[MCP Testing Guide](./mcp-testing.md)**.

## 🛠️ Manual MCP Configuration

If you need to run and wire up the MCP server manually, here's how:

### 1. Build the MCP Server

```bash
# Build the project
make install

# Or use Maven
mvn clean install -Pdownload-models
```

### 2. Run the Server Manually

The server is a long-running HTTP process now, not something the MCP client launches
per-session — start it once and leave it running:

```bash
# Starts local Keycloak too, then runs the server
make run

# Or run the JAR directly (Keycloak must already be running — see keycloak/README.md)
java -jar mcp/target/mcp-0.0.5.jar
```

### 3. Point clients at a custom URL

If the server is running elsewhere (a different port, or a remote deployment), update
your MCP client's URL/header configuration accordingly — see the Claude Code/Desktop
examples above, substituting the correct host and a token issued by that deployment's
Keycloak realm.

## 🔧 Advanced Configuration

### Environment Variables

Because the server now runs independently of any MCP client, environment variables must
be set wherever the **server** process runs (your shell before `make run`, a systemd unit,
or `docker run -e ...` for the image built by `make bundle`) — not in the client's MCP
config, which no longer launches the process:

```bash
# Set custom model directory
export SPRING_VISION_MODELS_DIR=/custom/models/path

# Enable GPU acceleration
export SPRING_VISION_DJL_DEVICE=gpu

# Set confidence thresholds
export SPRING_VISION_DJL_CONFIDENCE_THRESHOLD=0.7

# Point at a non-default Keycloak realm
export KEYCLOAK_ISSUER_URI=https://keycloak.example.com/realms/spring-vision

make run
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
1. Check the server is actually running: `curl -i http://localhost:8080/mcp` (should return `401`, not connection refused)
2. Verify the URL/port in your MCP client's config is correct
3. Restart your MCP client
4. Check logs: `jbang run.java 2>&1 | head -20`

#### "401 Unauthorized"
1. Your token likely expired — the desktop client's tokens last 1 hour. Re-fetch one (see **Authentication (Keycloak)** above) and update your client's `Authorization: Bearer` header.
2. Confirm Keycloak is actually running: `curl http://localhost:8180/realms/spring-vision/.well-known/openid-configuration`
3. Confirm `KEYCLOAK_ISSUER_URI` (if overridden) matches the realm your token was issued from.

#### "Permission denied"
```bash
# Make sure the JAR is executable
chmod +x ~/.springvision/mcp-0.0.4.jar
```

#### "Model download failed"
```bash
# Ensure you have internet access and sufficient disk space (~500MB)
# Try rebuilding with models
make clean install
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
