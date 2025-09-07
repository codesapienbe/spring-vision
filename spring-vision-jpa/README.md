# Spring Vision JPA Vector Similarity

This module provides JPA-based persistence and vector-similarity integration for the Spring Vision project. It offers:

- A JPA `FaceEmbedding` entity for storing face embeddings and metadata.
- Repository interfaces for basic CRUD and vendor-specific queries.
- Multiple provider implementations (JPA fallback, PostgreSQL/pgvector, Oracle, MySQL, H2 for tests).
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
      provider: pgvector   # options: pgvector, oracle, mysql, jpa, h2
  datasource:
    url: jdbc:postgresql://localhost:5432/springvision
    username: postgres
    password: password
```

3. Use the enhanced template for lookup/registration (auto-configured when enabled):

```java
@Autowired
private com.springvision.jpa.template.VectorEnabledVisionTemplate visionTemplate;

// Register or lookup faces
String embeddingId = visionTemplate.registerFace("person-123", imageBytes, options);
List<com.springvision.jpa.dto.FaceMatchResult> matches = visionTemplate.lookupFaces(queryImage, lookupOptions);
```

Testing & H2
------------
For tests and lightweight development you can use H2 as an alternative provider. Set:

```
spring.datasource.url=jdbc:h2:mem:testdb
spring.vision.vector.provider=h2
spring.jpa.hibernate.ddl-auto=create-drop
```

Contributing
------------
Follow the batch plan in `docs/jpa/TODO.md`. Each change should be small, compile cleanly, and include Javadoc for public types. 