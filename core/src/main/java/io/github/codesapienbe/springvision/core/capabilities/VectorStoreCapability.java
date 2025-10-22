package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;
import java.util.Map;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

/**
 * Capability interface for backends that expose direct access to a gallery/vector store.
 */
public interface VectorStoreCapability {

    List<Map<String,Object>> findEntriesByImageHash(String imageHash) throws BaseVisionException;

    List<Map<String,Object>> findEntriesByPersonId(String personId) throws BaseVisionException;

    void deleteEmbeddingById(String embeddingId) throws BaseVisionException;

    String storeGalleryEmbeddingWithHash(String personId, float[] embedding, String modelName, String imageHash) throws BaseVisionException;
}
