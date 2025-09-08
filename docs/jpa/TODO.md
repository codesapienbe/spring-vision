# Spring Vision JPA Vector Similarity Service - Implementation TODO

## Overview
This TODO implements JPA support for Spring Vision with focus on vector similarity search for face embeddings. Each batch is designed to be small, buildable, and testable independently.

---

## BATCH 1: Core Infrastructure and Base Entities
**Goal**: Set up foundational JPA entities and basic repository structure
**Estimated Time**: 4-6 hours

### 1.1 Create Base Audit Entity
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/AuditableEntity.java`
  ```java
  @MappedSuperclass
  @EntityListeners(AuditingEntityListener.class)
  public abstract class AuditableEntity {
      @CreatedDate
      @Column(name = "created_at", nullable = false, updatable = false)
      private LocalDateTime createdAt;
      
      @LastModifiedDate
      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
      
      @Version
      private Long version;
      
      // getters/setters
  }
  ```

### 1.2 Create Core Face Embedding Entity
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
  ```java
  @Entity
  @Table(name = "face_embeddings")
  public class FaceEmbedding extends AuditableEntity {
      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;
      
      @Column(nullable = false)
      private String personId;
      
      @Column(nullable = false) 
      private String modelName;
      
      @Column(nullable = false)
      private Integer dimension;
      
      @Lob
      @Column(name = "embedding_blob", nullable = false)
      private byte[] embeddingBlob;
      
      @jakarta.persistence.Column(name = "native_vector")
      private byte[] nativeVector;
      
      @Column
      private String imageHash;
      
      @Column
      private Double confidence;
      
      // constructors, getters, setters
  }
  ```

### 1.3 Create Basic Repository Interface
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/FaceEmbeddingRepository.java`
  ```java
  @Repository
  public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {
      List<FaceEmbedding> findByPersonId(String personId);
      List<FaceEmbedding> findByModelName(String modelName);
      List<FaceEmbedding> findByModelNameAndPersonId(String modelName, String personId);
      
      @Query("SELECT COUNT(e) FROM FaceEmbedding e WHERE e.modelName = :modelName")
      long countByModelName(@Param("modelName") String modelName);
  }
  ```

### 1.4 Create Basic JPA Configuration
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaConfiguration.java`
  ```java
  @Configuration
  @EnableJpaRepositories(basePackages = "com.springvision.jpa.repository")
  @EnableJpaAuditing
  @ConditionalOnProperty(value = "spring.vision.jpa.enabled", havingValue = "true", matchIfMissing = true)
  public class VisionJpaConfiguration {
      // Basic configuration only
  }
  ```

### 1.5 Add to Main POM
- [x] **File**: `pom.xml` - Add new module
  ```xml
  <modules>
      <!-- existing modules -->
      <module>spring-vision-jpa</module>
  </modules>
  ```

### 1.6 Create JPA Module POM
- [x] **File**: `spring-vision-jpa/pom.xml`
  ```xml
  <dependencies>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-data-jpa</artifactId>
      </dependency>
      <dependency>
          <groupId>com.springvision</groupId>
          <artifactId>spring-vision-core</artifactId>
          <version>${project.version}</version>
      </dependency>
  </dependencies>
  ```

### 1.7 Build and Test Batch 1
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Verify**: No compilation errors
- [x] **Test**: Create simple integration test for entity creation

---

## BATCH 2: Vector Utility Classes and DTOs
**Goal**: Create vector handling utilities and data transfer objects
**Estimated Time**: 3-4 hours

### 2.1 Create Vector Utility Class
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/util/VectorUtils.java`
  ```java
  public class VectorUtils {
      public static byte[] serializeFloatArray(float[] array) {
          // Convert float[] to byte[] using ByteBuffer
      }
      
      public static float[] deserializeFloatArray(byte[] bytes) {
          // Convert byte[] back to float[]
      }
      
      public static double cosineSimilarity(float[] vec1, float[] vec2) {
          // Calculate cosine similarity
      }
      
      public static double euclideanDistance(float[] vec1, float[] vec2) {
          // Calculate euclidean distance
      }
      
      public static String calculateImageHash(byte[] imageData) {
          // Calculate SHA-256 hash of image
      }
  }
  ```

