# Spring Vision CLI Installer

The Spring Vision CLI Installer provides a simple way to install and manage Spring Vision MCP Server for end users who don't want to deal with build tools or manual configuration.

## Quick Install

Download the installer JAR and run:

```bash
# Download the latest installer (replace with actual URL)
curl -L -o spring-vision-installer.jar https://github.com/codesapienbe/spring-vision/releases/download/v0.0.1/spring-vision-installer.jar

# Install Spring Vision
java -jar spring-vision-installer.jar install
```

That's it! The installer will:
- Copy Spring Vision to `~/.springvision/`
- Set up MCP configuration for Claude Desktop
- Create run scripts

## Commands

### Install
```bash
java -jar spring-vision-installer.jar install
```
Installs Spring Vision MCP Server to your system.

Options:
- `--force, -f`: Force reinstallation even if already installed
- `--version, -v`: Specific version to install (default: latest)
- `--no-mcp-config`: Skip MCP configuration setup

### Status
```bash
java -jar spring-vision-installer.jar status
```
Check if Spring Vision is installed and show installation details.

### Update
```bash
java -jar spring-vision-installer.jar update
```
Update Spring Vision to the latest version.

### Uninstall
```bash
java -jar spring-vision-installer.jar uninstall
```
Remove Spring Vision from your system.

Options:
- `--yes, -y`: Skip confirmation prompt

## What Gets Installed

The installer creates:
- `~/.springvision/spring-vision-mcp.jar` - The MCP server JAR
- `~/.springvision/run.java` - JBang runner script
- `~/.springvision/run.sh` - Shell script runner
- `~/.config/claude_desktop_config.json` - MCP configuration

## Running Spring Vision

After installation, you can run Spring Vision using:

```bash
# Using the shell script
~/.springvision/run.sh

# Using JBang
jbang ~/.springvision/run.java
```

## For Developers

If you're developing Spring Vision, you can build the installer locally:

```bash
# Build the project
mvn clean install

# Run the installer
java -jar cli/target/cli-0.0.1.jar install
```

## Requirements

- Java 21 or higher
- Internet connection (for downloading models on first run)
- MCP-compatible client (Claude Desktop, Cline, etc.)
