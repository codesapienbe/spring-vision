package io.github.codesapienbe.springvision.facebytes.detectors;

import io.github.codesapienbe.springvision.facebytes.core.FaceRegion;

import java.awt.image.BufferedImage;
import java.util.List;

public interface FaceDetector {
    List<FaceRegion> detectFaces(BufferedImage image);
}
