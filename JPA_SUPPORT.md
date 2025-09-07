# JPA Vector Similarity Service for Spring Vision

## Overview
This document outlines the implementation of JPA support focused specifically on vector similarity search for face recognition and computer vision applications. The design provides an abstract vector similarity service that supports multiple backends (FAISS, PGVector) while maintaining compatibility across different JPA-supported database vendors.

## Core Architecture

### 1. Vector Storage Entity Design

#### 1.1 Base Vector Entity
```java
@Entity
@Table(name = "face_embeddings")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "embedding_type", discriminatorType = DiscriminatorType.STRING)
public class FaceEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String personId;  // External identifier for the person
    
    @Column(nullable = false)
    private String modelName; // e.g., "arcface", "facenet", "openface"
    
    @Column(nullable = false)
    private Integer dimension; // Vector dimensionality (e.g., 512, 128)
    
    @Column(nullable = false, columnDefinition = "BLOB")
    @Lob
    private byte[] embeddingVector; // Serialized float[] array
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private String imageHash; // Optional: hash of source image
    
    @Column
    private Double confidence; // Embedding extraction confidence
    
    // Database-specific vector column (nullable for multi-DB support)
    @Column(columnDefinition = "vector(512)")
    private String pgVector; // PostgreSQL PGVector format
    
    @Column(columnDefinition = "TEXT")
    private String vectorJson; // JSON fallback for unsupported databases
}
```

#### 1.2 Detection Result with Embeddings
```java
@Entity
@Table(name = "detection_results")
public class DetectionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String imageId;
    
    @Column(nullable = false)
    private String label;
    
    @Column(nullable = false)
    private Double confidence;
    
    // Bounding box coordinates
    @Column(name = "bbox_x")
    private Double bboxX;
    
    @Column(name = "bbox_y") 
    private Double bboxY;
    
    @Column(name = "bbox_width")
    private Double bboxWidth;
    
    @Column(name = "bbox_height")
    private Double bboxHeight;
    
    @OneToMany(mappedBy = "detection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FaceEmbedding> embeddings = new ArrayList<>();
}
```

### 2. Abstract Vector Similarity Service

#### 2.1 Core Abstraction
```java
public interface VectorSimilarityService {
    
    /**
     * Store a face embedding for future similarity searches
     */
    String storeFaceEmbedding(StoreFaceEmbeddingRequest request);
    
    /**
     * Find similar faces based on embedding vector
     */
    List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request);
    
    /**
     * Verify if two embeddings belong to the same person
     */
    VerificationResult verifyFaces(VerificationRequest request);
    
    /**
     * Delete face embedding by ID
     */
    void deleteFaceEmbedding(String embeddingId);
    
    /**
     * Get service health and statistics
     */
    VectorServiceHealth getHealth();
    
    /**
     * Supported similarity metrics by this implementation
     */
    Set<SimilarityMetric> getSupportedMetrics();
}
```

#### 2.2 Request/Response DTOs
```java
public record StoreFaceEmbeddingRequest(
    String personId,
    float[] embedding,
    String modelName,
    String imageHash,
    Double confidence,
    Map<String, Object> metadata
) {}

public record SimilaritySearchRequest(
    float[] queryEmbedding,
    String modelName,
    SimilarityMetric metric,
    Double threshold,
    Integer limit,
    Set<String> includePersonIds,
    Set<String> excludePersonIds
) {}

public record SimilaritySearchResult(
    String embeddingId,
    String personId,
    Double similarity,
    Double distance,
    String modelName,
    LocalDateTime createdAt,
    Map<String, Object> metadata
) {}

public record VerificationRequest(
    float[] embedding1,
    float[] embedding2,
    String modelName,
    SimilarityMetric metric,
    Double threshold
) {}

public record VerificationResult(
    boolean isMatch,
    Double similarity,
    Double distance,
    SimilarityMetric metric,
    String decision
) {}

public enum SimilarityMetric {
    COSINE, EUCLIDEAN, DOT_PRODUCT, MANHATTAN, CHEBYSHEV
}
```

### 3. Implementation Strategies

