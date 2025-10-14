#!/bin/bash
# Script to update version across the entire project
# Usage: ./set-version.sh <new-version>

if [ -z "$1" ]; then
  echo "Usage: $0 <new-version>"
  echo "Example: $0 1.0.3"
  exit 1
fi

NEW_VERSION=$1

echo "Updating project version to $NEW_VERSION..."

# Update VERSION file
echo "$NEW_VERSION" > VERSION

# Update Maven POMs using versions:set
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

echo "✓ Version updated to $NEW_VERSION in all project files"
echo "✓ VERSION file updated"
echo "✓ All Maven POMs updated (parent and all submodules)"
echo ""
echo "The Docker image will automatically use the new version: spring-vision:$NEW_VERSION"

