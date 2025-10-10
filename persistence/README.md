# Spring Vision JPA Vector Similarity

This module provides JPA-based persistence and vector-similarity integration for the Spring Vision project. It offers:

- A JPA `FaceEmbedding` entity for storing face embeddings and metadata.
- Repository interfaces for basic CRUD and vendor-specific queries.
- Multiple provider implementations (JPA fallback, PostgreSQL/postgres, Oracle, MySQL, H2 for tests).
- Auto-configuration and schema management helpers.

Quick start
-----------

1. Add the dependency (via `spring-vision-starter` or module artifact):

```xml
<dependency>
  <groupId>com.springvision</groupId>
  <artifactId>spring-vision-starter</artifactId>
  <version>${project.version}</version>
</dependency>
```

2. Configure your datasource and select a vector provider:

```yaml
spring:
  vision:
    vector:
      provider: postgres   # options: postgres, oracle, mysql, jpa, h2
  datasource:
    url: jdbc:postgresql://localhost:5432/springvision
    username: postgres
    password: password
```

3. Use the enhanced template for lookup/registration (auto-configured when enabled):

```java
@Autowired
private io.github.codesapienbe.springvision.core.VisionTemplate visionTemplate;

// Use the VisionTemplate's vector methods (storeFaceEmbedding / lookupFaces)
String embeddingId = visionTemplate.storeFaceEmbedding("person-123", /* embedding */ new float[]{}, "arcface", "imageHash", 0.95, java.util.Map.of());
// Or perform a higher-level lookup provided by the template when enabled:
// List<io.github.codesapienbe.springvision.persistence.dto.FaceMatchResult> matches = visionTemplate.lookupFaces(queryImage, lookupOptions);
```
