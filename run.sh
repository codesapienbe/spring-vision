#!/bin/bash

# Spring Vision MCP Server Runner
# This script downloads and runs the latest Spring Vision MCP Server JAR

set -e

REPO="codesapienbe/spring-vision"
VERSION=$(curl -s https://api.github.com/repos/$REPO/releases/latest | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
JAR_URL="https://github.com/$REPO/releases/download/$VERSION/mcp-$VERSION.jar"
JAR_FILE="spring-vision-mcp-$VERSION.jar"

echo "🚀 Downloading Spring Vision MCP Server v$VERSION..."
curl -L -o "$JAR_FILE" "$JAR_URL"

echo "🏃 Running Spring Vision MCP Server..."
echo "Press Ctrl+C to stop the server"
echo ""

java -jar "$JAR_FILE" "$@"
