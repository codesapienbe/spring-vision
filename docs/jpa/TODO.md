# Spring Vision JPA Vector Similarity Service - Implementation TODO

## Overview
This TODO implements JPA support for Spring Vision with focus on vector similarity search for face embeddings. Each batch is designed to be small, buildable, and testable independently.

---

## BATCH 1: Core Infrastructure and Base Entities
**Goal**: Set up foundational JPA entities and basic repository structure
**Estimated Time**: 4-6 hours

### 1.1 Create Base Audit Entity
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/AuditableEntity.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
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
      
      // Start with BLOB storage only (will add vector types in later batches)
      @Lob
      @Column(name = "embedding_blob", nullable = false)
      private byte[] embeddingBlob;
      
      @Column
      private String imageHash;
      
      @Column
      private Double confidence;
      
      // constructors, getters, setters
  }
  ```

### 1.3 Create Basic Repository Interface
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/FaceEmbeddingRepository.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaConfiguration.java`
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
- [ ] **File**: `pom.xml` - Add new module
  ```xml
  <modules>
      <!-- existing modules -->
      <module>spring-vision-jpa</module>
  </modules>
  ```

### 1.6 Create JPA Module POM
- [ ] **File**: `spring-vision-jpa/pom.xml`
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Verify**: No compilation errors
- [ ] **Test**: Create simple integration test for entity creation

---

## BATCH 2: Vector Utility Classes and DTOs
**Goal**: Create vector handling utilities and data transfer objects
**Estimated Time**: 3-4 hours

### 2.1 Create Vector Utility Class
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/util/VectorUtils.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/StoreFaceEmbeddingRequest.java`
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

- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/SimilaritySearchRequest.java`
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

- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/SimilaritySearchResult.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/enums/SimilarityMetric.java`
  ```java
  public enum SimilarityMetric {
      COSINE, EUCLIDEAN, DOT_PRODUCT, MANHATTAN
  }
  ```

- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/enums/DatabaseVendor.java`
  ```java
  public enum DatabaseVendor {
      POSTGRESQL, ORACLE, MYSQL, H2, HSQLDB, UNKNOWN
  }
  ```

### 2.4 Build and Test Batch 2
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: Unit tests for VectorUtils methods
- [ ] **Verify**: All DTOs serialize/deserialize correctly

---

## BATCH 3: Abstract Vector Similarity Service
**Goal**: Create the core service interface and basic implementation
**Estimated Time**: 4-5 hours

### 3.1 Create Service Interface
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/VectorSimilarityService.java`
  ```java
  public interface VectorSimilarityService {
      String storeFaceEmbedding(StoreFaceEmbeddingRequest request);
      List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request);
      VerificationResult verifyFaces(VerificationRequest request);
      void deleteFaceEmbedding(String embeddingId);
      VectorServiceHealth getHealth();
      Set<SimilarityMetric> getSupportedMetrics();
  }
  ```

### 3.2 Create Database Vendor Detector
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/DatabaseVendorDetector.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/JpaVectorSimilarityService.java`
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: Integration test storing and retrieving embeddings
- [ ] **Verify**: Basic similarity search works

---

## BATCH 4: PostgreSQL PGVector Support
**Goal**: Add native PostgreSQL vector support with custom UserType
**Estimated Time**: 6-8 hours

### 4.1 Add PostgreSQL Dependencies
- [ ] **File**: `spring-vision-jpa/pom.xml` - Add dependencies
  ```xml
  <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <optional>true</optional>
  </dependency>
  ```

