package com.springvision.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entity storing face embeddings.
 */
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

    // Optional native vector columns for databases with vector support
    @jakarta.persistence.Column(name = "pgvector_embedding", columnDefinition = "vector")
    private float[] pgVectorEmbedding;

    @jakarta.persistence.Column(name = "oracle_embedding")
    private byte[] oracleEmbedding;

    @jakarta.persistence.Column(name = "mysql_embedding")
    private byte[] mysqlEmbedding;

    @Column
    private String imageHash;

    @Column
    private Double confidence;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public byte[] getEmbeddingBlob() {
        return embeddingBlob;
    }

    public void setEmbeddingBlob(byte[] embeddingBlob) {
        this.embeddingBlob = embeddingBlob;
    }

    public float[] getPgVectorEmbedding() {
        return pgVectorEmbedding;
    }

    public void setPgVectorEmbedding(float[] pgVectorEmbedding) {
        this.pgVectorEmbedding = pgVectorEmbedding;
    }

    public byte[] getOracleEmbedding() {
        return oracleEmbedding;
    }

    public void setOracleEmbedding(byte[] oracleEmbedding) {
        this.oracleEmbedding = oracleEmbedding;
    }

    public byte[] getMysqlEmbedding() {
        return mysqlEmbedding;
    }

    public void setMysqlEmbedding(byte[] mysqlEmbedding) {
        this.mysqlEmbedding = mysqlEmbedding;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
} 