### 2.2 Create Request/Response DTOs
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/StoreFaceEmbeddingRequest.java`
  ```java
  public record StoreFaceEmbeddingRequest(
      String personId,
      float[] embedding,
      String modelName,
      String imageHash,
      Double confidence,
      Map<String, Object> metadata
  ) {}
  ```

- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/SimilaritySearchRequest.java`
  ```java
  public record SimilaritySearchRequest(
      float[] queryEmbedding,
      String modelName,
      SimilarityMetric metric,
      Double threshold,
      Integer limit,
      Set<String> includePersonIds,
      Set<String> excludePersonIds
  ) {}
  ```

- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/SimilaritySearchResult.java`
  ```java
  public record SimilaritySearchResult(
      String embeddingId,
      String personId,
      Double similarity,
      Double distance,
      String modelName,
      LocalDateTime createdAt,
      Map<String, Object> metadata
  ) {}
  ```

### 2.3 Create Enums
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/enums/SimilarityMetric.java`
  ```java
  public enum SimilarityMetric {
      COSINE, EUCLIDEAN, DOT_PRODUCT, MANHATTAN
  }
  ```

- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/enums/DatabaseVendor.java`
  ```java
  public enum DatabaseVendor {
      POSTGRESQL, ORACLE, MYSQL, H2, HSQLDB, UNKNOWN
  }
  ```

### 2.4 Build and Test Batch 2
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: Unit tests for VectorUtils methods
- [x] **Verify**: All DTOs serialize/deserialize correctly

---

## BATCH 3: Abstract Vector Similarity Service
**Goal**: Create the core service interface and basic implementation
**Estimated Time**: 4-5 hours

### 3.1 Create Service Interface
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/VectorSimilarityService.java`
  ```java
  public interface VectorSimilarityService {
      String storeFaceEmbedding(StoreFaceEmbeddingRequest request);
      List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request);
      VerificationResult verifyFaces(VerificationRequest request);
      void deleteFaceEmbedding(String embeddingId);
      VectorServiceHealth getHealth();
      Set<String> getSupportedMetrics();
  }
  ```

### 3.2 Create Database Vendor Detector
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/DatabaseVendorDetector.java`
  ```java
  @Component
  public class DatabaseVendorDetector {
      private final DataSource dataSource;
      
      public DatabaseVendor detectVendor() {
          try (Connection connection = dataSource.getConnection()) {
              String url = connection.getMetaData().getURL().toLowerCase();
              if (url.contains("postgresql")) return DatabaseVendor.POSTGRESQL;
              if (url.contains("oracle")) return DatabaseVendor.ORACLE;
              if (url.contains("mysql")) return DatabaseVendor.MYSQL;
              if (url.contains("h2")) return DatabaseVendor.H2;
              return DatabaseVendor.UNKNOWN;
          }
      }
  }
  ```

### 3.3 Create Basic JPA Implementation (Fallback)
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/JpaVectorSimilarityService.java`
  ```java
  @Service
  @ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "jpa", matchIfMissing = true)
  public class JpaVectorSimilarityService implements VectorSimilarityService {
      
      private final FaceEmbeddingRepository embeddingRepository;
      
      @Override
      public String storeFaceEmbedding(StoreFaceEmbeddingRequest request) {
          FaceEmbedding embedding = new FaceEmbedding();
          embedding.setPersonId(request.personId());
          embedding.setModelName(request.modelName());
          embedding.setDimension(request.embedding().length);
          embedding.setEmbeddingBlob(VectorUtils.serializeFloatArray(request.embedding()));
          embedding.setImageHash(request.imageHash());
          embedding.setConfidence(request.confidence());
          
          FaceEmbedding saved = embeddingRepository.save(embedding);
          return saved.getId().toString();
      }
      
      @Override
      public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
          // Basic implementation using parallel streams for similarity calculation
          List<FaceEmbedding> allEmbeddings = embeddingRepository.findByModelName(request.modelName());
          
          return allEmbeddings.parallelStream()
              .map(embedding -> calculateSimilarity(embedding, request))
              .filter(result -> result.similarity() >= request.threshold())
              .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
              .limit(request.limit())
              .collect(Collectors.toList());
      }
      
      private SimilaritySearchResult calculateSimilarity(FaceEmbedding embedding, SimilaritySearchRequest request) {
          float[] storedVector = VectorUtils.deserializeFloatArray(embedding.getEmbeddingBlob());
          double similarity = VectorUtils.cosineSimilarity(request.queryEmbedding(), storedVector);
          
          return new SimilaritySearchResult(
              embedding.getId().toString(),
              embedding.getPersonId(),
              similarity,
              1.0 - similarity,
              embedding.getModelName(),
              embedding.getCreatedAt(),
              Map.of()
          );
      }
  }
  ```

