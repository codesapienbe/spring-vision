# Testing with Docker Compose

This guide explains how Spring Vision modules use Docker Compose for integration testing.

## Overview

Spring Vision uses **Spring Boot's native Docker Compose support** to automatically manage external services during integration tests. This eliminates the need for custom Maven plugins or complex test setup.

## How It Works

When you add `spring-boot-docker-compose` to your test dependencies, Spring Boot automatically:

1. **Detects** `docker-compose.yml` in your module directory
2. **Starts** all services defined in the compose file
3. **Waits** for health checks to pass
4. **Injects** connection properties as system properties
5. **Stops** and cleans up services after tests complete

## Quick Start

### 1. Create Module-Specific Compose File

Copy the template to your module:

```bash
cp templates/docker-compose.module.yml <your-module>/docker-compose.yml
```

### 2. Customize for Your Service

Edit the compose file to define your external service:

```yaml
version: '3.8'

services:
  myservice:
    image: myservice/image:latest
    container_name: sv-mymodule-it
    ports:
      - "127.0.0.1:5000:5000"
    healthcheck:
      test: [ "CMD-SHELL", "curl -f http://localhost:5000/ || exit 1" ]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 30s

networks:
  sv-mymodule-network:
    driver: bridge
```

### 3. Add Docker Compose Dependency

Add to your module's `pom.xml`:

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>test</scope>
</dependency>
```

### 4. Configure Test Properties

Create `src/test/resources/application-test.properties`:

```properties
# Enable docker-compose integration
spring.docker.compose.enabled=true
spring.docker.compose.file=docker-compose.yml
# Configure your service connection
spring.vision.mymodule.base-url=http://localhost:5000
spring.vision.mymodule.enabled=true
```

### 5. Write Integration Tests

```java

@SpringBootTest
@ActiveProfiles("test")
class MyModuleIntegrationTest {

    @Autowired
    private VisionTemplate visionTemplate;

    @Test
    void testWithRealService() {
        // Spring Boot has already started docker-compose
        // Your service is ready and healthy

        ImageData image = ImageData.fromFile("test.jpg");
        List<Detection> detections = visionTemplate.detect(image);

        assertThat(detections).isNotEmpty();
    }
}
```

### 6. Run Tests

```bash
# Run all tests (starts docker-compose automatically)
mvn verify

# Skip docker-compose if you have service running manually
mvn verify -Dspring.docker.compose.enabled=false
```

## Module Examples

### DeepFace Module

**`deepface/docker-compose.yml`:**

```yaml
version: '3.8'

services:
  deepface:
    image: serengil/deepface:latest
    container_name: sv-deepface-it
    ports:
      - "127.0.0.1:5000:5000"
    environment:
      - DEEPFACE_HOME=/root/.deepface
    volumes:
      - deepface-models:/root/.deepface
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:5000/" ]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 10s

volumes:
  deepface-models:
    driver: local
```

**Test configuration:**

```properties
spring.docker.compose.enabled=true
spring.vision.deepface.base-url=http://localhost:5000
spring.vision.deepface.enabled=true
```

### CompreFace Module

**`compreface/docker-compose.yml`:**

```yaml
version: '3.8'

services:
  compreface:
    image: exadel/compreface:latest
    container_name: sv-compreface-it
    environment:
      POSTGRES_URL: jdbc:postgresql://postgres:5432/compreface
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      EXTERNAL_DB: "true"
    ports:
      - "127.0.0.1:8000:80"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: [ "CMD-SHELL", "curl -f http://localhost:80/api/v1/recognition/detect || exit 1" ]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  postgres:
    image: postgres:15-alpine
    container_name: sv-compreface-postgres
    environment:
      POSTGRES_DB: compreface
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "127.0.0.1:5432:5432"
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  sv-compreface-network:
    driver: bridge
```

## Best Practices

### Health Checks Are Critical

Always define health checks for your services:

```yaml
healthcheck:
  test: [ "CMD-SHELL", "curl -f http://localhost:5000/health || exit 1" ]
  interval: 10s      # How often to check
  timeout: 5s        # Max time for health check
  retries: 30        # How many failures before giving up
  start_period: 30s  # Grace period on startup
