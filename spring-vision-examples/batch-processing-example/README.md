# Spring Vision - Batch Processing Example

This example demonstrates the batch processing capabilities of Spring Vision, showing how to efficiently process large volumes of images with progress monitoring, multiple detection types, and concurrent processing.

## Features Demonstrated

- **Basic Batch Processing**: Process multiple images in configurable batch sizes
- **Progress Monitoring**: Real-time progress updates with completion percentages
- **Multiple Detection Types**: Process images with different detection algorithms simultaneously
- **Batch Cancellation**: Cancel running batches gracefully
- **Error Handling**: Robust error handling and recovery
- **Concurrent Processing**: Efficient parallel processing of multiple batches

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Spring Vision Core and Starter dependencies

## Running the Example

1. **Build the project**:
   ```bash
   mvn clean compile
   ```

2. **Run the example**:
   ```bash
   mvn spring-boot:run
   ```

3. **Or run the JAR directly**:
   ```bash
   mvn package
   java -jar target/batch-processing-example-1.0.0.jar
   ```

## Example Output

When you run the example, you'll see output similar to:

```
INFO  - Starting Batch Processing Example
INFO  - === Basic Batch Processing Example ===
INFO  - Processed 10 images with 10 results
INFO  - Successful detections: 8, Average confidence: 0.85
INFO  - === Progress Monitoring Example ===
INFO  - Progress: 0.0% - Started processing 20 images (Status: STARTED)
INFO  - Progress: 25.0% - Processed 5/20 images (Status: RUNNING)
INFO  - Progress: 50.0% - Processed 10/20 images (Status: RUNNING)
INFO  - Progress: 75.0% - Processed 15/20 images (Status: RUNNING)
INFO  - Progress: 100.0% - Completed processing 20 images (Status: COMPLETED)
INFO  - Completed processing 20 images
INFO  - === Multiple Detection Types Example ===
INFO  - Processed 3 detection types for 15 images
INFO  - FACE: 12 detections, avg confidence: 0.87
INFO  - OBJECT: 15 detections, avg confidence: 0.92
INFO  - TEXT: 8 detections, avg confidence: 0.78
INFO  - === Batch Cancellation Example ===
INFO  - Progress: 0.0% - Started processing 50 images (Status: STARTED)
INFO  - Progress: 25.0% - Processed 12/50 images (Status: RUNNING)
INFO  - Cancelled batch: batch-1-abc12345
INFO  - Batch was cancelled or failed: Batch processing failed
INFO  - === Error Handling Example ===
INFO  - Empty batch processed successfully: 0 results
INFO  - Batch Processing Example completed
```

## Key Components

### BatchVisionProcessor

The main class that orchestrates batch processing:

```java
BatchVisionProcessor processor = new BatchVisionProcessor();

// Process a batch of images
CompletableFuture<List<VisionResult>> future = processor.processBatch(
    images,
    DetectionType.FACE,
    Map.of("confidence", 0.8)
);

List<VisionResult> results = future.get(30, TimeUnit.SECONDS);
```

### Progress Monitoring

Monitor progress in real-time:

```java
CompletableFuture<List<VisionResult>> future = processor.processBatch(
    images,
    DetectionType.OBJECT,
    Map.of("confidence", 0.7),
    progress -> {
        System.out.printf("Progress: %.1f%% - %s%n", 
                         progress.getCompletionPercentage() * 100,
                         progress.getMessage());
    }
);
```

### Multiple Detection Types

Process images with different detection algorithms:

```java
List<DetectionType> types = List.of(DetectionType.FACE, DetectionType.OBJECT);

CompletableFuture<List<BatchResult>> future = processor.processMultipleTypes(
    images,
    types,
    Map.of("confidence", 0.8)
);

List<BatchResult> results = future.get(60, TimeUnit.SECONDS);
```

### Batch Cancellation

Cancel running batches:

```java
String batchId = "batch-1-abc12345";
boolean cancelled = processor.cancelBatch(batchId);
if (cancelled) {
    System.out.println("Batch cancelled successfully");
}
```

## Configuration Options

The `BatchVisionProcessor` can be configured with:

- **Batch Size**: Number of images processed in each batch (default: 10)
- **Concurrency**: Maximum number of concurrent batches (default: 4)
- **Executor**: Custom thread pool for processing

```java
ExecutorService executor = Executors.newFixedThreadPool(8);
BatchVisionProcessor processor = new BatchVisionProcessor(executor, 20, 2);
```

## Error Handling

The batch processor provides comprehensive error handling:

- Individual batch failures don't affect other batches
- Progress callbacks include error information
- Graceful cancellation support
- Resource cleanup on shutdown

## Performance Considerations

- **Batch Size**: Larger batches reduce overhead but increase memory usage
- **Concurrency**: More concurrent batches improve throughput but require more resources
- **Memory**: Monitor memory usage with large image batches
- **Thread Pool**: Use appropriate thread pool size based on available CPU cores

## Integration with Spring Boot

The example shows how to integrate batch processing with Spring Boot:

```java
@Component
public class BatchProcessingService {
    
    private final BatchVisionProcessor processor;
    
    public BatchProcessingService() {
        this.processor = new BatchVisionProcessor();
    }
    
    public CompletableFuture<List<VisionResult>> processImages(List<ImageData> images) {
        return processor.processBatch(images, DetectionType.FACE, Map.of());
    }
}
```

## Testing

Run the tests to verify functionality:

```bash
mvn test
```

The test suite covers:
- Basic batch processing
- Progress monitoring
- Multiple detection types
- Batch cancellation
- Error handling
- Concurrent processing

## Next Steps

- Integrate with your own image data sources
- Configure custom detection parameters
- Implement batch result persistence
- Add monitoring and metrics
- Scale with distributed processing

## Support

For more information about Spring Vision batch processing, see:
- [API Documentation](../docs/API_DOCUMENTATION.md)
- [User Guide](../docs/USER_GUIDE.md)
- [Deployment Guide](../docs/DEPLOYMENT_GUIDE.md) 
