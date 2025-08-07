#!/bin/bash

# Spring Vision JavaFX Application Runner
# This script runs the JavaFX application with proper module configuration

echo "Spring Vision JavaFX Application"
echo "================================"

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
echo "Building JavaFX application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo ""
echo "Build completed successfully!"
echo ""
echo "Launching JavaFX application..."
echo ""

# Run the JavaFX application
# Note: JavaFX requires the --module-path and --add-modules arguments
java --module-path "$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
     -jar target/javafx-application-1.0.0-SNAPSHOT.jar

echo ""
echo "Application closed."
echo ""
echo "Usage notes:"
echo "============"
echo "1. The application will open a desktop window"
echo "2. Use 'Open Image' button or drag-and-drop to load an image"
echo "3. Click 'Detect Faces' to perform face detection"
echo "4. Results will be displayed with bounding boxes on the image"
echo "5. Supported formats: JPG, JPEG, PNG, BMP, GIF"
echo ""
echo "Troubleshooting:"
echo "==============="
echo "If you encounter JavaFX module errors:"
echo "1. Ensure you have JavaFX installed"
echo "2. Check that Java 21+ is being used"
echo "3. Verify Maven dependencies are correctly resolved"
