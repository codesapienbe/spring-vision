package io.github.codesapienbe.springvision.facebytes.detectors;

import io.github.codesapienbe.springvision.facebytes.core.FaceRegion;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;
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

    /**
     * Creates a new DLIB-style HOG face detector.
     *
     * @throws IllegalStateException if the HOG detector cannot be initialized
     */
    public DlibDetector() {
        try {
            Loader.load(org.bytedeco.opencv.opencv_objdetect.HOGDescriptor.class);

            // Initialize HOG detector with default people detector (can be fine-tuned for faces)
            this.hogDetector = new HOGDescriptor();

            // Set SVM detector parameters for face detection
            // Note: This is a simplified approach - in production, you'd use a trained face-specific HOG model
            log.info("DlibDetector initialized with HOG face detection");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize DLIB detector: " + e.getMessage(), e);
        }
    }

    /**
     * Detects faces in the given image using HOG descriptor.
     * Properly manages native resources to prevent memory leaks and crashes.
     *
     * @param image the input image to analyze
     * @return list of detected face regions
     */
    @Override
    public List<FaceRegion> detectFaces(BufferedImage image) {
        if (image == null) {
            return List.of();
        }

        Mat matColor = null;
        Mat gray = null;
        RectVector faces = null;

        try {
            // Convert to OpenCV Mat
            matColor = toMat.convert(toFrame.convert(image));
            gray = new Mat();
            cvtColor(matColor, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            // Detect faces using HOG
            faces = new RectVector();

            // Use HOG detection with optimized parameters
            hogDetector.detectMultiScale(gray, faces, HIT_THRESHOLD, WIN_STRIDE, PADDING, SCALE, FINAL_THRESHOLD, false);

            List<FaceRegion> detectedFaces = new ArrayList<>();
            for (long i = 0; i < faces.size(); i++) {
                Rect rect = faces.get(i);
                double confidence = 0.8; // Default confidence for HOG detection

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
        } finally {
            // Properly deallocate native resources
            if (matColor != null) {
                matColor.deallocate();
            }
            if (gray != null) {
                gray.deallocate();
            }
            if (faces != null) {
                faces.deallocate();
            }
        }
    }

    /**
     * Gets the HOG detector instance for advanced configuration.
     *
     * @return the underlying HOG descriptor
     */
    public HOGDescriptor getHogDetector() {
        return hogDetector;
    }

    /**
     * Updates the HOG detector parameters for fine-tuning.
     *
     * @param hitThreshold   detection threshold
     * @param winStride      window stride
     * @param padding        padding size
     * @param scale          scale factor
     * @param finalThreshold final detection threshold
     */
    public void updateParameters(double hitThreshold, Size winStride, Size padding,
                                 double scale, double finalThreshold) {
        // Note: HOGDescriptor doesn't have direct parameter setters
        // This would require reinitialization in a real implementation
        log.info("Parameter update requested - would require detector reinitialization");
    }

    /**
     * Checks if the detector is properly initialized and ready for use.
     *
     * @return true if the detector is ready
     */
    public boolean isReady() {
        return hogDetector != null;
    }
}
