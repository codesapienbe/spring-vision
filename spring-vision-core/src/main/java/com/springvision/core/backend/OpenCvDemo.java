package com.springvision.core.backend;

import com.springvision.core.BoundingBox;
import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * Demo class for OpenCV integration showing basic image loading and display.
 *
 * <p>This class demonstrates how to use OpenCV for basic image operations
 * including loading, displaying, and saving images. It serves as a simple
 * example of OpenCV integration with the Spring Vision framework.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * OpenCvDemo demo = new OpenCvDemo();
 * demo.loadAndDisplayImage("path/to/image.jpg");
 * demo.detectAndShowFaces("path/to/image.jpg");
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see OpenCvVisionBackend
 */
public class OpenCvDemo {

    private static final Logger logger = LoggerFactory.getLogger(OpenCvDemo.class);

    private final OpenCvVisionBackend backend;
    private final OpenCVFrameConverter.ToMat frameConverter;

    /**
     * Constructs a new OpenCV demo instance.
     */
    public OpenCvDemo() {
        this.backend = new OpenCvVisionBackend();
        this.frameConverter = new OpenCVFrameConverter.ToMat();
    }

    /**
     * Loads and displays an image using OpenCV.
     *
     * <p>This method demonstrates basic OpenCV image loading and display
     * functionality. It loads an image from a file and displays it in a window.</p>
     *
     * @param imagePath the path to the image file
     * @throws IOException if the image cannot be loaded
     */
    public void loadAndDisplayImage(String imagePath) throws IOException {
        logger.info("Loading and displaying image: {}", imagePath);

        try {
            // Load image using OpenCV
            Mat image = imread(imagePath);

            if (image.empty()) {
                throw new IOException("Failed to load image: " + imagePath);
            }

            logger.info("Image loaded successfully: {}x{} pixels", image.cols(), image.rows());

            // Display image in a window
            CanvasFrame frame = new CanvasFrame("OpenCV Image Demo", CanvasFrame.getDefaultGamma() / backend.getBackendId().hashCode());
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.showImage(frameConverter.convert(image));

            logger.info("Image displayed in window. Close the window to continue.");

            // Clean up
            image.releaseReference();

        } catch (Exception e) {
            logger.error("Failed to load and display image", e);
            throw new IOException("Failed to load and display image: " + imagePath, e);
        }
    }

    /**
     * Loads an image and performs face detection, then displays the results.
     *
     * <p>This method demonstrates the complete face detection pipeline using
     * the OpenCV backend. It loads an image, detects faces, and displays
     * the results with bounding boxes.</p>
     *
     * @param imagePath the path to the image file
     * @throws IOException if the image cannot be loaded or processed
     */
    public void detectAndShowFaces(String imagePath) throws IOException {
        logger.info("Detecting faces in image: {}", imagePath);

        try {
            // Initialize the backend
            backend.initialize();

            // Load image data
            byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
            ImageData imageData = ImageData.fromBytes(imageBytes);

            // Perform face detection
            VisionResult result = backend.detectFaces(imageData);

            logger.info("Face detection completed: {} faces detected", result.detectionCount());

            // Display results
            if (result.hasDetections()) {
                logger.info("Detected faces:");
                result.detections().forEach(detection -> {
                    logger.info("  - Face at {} with confidence: {:.2f}",
                        detection.boundingBox(), detection.confidence());
                });
            } else {
                logger.info("No faces detected in the image");
            }

            // Display the image with detection results
            displayImageWithDetections(imagePath, result);

        } catch (Exception e) {
            logger.error("Failed to detect faces", e);
            throw new IOException("Failed to detect faces in image: " + imagePath, e);
        } finally {
            // Clean up
            try {
                backend.shutdown();
            } catch (Exception e) {
                logger.warn("Failed to shut down backend", e);
            }
        }
    }

