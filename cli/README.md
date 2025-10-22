# Spring Vision CLI Setup Tool

A modern, user-friendly command-line interface for setting up the Spring Vision MCP Server.

## Overview

This CLI tool replaces the old `build.sh` script with a comprehensive, elegant command-line application built using Picocli. It downloads the latest Spring Vision MCP Server JAR file from GitHub releases and sets it up locally to avoid network timeouts during startup.

## Features

- 🌟 **Elegant UI**: Beautiful, colored console output with progress indicators
- ⚡ **Fast Setup**: Downloads and configures the MCP server automatically
- 🔄 **Force Redownload**: Option to force re-download of existing JAR files
- 📊 **Progress Tracking**: Real-time download progress with file size information
- 🛡️ **Error Handling**: Comprehensive error handling with helpful messages
- 🎨 **Customizable**: Support for colored output (can be disabled)
- 📝 **Verbose Mode**: Detailed logging for troubleshooting
- 🚀 **JBang Integration**: Run directly from GitHub releases without downloads

## Prerequisites

- **Java 21+**: Required for running the CLI tool
- **JBang**: Required for running the downloaded MCP server JAR
- **Internet Connection**: Required for downloading the JAR file

## Quick Start

The easiest way to use the CLI is directly via JBang from GitHub releases:

```bash
# Run setup directly from GitHub (downloads latest version automatically)
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar
```

That's it! The CLI will handle everything automatically.

## Usage

### Basic Setup

```bash
# Run the setup directly from GitHub releases
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar
```

### Force Re-download

```bash
# Force re-download even if JAR already exists
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar --force
```

### Disable Colors

```bash
# Disable colored output
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar --no-color
```

### Verbose Mode

```bash
# Enable verbose output for debugging
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar --verbose
```

### Help

```bash
# Show help information
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar --help
```

### Version

```bash
# Show version information
jbang https://github.com/codesapienbe/spring-vision/releases/latest/download/cli-0.0.4.jar --version
```

## What It Does

1. **Checks Prerequisites**: Verifies JBang installation
2. **Creates Directory**: Sets up `~/.springvision/` directory
3. **Fetches Release Info**: Gets latest release information from GitHub API
4. **Downloads JAR**: Downloads the MCP server JAR with progress tracking
5. **Verifies Download**: Ensures the download completed successfully
6. **Provides Instructions**: Shows how to run the server and update MCP config

## Output Example

```
╔════════════════════════════════════════════════════════════════╗
║                    🌟 Spring Vision CLI 🌟                    ║
║              MCP Server Setup & Management Tool               ║
╚════════════════════════════════════════════════════════════════╝

ℹ️  Checking JBang installation...
✅ JBang is installed ✓
📁 Creating Spring Vision directory: /home/user/.springvision
ℹ️  Directory already exists: /home/user/.springvision
📡 Fetching latest release information from GitHub...
✅ Latest version: 0.0.4
ℹ️  Found MCP JAR: mcp-0.0.4.jar
⬇️  Downloading Spring Vision MCP Server v0.0.4
   This may take a few minutes due to the large file size (~983MB)...
ℹ️  Download progress: 25% (245 MB / 983 MB)
ℹ️  Download progress: 50% (491 MB / 983 MB)
ℹ️  Download progress: 75% (737 MB / 983 MB)
✅ Download completed successfully! (983 MB)
✅ JAR saved to: /home/user/.springvision/mcp-0.0.4.jar

╔════════════════════════════════════════════════════════════════╗
║                       🎉 Setup Complete!                      ║
╚════════════════════════════════════════════════════════════════╝

MCP Server JAR: /home/user/.springvision/mcp-0.0.4.jar
Version: 0.0.4

To run the MCP server:
  jbang /home/user/.springvision/mcp-0.0.4.jar

To update your MCP client config (~/.cursor/mcp.json):
{
  "mcpServers": {
    "spring-vision": {
      "command": "jbang",
      "args": ["/home/user/.springvision/mcp-0.0.4.jar"]
    }
  }
}

✨ Setup completed successfully!
```

## Configuration

The tool creates the following directory structure:

```
~/.springvision/
└── mcp-{version}.jar    # Downloaded MCP server JAR
```

## Troubleshooting

### JBang Not Found

If you see an error about JBang not being installed:

```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Or visit: https://www.jbang.dev/download/
```

### Network Issues

If downloads fail due to network issues, try:

1. Check your internet connection
2. Use `--force` to retry the download
3. Check GitHub status: https://www.githubstatus.com/

### Permission Issues

If you encounter permission errors:

```bash
# Ensure the ~/.springvision directory is writable
chmod 755 ~/.springvision
```

## Migration from build.sh

This CLI tool completely replaces the functionality of `build.sh`. The old script has been removed. All features are preserved and enhanced:

- ✅ JAR downloading and setup
- ✅ JBang dependency checking
- ✅ Force re-download capability
- ✅ Setup instructions display
- ➕ Beautiful UI with colors and progress
- ➕ Better error handling
- ➕ Progress tracking
- ➕ Verbose mode for debugging

## Development

To contribute to the CLI tool:

```bash
# Run tests
mvn test -pl cli

# Run with Maven (for development)
mvn compile exec:java -pl cli -Dexec.mainClass="io.github.codesapienbe.springvision.cli.SpringVisionCliApplication"
```

## License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.
