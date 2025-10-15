package io.github.codesapienbe.springvision.facebytes.detectors;

import io.github.codesapienbe.springvision.facebytes.core.FaceRegion;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Interface for face detection implementations.
 */
public interface FaceDetector {
    /**
     * Detects faces in the given image.
     *
     * @param image the image to analyze
     * @return list of detected face regions
     */
    List<FaceRegion> detectFaces(BufferedImage image);
}