### 3.4 Build and Test Batch 3
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: Integration test storing and retrieving embeddings
- [x] **Verify**: Basic similarity search works

---

## BATCH 4: PostgreSQL Native (postgres/pgvector) Support
**Goal**: Add native PostgreSQL vector support with custom UserType
**Estimated Time**: 6-8 hours

### 4.1 Add PostgreSQL Dependencies
- [x] **File**: `spring-vision-jpa/pom.xml` - Add dependencies
  ```xml
  <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <optional>true</optional>
  </dependency>
  ```

### 4.2 PostgreSQL native support (postgres/pgvector)
 - [x] The project previously included a `PgVectorType` UserType helper. The current
   design consolidates native storage into a single `native_vector` column and uses
   `NativeVectorAdapter` to convert `native_vector` to provider-specific formats at
   runtime (e.g., Postgres `PGobject` for pgvector). The legacy `PgVectorType` helper
   has been removed and replaced by the adapter-based approach. Any remaining references
   to `pgvector` in comments or docs are informational and refer to the Postgres extension.

### 4.3 Update FaceEmbedding Entity for PostgreSQL
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
  ```java
  // Add to existing entity:
  
  @Type(PgVectorType.class)
  @Column(name = "pgvector_embedding", columnDefinition = "vector")
  private float[] pgVectorEmbedding;
  ```

### 4.4 Create PostgreSQL-specific Repository
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/PostgreSQLFaceEmbeddingRepository.java`
  ```java
  @Repository
  @ConditionalOnClass(name = "org.postgresql.util.PGobject")
  public interface PostgreSQLFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {
      
      @Query(value = """
          SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
                 (e.pgvector_embedding <=> CAST(?1 AS vector)) as distance 
          FROM face_embeddings e 
          WHERE e.model_name = ?2 
          AND (e.pgvector_embedding <=> CAST(?1 AS vector)) < ?3
          ORDER BY distance ASC
          LIMIT ?4
          """, nativeQuery = true)
      List<Object[]> findSimilarByCosineSimilarity(String queryVector, 
                                                  String modelName, 
                                                  Double threshold, 
                                                  Integer limit);
  }
  ```

### 4.5 Create PostgreSQL Service Implementation
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/PostgreSQLVectorSimilarityService.java`
  ```java
  @Service
  @ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "pgvector")
  @ConditionalOnClass(name = "org.postgresql.util.PGobject")
  public class PostgreSQLVectorSimilarityService implements VectorSimilarityService {
      
      private final PostgreSQLFaceEmbeddingRepository repository;
      
      @Override
      public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
          String vectorString = formatPgVector(request.queryEmbedding());
          
          List<Object[]> results = repository.findSimilarByCosineSimilarity(
              vectorString, 
              request.modelName(), 
              request.threshold(), 
              request.limit()
          );
          
          return results.stream()
              .map(this::mapToSimilarityResult)
              .collect(Collectors.toList());
      }
      
      private String formatPgVector(float[] embedding) {
          return "[" + Arrays.stream(embedding)
              .mapToObj(String::valueOf)
              .collect(Collectors.joining(",")) + "]";
      }
  }
  ```

### 4.6 Build and Test Batch 4
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: PostgreSQL integration test with vector operations
- [x] **Verify**: Postgres (pgvector) similarity search works correctly using `native_vector` and adapter conversion

---

## BATCH 5: Oracle 23ai Vector Support
**Goal**: Add Oracle Database 23ai native vector support
**Estimated Time**: 5-7 hours

### 5.1 Add Oracle Dependencies
- [x] **File**: `spring-vision-jpa/pom.xml`
  ```xml
  <dependency>
      <groupId>com.oracle.database.jdbc</groupId>
      <artifactId>ojdbc11</artifactId>
      <optional>true</optional>
  </dependency>
  <!-- Add Hibernate Vector module when available -->
  ```

### 5.2 Update Entity for Oracle Support
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
  ```java
  // Add to existing entity:
  
  @JdbcTypeCode(SqlTypes.VARBINARY) // Will be SqlTypes.VECTOR when available
  @Array(length = 512)
  @Column(name = "oracle_embedding")
  private float[] oracleEmbedding;
  ```

