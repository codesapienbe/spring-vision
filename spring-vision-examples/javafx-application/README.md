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
- **Multiple Backend Support**: OpenCV, FaceBytes, and DeepFace backends
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

2. **Select Backend** (Optional):
   - Use the backend dropdown to choose between OpenCV, FaceBytes, DeepFace, or CompreFace
   - OpenCV: Fast, local processing (default)
   - FaceBytes: Advanced face analysis capabilities
   - DeepFace: Containerized API with advanced ML models
   - CompreFace: Containerized API with face recognition and verification

3. **Detect Faces**:
   - Once an image is loaded, click the "Detect Faces" button
   - The application will process the image asynchronously
   - A progress indicator will show the processing status

4. **View Results**:
   - Detection results are displayed in the right panel
   - Bounding boxes are drawn directly on the image
   - Each detection shows confidence score and position information

5. **Clear Results**:
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
  backend: opencv  # or "facebytes" or "deepface"
  opencv:
    enabled: true
    confidence-threshold: 0.7
  deepface:
    enabled: false
    api-url: http://localhost:5000
    timeout: 30s
    max-retries: 3
  logging:
    level: INFO
    format: structured
```

### DeepFace Backend Setup

To use the DeepFace backend:

1. **Start the DeepFace API container**:

   ```bash
   # From the project root (recommended)
   docker-compose up -d deepface
   
   # Or start specific services
   docker-compose up -d deepface postgres redis
   ```

   The DeepFace container uses the official `serengil/deepface:latest` image with built-in HTTP API.

2. **Configure the application**:

   ```yaml
   vision:
     backend: deepface
     deepface:
       enabled: true
       api-url: http://localhost:5000
   ```

3. **Or use environment variables**:

   ```bash
   export VISION_BACKEND=deepface
   export VISION_DEEPFACE_ENABLED=true
   export VISION_DEEPFACE_API_URL=http://localhost:5000
   ```

4. **Run the application**:

   ```bash
   # From the javafx-application directory
   cd spring-vision-examples/javafx-application
   ./run.sh
   
   # Or using Maven from the module directory
   mvn spring-boot:run
   
   # Or from project root with module specification
   mvn spring-boot:run -pl spring-vision-examples/javafx-application
   ```

The DeepFace backend provides advanced face analysis including age, gender, emotion, and race detection.

### CompreFace Backend Setup

To use the CompreFace backend:

1. **Start the CompreFace API container**:

   ```bash
   # From the project root (recommended)
   docker-compose up -d compreface postgres
   
   # Or start specific services
   docker-compose up -d compreface postgres redis
   ```

   The CompreFace container uses the official `exadel/compreface:latest` image with built-in HTTP API.

2. **Configure the application**:

   ```yaml
   vision:
     backend: compreface
     compreface:
       enabled: true
       api-url: http://localhost:8000
   ```

3. **Or use environment variables**:

   ```bash
   export VISION_BACKEND=compreface
   export VISION_COMPREFACE_ENABLED=true
   export VISION_COMPREFACE_API_URL=http://localhost:8000
   ```

4. **Run the application**:

   ```bash
   # From the javafx-application directory
   cd spring-vision-examples/javafx-application
   ./run.sh
   
   # Or using Maven from the module directory
   mvn spring-boot:run
   
   # Or from project root with module specification
   mvn spring-boot:run -pl spring-vision-examples/javafx-application
   ```

The CompreFace backend provides face detection, recognition, and verification capabilities with a user-friendly API.

**Important Startup Notes:**
- CompreFace takes approximately 45 seconds to start due to service initialization
- Monitor startup progress with: `docker logs spring-vision-compreface -f`
- Wait for "exited: startup (exit status 0; expected)" message before using
- The service will automatically restart if database initialization is slow

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

### Example 3: DeepFace Backend Usage

1. Start the DeepFace API container:

   ```bash
   mvn spring-boot:run -Pjavafx
   ```

2. Launch the JavaFX application:

   ```bash
   ./run.sh
   ```

3. Select "deepface" from the backend dropdown

4. Load an image and click "Detect Faces"

5. View advanced analysis results including age, gender, and emotion detection

### Example 4: CompreFace Backend Usage

1. Start the CompreFace API container:

   ```bash
   docker-compose up -d compreface postgres
   ```

2. Launch the JavaFX application:

   ```bash
   # From the javafx-application directory
   cd spring-vision-examples/javafx-application
   ./run.sh
   
   # Or from project root
   mvn spring-boot:run -pl spring-vision-examples/javafx-application
   ```

3. Select "compreface" from the backend dropdown

4. Load an image and click "Detect Faces"

5. View face detection results with bounding boxes and confidence scores

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

5. **"DeepFace backend not working"**
   - Ensure the DeepFace container is running: `docker ps | grep deepface`
   - Check if the API is accessible: `curl http://localhost:5000/health`
   - Verify the container logs: `docker logs javafx-deepface`
   - Ensure port 5000 is not used by another application

6. **"CompreFace backend not working"**
   - Ensure the CompreFace container is running: `docker ps | grep compreface`
   - Check if the API is accessible: `curl http://localhost:8000/api/v1/recognition/detect`
   - Verify the container logs: `docker logs spring-vision-compreface -f`
   - **Wait for startup completion**: Look for "exited: startup (exit status 0; expected)" in logs
   - **Startup time**: CompreFace takes ~45 seconds to start - be patient
   - Ensure PostgreSQL is running: `docker ps | grep postgres`
   - Ensure port 8000 is not used by another application

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