### 4.2 Create PGVector UserType
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/hibernate/PgVectorType.java`
  ```java
  public class PgVectorType implements UserType<float[]> {
      
      @Override
      public int getSqlType() {
          return Types.OTHER;
      }
      
      @Override
      public Class<float[]> returnedClass() {
          return float[].class;
      }
      
      @Override
      public float[] nullSafeGet(ResultSet rs, String[] names, 
                                SharedSessionContractImplementor session, Object owner) 
                                throws SQLException {
          String vectorString = rs.getString(names[0]);
          if (vectorString == null) return null;
          return parsePgVector(vectorString);
      }
      
      @Override
      public void nullSafeSet(PreparedStatement st, float[] value, int index, 
                             SharedSessionContractImplementor session) 
                             throws SQLException {
          if (value == null) {
              st.setNull(index, Types.OTHER);
          } else {
              // Create PGobject for vector type
              Object pgObject = createPgVectorObject(value);
              st.setObject(index, pgObject);
          }
      }
      
      // Helper methods for PGVector format conversion
  }
  ```

### 4.3 Update FaceEmbedding Entity for PostgreSQL
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
  ```java
  // Add to existing entity:
  
  @Type(PgVectorType.class)
  @Column(name = "pgvector_embedding", columnDefinition = "vector")
  private float[] pgVectorEmbedding;
  ```

### 4.4 Create PostgreSQL-specific Repository
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/PostgreSQLFaceEmbeddingRepository.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/PostgreSQLVectorSimilarityService.java`
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: PostgreSQL integration test with vector operations
- [ ] **Verify**: PGVector similarity search works correctly

---

## BATCH 5: Oracle 23ai Vector Support
**Goal**: Add Oracle Database 23ai native vector support
**Estimated Time**: 5-7 hours

### 5.1 Add Oracle Dependencies
- [ ] **File**: `spring-vision-jpa/pom.xml`
  ```xml
  <dependency>
      <groupId>com.oracle.database.jdbc</groupId>
      <artifactId>ojdbc11</artifactId>
      <optional>true</optional>
  </dependency>
  <!-- Add Hibernate Vector module when available -->
  ```

### 5.2 Update Entity for Oracle Support
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/entity/FaceEmbedding.java`
  ```java
  // Add to existing entity:
  
  @JdbcTypeCode(SqlTypes.VARBINARY) // Will be SqlTypes.VECTOR when available
  @Array(length = 512)
  @Column(name = "oracle_embedding")
  private float[] oracleEmbedding;
  ```

### 5.3 Create Oracle-specific Repository
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/repository/OracleFaceEmbeddingRepository.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/service/OracleVectorSimilarityService.java`
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: Oracle integration test (if Oracle 23ai available)
- [ ] **Verify**: Service selection works correctly

---

## BATCH 6: Auto-Configuration and Schema Management  
**Goal**: Implement automatic database detection and schema creation
**Estimated Time**: 4-5 hours

### 6.1 Enhance Auto-Configuration
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaAutoConfiguration.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VectorSimilarityProperties.java`
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
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VectorSchemaManager.java`
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
              executeSQL("""
                  CREATE INDEX IF NOT EXISTS idx_face_embeddings_pgvector 
                  ON face_embeddings USING hnsw (pgvector_embedding vector_cosine_ops)
                  WITH (m = %d, ef_construction = %d)
                  """.formatted(
                      properties.getPostgresql().getHnswM(),
                      properties.getPostgresql().getHnswEfConstruction()
                  ));
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: Auto-configuration with different database types
- [ ] **Verify**: Schema creation works for available databases

---

## BATCH 7: Vision Template Integration
**Goal**: Integrate vector similarity with existing Vision Template
**Estimated Time**: 5-6 hours