```

Without health checks, Spring Boot won't know when services are ready, causing test failures.

### Use Localhost Binding

Always bind ports to `127.0.0.1` for security:

```yaml
ports:
  - "127.0.0.1:5000:5000"  # ✅ Good - localhost only
  - "5000:5000"            # ❌ Bad - exposed to network
```

### Container Naming Convention

Use consistent naming: `sv-{module}-it` (Spring Vision module integration test)

```yaml
container_name: sv-deepface-it
container_name: sv-compreface-it
```

### Network Isolation

Create module-specific networks to avoid conflicts:

```yaml
networks:
  sv-deepface-network:
    driver: bridge
```

### Volume Management

Use named volumes for persistent data:

```yaml
volumes:
  service-data:/data

volumes:
  service-data:
    driver: local
```

### Start Period for Slow Services

Some services (like CompreFace) take time to initialize. Use `start_period`:

```yaml
healthcheck:
  start_period: 60s  # Give service 60s to start before checking health
```

## Controlling Docker Compose

### Skip Docker Compose

If you already have services running:

```bash
mvn verify -Dspring.docker.compose.enabled=false
```

### Custom Compose File

Use a different compose file:

```properties
spring.docker.compose.file=docker-compose-custom.yml
```

### Lifecycle Management

Control when services start/stop:

```properties
# Start on test context startup (default)
spring.docker.compose.lifecycle-management=start-and-stop
# Don't stop after tests (for debugging)
spring.docker.compose.lifecycle-management=start-only
# Don't manage at all
spring.docker.compose.lifecycle-management=none
```

### Wait Timeout

Increase timeout for slow services:

```properties
# Wait up to 5 minutes for services to be healthy
spring.docker.compose.wait-timeout=5m
```

## Troubleshooting

### Service Won't Start

**Check logs:**

```bash
docker compose -f <module>/docker-compose.yml logs
```

**Common issues:**

- Port already in use: Change port in compose file
- Image not found: Pull image manually first
- Network conflict: Use unique network names

### Health Check Fails

**Test health check manually:**

```bash
docker compose -f <module>/docker-compose.yml up -d
curl http://localhost:5000/health
```

**Common issues:**

- Wrong health check URL
- Service needs more startup time (increase `start_period`)
- Missing dependencies (database not ready)

### Tests Time Out

**Increase wait timeout:**

```properties
spring.docker.compose.wait-timeout=10m
```

**Check service logs:**

```bash
docker compose -f <module>/docker-compose.yml logs -f
```

### Port Conflicts

**Check what's using the port:**

```bash
lsof -i :5000
```

**Solutions:**

- Stop conflicting service
- Change port in compose file
- Use different port for tests

## CI/CD Integration

### GitHub Actions

```yaml
name: Integration Tests

on: [ push, pull_request ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run integration tests
        run: mvn verify
        # Docker Compose automatically starts/stops
```

### GitLab CI

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  services:
    - docker:dind
  variables:
    DOCKER_HOST: tcp://docker:2375
  script:
    - mvn verify
```

## Performance Tips

### Reuse Containers

Use `start-only` during development to keep containers running:

```properties
spring.docker.compose.lifecycle-management=start-only
```

### Parallel Tests

Run tests in parallel but use different ports per module to avoid conflicts.

### Skip Unnecessary Tests

Skip integration tests during quick builds:

```bash
mvn install -DskipITs
```

## Migration from Custom Scripts

If you have custom Maven exec plugin configurations for docker-compose:

**Before (custom exec plugin):**

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>docker-compose-up</id>
            <phase>pre-integration-test</phase>
            <!-- complex configuration -->
        </execution>
    </executions>
</plugin>
```

**After (Spring Boot native):**

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>test</scope>
</dependency>
```

That's it! Much simpler and more reliable.

## References

- [Spring Boot Docker Compose Support](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.docker-compose)
- [Docker Compose File Reference](https://docs.docker.com/compose/compose-file/)
- [Health Check Best Practices](https://docs.docker.com/engine/reference/builder/#healthcheck)

## Need Help?

Check module READMEs for specific examples or open an issue on GitHub.