#### 3.1 PGVector Implementation
```java
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "pgvector")
@ConditionalOnClass(name = "org.postgresql.util.PGobject")
public class PgVectorSimilarityService implements VectorSimilarityService {
    
    private final JdbcTemplate jdbcTemplate;
    private final FaceEmbeddingRepository embeddingRepository;
    
    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        String sql = buildPgVectorQuery(request);
        
        return jdbcTemplate.query(sql, 
            rs -> mapToSimilarityResult(rs),
            prepareParameters(request));
    }
    
    private String buildPgVectorQuery(SimilaritySearchRequest request) {
        String distanceFunction = switch (request.metric()) {
            case COSINE -> "embedding_vector <=> ?::vector";
            case EUCLIDEAN -> "embedding_vector <-> ?::vector"; 
            case DOT_PRODUCT -> "embedding_vector <#> ?::vector";
            default -> throw new UnsupportedOperationException("Metric not supported: " + request.metric());
        };
        
        return """
            SELECT id, person_id, model_name, created_at, confidence,
                   %s as distance
            FROM face_embeddings 
            WHERE model_name = ?
            AND (%s) < ?
            ORDER BY distance ASC
            LIMIT ?
            """.formatted(distanceFunction, distanceFunction);
    }
    
    @Override
    public String storeFaceEmbedding(StoreFaceEmbeddingRequest request) {
        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(request.embedding().length);
        embedding.setEmbeddingVector(serializeFloatArray(request.embedding()));
        embedding.setPgVector(formatPgVector(request.embedding()));
        embedding.setCreatedAt(LocalDateTime.now());
        
        FaceEmbedding saved = embeddingRepository.save(embedding);
        return saved.getId().toString();
    }
    
    private String formatPgVector(float[] embedding) {
        return "[" + Arrays.stream(embedding)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(",")) + "]";
    }
}
```

#### 3.2 FAISS Implementation
```java
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "faiss")
@ConditionalOnClass(name = "com.github.jelmerk.knn.Index")
public class FaissVectorSimilarityService implements VectorSimilarityService {
    
    private final FaceEmbeddingRepository embeddingRepository;
    private final Map<String, Index<String, float[], FaceEmbeddingItem, Float>> faissIndexes = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeIndexes() {
        // Load existing embeddings and build FAISS indexes per model
        Map<String, List<FaceEmbedding>> embeddingsByModel = embeddingRepository
            .findAll()
            .stream()
            .collect(Collectors.groupingBy(FaceEmbedding::getModelName));
            
        embeddingsByModel.forEach(this::buildFaissIndex);
    }
    
    private void buildFaissIndex(String modelName, List<FaceEmbedding> embeddings) {
        if (embeddings.isEmpty()) return;
        
        int dimension = embeddings.get(0).getDimension();
        
        Index<String, float[], FaceEmbeddingItem, Float> index = HnswIndex
            .newBuilder(dimension, DistanceFunctions.COSINE, embeddings.size())
            .withM(16)
            .withEf(200)
            .withEfConstruction(200)
            .build();
            
        List<FaceEmbeddingItem> items = embeddings.stream()
            .map(this::toFaceEmbeddingItem)
            .collect(Collectors.toList());
            
        index.addAll(items);
        faissIndexes.put(modelName, index);
    }
    
    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        Index<String, float[], FaceEmbeddingItem, Float> index = faissIndexes.get(request.modelName());
        if (index == null) {
            return Collections.emptyList();
        }
        
        SearchResult<FaceEmbeddingItem, Float> searchResult = index.search(
            request.queryEmbedding(), 
            request.limit()
        );
        
        return searchResult.stream()
            .map(neighbor -> new SimilaritySearchResult(
                neighbor.item().getId(),
                neighbor.item().getPersonId(),
                1.0 - neighbor.distance(), // Convert distance to similarity
                neighbor.distance().doubleValue(),
                request.modelName(),
                neighbor.item().getCreatedAt(),
                neighbor.item().getMetadata()
            ))
            .filter(result -> result.similarity() >= request.threshold())
            .collect(Collectors.toList());
    }
    
    @Override
    public String storeFaceEmbedding(StoreFaceEmbeddingRequest request) {
        // Store in database
        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(request.embedding().length);
        embedding.setEmbeddingVector(serializeFloatArray(request.embedding()));
        embedding.setVectorJson(Arrays.toString(request.embedding()));
        embedding.setCreatedAt(LocalDateTime.now());
        
        FaceEmbedding saved = embeddingRepository.save(embedding);
        
        // Update FAISS index
        updateFaissIndex(saved, request.embedding());
        
        return saved.getId().toString();
    }
    
    private void updateFaissIndex(FaceEmbedding saved, float[] embedding) {
        Index<String, float[], FaceEmbeddingItem, Float> index = faissIndexes
            .computeIfAbsent(saved.getModelName(), this::createNewIndex);
            
        FaceEmbeddingItem item = new FaceEmbeddingItem(
            saved.getId().toString(),
            saved.getPersonId(),
            embedding,
            saved.getCreatedAt(),
            Map.of()
        );
        
        index.add(item);
    }
}
```