### 7.1 Create Enhanced Vision Template
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/template/VectorEnabledVisionTemplate.java`
  ```java
  @Service
  public class VectorEnabledVisionTemplate implements VisionTemplate {
      
      private final VisionTemplate delegate;
      private final VectorSimilarityService vectorService;
      
      public VectorEnabledVisionTemplate(VisionTemplate delegate, VectorSimilarityService vectorService) {
          this.delegate = delegate;
          this.vectorService = vectorService;
      }
      
      @Override
      public List<Detection> detectFaces(byte[] imageData) {
          return delegate.detectFaces(imageData);
      }
      
      // Implement all VisionTemplate methods by delegating
      
      /**
       * NEW: Face lookup using vector similarity search
       */
      public List<FaceMatchResult> lookupFaces(byte[] imageData, FaceLookupOptions options) {
          List<Detection> faces = detectFaces(imageData);
          
          List<FaceMatchResult> results = new ArrayList<>();
          
          for (Detection face : faces) {
              if (face.getEmbedding() != null) {
                  SimilaritySearchRequest request = new SimilaritySearchRequest(
                      face.getEmbedding(),
                      options.getModelName(),
                      options.getMetric(),
                      options.getThreshold(),
                      options.getLimit(),
                      options.getIncludePersonIds(),
                      options.getExcludePersonIds()
                  );
                  
                  List<SimilaritySearchResult> matches = vectorService.findSimilarFaces(request);
                  results.add(new FaceMatchResult(face, matches, options.getMetric()));
              }
          }
          
          return results;
      }
      
      /**
       * NEW: Register a person's face for future lookups
       */
      public String registerFace(String personId, byte[] imageData, FaceRegistrationOptions options) {
          List<Detection> faces = detectFaces(imageData);
          
          Detection bestFace = faces.stream()
              .max(Comparator.comparing(Detection::getConfidence))
              .orElseThrow(() -> new VisionProcessingException("No face detected"));
          
          if (bestFace.getEmbedding() == null) {
              throw new VisionProcessingException("No embedding available for detected face");
          }
          
          StoreFaceEmbeddingRequest request = new StoreFaceEmbeddingRequest(
              personId,
              bestFace.getEmbedding(),
              options.getModelName(),
              VectorUtils.calculateImageHash(imageData),
              bestFace.getConfidence(),
              options.getMetadata()
          );
          
          return vectorService.storeFaceEmbedding(request);
      }
  }
  ```

### 7.2 Create Face Lookup DTOs
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceLookupOptions.java`
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

- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceRegistrationOptions.java`
  ```java
  @Builder
  @Data
  public class FaceRegistrationOptions {
      private String modelName = "arcface";
      private Map<String, Object> metadata = Map.of();
  }
  ```

- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/dto/FaceMatchResult.java`
  ```java
  public record FaceMatchResult(
      Detection detectedFace,
      List<SimilaritySearchResult> matches,
      SimilarityMetric metric
  ) {}
  ```

### 7.3 Update Auto-Configuration for Template
- [ ] **File**: `spring-vision-jpa/src/main/java/com/springvision/jpa/config/VisionJpaAutoConfiguration.java`
  ```java
  // Add to existing configuration:
  
  @Bean
  @Primary
  @ConditionalOnProperty(value = "spring.vision.jpa.enhanced-template", havingValue = "true", matchIfMissing = true)
  public VisionTemplate vectorEnabledVisionTemplate(
          @Qualifier("originalVisionTemplate") VisionTemplate originalTemplate,
          VectorSimilarityService vectorService) {
      return new VectorEnabledVisionTemplate(originalTemplate, vectorService);
  }
  
  @Bean("originalVisionTemplate")
  @ConditionalOnMissingBean(name = "originalVisionTemplate")
  public VisionTemplate originalVisionTemplate() {
      // Return the existing VisionTemplate implementation
      return new DefaultVisionTemplate(); // or inject existing one
  }
  ```

### 7.4 Build and Test Batch 7
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Test**: Integration test with face lookup functionality
- [ ] **Verify**: Enhanced template works with vector similarity

---

## BATCH 8: Example Integration and Testing
**Goal**: Create examples and comprehensive testing
**Estimated Time**: 4-5 hours

### 8.1 Create REST Controller Example
- [ ] **File**: `spring-vision-examples/jpa-vector-example/src/main/java/com/springvision/example/controller/VectorFaceLookupController.java`
  ```java
  @RestController
  @RequestMapping("/api/faces")
  public class VectorFaceLookupController {
      
      @Autowired
      private VectorEnabledVisionTemplate visionTemplate;
      
      @PostMapping("/lookup")
      public ResponseEntity<List<PersonMatch>> lookupFaces(@RequestParam("file") MultipartFile file) {
          try {
              FaceLookupOptions options = FaceLookupOptions.builder()
                  .modelName("arcface")
                  .metric(SimilarityMetric.COSINE)
                  .threshold(0.75)
                  .limit(10)
                  .build();
              
              List<FaceMatchResult> results = visionTemplate.lookupFaces(file.getBytes(), options);
              
              List<PersonMatch> matches = results.stream()
                  .flatMap(result -> result.getMatches().stream())
                  .map(this::toPersonMatch)
                  .collect(Collectors.toList());
              
              return ResponseEntity.ok(matches);
          } catch (Exception e) {
              return ResponseEntity.badRequest().build();
          }
      }
      
      @PostMapping("/register")
      public ResponseEntity<String> registerPerson(
              @RequestParam String personId,
              @RequestParam("file") MultipartFile file) {
          try {
              FaceRegistrationOptions options = FaceRegistrationOptions.builder()
                  .modelName("arcface")
                  .metadata(Map.of("source", "web_upload", "timestamp", System.currentTimeMillis()))
                  .build();
              
              String embeddingId = visionTemplate.registerFace(personId, file.getBytes(), options);
              
              return ResponseEntity.ok(embeddingId);
          } catch (Exception e) {
              return ResponseEntity.badRequest().body("Failed to register face: " + e.getMessage());
          }
      }
  }
  ```

