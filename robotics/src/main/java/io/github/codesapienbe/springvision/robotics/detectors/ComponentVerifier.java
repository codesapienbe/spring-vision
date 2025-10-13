package io.github.codesapienbe.springvision.robotics.detectors;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component verifier for industrial assembly processes.
 *
 * <p>Verifies that the correct components are used during assembly by comparing detected
 * components against a reference database.</p>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
public class ComponentVerifier {

    private static final Logger logger = LoggerFactory.getLogger(ComponentVerifier.class);
    private static final Map<String, ComponentSpec> COMPONENT_DATABASE = new HashMap<>();

    static {
        COMPONENT_DATABASE.put("rect_large", new ComponentSpec("COMP-001", "Rectangular Large", 100, 50));
        COMPONENT_DATABASE.put("rect_small", new ComponentSpec("COMP-002", "Rectangular Small", 50, 25));
        COMPONENT_DATABASE.put("square", new ComponentSpec("COMP-003", "Square Component", 60, 60));
        COMPONENT_DATABASE.put("circle", new ComponentSpec("COMP-004", "Circular Component", 40, 40));
    }

    /**
     * Default constructor.
     */
    public ComponentVerifier() {
    }

    /**
     * Detects and verifies components in the given image.
     *
     * @param imageData the image data to analyze
     * @return list of detections with component verification results
     */
    public List<Detection> detect(ImageData imageData) {
        logger.debug("Starting component verification");
        List<Detection> verifications = new ArrayList<>();

        try {
            Mat image = convertToMat(imageData);
            if (image == null || image.empty()) {
                logger.warn("Invalid image data for component verification");
                return verifications;
            }

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(image, gray, opencv_imgproc.CV_BGR2GRAY);

            Mat binary = new Mat();
            opencv_imgproc.adaptiveThreshold(gray, binary, 255,
                opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                opencv_imgproc.THRESH_BINARY_INV, 11, 2);

            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            opencv_imgproc.findContours(binary, contours, hierarchy,
                opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            for (int i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);

                if (area > 100) {
                    Detection verification = verifyComponent(contour);
                    if (verification != null) {
                        verifications.add(verification);
                    }
                }
            }

            image.release();
            gray.release();
            binary.release();
            hierarchy.release();

        } catch (Exception e) {
            logger.error("Error during component verification", e);
        }

        logger.debug("Component verification complete. Verified {} components", verifications.size());
        return verifications;
    }

    private Detection verifyComponent(Mat contour) {
        Rect rect = opencv_imgproc.boundingRect(contour);
        double area = opencv_imgproc.contourArea(contour);
        double perimeter = opencv_imgproc.arcLength(contour, true);
        double aspectRatio = (double) rect.width() / rect.height();
        double circularity = (4 * Math.PI * area) / (perimeter * perimeter);

        ComponentIdentification identification = identifyComponent(
            rect.width(), rect.height(), aspectRatio, circularity
        );

        boolean verified = verification(identification);

        BoundingBox boundingBox = new BoundingBox(
            rect.x(), rect.y(), rect.width(), rect.height()
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("componentId", identification.componentId);
        attributes.put("partNumber", identification.componentId);
        attributes.put("componentName", identification.name);
        attributes.put("verified", verified);
        attributes.put("dimensions", Map.of(
            "width", rect.width(),
            "height", rect.height()
        ));
        attributes.put("aspectRatio", aspectRatio);
        attributes.put("circularity", circularity);

        if (!verified) {
            attributes.put("expectedComponent", "COMP-001");
            attributes.put("verificationStatus", "MISMATCH");
        } else {
            attributes.put("verificationStatus", "PASS");
        }

        double confidence = calculateVerificationConfidence(identification, verified);

        return new Detection("component", confidence, boundingBox, attributes);
    }

    private ComponentIdentification identifyComponent(
        int width, int height, double aspectRatio, double circularity) {

        if (circularity > 0.8) {
            ComponentSpec spec = COMPONENT_DATABASE.get("circle");
            return new ComponentIdentification(
                spec.partNumber, spec.name, "circle", 0.85
            );
        }

        if (aspectRatio > 0.9 && aspectRatio < 1.1) {
            ComponentSpec spec = COMPONENT_DATABASE.get("square");
            return new ComponentIdentification(
                spec.partNumber, spec.name, "square", 0.9
            );
        }

        if (aspectRatio > 1.5 && width > 80) {
            ComponentSpec spec = COMPONENT_DATABASE.get("rect_large");
            return new ComponentIdentification(
                spec.partNumber, spec.name, "rect_large", 0.88
            );
        }

        if (aspectRatio > 1.5) {
            ComponentSpec spec = COMPONENT_DATABASE.get("rect_small");
            return new ComponentIdentification(
                spec.partNumber, spec.name, "rect_small", 0.82
            );
        }

        return new ComponentIdentification(
            "UNKNOWN", "Unknown Component", "unknown", 0.5
        );
    }

    private boolean verification(ComponentIdentification identification) {
        return !identification.componentId.equals("UNKNOWN") &&
            identification.confidence > 0.7;
    }

    private double calculateVerificationConfidence(ComponentIdentification identification, boolean verified) {
        double baseConfidence = identification.confidence;

        if (verified) {
            return Math.min(baseConfidence + 0.1, 1.0);
        } else {
            return Math.max(baseConfidence - 0.2, 0.3);
        }
    }

    private Mat convertToMat(ImageData imageData) {
        try {
            byte[] bytes = imageData.data();
            Mat mat = new Mat(bytes);
            return org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(mat, org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR);
        } catch (Exception e) {
            logger.error("Error converting ImageData to Mat", e);
            return null;
        }
    }

    private static class ComponentSpec {
        final String partNumber;
        final String name;
        final int expectedWidth;
        final int expectedHeight;

        ComponentSpec(String partNumber, String name, int expectedWidth, int expectedHeight) {
            this.partNumber = partNumber;
            this.name = name;
            this.expectedWidth = expectedWidth;
            this.expectedHeight = expectedHeight;
        }
    }

    private static class ComponentIdentification {
        final String componentId;
        final String name;
        final String type;
        final double confidence;

        ComponentIdentification(String componentId, String name, String type, double confidence) {
            this.componentId = componentId;
            this.name = name;
            this.type = type;
            this.confidence = confidence;
        }
    }
}
