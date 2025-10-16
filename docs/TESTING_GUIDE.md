level:
io.github.codesapienbe.springvision: DEBUG
ai.djl: DEBUG

```

### Run Single Test with Debug

```bash
./mvnw test -Dtest=MyTest#testMethod -X
```

### Print Test Output

```java

@Test
void debugTest() {
    System.out.println("Debug info: " + backend.getVersion());
    // Test continues...
}
```

## Performance Testing

### Measure Execution Time

```java

@Test
void testPerformance() {
    long startTime = System.nanoTime();

    // Execute operation
    backend.detectFaces(image);

    long duration = System.nanoTime() - startTime;
    long durationMs = duration / 1_000_000;

    System.out.println("Execution time: " + durationMs + "ms");
    assertTrue(durationMs < 1000, "Operation too slow");
}
```

### Memory Usage

```java

@Test
void testMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long memBefore = runtime.totalMemory() - runtime.freeMemory();

    // Execute operation
    backend.detectFaces(image);

    long memAfter = runtime.totalMemory() - runtime.freeMemory();
    long memUsed = memAfter - memBefore;

    assertTrue(memUsed < 100_000_000, "Memory usage too high");
}
```

## CI/CD Integration

### Maven Surefire Configuration

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*IntegrationTest.java</exclude>
            <exclude>**/*PerformanceTest.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

### GitHub Actions

```yaml
- name: Run tests
  run: ./mvnw test

- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: '**/target/surefire-reports'
```

## Test Coverage

### Generate Coverage Report

```bash
./mvnw clean verify jacoco:report
```

### View Coverage Report

```bash
# Open in browser
open target/site/jacoco/index.html
```

### Coverage Goals

- Core module: > 80%
- Utility classes: > 90%
- Critical paths: 100%

## Troubleshooting

### Tests Fail with "Model Not Found"

- Check if `autoDownload` is enabled
- Verify internet connectivity
- Use mock models for unit tests

### Tests Timeout

- Increase timeout: `@Test(timeout = 30000)`
- Check for resource leaks
- Verify model files are cached

### Memory Issues

- Increase test JVM memory: `export MAVEN_OPTS="-Xmx2g"`
- Check for resource cleanup
- Use smaller test images

### Flaky Tests

- Avoid Thread.sleep(), use proper synchronization
- Mock external dependencies
- Use @RepeatedTest for consistency checks

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

# Spring Vision Testing Guide

## Quick Start

```bash
# Run all tests
./mvnw clean test

# Run specific module tests
./mvnw test -pl core

# Run with detailed output
./mvnw test -X

# Skip tests during build
./mvnw clean install -DskipTests
```

## Test Categories

### 1. Fast Unit Tests (< 1 second each)

These tests run automatically in every build:

- Configuration tests
- Model metadata tests
- Error handling tests
- Basic validation tests

### 2. Integration Tests (1-10 seconds each)

Tests that require initialization but not model downloads:

- Backend lifecycle tests
- Capability interface tests
- Spring context tests

### 3. Full Integration Tests (Disabled by default)

Tests requiring actual model downloads:

```java

@Test
@Disabled("Requires model download")
void testWithRealModel() {
    // Test implementation
}
```

Enable these manually when needed:

```bash
# Remove @Disabled annotation, then:
./mvnw test -Dtest=DjlVisionBackendIntegrationTest
```

### 4. Performance Tests (Disabled by default)

Benchmarking and stress tests:

```bash
# Remove @Disabled annotation from DjlPerformanceTest, then:
./mvnw test -Dtest=DjlPerformanceTest
```

## Writing New Tests

### Test Template

```java
package io.github.codesapienbe.springvision.core.djl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

class MyFeatureTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        DjlProperties properties = new DjlProperties();
        properties.setEnabled(true);
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");

        backend = new DjlVisionBackend(properties);
    }

    @Test
    void testBasicFunctionality() {
        // Arrange
        // Act
        // Assert
        assertNotNull(backend);
    }

    @Test
    @Disabled("Long-running test")
    void testPerformanceScenario() {
        // Performance test implementation
    }
}
```

### Testing Best Practices

1. **Use Descriptive Names**
   ```java
   // Good
   @Test
   void testFaceDetectionWithMultipleFacesReturnsAllDetections() { }
   
   // Bad
   @Test
   void test1() { }
   ```

2. **Follow AAA Pattern**
   ```java
   @Test
   void testExample() {
       // Arrange - Set up test data
       ImageData image = createTestImage();
       
       // Act - Execute the functionality
       List<Detection> result = backend.detectFaces(image);
       
       // Assert - Verify the outcome
       assertNotNull(result);
       assertEquals(2, result.size());
   }
   ```

3. **Test One Thing Per Test**
   ```java
   // Good - Separate tests
   @Test
   void testHealthyStatusWhenInitialized() { }
   
   @Test
   void testUnhealthyStatusWhenNotInitialized() { }
   
   // Bad - Multiple assertions
   @Test
   void testHealthStatus() {
       // Testing too many scenarios
   }
   ```

4. **Use Test Helpers**
   ```java
   private byte[] createTestImage(int width, int height) {
       BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
       // ... image creation logic
       return convertToBytes(image);
   }
   ```

5. **Handle Test Resources**
   ```java
   @TempDir
   Path tempDir;  // JUnit 5 temporary directory
   
   @Test
   void testWithTempFile() {
       Path testFile = tempDir.resolve("test.png");
       // Use testFile
   }
   ```

## Testing Specific Components

### Testing DJL Backend

```java

@Test
void testDjlBackendInitialization() {
    DjlProperties properties = new DjlProperties();
    properties.setEngine("PyTorch");

    DjlVisionBackend backend = new DjlVisionBackend(properties);

    assertEquals("djl", backend.getBackendId());
    assertFalse(backend.isHealthy()); // Not initialized yet
}
```

### Testing Configuration

```java

@SpringBootTest
@TestPropertySource(properties = {
        "spring.vision.djl.enabled=true",
        "spring.vision.djl.engine=PyTorch"
})
class ConfigTest {
    @Autowired
    private DjlProperties properties;

    @Test
    void testConfiguration() {
        assertTrue(properties.isEnabled());
        assertEquals("PyTorch", properties.getEngine());
    }
}
```

### Testing Error Handling

```java

@Test
void testInvalidImageThrowsException() {
    byte[] invalidData = {1, 2, 3};
    ImageData image = new ImageData(invalidData, "image/png", System.currentTimeMillis(), "test");

    assertThrows(VisionBackendException.class, () -> {
        backend.detectFaces(image);
    });
}
```

### Testing Async Operations

```java

@Test
void testConcurrentProcessing() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Future<List<Detection>>> futures = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
        futures.add(executor.submit(() -> backend.detectFaces(createTestImage())));
    }

    for (Future<List<Detection>> future : futures) {
        assertNotNull(future.get(10, TimeUnit.SECONDS));
    }

    executor.shutdown();
}
```

## Test Data Management

### Synthetic Images

```java
private byte[] createTestImage() {
    BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 320, 240);
    g.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "PNG", baos);
    return baos.toByteArray();
}
```

### Loading Test Resources

```java

@Test
void testWithResourceFile() throws Exception {
    InputStream is = getClass().getResourceAsStream("/test-images/face.jpg");
    byte[] imageData = is.readAllBytes();

    ImageData image = new ImageData(imageData, "image/jpeg", System.currentTimeMillis(), "test");
    // Test with real image
}
```

## Debugging Tests

### Enable Debug Logging

```yaml
# application-test.yml
logging:
package io.github.codesapienbe.springvision.core.djl;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Error handling and edge case tests for DJL backend.
 */
class DjlErrorHandlingTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        DjlProperties properties = new DjlProperties();
        properties.setEnabled(true);
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        
        backend = new DjlVisionBackend(properties);
    }

    @Test
    void testNullImageData() {
        assertThrows(NullPointerException.class, () -> {
            backend.detectFaces(null);
        });
    }

    @Test
    void testEmptyImageData() {
        byte[] emptyData = new byte[0];
        ImageData image = new ImageData(emptyData, "image/png", System.currentTimeMillis(), "empty");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testInvalidImageData() {
        byte[] invalidData = {1, 2, 3, 4, 5};
        ImageData image = new ImageData(invalidData, "image/png", System.currentTimeMillis(), "invalid");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testUnsupportedImageFormat() {
        byte[] data = new byte[100];
        ImageData image = new ImageData(data, "image/unsupported", System.currentTimeMillis(), "test");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testOperationBeforeInitialization() {
        byte[] data = new byte[100];
        ImageData image = new ImageData(data, "image/png", System.currentTimeMillis(), "test");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testMultipleShutdowns() {
        // First shutdown
        assertDoesNotThrow(() -> backend.shutdown());
        
        // Second shutdown should not throw
        assertDoesNotThrow(() -> backend.shutdown());
        
        // Third shutdown should not throw
        assertDoesNotThrow(() -> backend.shutdown());
    }

    @Test
    void testHealthCheckConsistency() {
        // Before initialization
        assertFalse(backend.isHealthy());
        
        var healthInfo = backend.getHealthInfo();
        assertNotNull(healthInfo);
        assertEquals("djl", healthInfo.getBackendId());
    }

    @Test
    void testInvalidDeviceConfiguration() {
        DjlProperties properties = new DjlProperties();
        properties.setDevice("invalid-device");
        
        // Should handle gracefully and fall back to CPU
        assertDoesNotThrow(() -> {
            new DjlVisionBackend(properties);
        });
    }

    @Test
    void testNullProperties() {
        // Should use defaults
        assertDoesNotThrow(() -> {
            new DjlVisionBackend(null);
        });
    }

    @Test
    void testExtremelySmallImage() {
        // 1x1 pixel image
        byte[] smallImage = createMinimalImage(1, 1);
        ImageData image = new ImageData(smallImage, "image/png", System.currentTimeMillis(), "tiny");

        // Should handle gracefully
        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testCorruptedImageHeader() {
        byte[] corruptedData = new byte[1000];
        // Fill with random data
        for (int i = 0; i < corruptedData.length; i++) {
            corruptedData[i] = (byte) (Math.random() * 256);
        }
        
        ImageData image = new ImageData(corruptedData, "image/png", System.currentTimeMillis(), "corrupted");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(image);
        });
    }

    @Test
    void testConcurrentShutdown() throws InterruptedException {
        Thread t1 = new Thread(() -> backend.shutdown());
        Thread t2 = new Thread(() -> backend.shutdown());
        Thread t3 = new Thread(() -> backend.shutdown());

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        assertFalse(backend.isHealthy());
    }

    @Test
    void testGetVersionNeverReturnsNull() {
        assertNotNull(backend.getVersion());
        assertFalse(backend.getVersion().isEmpty());
    }

    @Test
    void testGetBackendIdNeverReturnsNull() {
        assertNotNull(backend.getBackendId());
        assertEquals("djl", backend.getBackendId());
    }

    @Test
    void testGetDisplayNameNeverReturnsNull() {
        assertNotNull(backend.getDisplayName());
        assertFalse(backend.getDisplayName().isEmpty());
    }

    @Test
    void testGetSupportedDetectionTypesNeverReturnsNull() {
        assertNotNull(backend.getSupportedDetectionTypes());
        assertFalse(backend.getSupportedDetectionTypes().isEmpty());
    }

    @Test
    void testHealthInfoNeverReturnsNull() {
        assertNotNull(backend.getHealthInfo());
    }

    private byte[] createMinimalImage(int width, int height) {
        // Create a minimal valid PNG image
        return new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A  // PNG signature
        };
    }
}

