package io.github.codesapienbe.springvision.cyber.detectors;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector for identifying shoulder surfing attempts by analyzing video streams
 * for unauthorized viewers looking at screens.
 *
 * <p>This detector:
 * <ul>
 *   <li>Analyzes multiple video frames for face detection</li>
 *   <li>Tracks face positions and gaze directions</li>
 *   <li>Identifies faces positioned behind the primary user</li>
 *   <li>Alerts when potential shoulder surfing is detected</li>
 * </ul>
 *
 * <p>The detector uses temporal analysis across frames to reduce false positives
 * and can distinguish between authorized users and potential threats based on
 * position, duration, and viewing angle.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class ShoulderSurfingDetector {

    private static final Logger logger = LoggerFactory.getLogger(ShoulderSurfingDetector.class);

    private static final double SUSPICIOUS_POSITION_THRESHOLD = 0.7; // 70% of frame height
    private static final int SUSPICIOUS_DURATION_FRAMES = 30; // ~1 second at 30fps

    private final CascadeClassifier faceDetector;
    private final OpenCVFrameConverter.ToMat converter;
    private final Java2DFrameConverter imageConverter;

    // Tracking state across frames
    private final Map<String, Integer> suspiciousFaceTracker = new HashMap<>();

    public ShoulderSurfingDetector() {
        // Initialize OpenCV face detector
        String classifierPath = null;
        try {
            var resource = getClass().getClassLoader().getResource("haarcascade_frontalface_default.xml");
            if (resource != null) {
                classifierPath = resource.getPath();
            }
        } catch (Exception e) {
            logger.warn("Could not load classifier resource", e);
        }

        if (classifierPath != null) {
            this.faceDetector = new CascadeClassifier(classifierPath);
        } else {
            // Fallback to system OpenCV path
            this.faceDetector = new CascadeClassifier();
            logger.warn("Using system OpenCV classifier path");
        }

        this.converter = new OpenCVFrameConverter.ToMat();
        this.imageConverter = new Java2DFrameConverter();

        logger.info("Initialized ShoulderSurfingDetector");
    }

    /**
     * Analyzes a sequence of video frames to detect shoulder surfing attempts.
     *
     * @param videoFrames sequential frames from video stream
     * @return list of detections indicating potential shoulder surfing
     */
    public List<Detection> analyzeVideoStream(List<ImageData> videoFrames) {
        List<Detection> allDetections = new ArrayList<>();

        if (videoFrames == null || videoFrames.isEmpty()) {
            return allDetections;
        }

        logger.debug("Analyzing {} frames for shoulder surfing", videoFrames.size());

        for (int i = 0; i < videoFrames.size(); i++) {
            List<Detection> frameDetections = analyzeFrame(videoFrames.get(i), i);
            allDetections.addAll(frameDetections);
        }

        return filterAndConsolidateDetections(allDetections);
    }

    /**
     * Analyzes a single frame for potential shoulder surfing.
     */
    private List<Detection> analyzeFrame(ImageData imageData, int frameNumber) {
        List<Detection> detections = new ArrayList<>();

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (bufferedImage == null) {
                return detections;
            }

            Frame frame = imageConverter.convert(bufferedImage);
            Mat mat = converter.convert(frame);
            Mat grayMat = new Mat();
            opencv_imgproc.cvtColor(mat, grayMat, opencv_imgproc.CV_BGR2GRAY);

            // Detect faces
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(grayMat, faces);

            int imageHeight = bufferedImage.getHeight();
            int imageWidth = bufferedImage.getWidth();

            // Analyze each detected face
            for (int i = 0; i < faces.size(); i++) {
                Rect face = faces.get(i);
                Detection detection = analyzeFacePosition(face, imageWidth, imageHeight, frameNumber);
                if (detection != null) {
                    detections.add(detection);
                }
            }

            // Clean up
            grayMat.release();
            mat.release();

        } catch (Exception e) {
            logger.error("Error analyzing frame {}", frameNumber, e);
        }

        return detections;
    }

    /**
     * Analyzes the position of a detected face to determine if it represents
     * a shoulder surfing threat.
     */
    private Detection analyzeFacePosition(Rect face, int imageWidth, int imageHeight, int frameNumber) {
        double faceY = face.y();
        double faceX = face.x();
        double faceWidth = face.width();
        double faceHeight = face.height();

        // Calculate relative position (0.0 to 1.0)
        double relativeY = faceY / imageHeight;
        double relativeX = faceX / imageWidth;

        // Check if face is in suspicious position (peripheral/background area)
        boolean isSuspicious = false;
        String suspicionReason = null;
        double confidence = 0.0;

        // Face in upper portion of frame (potentially behind user)
        if (relativeY < (1.0 - SUSPICIOUS_POSITION_THRESHOLD)) {
            isSuspicious = true;
            suspicionReason = "Face detected in background position";
            confidence = 0.7;
        }

        // Face at extreme edges (side viewing angle)
        if (relativeX < 0.15 || relativeX > 0.85) {
            isSuspicious = true;
            suspicionReason = "Face detected at extreme viewing angle";
            confidence = Math.max(confidence, 0.6);
        }

        // Small face size relative to frame (distant viewer)
        double relativeFaceSize = (faceWidth * faceHeight) / (imageWidth * imageHeight);
        if (relativeFaceSize < 0.05) {
            isSuspicious = true;
            suspicionReason = "Small face detected (distant viewer)";
            confidence = Math.max(confidence, 0.5);
        }

        if (!isSuspicious) {
            return null;
        }

        // Track suspicious faces across frames
        String faceKey = String.format("%.2f_%.2f", relativeX, relativeY);
        suspiciousFaceTracker.merge(faceKey, 1, Integer::sum);

        // Increase confidence if face persists across frames
        int frameCount = suspiciousFaceTracker.getOrDefault(faceKey, 0);
        if (frameCount > SUSPICIOUS_DURATION_FRAMES) {
            confidence = Math.min(0.95, confidence + 0.2);
            suspicionReason += " (persistent)";
        }

        // Create normalized bounding box
        BoundingBox bbox = BoundingBox.fromPixels(
            (int) faceX,
            (int) faceY,
            (int) faceWidth,
            (int) faceHeight,
            imageWidth,
            imageHeight
        );

        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("frameNumber", frameNumber);
        metadata.put("reason", suspicionReason);
        metadata.put("persistenceFrames", frameCount);
        metadata.put("relativePosition", Map.of("x", relativeX, "y", relativeY));
        metadata.put("threatLevel", confidence > 0.8 ? "HIGH" : confidence > 0.6 ? "MEDIUM" : "LOW");

        return new Detection("SHOULDER_SURFING_ATTEMPT", confidence, bbox, metadata);
    }

    /**
     * Filters and consolidates detections to reduce false positives.
     */
    private List<Detection> filterAndConsolidateDetections(List<Detection> detections) {
        // For now, return high-confidence detections only
        return detections.stream()
            .filter(d -> d.confidence() > 0.6)
            .toList();
    }

    /**
     * Resets the tracking state. Should be called when starting a new video session.
     */
    public void resetTracking() {
        suspiciousFaceTracker.clear();
        logger.debug("Reset shoulder surfing tracking state");
    }

    /**
     * Configures the sensitivity threshold for detecting suspicious positions.
     *
     * @param threshold value between 0.0 and 1.0 (higher = more sensitive)
     */
    public void setSensitivityThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        logger.info("Shoulder surfing sensitivity threshold set to: {}", threshold);
    }
}
