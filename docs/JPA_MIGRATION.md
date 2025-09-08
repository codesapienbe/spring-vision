# Migrating to JPA Vector Similarity

This guide describes how to add and migrate to the Spring Vision JPA Vector Similarity module.

Overview
--------
The JPA module (`spring-vision-jpa`) provides persistent storage for face embeddings and optional native vector support for databases such as PostgreSQL (pgvector), Oracle 23ai and MySQL. When a native provider is unavailable, a JPA fallback stores embeddings as blobs and performs in-memory similarity search.

Steps to add JPA vector similarity
---------------------------------
1. Add the optional starter or module dependency to your project:

```xml
<dependency>
  <groupId>com.springvision</groupId>
  <artifactId>spring-vision-starter</artifactId>
  <version>${project.version}</version>
</dependency>
```

2. Choose a vector provider and configure your datasource:

- For PostgreSQL + postgres provider (pgvector extension):
  - Ensure `pgvector` extension is installed on the DB instance.
  - Set `spring.vision.vector.provider=postgres`.

- For Oracle or MySQL native support, set `spring.vision.vector.provider=oracle` or `mysql` and provide the correct driver and connection details.

- For simple testing or local development use H2 with `spring.vision.vector.provider=h2`.

3. Configure JPA / datasource settings in `application.yml` or `application.properties`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/springvision
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update
```

4. (Optional) Enable enhanced template to wire vector-enabled `VisionTemplate`:

```yaml
spring:
  vision:
    jpa:
      enhanced-template: true
```

Migration notes
---------------
- The `FaceEmbedding` entity stores embeddings as a BLOB (`embedding_blob`) accessible via the `FaceEmbeddingRepository`. Vendor-specific columns are present but optional.
- When switching from the JPA fallback to a native vector provider (e.g. Postgres/pgvector), consider a migration strategy to populate the `native_vector` column from existing blobs.
- The module provides `VectorSchemaManager` to create vendor-specific schema actions at application startup (e.g., `CREATE EXTENSION vector` and indexes for PostgreSQL). Use caution in production; prefer migration scripts under DBA control.

Testing & Development
---------------------
- For CI and local testing, H2 is supported as a lightweight provider. Use `spring.datasource.url=jdbc:h2:mem:testdb` and `spring.vision.vector.provider=h2`.
- Tests in the module use an in-memory H2 database and create the required `face_embeddings` table automatically.

Troubleshooting
---------------
- If the application fails to start due to missing native drivers, set `spring.vision.vector.provider=jpa` to use the JPA fallback.
- Ensure DB-specific drivers are present on the classpath when using native providers (PostgreSQL, Oracle, MySQL).

Contact / Next steps
--------------------
- See `docs/jpa/TODO.md` for remaining work items and progressive batches.
- Open issues or PRs should include reproducible steps and test coverage where applicable. 