#### 3.3 Fallback Implementation (Pure JPA)
```java
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "jpa", matchIfMissing = true)
public class JpaVectorSimilarityService implements VectorSimilarityService {
    
    private final FaceEmbeddingRepository embeddingRepository;
    
    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        List<FaceEmbedding> allEmbeddings = embeddingRepository
            .findByModelName(request.modelName());
            
        return allEmbeddings.parallelStream()
            .map(embedding -> {
                float[] storedVector = deserializeFloatArray(embedding.getEmbeddingVector());
                double similarity = calculateSimilarity(
                    request.queryEmbedding(), 
                    storedVector, 
                    request.metric()
                );
                
                return new SimilaritySearchResult(
                    embedding.getId().toString(),
                    embedding.getPersonId(),
                    similarity,
                    1.0 - similarity, // Convert similarity to distance
                    embedding.getModelName(),
                    embedding.getCreatedAt(),
                    Map.of()
                );
            })
            .filter(result -> result.similarity() >= request.threshold())
            .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
            .limit(request.limit())
            .collect(Collectors.toList());
    }
    
    private double calculateSimilarity(float[] vec1, float[] vec2, SimilarityMetric metric) {
        return switch (metric) {
            case COSINE -> cosineSimilarity(vec1, vec2);
            case EUCLIDEAN -> 1.0 / (1.0 + euclideanDistance(vec1, vec2));
            case DOT_PRODUCT -> dotProduct(vec1, vec2);
            default -> throw new UnsupportedOperationException("Metric not supported: " + metric);
        };
    }
}
```

### 4. Configuration and Auto-Configuration

#### 4.1 Vector Service Configuration
```java
@Configuration
@EnableConfigurationProperties(VectorSimilarityProperties.class)
public class VectorSimilarityAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public VectorSimilarityService vectorSimilarityService(
            VectorSimilarityProperties properties,
            FaceEmbeddingRepository repository,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        
        return switch (properties.getProvider()) {
            case PGVECTOR -> new PgVectorSimilarityService(jdbcTemplate, repository);
            case FAISS -> new FaissVectorSimilarityService(repository);
            case JPA -> new JpaVectorSimilarityService(repository);
        };
    }
    
    @Bean
    public VectorEnabledVisionTemplate vectorVisionTemplate(
            VisionTemplate delegateTemplate,
            VectorSimilarityService vectorService) {
        return new VectorEnabledVisionTemplate(delegateTemplate, vectorService);
    }
}

@ConfigurationProperties(prefix = "spring.vision.vector")
@Data
public class VectorSimilarityProperties {
    private VectorProvider provider = VectorProvider.JPA;
    private Faiss faiss = new Faiss();
    private Pgvector pgvector = new Pgvector();
    
    @Data
    public static class Faiss {
        private boolean enabled = true;
        private int hnswM = 16;
        private int hnswEfConstruction = 200;
        private int hnswEf = 200;
    }
    
    @Data
    public static class Pgvector {
        private boolean enabled = true;
        private String indexType = "hnsw";
        private int dimensions = 512;
    }
    
    public enum VectorProvider {
        PGVECTOR, FAISS, JPA
    }
}
```

