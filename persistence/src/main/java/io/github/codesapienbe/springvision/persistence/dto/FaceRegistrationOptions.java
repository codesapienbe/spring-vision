package io.github.codesapienbe.springvision.persistence.dto;

import java.util.Map;

/**
 * Options for registering a face into the vector store.
 */
public class FaceRegistrationOptions {

    private String modelName = "arcface";
    private Map<String, Object> metadata = java.util.Map.of();

    /**
     * Default constructor.
     */
    public FaceRegistrationOptions() {
    }

    /**
     * Gets the model name.
     *
     * @return the model name
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the model name.
     *
     * @param modelName the model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Gets the metadata map.
     *
     * @return the metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata map.
     *
     * @param metadata the metadata
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