### 8.2 Create Integration Tests
- [ ] **File**: `spring-vision-jpa/src/test/java/com/springvision/jpa/VectorSimilarityIntegrationTest.java`
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
- [ ] **File**: `spring-vision-jpa/src/test/java/com/springvision/jpa/util/VectorUtilsTest.java`
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
- [ ] **File**: `spring-vision-starter/pom.xml`
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
- [ ] **Command**: `mvn clean test -pl spring-vision-jpa`
- [ ] **Test**: All integration and unit tests pass
- [ ] **Verify**: Example controller works with different databases

---

## BATCH 9: Documentation and Configuration
**Goal**: Create comprehensive documentation and configuration examples
**Estimated Time**: 3-4 hours

### 9.1 Create Configuration Documentation
- [ ] **File**: `spring-vision-jpa/README.md`
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
        provider: pgvector  # pgvector, oracle, mysql, jpa
    datasource:
      url: jdbc:postgresql://localhost:5432/springvision
      username: postgres
      password: password
  ```
  
  ### 3. Use Face Lookup
  ```java
  @Autowired
  private VectorEnabledVisionTemplate visionTemplate;
  
  List<FaceMatchResult> matches = visionTemplate.lookupFaces(imageBytes, options);
  ```
  ```

### 9.2 Create Configuration Properties Documentation
- [ ] **File**: `spring-vision-jpa/src/main/resources/META-INF/spring-configuration-metadata.json`
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
- [ ] **File**: `spring-vision-jpa/src/main/resources/application-postgresql.yml`
  ```yaml
  spring:
    vision:
      vector:
        provider: pgvector
        postgresql:
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

- [ ] **File**: `spring-vision-jpa/src/main/resources/application-oracle.yml`
  ```yaml
  spring:
    vision:
      vector:
        provider: oracle
        oracle:
          index-type: hnsw
          target-accuracy: 95
    datasource:
      url: jdbc:oracle:thin:@localhost:1521/FREEPDB1
      username: vector
      password: vector
      driver-class-name: oracle.jdbc.OracleDriver
  ```

### 9.4 Update Main Documentation
- [ ] **File**: `README.md` - Add JPA section
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
- [ ] **Command**: `mvn clean compile -pl spring-vision-jpa`
- [ ] **Verify**: Documentation is comprehensive and accurate
- [ ] **Test**: Configuration examples work correctly

---

## FINAL BATCH: Integration and Cleanup
**Goal**: Final integration, testing, and cleanup
**Estimated Time**: 2-3 hours

### Final.1 Update Main Build
- [ ] **Command**: `mvn clean compile` (root level)
- [ ] **Verify**: All modules compile successfully
- [ ] **Test**: `mvn clean test` (all tests pass)

### Final.2 Create Migration Guide
- [ ] **File**: `docs/JPA_MIGRATION.md`
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
- [ ] **Test PostgreSQL**: `mvn test -Dspring.profiles.active=postgresql`
- [ ] **Test H2 Fallback**: `mvn test -Dspring.profiles.active=h2`
- [ ] **Test Auto-detection**: Verify vendor detection works

### Final.4 Update TODO Status
- [ ] **Mark completed batches** in main TODO
- [ ] **Update project status** in README
- [ ] **Create release notes** if needed

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
      provider: auto  # auto, pgvector, oracle, mysql, jpa
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
private VectorEnabledVisionTemplate visionTemplate;

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