#### 4.2 Enhanced Vision Template
```java
@Service
public class VectorEnabledVisionTemplate implements VisionTemplate {
    
    private final VisionTemplate delegate;
    private final VectorSimilarityService vectorService;
    
    @Override
    public List<Detection> detectFaces(byte[] imageData, VisionContext context) {
        List<Detection> detections = delegate.detectFaces(imageData, context);
        
        if (context.isStoreEmbeddings()) {
            storeDetectionEmbeddings(detections, imageData, context);
        }
        
        return detections;
    }
    
    /**
     * New face lookup functionality
     */
    public List<FaceMatchResult> lookupFaces(byte[] imageData, FaceLookupOptions options) {
        // Extract embeddings from image
        List<Detection> faces = delegate.detectFaces(imageData);
        
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
                
                results.add(new FaceMatchResult(
                    face,
                    matches,
                    options.getMetric()
                ));
            }
        }
        
        return results;
    }
    
    /**
     * Face verification functionality
     */
    public FaceVerificationResult verifyFaces(byte[] image1, byte[] image2, FaceVerificationOptions options) {
        List<Detection> faces1 = delegate.detectFaces(image1);
        List<Detection> faces2 = delegate.detectFaces(image2);
        
        if (faces1.isEmpty() || faces2.isEmpty()) {
            return FaceVerificationResult.noFacesDetected();
        }
        
        // Take highest confidence faces
        Detection face1 = faces1.stream()
            .max(Comparator.comparing(Detection::getConfidence))
            .orElseThrow();
            
        Detection face2 = faces2.stream()
            .max(Comparator.comparing(Detection::getConfidence))
            .orElseThrow();
        
        if (face1.getEmbedding() == null || face2.getEmbedding() == null) {
            return FaceVerificationResult.noEmbeddings();
        }
        
        VerificationRequest request = new VerificationRequest(
            face1.getEmbedding(),
            face2.getEmbedding(),
            options.getModelName(),
            options.getMetric(),
            options.getThreshold()
        );
        
        VerificationResult result = vectorService.verifyFaces(request);
        
        return new FaceVerificationResult(
            face1,
            face2,
            result,
            options
        );
    }
    
    /**
     * Register a person's face for future lookups
     */
    public String registerFace(String personId, byte[] imageData, FaceRegistrationOptions options) {
        List<Detection> faces = delegate.detectFaces(imageData);
        
        Detection bestFace = faces.stream()
            .max(Comparator.comparing(Detection::getConfidence))
            .orElseThrow(() -> new VisionProcessingException("No face detected in image"));
        
        if (bestFace.getEmbedding() == null) {
            throw new VisionProcessingException("No embedding available for detected face");
        }
        
        StoreFaceEmbeddingRequest request = new StoreFaceEmbeddingRequest(
            personId,
            bestFace.getEmbedding(),
            options.getModelName(),
            calculateImageHash(imageData),
            bestFace.getConfidence(),
            options.getMetadata()
        );
        
        return vectorService.storeFaceEmbedding(request);
    }
}
```

### 5. Database Schema Management

#### 5.1 Multi-Database Support
```java
@Component
public class VectorSchemaManager {
    
    private final DataSource dataSource;
    private final VectorSimilarityProperties properties;
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        DatabaseVendor vendor = detectDatabaseVendor();
        createVectorSchema(vendor);
    }
    
    private void createVectorSchema(DatabaseVendor vendor) {
        switch (vendor) {
            case POSTGRESQL -> createPostgreSQLSchema();
            case MYSQL -> createMySQLSchema();
            case ORACLE -> createOracleSchema();
            case H2 -> createH2Schema();
            default -> createGenericSchema();
        }
    }
    
    private void createPostgreSQLSchema() {
        if (properties.getPgvector().isEnabled()) {
            executeSQL("CREATE EXTENSION IF NOT EXISTS vector");
            executeSQL("""
                ALTER TABLE face_embeddings 
                ADD COLUMN IF NOT EXISTS pgvector_column vector(%d)
                """.formatted(properties.getPgvector().getDimensions()));
            executeSQL("""
                CREATE INDEX IF NOT EXISTS idx_face_embeddings_vector 
                ON face_embeddings USING hnsw (pgvector_column vector_cosine_ops)
                """);
        }
    }
    
    private void createGenericSchema() {
        // Fallback to JSON storage for unsupported databases
        executeSQL("""
            ALTER TABLE face_embeddings 
            ADD COLUMN IF NOT EXISTS vector_json TEXT
            """);
        executeSQL("""
            CREATE INDEX IF NOT EXISTS idx_face_embeddings_model_person 
            ON face_embeddings (model_name, person_id)
            """);
    }
}
```

