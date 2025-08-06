# Basic Face Detection Example

A simple Spring Boot application demonstrating basic face detection capabilities using the Spring Vision framework.

## Overview

This example showcases how to integrate the Spring Vision framework into a Spring Boot application to perform face detection on uploaded images. It provides a web interface for uploading images and viewing detection results.

## Features

- **Web Interface**: Simple HTML form for image upload
- **Face Detection**: Automatic face detection using OpenCV
- **Result Visualization**: Display detection results with confidence scores
- **Health Monitoring**: Built-in health checks and metrics
- **Error Handling**: Comprehensive error handling and validation
- **Performance Metrics**: Processing time and detection statistics

## Prerequisites

- Java 21 or later
- Maven 3.6 or later
- Spring Vision framework (automatically included)

## Quick Start

### 1. Build and Run

```bash
# Navigate to the example directory
cd examples/basic-face-detection

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run
```

### 2. Access the Application

- **Main Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Endpoint**: http://localhost:8080/api/vision/health

### 3. Upload an Image

1. Open your browser and go to http://localhost:8080
2. Click "Choose File" and select an image containing faces
3. Click "Detect Faces" to process the image
4. View the detection results

## Project Structure

```
basic-face-detection/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/springvision/examples/basicfacedetection/
│   │   │       ├── BasicFaceDetectionApplication.java
│   │   │       ├── controller/
│   │   │       │   └── FaceDetectionController.java
│   │   │       ├── service/
│   │   │       │   └── FaceDetectionService.java
│   │   │       └── model/
│   │   │           └── DetectionResult.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/
│   │       │   ├── index.html
│   │       │   └── result.html
│   │       └── static/
│   │           ├── css/
│   │           └── js/
│   └── test/
│       └── java/
│           └── com/springvision/examples/basicfacedetection/
│               ├── controller/
│               └── service/
├── pom.xml
└── README.md
```

## Configuration

### Application Properties

The application uses the following key configuration:

```yaml
vision:
  enabled: true
  backend: opencv
  opencv:
    confidence-threshold: 0.8
    max-image-size: 10485760  # 10MB

server:
  port: 8080
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

### Environment Profiles

- **Development** (`dev`): Debug logging, lower confidence threshold
- **Production** (`prod`): Info logging, higher confidence threshold, GPU acceleration
- **Test** (`test`): Minimal logging, lower confidence threshold

## Usage Examples

### Web Interface

1. **Upload Image**: Use the web form to upload an image file
2. **View Results**: See detected faces with confidence scores
3. **Download Results**: Save detection results as JSON

### API Usage

```bash
# Health check
curl http://localhost:8080/actuator/health

# Face detection via API
curl -X POST http://localhost:8080/api/vision/detect/faces \
  -F "file=@image.jpg"

# Get application info
curl http://localhost:8080/api/vision/info
```

### Programmatic Usage

```java
@Service
public class MyService {
    @Autowired
    private VisionTemplate visionTemplate;
    
    public void detectFaces(byte[] imageData) {
        VisionResult result = visionTemplate.detectFaces(imageData);
        
        if (result.hasDetections()) {
            result.detections().forEach(detection -> {
                System.out.println("Face detected: " + detection.confidence());
            });
        }
    }
}
```

## API Endpoints

### Web Endpoints

- `GET /` - Main application page
- `POST /detect` - Face detection form submission
- `GET /result` - Display detection results

### REST API Endpoints

- `GET /api/vision/health` - Health check
- `GET /api/vision/info` - Application information
- `POST /api/vision/detect/faces` - Face detection (multipart)

### Management Endpoints

- `GET /actuator/health` - Health status
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Performance metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Testing

### Unit Tests

```bash
# Run unit tests
mvn test

# Run specific test
mvn test -Dtest=FaceDetectionServiceTest
```

### Integration Tests

```bash
# Run integration tests
mvn verify

