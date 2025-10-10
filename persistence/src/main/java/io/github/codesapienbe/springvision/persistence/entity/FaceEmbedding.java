package io.github.codesapienbe.springvision.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * JPA entity for storing face embeddings in a relational database.
 * This entity includes the embedding itself, metadata about the person and model, and auditing fields.
 */
@Entity
@Table(name = "face_embeddings")
public class FaceEmbedding extends AuditableEntity {

    /**
     * The unique identifier for the embedding record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * An identifier for the person associated with this face embedding.
     */
    @Column(nullable = false)
    private String personId;

    /**
     * The name of the model used to generate the embedding (e.g., "arcface").
     */
    @Column(nullable = false)
    private String modelName;

    /**
     * The dimension of the embedding vector (e.g., 512).
     */
    @Column(nullable = false)
    private Integer dimension;

    /**
     * The raw embedding data, stored as a portable byte array (blob).
     */
    @Lob
    @Column(name = "embedding_blob", nullable = false)
    private byte[] embeddingBlob;

    /**
     * A database-native representation of the vector, for use with specialized vector extensions (e.g., pgvector).
     */
    @jakarta.persistence.Column(name = "native_vector")
    private byte[] nativeVector;

    /**
     * The SHA-256 hash of the source image from which the embedding was generated.
     */
    @Column
    private String imageHash;

    /**
     * The confidence score of the face detection.
     */
    @Column
    private Double confidence;

    /**
     * Default constructor for JPA.
     */
    public FaceEmbedding() {
    }

    /**
     * Gets the unique identifier.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     *
     * @param id the ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the person identifier.
     *
     * @return the person ID
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets the person identifier.
     *
     * @param personId the person ID
     */
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    /**
     * Gets the model name used for the embedding.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the model name used for the embedding.
     *
     * @param modelName the model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Gets the embedding dimension.
     *
     * @return the dimension
     */
    public Integer getDimension() {
        return dimension;
    }

    /**
     * Sets the embedding dimension.
     *
     * @param dimension the dimension
     */
    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    /**
     * Gets the embedding data as a byte array.
     *
     * @return the embedding blob
     */
    public byte[] getEmbeddingBlob() {
        return embeddingBlob;
    }

    /**
     * Sets the embedding data as a byte array.
     *
     * @param embeddingBlob the embedding blob
     */
    public void setEmbeddingBlob(byte[] embeddingBlob) {
        this.embeddingBlob = embeddingBlob;
    }

    /**
     * Gets the native database-specific vector representation.
     *
     * @return the native vector
     */
    public byte[] getNativeVector() {
        return nativeVector;
    }

    /**
     * Sets the native database-specific vector representation.
     *
     * @param nativeVector the native vector
     */
    public void setNativeVector(byte[] nativeVector) {
        this.nativeVector = nativeVector;
    }

    /**
     * Gets the source image hash.
     *
     * @return the image hash
     */
    public String getImageHash() {
        return imageHash;
    }

    /**
     * Sets the source image hash.
     *
     * @param imageHash the image hash
     */
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    /**
     * Gets the detection confidence score.
     *
     * @return the confidence score
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * Sets the detection confidence score.
     *
     * @param confidence the confidence score
     */
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
