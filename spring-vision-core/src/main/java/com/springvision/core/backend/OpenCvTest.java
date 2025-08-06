package com.springvision.core.backend;

import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Simple test class to verify OpenCV functionality with embedded libraries.
 *
 * <p>This class can be used to test if OpenCV is properly loaded and working
 * with the embedded native libraries.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class OpenCvTest {

    private static final Logger logger = LoggerFactory.getLogger(OpenCvTest.class);

    /**
     * Main method to run OpenCV tests.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("Starting OpenCV functionality test...");

        try {
            // Test 1: Basic Mat creation
            logger.info("Test 1: Creating Mat instance...");
            Mat testMat = new Mat();
            testMat.releaseReference();
            logger.info("✓ Mat creation successful");

            // Test 2: Static imports
            logger.info("Test 2: Testing static imports...");
            int testValue = CV_8UC3;
            logger.info("✓ Static imports successful: CV_8UC3 = {}", testValue);

            // Test 3: Basic image processing
            logger.info("Test 3: Basic image processing...");
            Mat image = new Mat(100, 100, CV_8UC3);
            Mat gray = new Mat();
            cvtColor(image, gray, COLOR_BGR2GRAY);
            logger.info("✓ Image processing successful");

            // Test 4: Cascade classifier
            logger.info("Test 4: Testing cascade classifier...");
            org.bytedeco.opencv.opencv_objdetect.CascadeClassifier classifier =
                new org.bytedeco.opencv.opencv_objdetect.CascadeClassifier();
            logger.info("✓ Cascade classifier creation successful");

            logger.info("All OpenCV tests passed! Embedded libraries are working correctly.");

        } catch (UnsatisfiedLinkError e) {
            logger.error("✗ Native library loading failed: {}", e.getMessage());
            System.exit(1);
        } catch (ExceptionInInitializerError e) {
            logger.error("✗ OpenCV initialization failed: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("✗ Unexpected error: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
