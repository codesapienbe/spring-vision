#!/bin/bash

# Spring Vision PicoCLI Application Runner
# This script runs the PicoCLI application with various example commands

echo "Spring Vision PicoCLI Application"
echo "================================="

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Build the application
echo "Building PicoCLI application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo ""
echo "Build completed successfully!"
echo ""
echo "Usage examples:"
echo "=============="
echo ""
echo "1. Show help:"
echo "   java -jar target/picocli-application-1.0.jar --help"
echo ""
echo "2. Detect faces in a single image:"
echo "   java -jar target/picocli-application-1.0.jar detect /path/to/image.jpg"
echo ""
echo "3. Detect faces with JSON output:"
echo "   java -jar target/picocli-application-1.0.jar detect /path/to/image.jpg -o json"
echo ""
echo "4. Detect faces with verbose output:"
echo "   java -jar target/picocli-application-1.0.jar detect /path/to/image.jpg -v"
echo ""
echo "5. Save results to file:"
echo "   java -jar target/picocli-application-1.0.jar detect /path/to/image.jpg --save-results results.txt"
echo ""
echo "6. Check health status:"
echo "   java -jar target/picocli-application-1.0.jar health"
echo ""
echo "Note: Replace /path/to/image.jpg with the actual path to your image file."
echo ""
echo "Supported image formats: JPG, JPEG, PNG, BMP, TIFF"
echo "Supported output formats: text, json, csv"