### 5.3 Create Oracle-specific Repository
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/OracleFaceEmbeddingRepository.java`
  ```java
  @Repository
  @ConditionalOnClass(name = "oracle.jdbc.OracleConnection")
  public interface OracleFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {
      
      @Query(value = """
          SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
                 VECTOR_DISTANCE(e.oracle_embedding, :queryVector, COSINE) as distance
          FROM face_embeddings e 
          WHERE e.model_name = :modelName
          AND VECTOR_DISTANCE(e.oracle_embedding, :queryVector, COSINE) < :threshold
          ORDER BY distance ASC
          FETCH FIRST :limit ROWS ONLY
          """, nativeQuery = true)
      List<Object[]> findSimilarByCosineSimilarity(@Param("queryVector") byte[] queryVector,
                                                   @Param("modelName") String modelName, 
                                                   @Param("threshold") Double threshold, 
                                                   @Param("limit") Integer limit);
  }
  ```

### 5.4 Create Oracle Service Implementation
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/OracleVectorSimilarityService.java`
  ```java
  @Service
  @ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "oracle")
  @ConditionalOnClass(name = "oracle.jdbc.OracleConnection")
  public class OracleVectorSimilarityService implements VectorSimilarityService {
      
      private final OracleFaceEmbeddingRepository repository;
      
      @Override
      public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
          byte[] vectorBytes = floatArrayToOracleVector(request.queryEmbedding());
          
          List<Object[]> results = repository.findSimilarByCosineSimilarity(
              vectorBytes,
              request.modelName(), 
              request.threshold(), 
              request.limit()
          );
          
          return results.stream()
              .map(this::mapToSimilarityResult)
              .collect(Collectors.toList());
      }
      
      private byte[] floatArrayToOracleVector(float[] embedding) {
          // Convert float[] to Oracle VECTOR format
      }
  }
  ```

### 5.5 Build and Test Batch 5
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: Oracle integration test (if Oracle 23ai available)
- [x] **Verify**: Service selection works correctly

---

## BATCH 6: Auto-Configuration and Schema Management  
**Goal**: Implement automatic database detection and schema creation
**Estimated Time**: 4-5 hours

### 6.1 Enhance Auto-Configuration
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaAutoConfiguration.java`
  ```java
  @Configuration
  @EnableConfigurationProperties(VectorSimilarityProperties.class)
  public class VisionJpaAutoConfiguration {
      
      @Bean
      @ConditionalOnMissingBean
      public VectorSimilarityService vectorSimilarityService(
              VectorSimilarityProperties properties,
              FaceEmbeddingRepository repository,
              DatabaseVendorDetector vendorDetector,
              @Autowired(required = false) JdbcTemplate jdbcTemplate) {
          
          DatabaseVendor vendor = vendorDetector.detectVendor();
          String configuredProvider = properties.getProvider().name().toLowerCase();
          
          return switch (vendor) {
              case POSTGRESQL -> configuredProvider.equals("pgvector") ? 
                  new PostgreSQLVectorSimilarityService(repository, jdbcTemplate) :
                  new JpaVectorSimilarityService(repository);
              case ORACLE -> configuredProvider.equals("oracle") ?
                  new OracleVectorSimilarityService(repository, jdbcTemplate) :
                  new JpaVectorSimilarityService(repository);
              default -> new JpaVectorSimilarityService(repository);
          };
      }
  }
  ```

### 6.2 Create Configuration Properties
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VectorSimilarityProperties.java`
  ```java
  @ConfigurationProperties(prefix = "spring.vision.vector")
  @Data
  public class VectorSimilarityProperties {
      private VectorProvider provider = VectorProvider.AUTO;
      private PostgreSQL postgresql = new PostgreSQL();
      private Oracle oracle = new Oracle();
      private MySQL mysql = new MySQL();
      
      @Data
      public static class PostgreSQL {
          private boolean enabled = true;
          private String indexType = "hnsw";
          private int hnswM = 16;
          private int hnswEfConstruction = 64;
      }
      
      @Data
      public static class Oracle {
          private boolean enabled = true;
          private String indexType = "hnsw";
          private int targetAccuracy = 95;
      }
      
      public enum VectorProvider {
          AUTO, PGVECTOR, ORACLE, MYSQL, JPA
      }
  }
  ```