### 6. Configuration Properties

```yaml
spring:
  vision:
    vector:
      provider: pgvector  # pgvector, faiss, jpa
      faiss:
        enabled: true
        hnsw-m: 16
        hnsw-ef-construction: 200
        hnsw-ef: 200
      pgvector:
        enabled: true
        index-type: hnsw
        dimensions: 512
      jpa:
        batch-size: 1000
        similarity-cache-size: 10000
        
  datasource:
    url: jdbc:postgresql://localhost:5432/springvision
    username: postgres
    password: password
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

## Usage Examples

### Face Lookup
```java
@Autowired
private VectorEnabledVisionTemplate visionTemplate;

// Lookup similar faces
public List<PersonMatch> findSimilarPeople(MultipartFile imageFile) {
    byte[] imageData = imageFile.getBytes();
    
    FaceLookupOptions options = FaceLookupOptions.builder()
        .modelName("arcface")
        .metric(SimilarityMetric.COSINE)
        .threshold(0.7)
        .limit(10)
        .build();
    
    List<FaceMatchResult> results = visionTemplate.lookupFaces(imageData, options);
    
    return results.stream()
        .flatMap(result -> result.getMatches().stream())
        .map(this::toPersonMatch)
        .collect(Collectors.toList());
}

// Register a new face
public String registerNewPerson(String personId, MultipartFile imageFile) {
    byte[] imageData = imageFile.getBytes();
    
    FaceRegistrationOptions options = FaceRegistrationOptions.builder()
        .modelName("arcface")
        .metadata(Map.of("source", "web_upload"))
        .build();
    
    return visionTemplate.registerFace(personId, imageData, options);
}

// Verify two faces
public boolean verifyPersonIdentity(MultipartFile image1, MultipartFile image2) {
    FaceVerificationOptions options = FaceVerificationOptions.builder()
        .modelName("arcface")
        .metric(SimilarityMetric.COSINE)
        .threshold(0.8)
        .build();
    
    FaceVerificationResult result = visionTemplate.verifyFaces(
        image1.getBytes(), 
        image2.getBytes(), 
        options
    );
    
    return result.isMatch();
}
```

## Benefits

1. **Multi-Database Support**: Works with PostgreSQL (PGVector), any JPA database with fallback
2. **Performance Options**: FAISS for speed, PGVector for scale, JPA for simplicity  
3. **Extensible Design**: Easy to add new vector backends
4. **Production Ready**: Built-in caching, connection pooling, error handling
5. **Face Recognition Focus**: Optimized specifically for face embedding similarity
6. **Zero Configuration**: Sensible defaults with optional tuning
7. **Backward Compatible**: Seamlessly integrates with existing Spring Vision

## Migration Path

### Phase 1: Core Infrastructure
- [ ] Implement base vector entity and repository
- [ ] Create abstract similarity service interface
- [ ] Add basic JPA implementation

### Phase 2: Enhanced Backends  
- [ ] Implement PGVector support
- [ ] Add FAISS integration with Java bindings
- [ ] Create auto-configuration

### Phase 3: Vision Template Integration
- [ ] Enhance VisionTemplate with vector capabilities
- [ ] Add face lookup and verification methods
- [ ] Implement face registration workflow

### Phase 4: Production Features
- [ ] Add comprehensive testing
- [ ] Implement performance optimizations
- [ ] Add monitoring and metrics
- [ ] Create migration tools and documentation