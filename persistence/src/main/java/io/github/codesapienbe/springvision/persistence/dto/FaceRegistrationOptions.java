package io.github.codesapienbe.springvision.persistence.dto;

import java.util.Map;

/**
 * Encapsulates the options for registering a new face into the vector database.
 * This includes specifying the model to be used for generating the face embedding and any custom metadata to be stored alongside it.
 */
public class FaceRegistrationOptions {

    /**
     * The name of the facial recognition model to use for generating the embedding.
     * Defaults to "arcface".
     */
    private String modelName = "arcface";

    /**
     * A map of custom metadata to be stored with the face embedding.
     * This can include any relevant information, such as user IDs, timestamps, or source information.
     */
    private Map<String, Object> metadata = java.util.Map.of();

    /**
     * Constructs a new instance with default options.
     */
    public FaceRegistrationOptions() {
    }

    /**
     * Gets the name of the model to be used for embedding.
     *
     * @return the model name.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Sets the name of the model to be used for embedding.
     *
     * @param modelName the name of the model.
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Gets the custom metadata associated with the registration.
     *
     * @return the metadata map.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the custom metadata to be stored with the face embedding.
     *
     * @param metadata a map of key-value pairs.
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
