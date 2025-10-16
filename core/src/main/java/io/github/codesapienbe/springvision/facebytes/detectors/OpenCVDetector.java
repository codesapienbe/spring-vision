package io.github.codesapienbe.springvision.facebytes.detectors;

import io.github.codesapienbe.springvision.facebytes.core.FaceRegion;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenCV-based face detector using Haar cascade classifiers.
 * Provides robust face detection with proper native resource management.
 *
 * @author FaceBytes Team
 * @since 1.0.0
 */
public final class OpenCVDetector implements FaceDetector {

    private static final String CASCADE_CLASSPATH = "/models/haarcascades/haarcascade_frontalface_default.xml";
    private static final String CASCADE_GENERATED = "models/haarcascades/haarcascade_frontalface_default.xml";
    private static final String CASCADE_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";

    private static final Logger log = LoggerFactory.getLogger(OpenCVDetector.class);

    private final CascadeClassifier classifier;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter toFrame = new Java2DFrameConverter();

    /**
     * Creates a new OpenCV face detector with proper native library initialization.
     *
     * @throws IllegalStateException if the Haar cascade classifier cannot be loaded
     */
    public OpenCVDetector() {
        CascadeClassifier tempClassifier = null;
        try {
            // Try to load JavaCV native libraries
            try {
                Loader.load(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier.class);
            } catch (UnsatisfiedLinkError e) {
                log.error("Failed to load JavaCV native libraries. This usually means the native OpenCV libraries are not installed or not in the library path: {}", e.getMessage());
                throw new IllegalStateException("JavaCV native libraries not available: " + e.getMessage() +
                    ". Please ensure OpenCV native libraries are installed in the system.", e);
            }

            String cascadePath = ensureCascadeAvailable();
            tempClassifier = new CascadeClassifier(cascadePath);
            if (tempClassifier.empty()) {
                throw new IllegalStateException("Failed to load Haar cascade from: " + cascadePath);
            }
            log.info("OpenCV detector initialized successfully with cascade: {}", cascadePath);
        } catch (Exception e) {
            log.error("Failed to initialize OpenCV detector: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize OpenCV detector: " + e.getMessage(), e);
        }
        this.classifier = tempClassifier;
    }

    /**
     * Detects faces in the given image using OpenCV Haar cascade classifier.
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
            // Convert BufferedImage to OpenCV Mat
            matColor = toMat.convert(toFrame.convert(image));
            gray = new Mat();

            // Convert to grayscale and equalize histogram
            cvtColor(matColor, gray, COLOR_BGR2GRAY);
            equalizeHist(gray, gray);

            // Detect faces using Haar cascade
            faces = new RectVector();
            // Tuned parameters: slightly more conservative minNeighbors to reduce false positives,
            // and smaller minSize to allow small faces in crowds.
            classifier.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(20, 20), new Size());

            // Convert detection results to FaceRegion objects
            List<FaceRegion> result = new ArrayList<>();
            for (long i = 0; i < faces.size(); i++) {
                Rect r = faces.get(i);
                result.add(new FaceRegion(r.x(), r.y(), r.width(), r.height(), 0.9, null));
            }
            return result;

        } catch (Exception e) {
            // Log error and return empty list to prevent crashes
            log.error("Face detection failed: {}", e.getMessage(), e);
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
     * Ensures the Haar cascade classifier file is available locally.
     * Tries multiple sources: classpath, generated resources, and downloads if needed.
     *
     * @return path to the cascade classifier file
     * @throws IllegalStateException if the cascade file cannot be obtained
     */
    private String ensureCascadeAvailable() {
        // 1) Try classpath
        try (InputStream is = getClass().getResourceAsStream(CASCADE_CLASSPATH)) {
            if (is != null) {
                Path tmp = Files.createTempFile("haarcascade", ".xml");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toAbsolutePath().toString();
            }
        } catch (IOException ignored) {
        }

        // 2) Try generated resources
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CASCADE_GENERATED)) {
            if (is != null) {
                Path tmp = Files.createTempFile("haarcascade", ".xml");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toAbsolutePath().toString();
            }
        } catch (IOException ignored) {
        }

        // 3) Cache in user home
        try {
            Path cacheDir = Path.of(System.getProperty("user.home", "."), ".spring-vision", "facebytes", "models");
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve("haarcascade_frontalface_default.xml");
            if (Files.exists(target) && Files.size(target) > 0) {
                return target.toAbsolutePath().toString();
            }
            // Download securely with timeouts
            java.net.URL url = new java.net.URL(CASCADE_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "spring-vision-facebytes/1.0");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                if (Files.size(target) == 0) {
                    throw new IOException("Downloaded cascade file is empty");
                }
                return target.toAbsolutePath().toString();
            }
            throw new IOException("HTTP " + code + " for cascade download");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Haar cascade: " + e.getMessage(), e);
        }
    }
}
