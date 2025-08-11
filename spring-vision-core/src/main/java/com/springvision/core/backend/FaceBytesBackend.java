package com.springvision.core.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deepface.core.DeepFace;
import com.deepface.core.EmbeddingResult;
import com.deepface.core.FaceRegion;
import com.springvision.core.BackendHealthInfo;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/**
 * VisionBackend implementation backed by the FaceBytes (DeepFace Java) module.
 *
 * <p>This backend provides face detection by delegating to FaceBytes' OpenCV Haar cascade
 * face detector and mapping results into Spring Vision core domain objects.</p>
 */
public final class FaceBytesBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(FaceBytesBackend.class);

    private static final String BACKEND_ID = "facebytes";
    private static final String DISPLAY_NAME = "FaceBytes Backend";
    private static final String VERSION = "1.0.0";

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.FACE);
    }

    @Override
    public boolean isHealthy() {
        try {
            // Lightweight health check: attempt to load FaceBytes classes
            Class.forName("com.deepface.core.DeepFace");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long start = System.currentTimeMillis();
        boolean ok = isHealthy();
        long took = System.currentTimeMillis() - start;
        if (ok) {
            return BackendHealthInfo.healthy(BACKEND_ID, "FaceBytes available", took, Map.of(
                "displayName", DISPLAY_NAME,
                "version", VERSION
            ));
        }
        return BackendHealthInfo.unhealthy(BACKEND_ID, "FaceBytes unavailable",
            "Required classes not found", took);
    }

    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("ImageData must not be null or empty");
        }
        long start = System.currentTimeMillis();
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (img == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image data");
            }
            List<EmbeddingResult> embeddings = DeepFace.represent(img);
            List<Detection> detections = new ArrayList<>(embeddings.size());
            for (EmbeddingResult er : embeddings) {
                FaceRegion r = er.faceRegion();
                // Normalize bounding box to [0,1]
                double nx = clamp01((double) r.x() / img.getWidth());
                double ny = clamp01((double) r.y() / img.getHeight());
                double nw = clamp01((double) r.width() / img.getWidth());
                double nh = clamp01((double) r.height() / img.getHeight());
                var bbox = new com.springvision.core.BoundingBox(nx, ny, nw, nh);
                double confidence = clamp01(r.confidence());
                detections.add(new Detection("face", confidence, bbox, Map.of()));
            }
            double avg = detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0);
            long took = System.currentTimeMillis() - start;
            return new VisionResult(DetectionType.FACE, detections, avg, took, Instant.now(), Map.of(
                "backend", BACKEND_ID,
                "source", "facebytes"
            ));
        } catch (Exception e) {
            // Gracefully handle expected "no faces/embeddings" cases without logging errors
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (e instanceof IllegalArgumentException && (msg.contains("No faces detected") || msg.contains("No valid face embeddings produced"))) {
                long took = System.currentTimeMillis() - start;
                logger.warn("FaceBytes detection returned no results: {}", msg);
                return VisionResult.empty(DetectionType.FACE, took);
            }
            logger.error("FaceBytes detectFaces failed", e);
            long took = System.currentTimeMillis() - start;
            return VisionResult.empty(DetectionType.FACE, took);
        }
    }

    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException("Object detection not supported by FaceBytes backend");
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