### 6.3 Create Schema Manager
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VectorSchemaManager.java`
  ```java
  @Component
  public class VectorSchemaManager {
      
      private final DatabaseVendorDetector vendorDetector;
      private final JdbcTemplate jdbcTemplate;
      private final VectorSimilarityProperties properties;
      
      @EventListener
      public void onApplicationReady(ApplicationReadyEvent event) {
          DatabaseVendor vendor = vendorDetector.detectVendor();
          createVectorSchema(vendor);
      }
      
      private void createVectorSchema(DatabaseVendor vendor) {
          switch (vendor) {
              case POSTGRESQL -> createPostgreSQLSchema();
              case ORACLE -> createOracleSchema();
              case MYSQL -> createMySQLSchema();
          }
      }
      
      private void createPostgreSQLSchema() {
          if (properties.getPostgresql().isEnabled()) {
              executeSQL("CREATE EXTENSION IF NOT EXISTS vector");
              executeSQL(String.format("CREATE INDEX IF NOT EXISTS idx_face_embeddings_native_vector ON face_embeddings USING hnsw (native_vector vector_cosine_ops) WITH (m = %d, ef_construction = %d)",
                      properties.getPostgresql().getHnswM(), properties.getPostgresql().getHnswEfConstruction()));
          }
      }
      
      private void executeSQL(String sql) {
          try {
              jdbcTemplate.execute(sql);
          } catch (Exception e) {
              // Log warning but don't fail startup
          }
      }
  }
  ```

### 6.4 Build and Test Batch 6
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: Auto-configuration with different database types
- [x] **Verify**: Schema creation works for available databases

---

## BATCH 7: Vision Template Integration
**Goal**: Integrate vector similarity with existing Vision Template
**Estimated Time**: 5-6 hours

### 7.1 Vector features merged into VisionTemplate (adapter removed)
- [x] **Removed**: `VectorEnabledVisionTemplate` has been removed from the codebase; its functionality is merged into `com.springvision.core.VisionTemplate`.

Use `VisionTemplate` directly for embedding storage and face lookup APIs.

Example (use `VisionTemplate` directly):
```java
@Autowired
private com.springvision.core.VisionTemplate visionTemplate;

// Store an embedding via the template
String embeddingId = visionTemplate.storeFaceEmbedding(personId, embedding, "arcface", imageHash, confidence, Map.of());

// Lookup faces (when enhanced-template enabled)
// List<com.springvision.jpa.dto.FaceMatchResult> matches = visionTemplate.lookupFaces(imageData, lookupOptions);
```

### 7.2 Create Face Lookup DTOs
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceLookupOptions.java`
  ```java
  @Builder
  @Data
  public class FaceLookupOptions {
      private String modelName = "arcface";
      private SimilarityMetric metric = SimilarityMetric.COSINE;
      private Double threshold = 0.7;
      private Integer limit = 10;
      private Set<String> includePersonIds = Set.of();
      private Set<String> excludePersonIds = Set.of();
  }
  ```

- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceRegistrationOptions.java`
  ```java
  @Builder
  @Data
  public class FaceRegistrationOptions {
      private String modelName = "arcface";
      private Map<String, Object> metadata = Map.of();
  }
  ```

- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceMatchResult.java`
  ```java
  public record FaceMatchResult(
      Detection detectedFace,
      List<SimilaritySearchResult> matches,
      SimilarityMetric metric
  ) {}
  ```

