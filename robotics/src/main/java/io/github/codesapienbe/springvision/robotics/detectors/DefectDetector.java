package io.github.codesapienbe.springvision.robotics.detectors;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector for identifying defects in manufactured products.
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
public class DefectDetector {

    private static final Logger logger = LoggerFactory.getLogger(DefectDetector.class);
    private static final double MINOR_DEFECT_THRESHOLD = 0.3;
    private static final double MAJOR_DEFECT_THRESHOLD = 0.6;
    private static final double CRITICAL_DEFECT_THRESHOLD = 0.8;

    public List<Detection> detect(ImageData imageData) {
        logger.debug("Starting defect detection on image");
        List<Detection> defects = new ArrayList<>();

        try {
            Mat image = convertToMat(imageData);
            if (image == null || image.empty()) {
                logger.warn("Invalid image data for defect detection");
                return defects;
            }

            Mat gray = new Mat();
            opencv_imgproc.cvtColor(image, gray, opencv_imgproc.CV_BGR2GRAY);

            Mat blurred = new Mat();
            opencv_imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

            Mat edges = new Mat();
            opencv_imgproc.Canny(blurred, edges, 50, 150);

            MatVector contours = new MatVector();
            Mat hierarchy = new Mat();
            opencv_imgproc.findContours(edges, contours, hierarchy,
                opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

            for (int i = 0; i < contours.size(); i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);

                if (area > 50) {
                    Rect boundingRect = opencv_imgproc.boundingRect(contour);
                    Detection defect = analyzeDefectRegion(boundingRect, area, image.size());
                    if (defect != null) {
                        defects.add(defect);
                    }
                }
            }

            image.release();
            gray.release();
            blurred.release();
            edges.release();
            hierarchy.release();

        } catch (Exception e) {
            logger.error("Error during defect detection", e);
        }

        logger.debug("Defect detection complete. Found {} defects", defects.size());
        return defects;
    }

    private Detection analyzeDefectRegion(Rect rect, double area, Size imageSize) {
        double aspectRatio = (double) rect.width() / rect.height();
        double relativeSize = area / (imageSize.width() * imageSize.height());

        String defectType = classifyDefectType(aspectRatio, relativeSize);
        String severity = calculateSeverity(relativeSize);
        double confidence = calculateConfidence(aspectRatio, relativeSize);

        BoundingBox boundingBox = new BoundingBox(
            rect.x(), rect.y(), rect.width(), rect.height()
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("defectType", defectType);
        attributes.put("severity", severity);
        attributes.put("relativeSize", relativeSize);
        attributes.put("aspectRatio", aspectRatio);
        attributes.put("area", area);

        return new Detection("defect", confidence, boundingBox, attributes);
    }

    private String classifyDefectType(double aspectRatio, double relativeSize) {
        if (aspectRatio > 3.0) {
            return "scratch";
        } else if (relativeSize < 0.001) {
            return "spot";
        } else if (aspectRatio < 0.5) {
            return "dent";
        } else {
            return "crack";
        }
    }

    private String calculateSeverity(double relativeSize) {
        if (relativeSize > CRITICAL_DEFECT_THRESHOLD) {
            return "critical";
        } else if (relativeSize > MAJOR_DEFECT_THRESHOLD) {
            return "major";
        } else if (relativeSize > MINOR_DEFECT_THRESHOLD) {
            return "minor";
        } else {
            return "negligible";
        }
    }

    private double calculateConfidence(double aspectRatio, double relativeSize) {
        double baseConfidence = 0.6;
        double sizeBonus = Math.min(relativeSize * 10, 0.3);
        return Math.min(baseConfidence + sizeBonus, 1.0);
    }

    private Mat convertToMat(ImageData imageData) {
        try {
            byte[] bytes = imageData.data();
            Mat mat = new Mat(bytes);
            return opencv_imgcodecs.imdecode(mat, opencv_imgcodecs.IMREAD_COLOR);
        } catch (Exception e) {
            logger.error("Error converting ImageData to Mat", e);
            return null;
        }
    }
}