# Run with specific profile
mvn verify -Dspring.profiles.active=test
```

### Manual Testing

1. **Test with Sample Images**:
   - Use images with clear faces
   - Test with multiple faces
   - Test with different image formats

2. **Test Error Handling**:
   - Upload invalid files
   - Test with very large images
   - Test with images without faces

3. **Test Performance**:
   - Monitor processing time
   - Check memory usage
   - Test concurrent uploads

## Performance Considerations

### Optimization Tips

1. **Image Size**: Keep images under 5MB for optimal performance
2. **Image Format**: Use JPEG for photos, PNG for graphics
3. **Resolution**: Higher resolution doesn't always improve accuracy
4. **Confidence Threshold**: Adjust based on your needs

### Monitoring

- **Processing Time**: Monitor via `/actuator/metrics/vision.processing.time`
- **Detection Count**: Track via `/actuator/metrics/vision.detections.total`
- **Error Rate**: Monitor via `/actuator/metrics/vision.errors.total`

## Troubleshooting

### Common Issues

1. **No Faces Detected**
   - Check image quality and lighting
   - Lower confidence threshold
   - Verify image contains faces

2. **Slow Performance**
   - Reduce image size
   - Check system resources
   - Enable GPU acceleration (if available)

3. **Upload Errors**
   - Check file size limits
   - Verify supported formats
   - Check disk space

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.springvision: DEBUG
    com.springvision.examples: DEBUG
```

### Health Checks

Monitor application health:

```bash
curl http://localhost:8080/actuator/health
```

## Customization

### Adding New Features

1. **Additional Detection Types**:
   ```java
   VisionResult result = visionTemplate.detectObjects(imageData);
   ```

2. **Custom Processing**:
   ```java
   // Add image preprocessing
   // Add result post-processing
   // Add custom validation
   ```

3. **Enhanced UI**:
   - Add result visualization
   - Add progress indicators
   - Add batch processing

### Configuration Customization

```yaml
vision:
  opencv:
    confidence-threshold: 0.9  # Higher accuracy
    gpu-acceleration: true     # Enable GPU
    max-image-size: 20971520   # 20MB limit
```

## Security Considerations

### Production Deployment

1. **File Validation**: Validate uploaded files
2. **Size Limits**: Enforce file size restrictions
3. **Authentication**: Add user authentication
4. **HTTPS**: Use HTTPS in production

### Security Configuration

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}

spring:
  security:
    user:
      name: admin
      password: ${ADMIN_PASSWORD}
```

## Deployment

### Local Development

```bash
mvn spring-boot:run
```

### Production

```bash
# Build JAR
mvn clean package

# Run with production profile
java -jar target/basic-face-detection-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

### Docker

```bash
# Build image
docker build -t basic-face-detection .

# Run container
docker run -p 8080:8080 basic-face-detection
```

## Contributing

### Adding Features

1. **Create Feature Branch**:
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Add Tests**:
   - Unit tests for new functionality
   - Integration tests for API changes
   - Update existing tests

3. **Update Documentation**:
   - Update README.md
   - Add API documentation
   - Update configuration examples

4. **Submit Pull Request**:
   - Follow contribution guidelines
   - Include tests and documentation
   - Provide clear description

### Code Standards

- Follow Java coding conventions
- Add Javadoc for public methods
- Use meaningful variable names
- Include error handling
- Add logging for debugging

## Support

### Getting Help

1. **Check Documentation**:
   - Review this README
   - Check framework documentation
   - Look at example code

2. **Debug Issues**:
   - Enable debug logging
   - Check application logs
   - Use health endpoints

3. **Report Issues**:
   - Open GitHub issue
   - Include error details
   - Provide reproduction steps

### Resources

- [Spring Vision Documentation](../../docs/)
- [API Documentation](../../docs/API_DOCUMENTATION.md)
- [User Guide](../../docs/USER_GUIDE.md)
- [Deployment Guide](../../docs/DEPLOYMENT_GUIDE.md)

## License

This example is licensed under the Apache License, Version 2.0. See the [LICENSE](../../LICENSE) file for details. 
