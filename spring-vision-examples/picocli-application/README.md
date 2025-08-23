# Spring Vision PicoCLI Application

A command-line interface application for face detection using the Spring Vision framework.

## Overview

The PicoCLI Application provides a powerful command-line interface for performing face detection on image files. It leverages the Spring Vision framework and Apache Commons CLI to deliver a robust, user-friendly CLI experience.

## Features

- **Single File Processing**: Detect faces in individual image files
- **Batch Processing**: Process multiple files in a directory
- **Interactive Mode**: Guided prompts for single-image detection
- **Progress Indicator**: Optional progress display for batch runs
- **Multiple Output Formats**: Support for text, JSON, and CSV output formats
- **Health Monitoring**: Check the status of the vision backend
- **Verbose Logging**: Detailed logging for debugging and monitoring
- **File Validation**: Comprehensive input validation and error handling
- **Structured Logging**: JSON-formatted logs for monitoring systems

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Spring Vision framework dependencies
- OpenCV backend (automatically configured)

## Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd spring-vision-examples/picocli-application
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

### Basic Commands

#### Show Help
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --help
```

#### Interactive Mode
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --interactive
```

#### Detect Faces in a Single Image
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect /path/to/image.jpg
```

#### Detect Faces with JSON Output
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect /path/to/image.jpg -f json
```

#### Detect Faces with Verbose Output
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect /path/to/image.jpg -V
```

#### Batch Processing with Progress
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --batch /path/to/images --confidence 0.7 --progress -f csv
```

#### Check Health Status
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --health
```

### Command Options

- `-f, --format <format>`: Output format: json, text, csv (default: text)
- `-c, --confidence <threshold>`: Confidence threshold 0.0-1.0 (default: 0.5)
- `-p, --progress`: Show progress during batch processing
- `-i, --interactive`: Run in interactive mode
- `-V, --verbose`: Enable verbose output
- `-v, --version`: Show version information
- `-h, --help`: Show help information

## Output Formats

### Text Format (Default)
```
Face Detection Results for: sample.jpg
==================================================
Total faces detected: 3

Detected faces:
  Face 1: Confidence=95.67%, BoundingBox=(120,80,150,180)
  Face 2: Confidence=87.23%, BoundingBox=(320,90,140,170)
  Face 3: Confidence=92.45%, BoundingBox=(520,85,145,175)
```

### JSON Format
```json
{
  "filename": "sample.jpg",
  "totalFaces": 3,
  "detections": [
    {
      "index": 1,
      "confidence": 0.9567,
      "boundingBox": {
        "x": 120,
        "y": 80,
        "width": 150,
        "height": 180
      }
    }
  ]
}
```

### CSV Format
```csv
filename,face_index,confidence,x,y,width,height
"sample.jpg",1,0.9567,120,80,150,180
"sample.jpg",2,0.8723,320,90,140,170
"sample.jpg",3,0.9245,520,85,145,175
```

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

# Application-specific settings
app:
  cli:
    default-output-format: text
    enable-verbose-logging: false
    max-file-size: 50MB
    supported-formats:
      - jpg
      - jpeg
      - png
      - bmp
      - tiff
```

## Logging

The application provides comprehensive logging with multiple output formats:

- **Console Output**: Human-readable format for CLI users
- **File Logging**: Persistent logs in `logs/picocli-application.log`
- **JSON Logging**: Structured logs in `logs/picocli-application.json` for monitoring systems

### Log Levels

- `ERROR`: Critical errors that prevent operation
- `WARN`: Warning conditions that may affect performance
- `INFO`: General information about application operation
- `DEBUG`: Detailed debugging information (when verbose mode is enabled)

## Error Handling

The application provides comprehensive error handling:

- **File Not Found**: Clear error messages for missing files
- **Permission Errors**: Validation of file read permissions
- **Vision Processing Errors**: Detailed error messages from the vision backend
- **Invalid Input**: Validation of command-line arguments and file formats

## Examples

### Example 1: Basic Face Detection
```bash
$ java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect family-photo.jpg
Face Detection Results for: family-photo.jpg
==================================================
Total faces detected: 4

Detected faces:
  Face 1: Confidence=96.23%, BoundingBox=(45,120,180,220)
  Face 2: Confidence=94.67%, BoundingBox=(280,110,175,210)
  Face 3: Confidence=91.45%, BoundingBox=(520,125,170,205)
  Face 4: Confidence=89.12%, BoundingBox=(750,115,185,215)
```

### Example 2: JSON Output for Scripting
```bash
$ java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect image.jpg -f json
{
  "filename": "image.jpg",
  "totalFaces": 1,
  "detections": [
    {
      "index": 1,
      "confidence": 0.9234,
      "boundingBox": {
        "x": 150,
        "y": 100,
        "width": 200,
        "height": 250
      }
    }
  ]
}
```

### Example 3: Batch Processing with Progress
```bash
$ java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --batch /path/to/images/ -f csv --progress
Processing 15 images...
Completed: 15/15 (100%)
Results printed to stdout
```

### Example 4: Interactive Mode
```bash
$ java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --interactive
Image file path > /path/to/image.jpg
Output format [text|json|csv] (default: text) > json
Confidence threshold [0.0-1.0] (default: 0.5) > 0.6
...
```

## Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/springvision/examples/picocliapplication/
│   │       └── PicoCLIApplication.java
│   └── resources/
│       ├── application.yml
│       └── logback-spring.xml
└── test/
    └── java/
        └── com/springvision/examples/picocliapplication/
```

### Building from Source
```bash
# Clean and compile
mvn clean compile

# Package application
mvn package

# Run with Maven
mvn spring-boot:run -- --detect /path/to/image.jpg
```

## Troubleshooting

### Common Issues

1. **"Image file not found"**
   - Verify the file path is correct
   - Ensure the file exists and is readable

2. **"Vision processing error"**
   - Check that OpenCV is properly installed
   - Verify the image format is supported
   - Check application logs for detailed error information

3. **"Permission denied"**
   - Ensure the application has read access to the image file
   - Check file permissions and ownership

### Debug Mode

Enable verbose logging for detailed debugging:
```bash
java -jar target/picocli-application-1.0.0-SNAPSHOT.jar --detect image.jpg -V
```

### Log Files

Check the log files for detailed error information:
- `logs/picocli-application.log` - Human-readable logs
- `logs/picocli-application.json` - Structured JSON logs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Ensure build passes
5. Submit a pull request

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
