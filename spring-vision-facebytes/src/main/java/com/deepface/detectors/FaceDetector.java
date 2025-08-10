package com.deepface.detectors;

import com.deepface.core.FaceRegion;
import java.awt.image.BufferedImage;
import java.util.List;

public interface FaceDetector {
    List<FaceRegion> detectFaces(BufferedImage image);
}