### 7.3 Update Auto-Configuration for Template
- [x] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaAutoConfiguration.java`
  ```java
  // Add to existing configuration:
  
  @Bean
  @Primary
  @ConditionalOnProperty(value = "spring.vision.jpa.enhanced-template", havingValue = "true", matchIfMissing = true)
  public com.springvision.core.VisionTemplate createEnhancedVisionTemplate(
          @Qualifier("originalVisionTemplate") com.springvision.core.VisionTemplate originalTemplate,
          VectorSimilarityService vectorService) {
      // The enhanced VisionTemplate is created by delegating to the original template and
      // wiring the active VectorService. The old adapter class has been removed; this
      // factory returns a VisionTemplate that uses the configured VectorService directly.
      return new com.springvision.core.VisionTemplate(originalTemplate.getBackend(), (com.springvision.core.VectorService) vectorService);
  }
  
  @Bean("originalVisionTemplate")
  @ConditionalOnMissingBean(name = "originalVisionTemplate")
  public com.springvision.core.VisionTemplate originalVisionTemplate() {
      // Return the existing VisionTemplate implementation
      return new com.springvision.core.DefaultVisionTemplate(); // or inject existing one
  }
  ```

### 7.4 Build and Test Batch 7
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Test**: Integration test with face lookup functionality
- [x] **Verify**: Enhanced template works with vector similarity

---

## BATCH 8: Example Integration and Testing
**Goal**: Create examples and comprehensive testing
**Estimated Time**: 4-5 hours

### 8.1 Create REST Controller Example
- [x] **File**: `spring-vision-examples/jpa-vector-example/src/main/java/com/springvision/example/controller/VectorFaceLookupController.java`
  ```java
  @RestController
  @RequestMapping("/api/faces")
  public class VectorFaceLookupController {

      @Autowired
      private com.springvision.core.VisionTemplate visionTemplate;

      @PostMapping("/lookup")
      public ResponseEntity<List<PersonMatch>> lookupFaces(@RequestParam("file") MultipartFile file) {
          try {
              byte[] data = file.getBytes();
              com.springvision.core.ImageData img = com.springvision.core.ImageData.fromBytes(data);
              List<float[]> embeddings = visionTemplate.extractEmbeddings(img);
              if (embeddings == null || embeddings.isEmpty()) return ResponseEntity.ok(List.of());

              float[] emb = embeddings.get(0);
              FaceLookupOptions options = FaceLookupOptions.builder()
                  .modelName("arcface")
                  .metric(com.springvision.jpa.enums.SimilarityMetric.COSINE)
                  .threshold(0.75)
                  .limit(10)
                  .build();

              List<java.util.Map<String,Object>> matches = visionTemplate.lookupFaces(
                  emb,
                  options.getModelName(),
                  options.getMetric().name(),
                  options.getThreshold(),
                  options.getLimit(),
                  options.getIncludePersonIds(),
                  options.getExcludePersonIds()
              );

              // Convert matches to PersonMatch as needed
              List<PersonMatch> personMatches = matches.stream()
                  .map(m -> toPersonMatch(m))
                  .collect(Collectors.toList());

              return ResponseEntity.ok(personMatches);
          } catch (Exception e) {
              return ResponseEntity.badRequest().build();
          }
      }

      @PostMapping("/register")
      public ResponseEntity<String> registerPerson(@RequestParam String personId, @RequestParam("file") MultipartFile file) {
          try {
              byte[] data = file.getBytes();
              com.springvision.core.ImageData img = com.springvision.core.ImageData.fromBytes(data);

              com.springvision.core.VisionResult vr = visionTemplate.detectFaces(img);
              List<com.springvision.core.Detection> detections = vr.detections();
              if (detections.isEmpty()) return ResponseEntity.badRequest().body("No face detected");

              com.springvision.core.Detection best = detections.stream().max((a,b)->Double.compare(a.confidence(), b.confidence())).get();
              List<float[]> embeddings = visionTemplate.extractEmbeddings(img);
              if (embeddings == null || embeddings.isEmpty()) return ResponseEntity.badRequest().body("No embedding extracted");

              float[] emb = embeddings.get(0);
              String id = visionTemplate.storeFaceEmbedding(personId, emb, "arcface", com.springvision.jpa.util.VectorUtils.calculateImageHash(data), best.confidence(), java.util.Map.of("source","web_upload"));

              return ResponseEntity.ok(id);
          } catch (Exception e) {
              return ResponseEntity.badRequest().body("Failed to register face: " + e.getMessage());
          }
      }
  }
  ```

### 8.2 Create Integration Tests
- [x] **File**: `spring-vision-jpa/src/test/java/com/springvision/jpa/VectorSimilarityIntegrationTest.java`
  ```java
  @SpringBootTest
  @TestPropertySource(properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb",
      "spring.jpa.hibernate.ddl-auto=create-drop"
  })
  class VectorSimilarityIntegrationTest {
      
      @Autowired
      private VectorSimilarityService vectorService;
      
      @Test
      void testStoreFaceEmbedding() {
          StoreFaceEmbeddingRequest request = new StoreFaceEmbeddingRequest(
              "person-123",
              new float[]{0.1f, 0.2f, 0.3f},
              "arcface",
              "hash123",
              0.95,
              Map.of()
          );
          
          String embeddingId = vectorService.storeFaceEmbedding(request);
          
          assertThat(embeddingId).isNotNull();
      }
      
      @Test
      void testFindSimilarFaces() {
          // Store some test embeddings first
          storeTestEmbeddings();
          
          SimilaritySearchRequest searchRequest = new SimilaritySearchRequest(
              new float[]{0.1f, 0.2f, 0.3f},
              "arcface",
              SimilarityMetric.COSINE,
              0.5,
              5,
              Set.of(),
              Set.of()
          );
          
          List<SimilaritySearchResult> results = vectorService.findSimilarFaces(searchRequest);
          
          assertThat(results).isNotEmpty();
          assertThat(results.get(0).similarity()).isGreaterThan(0.5);
      }
  }
  ```

### 8.3 Create Unit Tests for Vector Utils
- [x] **File**: `spring-vision-jpa/src/test/java/com/springvision/jpa/util/VectorUtilsTest.java`
  ```java
  class VectorUtilsTest {
      
      @Test
      void testSerializeDeserializeFloatArray() {
          float[] original = {0.1f, 0.2f, 0.3f, 0.4f};
          
          byte[] serialized = VectorUtils.serializeFloatArray(original);
          float[] deserialized = VectorUtils.deserializeFloatArray(serialized);
          
          assertThat(deserialized).containsExactly(original);
      }
      
      @Test
      void testCosineSimilarity() {
          float[] vec1 = {1.0f, 0.0f, 0.0f};
          float[] vec2 = {0.0f, 1.0f, 0.0f};
          
          double similarity = VectorUtils.cosineSimilarity(vec1, vec2);
          
          assertThat(similarity).isEqualTo(0.0, within(0.001));
      }
  }
  ```

### 8.4 Add to Spring Boot Starter
- [x] **File**: `spring-vision-starter/pom.xml`
  ```xml
  <!-- Add JPA module as optional dependency -->
  <dependency>
      <groupId>com.springvision</groupId>
      <artifactId>spring-vision-jpa</artifactId>
      <version>${project.version}</version>
      <optional>true</optional>
  </dependency>
  ```

### 8.5 Build and Test Batch 8
- [x] **Command**: `mvn clean test -pl spring-vision-jpa`
- [x] **Test**: All integration and unit tests pass
- [x] **Verify**: Example controller works with different databases

---

## BATCH 9: Documentation and Configuration
**Goal**: Create comprehensive documentation and configuration examples
**Estimated Time**: 3-4 hours

### 9.1 Create Configuration Documentation
- [x] **File**: `spring-vision-jpa/README.md`
  ```markdown
  # Spring Vision JPA Vector Similarity
  
  ## Quick Start
  
  ### 1. Add Dependency
  ```xml
  <dependency>
      <groupId>com.springvision</groupId>
      <artifactId>spring-vision-starter</artifactId>
      <version>1.0.0-SNAPSHOT</version>
  </dependency>
  ```
  
  ### 2. Configure Database
  ```yaml
  spring:
    vision:
      vector:
        provider: postgres  # postgres, oracle, mysql, jpa
    datasource:
      url: jdbc:postgresql://localhost:5432/springvision
      username: postgres
      password: password
  ```
  
  ### 3. Use Face Lookup
  ```java
  @Autowired
  private com.springvision.core.VisionTemplate visionTemplate;
  
  List<FaceMatchResult> matches = visionTemplate.lookupFaces(imageBytes, options);
  ```
  ```

### 9.2 Create Configuration Properties Documentation
- [x] **File**: `spring-vision-jpa/src/main/resources/META-INF/spring-configuration-metadata.json`
  ```json
  {
    "properties": [
      {
        "name": "spring.vision.jpa.enabled",
        "type": "java.lang.Boolean",
        "defaultValue": true,
        "description": "Whether to enable Spring Vision JPA support."
      },
      {
        "name": "spring.vision.vector.provider",
        "type": "com.springvision.jpa.config.VectorSimilarityProperties$VectorProvider",
        "defaultValue": "AUTO",
        "description": "Vector similarity provider to use."
      }
    ]
  }
  ```

### 9.3 Create Application Properties Examples
- [x] **File**: `spring-vision-jpa/src/main/resources/application-postgresql.yml`
  ```yaml
  spring:
    vision:
      vector:
        provider: postgres
        postgresql:
          enabled: true
          index-type: hnsw
          hnsw-m: 16
          hnsw-ef-construction: 64
    datasource:
      url: jdbc:postgresql://localhost:5432/springvision
      username: postgres  
      password: password
      driver-class-name: org.postgresql.Driver
    jpa:
      hibernate:
        ddl-auto: update
      show-sql: false
  ```

- [x] **File**: `spring-vision-jpa/src/main/resources/application-oracle.yml`
  ```yaml
  spring:
    vision:
      vector:
        provider: oracle
        oracle:
          enabled: true
          index-type: hnsw
          target-accuracy: 95
    datasource:
      url: jdbc:oracle:thin:@localhost:1521/FREEPDB1
      username: vector
      password: vector
      driver-class-name: oracle.jdbc.OracleDriver
  ```

### 9.4 Update Main Documentation
- [x] **File**: `README.md` - Add JPA section
  ```markdown
  ## JPA Vector Similarity Support
  
  Spring Vision now supports persistent vector similarity search for face recognition:
  
  ### Supported Databases
  - PostgreSQL with PGVector
  - Oracle Database 23ai
  - MySQL 9+ (with vector support)
  - Any JPA database (fallback mode)
  
  ### Face Lookup Example
  ```java
  // Register a face
  String embeddingId = visionTemplate.registerFace("person-123", imageBytes, options);
  
  // Look up similar faces
  List<FaceMatchResult> matches = visionTemplate.lookupFaces(queryImage, lookupOptions);
  ```
  ```

### 9.5 Build and Test Batch 9
- [x] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [x] **Verify**: Documentation is comprehensive and accurate
- [x] **Test**: Configuration examples work correctly

---

## FINAL BATCH: Integration and Cleanup
**Goal**: Final integration, testing, and cleanup
**Estimated Time**: 2-3 hours

### Final.1 Update Main Build
- [x] **Command**: `mvn clean compile` (root level)
- [x] **Verify**: All modules compile successfully
- [x] **Test**: `mvn clean test` (all tests pass)

### Final.2 Create Migration Guide
- [x] **File**: `docs/JPA_MIGRATION.md`
  ```markdown
  # Migrating to JPA Vector Similarity
  
  ## Existing Applications
  
  Existing Spring Vision applications will continue to work unchanged. To add vector similarity:
  
  1. Add JPA dependency
  2. Configure database  
  3. Use enhanced template methods
  
  ## Breaking Changes
  
  None. All existing APIs remain unchanged.
  ```

### Final.3 Validate All Configurations
- [x] **Test PostgreSQL**: `mvn test -Dspring.profiles.active=postgresql`
- [x] **Test H2 Fallback**: `mvn test -Dspring.profiles.active=h2`
- [x] **Test Auto-detection**: Verify vendor detection works

### Final.4 Update TODO Status
- [x] **Mark completed batches** in main TODO
- [x] **Update project status** in README
- [x] **Create release notes** if needed

---

## Configuration Reference

### Database-Specific Setup

#### PostgreSQL with PGVector
```sql
-- Enable extension
CREATE EXTENSION vector;