    /**
     * Performs a simple image transformation (resize and grayscale).
     *
     * <p>This method demonstrates basic image processing operations using OpenCV.
     * It loads an image, resizes it, converts it to grayscale, and saves the result.</p>
     *
     * @param inputPath the path to the input image
     * @param outputPath the path to save the transformed image
     * @param newWidth the new width for the resized image
     * @param newHeight the new height for the resized image
     * @throws IOException if the image cannot be processed
     */
    public void transformImage(String inputPath, String outputPath, int newWidth, int newHeight)
            throws IOException {
        logger.info("Transforming image: {} -> {} ({}x{})", inputPath, outputPath, newWidth, newHeight);

        try {
            // Load image
            Mat image = imread(inputPath);

            if (image.empty()) {
                throw new IOException("Failed to load image: " + inputPath);
            }

            // Resize image
            Mat resizedImage = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.resize(image, resizedImage,
                new org.bytedeco.opencv.opencv_core.Size(newWidth, newHeight));

            // Convert to grayscale
            Mat grayImage = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.cvtColor(resizedImage, grayImage,
                org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY);

            // Save transformed image
            boolean saved = imwrite(outputPath, grayImage);

            if (!saved) {
                throw new IOException("Failed to save transformed image: " + outputPath);
            }

            logger.info("Image transformation completed successfully");

            // Clean up
            image.releaseReference();
            resizedImage.releaseReference();
            grayImage.releaseReference();

        } catch (Exception e) {
            logger.error("Failed to transform image", e);
            throw new IOException("Failed to transform image: " + inputPath, e);
        }
    }

    /**
     * Displays an image with detection results overlaid.
     *
     * @param imagePath the path to the image
     * @param result the detection results
     * @throws IOException if the image cannot be displayed
     */
    private void displayImageWithDetections(String imagePath, VisionResult result) throws IOException {
        try {
            // Load image
            Mat image = imread(imagePath);

            if (image.empty()) {
                throw new IOException("Failed to load image: " + imagePath);
            }

            // Draw detection results on the image
            if (result.hasDetections()) {
                for (var detection : result.detections()) {
                    BoundingBox box = detection.boundingBox();

                    // Convert normalized coordinates to pixel coordinates
                    int x = (int) (box.x() * image.cols());
                    int y = (int) (box.y() * image.rows());
                    int width = (int) (box.width() * image.cols());
                    int height = (int) (box.height() * image.rows());

                    // Draw rectangle around detection
                    org.bytedeco.opencv.global.opencv_imgproc.rectangle(image,
                        new org.bytedeco.opencv.opencv_core.Point(x, y),
                        new org.bytedeco.opencv.opencv_core.Point(x + width, y + height),
                        new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 255), // Green color
                        2, // Thickness
                        8, // Line type
                        0 // Shift
                    );

                    // Add confidence text
                    String confidenceText = String.format("%.2f", detection.confidence());
                    org.bytedeco.opencv.global.opencv_imgproc.putText(image, confidenceText,
                        new org.bytedeco.opencv.opencv_core.Point(x, y - 10),
                        org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                        0.5, // Font scale
                        new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 255), // Green color
                        1, // Thickness
                        8, // Line type
                        false // Bottom left origin
                    );
                }
            }

            // Display image with detections
            CanvasFrame frame = new CanvasFrame("Face Detection Results",
                CanvasFrame.getDefaultGamma() / backend.getBackendId().hashCode());
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.showImage(frameConverter.convert(image));

            logger.info("Image with detection results displayed. Close the window to continue.");

            // Clean up
            image.releaseReference();

        } catch (Exception e) {
            logger.error("Failed to display image with detections", e);
            throw new IOException("Failed to display image with detections", e);
        }
    }

    /**
     * Main method for running the demo.
     *
     * @param args command line arguments (image path)
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: OpenCvDemo <image-path>");
            System.exit(1);
        }

        String imagePath = args[0];
        OpenCvDemo demo = new OpenCvDemo();

        try {
            // Load and display the image
            demo.loadAndDisplayImage(imagePath);

            // Wait a bit, then perform face detection
            Thread.sleep(2000);

            // Detect faces
            demo.detectAndShowFaces(imagePath);

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
