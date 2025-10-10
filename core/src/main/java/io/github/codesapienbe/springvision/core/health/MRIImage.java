package io.github.codesapienbe.springvision.core.health;

import java.util.Map;

/**
 * Core DTO representing an MRI image for classification.
 */
public final class MRIImage {
    private final byte[] pixels;
    private final int width;
    private final int height;
    private final String modality;
    private final Map<String, String> metadata;

    public MRIImage(byte[] pixels, int width, int height, String modality, Map<String, String> metadata) {
        this.pixels = pixels == null ? new byte[0] : pixels.clone();
        this.width = width;
        this.height = height;
        this.modality = modality;
        this.metadata = metadata;
    }

    public byte[] getPixels() {
        return pixels.clone();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getModality() {
        return modality;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}