-- Create table (handled by JPA)
-- Index will be created automatically
```

#### Oracle 23ai Setup
```sql
-- Vector indexes created automatically
-- Ensure Oracle 23ai with AI Vector Search
```

### Application Configuration

```yaml
# Complete configuration example
spring:
  vision:
    jpa:
      enabled: true
      enhanced-template: true
    vector:
      provider: auto  # auto, postgres, oracle, mysql, jpa
      postgresql:
        enabled: true
        index-type: hnsw
        hnsw-m: 16
        hnsw-ef-construction: 64
      oracle:
        enabled: true
        index-type: hnsw
        target-accuracy: 95
        
  datasource:
    url: jdbc:postgresql://localhost:5432/springvision
    username: postgres
    password: password
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Usage Examples

```java
// Basic usage
@Autowired
private com.springvision.core.VisionTemplate visionTemplate;

// Register a face
FaceRegistrationOptions regOptions = FaceRegistrationOptions.builder()
    .modelName("arcface")
    .metadata(Map.of("source", "enrollment"))
    .build();
String embeddingId = visionTemplate.registerFace("person-123", imageBytes, regOptions);

// Look up faces
FaceLookupOptions lookupOptions = FaceLookupOptions.builder()
    .modelName("arcface")
    .metric(SimilarityMetric.COSINE)
    .threshold(0.7)
    .limit(10)
    .build();
List<FaceMatchResult> matches = visionTemplate.lookupFaces(queryImageBytes, lookupOptions);
```

---

## Build and Test Commands

### Individual Batch Testing
```bash
# Compile specific module
mvn clean compile -pl spring-vision-jpa

# Test specific module  
mvn clean test -pl spring-vision-jpa

# Integration test with PostgreSQL
mvn clean test -pl spring-vision-jpa -Dspring.profiles.active=postgresql

# Full build
mvn clean install -DskipTests

# Full test suite
mvn clean test
```

### Development Workflow

1. **Start with Batch 1**: Basic entities and repositories
2. **Test each batch**: Ensure it builds and basic functionality works
3. **Add database support**: PostgreSQL first, then Oracle
4. **Integrate with Vision Template**: Add face lookup capabilities
5. **Create examples**: REST controller and documentation
6. **Final testing**: Complete integration tests

Each batch should take 4-8 hours and result in a buildable, testable increment.