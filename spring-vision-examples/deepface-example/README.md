# DeepFace Example Application

This example demonstrates how to use the DeepFace backend with Spring Vision to perform advanced face detection and analysis.

## Prerequisites

1. **Docker and Docker Compose** - Required to run the DeepFace API container
2. **Java 21+** - Required to run the Spring Boot application
3. **Maven** - Required to build the project

## Quick Start

### 1. Start the DeepFace API Container

```bash
# Start the DeepFace API container
docker-compose -f ../../docker-compose.deepface.yml up -d

# Verify the container is running
docker ps | grep deepface
```

### 2. Build the Example

```bash
# Build the example application
mvn clean package
```

### 3. Run the Example

```bash
# Run with a test image
java -jar target/deepface-example-1.0.jar /path/to/face.jpg

# Or run with Maven
mvn spring-boot:run -- -Dspring-boot.run.arguments="/path/to/face.jpg"
```

## Configuration

The example is configured to use the DeepFace backend by default. You can modify the configuration in `src/main/resources/application.yml`:

```yaml
vision:
  backend: deepface
  deepface:
    api-url: http://localhost:5000
    timeout: 30s
    max-retries: 3
```

## What the Example Does

1. **Initializes the DeepFace backend** - Connects to the DeepFace API container
2. **Performs health checks** - Ensures the API is available and healthy
3. **Loads and processes images** - Detects faces and extracts attributes
4. **Displays results** - Shows detected faces with confidence scores and attributes

## Expected Output

```
=== DeepFace Example Application ===
Using DeepFace backend: DeepFace Vision Backend
DeepFace backend is healthy and ready
Processing image: /path/to/face.jpg
Image loaded: 123456 bytes, format: jpeg
Detecting faces...
Found 2 faces:
  - Face at BoundingBox{x=0.123, y=0.456, width=0.234, height=0.345} with confidence: 0.95
    Age: 25
    Gender: Man
    Emotion: happy
  - Face at BoundingBox{x=0.567, y=0.789, width=0.123, height=0.234} with confidence: 0.87
    Age: 30
    Gender: Woman
    Emotion: neutral
Processing completed in 1250ms
```

## Troubleshooting

### Container Issues

If the DeepFace container won't start:

```bash
# Check container logs
docker logs spring-vision-deepface

# Check if port 5000 is available
netstat -tulpn | grep 5000

# Restart the container
docker-compose -f ../../docker-compose.deepface.yml restart
```

### API Connection Issues

If the application can't connect to the DeepFace API:

```bash
# Test the API directly
curl http://localhost:5000/health

# Check if the container is healthy
docker inspect spring-vision-deepface | grep Health
```

### Performance Issues

If face detection is slow:

1. **Check container resources** - Ensure the container has enough CPU and memory
2. **Optimize image size** - Use smaller images for faster processing
3. **Check network latency** - Ensure low latency between application and container

## Advanced Usage

### Custom Configuration

You can customize the DeepFace backend configuration:

```java
// Create a custom DeepFace backend
DeepFaceVisionBackend backend = new DeepFaceVisionBackend(
    "http://your-deepface-api:5000",
    Duration.ofSeconds(60)
);

// Initialize and use
backend.initialize();
VisionResult result = backend.detectFaces(imageData);
```

### Face Analysis

The DeepFace backend can perform detailed face analysis:

```java
// Analyze face attributes
Map<String, Object> analysis = backend.analyzeFace(imageData);
System.out.println("Age: " + analysis.get("age"));
System.out.println("Gender: " + analysis.get("gender"));
System.out.println("Emotion: " + analysis.get("emotion"));
System.out.println("Race: " + analysis.get("race"));
```

### Face Verification

Compare two faces to determine if they belong to the same person:

```java
// Verify if two faces are the same person
Map<String, Object> verification = backend.verifyFaces(image1Data, image2Data);
boolean isSamePerson = (Boolean) verification.get("verified");
double similarity = (Double) verification.get("distance");
```

## Cleanup

When you're done testing:

```bash
# Stop the DeepFace container
docker-compose -f ../../docker-compose.deepface.yml down

# Remove the container and volumes
docker-compose -f ../../docker-compose.deepface.yml down -v
```

## Next Steps

1. **Explore the API** - Check the DeepFace API documentation for more endpoints
2. **Scale the solution** - Run multiple DeepFace containers behind a load balancer
3. **Add authentication** - Implement API authentication for production use
4. **Monitor performance** - Add metrics and monitoring for production deployment 
