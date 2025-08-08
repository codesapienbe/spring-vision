# Spring Vision JavaFX Application

A desktop GUI application for face detection using the Spring Vision framework and JavaFX.

## Overview

The JavaFX Application provides a modern, user-friendly desktop interface for performing face detection on image files. It leverages the Spring Vision framework and JavaFX to deliver a rich desktop experience with drag-and-drop support, real-time processing, and visual result display.

## Features

- **Modern Desktop GUI**: Clean, intuitive interface built with JavaFX
- **Drag-and-Drop Support**: Easy image loading with drag-and-drop functionality
- **Real-Time Processing**: Asynchronous face detection with progress indicators
- **Visual Result Display**: Bounding boxes drawn directly on images
- **Multiple Image Formats**: Support for JPG, JPEG, PNG, BMP, and GIF
- **File Browser Integration**: Native file chooser for image selection
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Structured Logging**: JSON-formatted logs for monitoring and debugging

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Spring Vision framework dependencies
- OpenCV backend (automatically configured)
- JavaFX runtime (included in dependencies)

## Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd spring-vision-examples/javafx-application
   ```

2. Build the application:
   ```bash
   mvn clean package
   ```

3. Run the application:
   ```bash
   ./run.sh
   ```

## Usage

### Starting the Application

The application can be started in several ways:

#### Using the Run Script (Recommended)
```bash
./run.sh
```

#### Using Maven
```bash
mvn javafx:run
```

#### Using Java Directly
```bash
java --module-path "$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics \
     -jar target/javafx-application-1.0.0-SNAPSHOT.jar
```

### Using the Application

1. **Load an Image**:
   - Click the "Open Image" button to browse for an image file
   - Or drag and drop an image file onto the application window

2. **Detect Faces**:
   - Once an image is loaded, click the "Detect Faces" button
   - The application will process the image asynchronously
   - A progress indicator will show the processing status

3. **View Results**:
   - Detection results are displayed in the right panel
   - Bounding boxes are drawn directly on the image
   - Each detection shows confidence score and position information

4. **Clear Results**:
   - Use the "Clear" button to reset the display and load a new image

## User Interface

### Main Window Layout

The application features a modern, responsive layout:

- **Top Toolbar**: Contains main action buttons (Open Image, Detect Faces, Clear)
- **Image Display Area**: Shows the loaded image with detection overlays
- **Results Panel**: Displays detection results and statistics
- **Status Bar**: Shows current application status and version information

### Controls

| Control | Description |
|---------|-------------|
| **Open Image** | Opens file browser to select an image |
| **Detect Faces** | Performs face detection on loaded image |
| **Clear** | Clears current results and resets display |

### Supported Image Formats

- **JPEG** (.jpg, .jpeg)
- **PNG** (.png)
- **BMP** (.bmp)
- **GIF** (.gif)

### Result Display

Detection results include:

- **Face Count**: Total number of faces detected
- **Confidence Score**: Detection confidence percentage
- **Bounding Box**: Position and size of detected faces
- **Visual Overlay**: Red rectangles drawn on the image

## Configuration

The application uses Spring Boot's autoconfiguration and can be customized through `application.yml`:

```yaml
# Vision configuration
vision:
  backend:
    type: opencv
    enabled: true
  logging:
    level: INFO
    format: structured
```

## Logging

The application provides comprehensive logging with multiple output formats:

- **Console Output**: Real-time logging during application execution
- **File Logging**: Persistent logs in `logs/javafx-application.log`
- **JSON Logging**: Structured logs in `logs/javafx-application.json` for monitoring systems

### Log Levels

- `ERROR`: Critical errors that prevent operation
- `WARN`: Warning conditions that may affect performance
- `INFO`: General information about application operation
- `DEBUG`: Detailed debugging information

## Error Handling

The application provides comprehensive error handling:

- **File Loading Errors**: Clear error messages for invalid or missing files
- **Processing Errors**: Detailed error messages from the vision backend
- **UI Errors**: Graceful handling of interface-related issues
- **System Errors**: Proper error reporting for system-level problems

## Examples

### Example 1: Basic Face Detection

1. Launch the application
2. Click "Open Image" and select a photo
3. Click "Detect Faces"
4. View results in the right panel
5. See bounding boxes drawn on the image

### Example 2: Drag-and-Drop Workflow

1. Launch the application
2. Drag an image file from your file manager
3. Drop it onto the application window
4. The image loads automatically
5. Click "Detect Faces" to process

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/springvision/examples/javafxapplication/
│   │       └── JavaFXApplication.java
│   └── resources/
│       ├── application.yml
│       └── logback-spring.xml
└── test/
    └── java/
        └── com/springvision/examples/javafxapplication/
```

### Building from Source

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run with Maven
mvn javafx:run
```

### Key Components

- **JavaFXApplication**: Main application class with Spring Boot integration
- **FaceDetectionApp**: JavaFX application class handling the GUI
- **VisionTemplate**: Spring Vision integration for face detection
- **Async Processing**: CompletableFuture-based asynchronous image processing

## Troubleshooting

### Common Issues

1. **"JavaFX module not found"**
   - Ensure JavaFX dependencies are properly included
   - Check that Java 21+ is being used
   - Verify Maven dependencies are resolved

2. **"Image not loading"**
   - Verify the image file exists and is readable
   - Check that the image format is supported
   - Ensure the file is not corrupted

3. **"Face detection not working"**
   - Check that OpenCV is properly installed
   - Verify the image contains visible faces
   - Check application logs for error details

4. **"Application won't start"**
   - Ensure Java 21+ is installed and in PATH
   - Check that Maven is properly configured
   - Verify all dependencies are available

### Debug Mode

Enable verbose logging for detailed debugging:

```yaml
logging:
  level:
    com.springvision.examples.javafxapplication: DEBUG
```

### Log Files

Check the log files for detailed error information:
- `logs/javafx-application.log` - Human-readable logs
- `logs/javafx-application.json` - Structured JSON logs

### Performance Issues

1. **Slow Processing**:
   - Reduce image size before processing
   - Check system resources (CPU, memory)
   - Ensure OpenCV is properly optimized

2. **Memory Issues**:
   - Close other applications to free memory
   - Process smaller images
   - Restart the application if needed

## Platform Support

### Supported Operating Systems

- **Windows**: Windows 10/11 (64-bit)
- **macOS**: macOS 10.15+ (Catalina and later)
- **Linux**: Ubuntu 18.04+, CentOS 7+, and other modern distributions

### System Requirements

- **Minimum**: 4GB RAM, 2GB free disk space
- **Recommended**: 8GB RAM, 4GB free disk space
- **Display**: 1024x768 minimum resolution

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Development Guidelines

- Follow JavaFX best practices for UI development
- Use proper async patterns for long-running operations
- Maintain responsive UI during processing
- Add comprehensive error handling
- Include proper logging for debugging

## License

This project is licensed under the same license as the Spring Vision framework.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review the application logs
3. Create an issue in the project repository
4. Contact the development team

---

*Last Updated: 2025-08-07*
*Version: 1.0.0-SNAPSHOT* 
