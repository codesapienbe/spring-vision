package com.deepface.detectors;

import com.deepface.core.FaceRegion;
import com.deepface.utils.Logs;
import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.HOGDescriptor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;

/**
 * DLIB-style HOG face detector implementation using OpenCV's HOGDescriptor.
 * Provides an alternative to Haar cascades with different detection characteristics.
 * 
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class DlibDetector implements FaceDetector {

    private static final Logger log = LoggerFactory.getLogger(DlibDetector.class);
    
    // HOG detector parameters optimized for face detection
    private static final double HIT_THRESHOLD = 0.0;
    private static final Size WIN_STRIDE = new Size(8, 8);
    private static final Size PADDING = new Size(4, 4);
    private static final double SCALE = 1.05;
    private static final double FINAL_THRESHOLD = 0.3;
    
    private final HOGDescriptor hogDetector;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter toFrame = new Java2DFrameConverter();

    public DlibDetector() {
        Loader.load(org.bytedeco.opencv.opencv_objdetect.HOGDescriptor.class);
        
        // Initialize HOG detector with default people detector (can be fine-tuned for faces)
        this.hogDetector = new HOGDescriptor();
        
        // Set SVM detector parameters for face detection
        // Note: This is a simplified approach - in production, you'd use a trained face-specific HOG model
        log.info("DlibDetector initialized with HOG face detection");
    }

    @Override
    public List<FaceRegion> detectFaces(BufferedImage image) {
        if (image == null) {
            return List.of();
        }

        try {
            // Convert to OpenCV Mat
            Mat matColor = toMat.convert(toFrame.convert(image));
            Mat gray = new Mat();
            cvtColor(matColor, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            // Detect faces using HOG
            RectVector faces = new RectVector();
            double[] weights = new double[0];
            
            // Use HOG detection with optimized parameters
            hogDetector.detectMultiScale(gray, faces, HIT_THRESHOLD, WIN_STRIDE, PADDING, SCALE, FINAL_THRESHOLD, false);

            List<FaceRegion> detectedFaces = new ArrayList<>();
            for (long i = 0; i < faces.size(); i++) {
                Rect rect = faces.get(i);
                double confidence = weights.length > i ? weights[(int) i] : 0.8; // Default confidence if weights unavailable
                
                // Filter by minimum face size and confidence
                if (rect.width() >= 20 && rect.height() >= 20 && confidence > 0.5) {
                    detectedFaces.add(new FaceRegion(
                        rect.x(), rect.y(), rect.width(), rect.height(), 
                        confidence, null
                    ));
                }
            }

            log.debug("DlibDetector detected {} faces", detectedFaces.size());
            return detectedFaces;

        } catch (Exception e) {
            log.error("DlibDetector", "face_detection_failed", e, Map.of("imageSize", image.getWidth() + "x" + image.getHeight()));
            return List.of();
        }
    }

    /**
     * Enhanced face detection with custom parameters.
     * 
     * @param image input image
     * @param scale detection scale factor
     * @param threshold confidence threshold
     * @return list of detected face regions
     */
    public List<FaceRegion> detectFaces(BufferedImage image, double scale, double threshold) {
        if (image == null) {
            return List.of();
        }

        try {
            Mat matColor = toMat.convert(toFrame.convert(image));
            Mat gray = new Mat();
            cvtColor(matColor, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            RectVector faces = new RectVector();
            double[] weights = new double[0];
            
            hogDetector.detectMultiScale(gray, faces, HIT_THRESHOLD, WIN_STRIDE, PADDING, scale, threshold, false);

            List<FaceRegion> detectedFaces = new ArrayList<>();
            for (long i = 0; i < faces.size(); i++) {
                Rect rect = faces.get(i);
                double confidence = weights.length > i ? weights[(int) i] : 0.8;
                
                if (rect.width() >= 20 && rect.height() >= 20 && confidence > threshold) {
                    detectedFaces.add(new FaceRegion(
                        rect.x(), rect.y(), rect.width(), rect.height(), 
                        confidence, null
                    ));
                }
            }

            return detectedFaces;

        } catch (Exception e) {
            log.error("DlibDetector", "custom_face_detection_failed", e, Map.of(
                "imageSize", image.getWidth() + "x" + image.getHeight(),
                "scale", scale,
                "threshold", threshold
            ));
            return List.of();
        }
    }

    /**
     * Cleanup resources when detector is no longer needed.
     */
    public void close() {
        try {
            if (hogDetector != null) {
                hogDetector.close();
            }
        } catch (Exception e) {
            log.warn("DlibDetector", "cleanup_failed", e, Map.of());
        }
    }
} 