# MCP Server Setup Guide

[Docs Home](../index.md) · [Quick Start](./quick-start.md) · [MCP Testing](./mcp-testing.md) · [API Usage](../development/API_USAGE.md)

This guide provides comprehensive instructions for setting up and configuring the Spring Vision MCP (Model Context Protocol) server. The MCP server allows you to use Spring Vision's computer vision capabilities through AI assistants like Claude, Cursor, and other MCP-compatible tools.

## 🎯 What is MCP?

The Model Context Protocol (MCP) is a standard for connecting AI assistants to external tools and data sources. Spring Vision provides an MCP server that exposes computer vision capabilities as tools that AI assistants can use.

## 🖥️ Claude Desktop extension (no setup)

Download `spring-vision-<version>-macos.mcpb` (Mac with Apple Silicon) or
`spring-vision-<version>-windows.mcpb` (64-bit Windows) and double-click it, or use Claude Desktop's
**Settings → Extensions → Install from file**. The extension carries its own Java runtime, so there
is nothing to install first and nothing to configure.

On the first run the server downloads its models (about 700 MB in total, smallest first) into
`~/.springvision/models`; OCR is ready within seconds, the large models follow in the background.
Until a tool's model is in place the tool says spring-vision is still setting up and does nothing;
the `setup_status` tool shows the progress. Images never leave the machine.

Build the extensions yourself with `make mcpb` (on an Apple Silicon Mac); they land in `dist/`.
Intel Macs are not supported: PyTorch, which face recognition uses, is no longer built for them.

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

## 💻 Local use: Claude Code over stdio (no token)

On your own machine, let the MCP client launch the server itself. The `stdio` profile runs
no web server and opens no port, so there is no token and no identity provider:

```bash
make mcp-sync   # builds the jar with bundled models into ~/.springvision and registers it
```

or register a jar you already built:

```bash
claude mcp add --scope user spring-vision -- \
  java -jar ~/.springvision/mcp-0.0.6.jar --spring.profiles.active=stdio
```

`java` must be 25 or newer; `make mcp-sync` writes the absolute path of the JDK pinned in
`.sdkmanrc`, so the client's `PATH` doesn't matter. `claude mcp list` should show
`spring-vision ✔ Connected` (start-up takes about 10 seconds).

Over stdio the only caller is the process that launched the server, as the same OS user. It
has the roles in `spring.vision.mcp.security.stdio-roles` (by default `mcp-admin`, so every
tool; see below). Logs go to stderr and `~/.springvision/mcp.log`; stdout carries only MCP
messages. For a shared or remote server, use Streamable-HTTP with a token, below.

## 🔑 Authentication and tool access (Streamable-HTTP)

The HTTP server runs no identity provider of its own. It is an OAuth2 resource server: every
request to `/mcp` needs a bearer JWT from the OIDC issuer you point it at (Keycloak, Auth0,
Entra ID, …), and start-up fails if none is configured:

```bash
export OAUTH2_ISSUER_URI=https://idp.example.com/realms/your-realm
make run
```

Which tools a caller may use is decided in `mcp/src/main/resources/application.yml`, under
`spring.vision.mcp.security`, from the roles in the token:

```yaml
spring:
  vision:
    mcp:
      security:
        roles-claim-path: [realm_access, roles]   # where the token keeps its role list
        tool-access:
          - role: mcp-admin
            tools: ["*"]                          # every tool
          - role: mcp-user
            tools: [count_faces_b, extract_text_b, ...]
        stdio-roles: [mcp-admin]                  # the local caller over stdio
```

- **Roles claim**: one element per nesting level. Keycloak realm roles are
  `[realm_access, roles]`, client roles `[resource_access, <client-id>, roles]` (a client id
  with dots is fine), a flat claim is `[roles]`.
- **Deny by default**: a tool runs only if one of the caller's roles is granted it. A token
  with none of the configured roles can connect but cannot run any tool.
- **Typos fail fast**: a granted name that matches no tool stops start-up with the unknown
  names listed.
- Override per deployment like any Spring property, for example with an extra
  `--spring.config.additional-location=file:/etc/spring-vision/access.yml`.

The shipped defaults give `mcp-admin` every tool and `mcp-user` everything except the
sensitive set: biometric templates and identification (embeddings, face verification and
1:N search, the identity store, access authentication), identity and driving-licence
documents, licence plates, NSFW, deepfake and threat detection. Calling a tool the caller is
not granted returns an access-denied error, not tool output.

## ⚙️ MCP Client Configuration

For the HTTP server: it must already be running (`make run`) and reachable at its `/mcp`
endpoint, and clients connect to it rather than launching it, with a token from your
issuer. For local use without a token, see the stdio section above.

### Claude Code (HTTP)

```bash
claude mcp add --scope user --transport http \
  --header "Authorization: Bearer <token-from-your-issuer>" \
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
      "args": ["mcp-remote", "http://localhost:8080/mcp", "--header", "Authorization: Bearer <token-from-your-issuer>"]
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
# Builds and runs the server (needs OAUTH2_ISSUER_URI, see above)
make run

# Or run the JAR directly
OAUTH2_ISSUER_URI=https://idp.example.com/realms/your-realm java -jar mcp/target/mcp-0.0.6.jar
```

### 3. Point clients at a custom URL

If the server is running elsewhere (a different port, or a remote deployment), update
your MCP client's URL/header configuration accordingly — see the Claude Code/Desktop
examples above, substituting the correct host and a token from that deployment's issuer.

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

# The OIDC issuer whose JWTs the HTTP server accepts (required)
export OAUTH2_ISSUER_URI=https://idp.example.com/realms/your-realm

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
1. Your token likely expired. Fetch a new one from your issuer and update your client's `Authorization: Bearer` header.
2. Confirm the token's `iss` claim equals `OAUTH2_ISSUER_URI` exactly, and that the issuer's `/.well-known/openid-configuration` is reachable from the server.

#### "Access denied" on a tool
The token is valid but none of its roles is granted that tool. Check the roles at
`spring.vision.mcp.security.roles-claim-path` in the token, and the grants in
`spring.vision.mcp.security.tool-access`.

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
- **Object Detection** - not available until a licence-clean model is bundled (fails with `VisionUnsupportedException`)
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
