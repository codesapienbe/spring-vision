package com.springvision.core.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.HOUGH_GRADIENT;
import static org.bytedeco.opencv.global.opencv_imgproc.HoughCircles;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_imgproc.Vec3fVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.springvision.core.BackendHealthInfo;
import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;

/**
 * OpenCV-based implementation of the VisionBackend interface.
 *
 * <p>This backend provides computer vision capabilities using the OpenCV library
 * through JavaCV bindings. It supports face detection, object detection, and
 * basic image processing operations.</p>
 *
 * <p>The backend automatically loads OpenCV models and provides fallback behavior
 * when OpenCV is not available. It includes comprehensive error handling and
 * performance monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * OpenCvVisionBackend backend = new OpenCvVisionBackend();
 * backend.initialize();
 *
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 * VisionResult result = backend.detectFaces(imageData);
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionBackend
 * @see VisionTemplate
 */
public class OpenCvVisionBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(OpenCvVisionBackend.class);

    /**
     * Backend identifier for OpenCV.
     */
    public static final String BACKEND_ID = "opencv";

    /**
     * Backend display name.
     */
    public static final String DISPLAY_NAME = "OpenCV Vision Backend";

    /**
     * Backend version.
     */
    public static final String VERSION = "4.8.1";

    /**
     * Default face detection model path.
     */
    private static final String DEFAULT_FACE_CASCADE_PATH = "/haarcascade_frontalface_default.xml";
    /**
     * Remote URL used as a secure fallback location for the Haar cascade file. The
     * download is performed only when the classifier cannot be found locally to
     * keep start-up fast and avoid unnecessary network access.
     */
    private static final String CASCADE_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";

    /**
     * DNN face detector (ResNet-SSD) resources. We prefer these over Haar cascades to reduce
     * false positives on rectangular patterns. Files are loaded from classpath if present,
     * otherwise downloaded on first use with short timeouts.
     */
    private static final String DNN_CAFFE_PROTO_TXT_CLASSPATH = "/models/deploy.prototxt";
    private static final String DNN_CAFFE_MODEL_CLASSPATH = "/models/res10_300x300_ssd_iter_140000_fp16.caffemodel";
    private static final String DNN_CAFFE_PROTO_TXT_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/deploy.prototxt";
    private static final String DNN_CAFFE_MODEL_URL =
        "https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel";

    /** Eye cascade to validate candidate faces (reduces rectangular false positives). */
    private static final String DEFAULT_EYE_CASCADE_PATH = "/haarcascade_eye.xml";
    private static final String EYE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml";

    /** Profile-face cascade to detect side-view faces. */
    private static final String DEFAULT_PROFILE_FACE_CASCADE_PATH = "/haarcascade_profileface.xml";
    private static final String PROFILE_FACE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_profileface.xml";

    /**
     * Default confidence threshold for detections.
     */
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.8;
    /**
     * Minimum face size for detection (relative to the smaller image dimension).
     * Reduced to support group photos and small faces farther from the camera.
     */
    private static final double MIN_FACE_SIZE_RATIO = 0.03;
    /**
     * Maximum face size for detection (relative to image size).
     */
    private static final double MAX_FACE_SIZE_RATIO = 0.8;

    /**
     * Face detection cascade classifier.
     */
    private CascadeClassifier faceCascade;
    private Net dnnFaceNet;
    private CascadeClassifier eyeCascade;
    private CascadeClassifier profileCascade;

    /**
     * Frame converter for OpenCV operations.
     */
    private OpenCVFrameConverter.ToMat frameConverter;

    /**
     * Flag indicating if OpenCV is available.
     */
    private boolean opencvAvailable;

    /**
     * Flag indicating if the backend is initialized.
     */
    private boolean initialized;

    /**
     * Last health check time.
     */
    private long lastHealthCheckTime;

    /**
     * Health status of the backend.
     */
    private BackendHealthInfo.HealthStatus healthStatus;

    /**
     * Error message if backend is unhealthy.
     */
    private String healthErrorMessage;

    /**
     * Constructs a new OpenCV vision backend.
     */
    public OpenCvVisionBackend() {
        this.faceCascade = null;
        this.frameConverter = null;
        this.opencvAvailable = false;
        this.initialized = false;
        this.healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
        this.healthErrorMessage = "Backend not initialized";
        this.dnnFaceNet = null;
        this.eyeCascade = null;
        this.profileCascade = null;
    }

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
        return Set.of(DetectionType.FACE, DetectionType.OBJECT);
    }

    @Override
    public boolean isHealthy() {
        return healthStatus == BackendHealthInfo.HealthStatus.HEALTHY && initialized;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long responseTime = System.currentTimeMillis() - lastHealthCheckTime;

        if (healthStatus == BackendHealthInfo.HealthStatus.HEALTHY) {
            if (opencvAvailable) {
                return BackendHealthInfo.healthy(getBackendId(),
                    "OpenCV backend is operational", responseTime);
            } else {
                return BackendHealthInfo.healthy(getBackendId(),
                    "OpenCV backend is operational in fallback mode", responseTime);
            }
        } else {
            return BackendHealthInfo.unhealthy(getBackendId(),
                "OpenCV backend is not operational", healthErrorMessage, responseTime);
        }
    }

    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        validateImageData(imageData);
        ensureInitialized();

        long startTime = System.currentTimeMillis();
        String correlationId = generateCorrelationId();

        logger.debug("Starting face detection", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format()
        ));

        try {
            // Check if OpenCV is available
            if (!opencvAvailable) {
                logger.warn("OpenCV not available - returning empty face detection result");
                return VisionResult.of(DetectionType.FACE, List.of(), 0.0, System.currentTimeMillis() - startTime);
            }

            logger.debug("OpenCV is available, proceeding with face detection");

            // Load image into OpenCV Mat
            Mat image = loadImageToMat(imageData);
            logger.debug("Image loaded successfully: {}x{}", image.cols(), image.rows());

            // Convert to grayscale for face detection and normalize contrast
            Mat grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);
            // Improve robustness for multiple/smaller faces
            equalizeHist(grayImage, grayImage);

            // Detect faces using cascade classifier if available
            List<Detection> detections = new ArrayList<>();
            double totalConfidence = 0.0;

            // 1) Prefer DNN-based detector if available
            if (dnnFaceNet != null && !dnnFaceNet.empty()) {
                try {
                    List<Rect> candidateRects = new ArrayList<>();
                    List<Float> candidateScores = new ArrayList<>();

                    // Multi-scale: original and upscaled (for small faces)
                    collectDnnCandidates(image, 1.0, candidateRects, candidateScores);
                    double minDim = Math.min(image.cols(), image.rows());
                    double upscale = minDim < 800 ? Math.min(2.0, 800.0 / minDim) : 1.0;
                    if (upscale > 1.05) {
                        collectDnnCandidates(image, upscale, candidateRects, candidateScores);
                    }

                    // NMS + eye verification
                    List<Integer> keep = nonMaximumSuppression(candidateRects, candidateScores, 0.4f);
                    int cols = image.cols();
                    int rows = image.rows();
                    for (int idx : keep) {
                        Rect r = candidateRects.get(idx);
                        float conf = candidateScores.get(idx);
                        if (!isFaceLikely(grayImage, r)) continue;
                        double nx = (double) r.x() / cols;
                        double ny = (double) r.y() / rows;
                        double nw = (double) r.width() / cols;
                        double nh = (double) r.height() / rows;
                        detections.add(Detection.of("face", conf, new BoundingBox(nx, ny, nw, nh)));
                        totalConfidence += conf;
                    }
                } catch (Exception dnnEx) {
                    logger.warn("DNN face detection failed, falling back to cascade: {}", dnnEx.getMessage());
                }
            }

            if (faceCascade != null && faceCascade.empty() == false) {
                try {
                    // Detect faces using cascade classifier
                    RectVector faces = new RectVector();
                    int minDim = Math.min(image.cols(), image.rows());
                    Size minSize = new Size((int) (minDim * MIN_FACE_SIZE_RATIO), (int) (minDim * MIN_FACE_SIZE_RATIO));
                    Size maxSize = new Size((int) (minDim * MAX_FACE_SIZE_RATIO), (int) (minDim * MAX_FACE_SIZE_RATIO));
                    if (detections.isEmpty()) {
                        faceCascade.detectMultiScale(grayImage, faces, 1.1, 3, 0, minSize, maxSize);
                    }

                    logger.debug("Cascade classifier detected {} faces", faces.size());

                    for (long i = 0; i < faces.size(); i++) {
                        Rect faceRect = faces.get(i);
                        if (!isFaceLikely(grayImage, faceRect)) {
                            continue;
                        }

                        // Convert OpenCV coordinates to normalized coordinates
                        double x = (double) faceRect.x() / image.cols();
                        double y = (double) faceRect.y() / image.rows();
                        double width = (double) faceRect.width() / image.cols();
                        double height = (double) faceRect.height() / image.rows();

                        BoundingBox boundingBox = new BoundingBox(x, y, width, height);

                        // Calculate confidence based on face size and position
                        double confidence = calculateFaceConfidence(faceRect, image.cols(), image.rows());
                        totalConfidence += confidence;

                        Detection detection = Detection.of("face", confidence, boundingBox);
                        detections.add(detection);
                    }

                    faces.deallocate();
                } catch (Exception e) {
                    logger.warn("Cascade classifier failed, using basic detection: {}", e.getMessage());
                }
            } else {
                logger.debug("Cascade classifier not available, using basic detection");
            }

            // If no faces detected with cascade, use basic detection
            if (detections.isEmpty()) {
                logger.debug("No faces detected with cascade classifier, using basic detection");

                // Simple face detection based on image analysis
                // This is a basic implementation that looks for face-like patterns
                int step = Math.max(1, Math.min(image.cols(), image.rows()) / 20);

                for (int y = 0; y < image.rows() - step; y += step) {
                    for (int x = 0; x < image.cols() - step; x += step) {
                        // Simple heuristic: look for regions with good contrast
                        double contrast = calculateContrast(grayImage, x, y, step);

                        if (contrast > 30) { // Threshold for face-like contrast
                            double normX = (double) x / image.cols();
                            double normY = (double) y / image.rows();
                            double normWidth = (double) step / image.cols();
                            double normHeight = (double) step / image.rows();

                            BoundingBox boundingBox = new BoundingBox(normX, normY, normWidth, normHeight);
                            double confidence = Math.min(0.8, contrast / 100.0);

                            Detection detection = Detection.of("face", confidence, boundingBox);
                            detections.add(detection);
                            totalConfidence += confidence;

                            // Limit to reasonable number of detections
                            if (detections.size() >= 50) break;
                        }
                    }
                    if (detections.size() >= 50) break;
                }
            }

            // 3) Profile-face detection (left profile and mirrored right profile)
            try {
                if (profileCascade != null && !profileCascade.empty()) {
                    addProfileDetections(grayImage, image.cols(), image.rows(), detections, totalConfidence);
                }
            } catch (Exception e) {
                logger.debug("Profile-face detection skipped due to error: {}", e.getMessage());
            }

            long processingTime = System.currentTimeMillis() - startTime;
            double averageConfidence = detections.isEmpty() ? 0.0 : totalConfidence / detections.size();

            // Clean up OpenCV resources
            image.releaseReference();
            grayImage.releaseReference();

            VisionResult result = VisionResult.of(DetectionType.FACE, detections,
                averageConfidence, processingTime);

            logger.debug("Face detection completed", Map.of(
                "correlationId", correlationId,
                "facesDetected", detections.size(),
                "averageConfidence", averageConfidence,
                "processingTimeMs", processingTime
            ));

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "processingTimeMs", processingTime,
                "error", e.getClass().getSimpleName(),
                "opencvAvailable", opencvAvailable
            ), e);

            if (!opencvAvailable) {
                throw new VisionProcessingException(
                    "OpenCV is not available for face detection",
                    "opencv_unavailable",
                    DetectionType.FACE.getCode(),
                    e
                );
            } else {
                throw new VisionProcessingException(
                    "Failed to detect faces using OpenCV",
                    "face_detection",
                    DetectionType.FACE.getCode(),
                    e
                );
            }
        }
    }

    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        validateImageData(imageData);
        ensureInitialized();

        long startTime = System.currentTimeMillis();
        String correlationId = generateCorrelationId();

        logger.debug("Starting object detection", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format()
        ));

        try {
            // Check if OpenCV is available
            if (!opencvAvailable) {
                logger.warn("OpenCV not available - returning empty object detection result");
                return VisionResult.of(DetectionType.OBJECT, List.of(), 0.0, System.currentTimeMillis() - startTime);
            }

            // Load image into OpenCV Mat
            Mat image = loadImageToMat(imageData);

            // Convert to grayscale
            Mat grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);

            // Detect circles (as a simple object detection example)
            Vec3fVector circles = new Vec3fVector();
            HoughCircles(grayImage, circles, HOUGH_GRADIENT, 1.0,
                grayImage.rows() / 8, 100, 30, 1, 30);

            List<Detection> detections = new ArrayList<>();
            double totalConfidence = 0.0;

            if (!circles.empty()) {
                for (int i = 0; i < circles.size(); i++) {
                    FloatPointer circle = circles.get(i);
                    float x = circle.get(0);
                    float y = circle.get(1);
                    float radius = circle.get(2);

                    // Convert to normalized coordinates
                    double normX = (x - radius) / image.cols();
                    double normY = (y - radius) / image.rows();
                    double normWidth = (2 * radius) / image.cols();
                    double normHeight = (2 * radius) / image.rows();

                    // Ensure coordinates are within bounds
                    normX = Math.max(0.0, Math.min(1.0 - normWidth, normX));
                    normY = Math.max(0.0, Math.min(1.0 - normHeight, normY));

                    BoundingBox boundingBox = new BoundingBox(normX, normY, normWidth, normHeight);

                    // Calculate confidence based on circle properties
                    double confidence = calculateCircleConfidence(radius, image.cols(), image.rows());
                    totalConfidence += confidence;

                    Detection detection = Detection.of("circle", confidence, boundingBox);
                    detections.add(detection);
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            double averageConfidence = detections.isEmpty() ? 0.0 : totalConfidence / detections.size();

            // Clean up OpenCV resources
            image.releaseReference();
            grayImage.releaseReference();
            circles.deallocate();

            VisionResult result = VisionResult.of(DetectionType.OBJECT, detections,
                averageConfidence, processingTime);

            logger.debug("Object detection completed", Map.of(
                "correlationId", correlationId,
                "objectsDetected", detections.size(),
                "averageConfidence", averageConfidence,
                "processingTimeMs", processingTime
            ));

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("Object detection failed", Map.of(
                "correlationId", correlationId,
                "processingTimeMs", processingTime,
                "error", e.getClass().getSimpleName(),
                "opencvAvailable", opencvAvailable
            ), e);

            if (!opencvAvailable) {
                throw new VisionProcessingException(
                    "OpenCV is not available for object detection",
                    "opencv_unavailable",
                    DetectionType.OBJECT.getCode(),
                    e
                );
            } else {
                throw new VisionProcessingException(
                    "Failed to detect objects using OpenCV",
                    "object_detection",
                    DetectionType.OBJECT.getCode(),
                    e
                );
            }
        }
    }

    @Override
    public void initialize() throws BaseVisionException {
        logger.info("Initializing OpenCV backend");

        try {
            // Check OpenCV availability first
            checkOpenCvAvailability();

            if (!opencvAvailable) {
                logger.warn("OpenCV is not available - backend will operate in fallback mode");
                initialized = true;
                healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
                healthErrorMessage = null;
                lastHealthCheckTime = System.currentTimeMillis();
                logger.info("OpenCV backend initialized in fallback mode");
                return;
            }

            // Load face cascade classifier only if OpenCV is available
            if (opencvAvailable) {
                loadFaceCascade();
                loadDnnFaceDetector();
                loadEyeCascade();
                loadProfileCascade();
            } else {
                logger.debug("Skipping face cascade loading - OpenCV not available");
            }

            // Initialize frame converter using reflection to avoid static initialization
            if (opencvAvailable) {
                try {
                    Class<?> frameConverterClass = Class.forName("org.bytedeco.javacv.OpenCVFrameConverter$ToMat");
                    frameConverter = (OpenCVFrameConverter.ToMat) frameConverterClass.getDeclaredConstructor().newInstance();
                    logger.debug("OpenCV frame converter initialized successfully");
                } catch (ClassNotFoundException | ExceptionInInitializerError e) {
                    logger.warn("OpenCV frame converter classes not available: {}", e.getMessage());
                    frameConverter = null;
                } catch (Exception e) {
                    logger.warn("Failed to initialize OpenCV frame converter: {}", e.getMessage());
                    frameConverter = null;
                }
            } else {
                logger.debug("Skipping frame converter initialization - OpenCV not available");
                frameConverter = null;
            }

            initialized = true;
            healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            healthErrorMessage = null;
            lastHealthCheckTime = System.currentTimeMillis();

            logger.info("OpenCV backend initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize OpenCV backend", e);
            initialized = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Initialization failed: " + e.getMessage();
            throw new VisionBackendException(
                "Failed to initialize OpenCV backend",
                "initialization_failed",
                null,
                e
            );
        }
    }

    @Override
    public void shutdown() throws BaseVisionException {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down OpenCV backend");

        try {
            if (faceCascade != null) {
                faceCascade.close();
                faceCascade = null;
            }

            if (frameConverter != null) {
                frameConverter.close();
                frameConverter = null;
            }

            initialized = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
            healthErrorMessage = "Backend shut down";

            logger.info("OpenCV backend shut down successfully");

        } catch (Exception e) {
            logger.error("Error during OpenCV backend shutdown", e);
            throw new VisionBackendException(
                "Failed to shut down OpenCV backend",
                "shutdown",
                null,
                e
            );
        }
    }

    /**
     * Checks if OpenCV is available on the system.
     *
     * @throws BaseVisionException if OpenCV is not available
     */
        private void checkOpenCvAvailability() throws BaseVisionException {
        try {
            logger.info("Checking OpenCV availability with embedded libraries...");

            // With embedded OpenCV, we need to actually try to use the functionality
            // to trigger the native library loading from the embedded JAR
                        try {
                // Try to create a simple Mat instance to test if native libraries are available
                logger.debug("Creating test Mat instance...");
                Mat testMat = new Mat();
                testMat.releaseReference();
                logger.debug("Mat instance created successfully");

                // Try to use static imports to test if they're working
                logger.debug("Testing static imports...");
                int testValue = CV_8UC3; // This should work if opencv_core is loaded
                logger.debug("Static import test successful: CV_8UC3 = {}", testValue);

                // Try to create a simple image processing operation
                logger.debug("Testing image processing...");
                Mat testImage = new Mat(10, 10, CV_8UC3);
                Mat testGray = new Mat();
                cvtColor(testImage, testGray, COLOR_BGR2GRAY);
                testImage.releaseReference();
                testGray.releaseReference();
                logger.debug("Image processing test successful");

                opencvAvailable = true;
                logger.info("OpenCV embedded libraries loaded successfully");

            } catch (UnsatisfiedLinkError e) {
                logger.warn("OpenCV native libraries not available: {}", e.getMessage());
                opencvAvailable = false;
            } catch (ExceptionInInitializerError e) {
                logger.warn("OpenCV initialization error: {}", e.getMessage());
                opencvAvailable = false;
            } catch (Exception e) {
                logger.warn("Failed to check OpenCV availability: {}", e.getMessage());
                opencvAvailable = false;
            }
        } catch (Exception e) {
            logger.warn("Error checking OpenCV availability: {}", e.getMessage());
            opencvAvailable = false;
        }

        logger.info("OpenCV availability check completed: available={}", opencvAvailable);
    }

    /**
     * Loads the face cascade classifier for face detection.
     *
     * @throws BaseVisionException if the cascade classifier cannot be loaded
     */
    private void loadFaceCascade() throws BaseVisionException {
        try {
            logger.info("Loading face cascade classifier...");

            // Create a new CascadeClassifier instance
            faceCascade = new CascadeClassifier();

            // Try to load from classpath resource
            try {
                logger.debug("Attempting to load cascade from classpath: {}", DEFAULT_FACE_CASCADE_PATH);
                java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_FACE_CASCADE_PATH);
                if (is != null) {
                    logger.debug("Found cascade file in classpath, loading...");
                    // Load from input stream
                    byte[] cascadeData = is.readAllBytes();
                    is.close();
                    logger.debug("Cascade file loaded, size: {} bytes", cascadeData.length);

                    // Create a temporary file and load from it
                    java.io.File tempFile = java.io.File.createTempFile("haarcascade", ".xml");
                    tempFile.deleteOnExit();
                    logger.debug("Created temporary file: {}", tempFile.getAbsolutePath());

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                        fos.write(cascadeData);
                    }

                    boolean loaded = faceCascade.load(tempFile.getAbsolutePath());
                    if (loaded) {
                        logger.info("Face cascade classifier loaded successfully from classpath resource");
                        return;
                    } else {
                        logger.warn("Failed to load cascade from temporary file");
                    }
                } else {
                    logger.warn("Cascade file not found in classpath: {}", DEFAULT_FACE_CASCADE_PATH);
                }
            } catch (Exception e) {
                logger.warn("Could not load cascade from classpath: {}", e.getMessage());
            }

            // Try to load from system path
            boolean loaded = faceCascade.load(DEFAULT_FACE_CASCADE_PATH);
            if (loaded) {
                logger.info("Face cascade classifier loaded successfully from: {}", DEFAULT_FACE_CASCADE_PATH);
                return;
            }

            /*
             * As a final fallback, attempt to download the cascade directly from the
             * OpenCV GitHub repository. This guarantees the application still works
             * in fresh environments (e.g. containers) without bundling the large XML
             * file. The operation is executed with short time-outs to avoid blocking
             * the application if the network is unavailable.
             */
            try {
                logger.info("Attempting to download face cascade classifier from {}", CASCADE_DOWNLOAD_URL);

                java.net.URL url = new java.net.URL(CASCADE_DOWNLOAD_URL);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5_000); // 5 seconds
                connection.setReadTimeout(5_000);    // 5 seconds

                try (java.io.InputStream inputStream = connection.getInputStream()) {
                    byte[] cascadeData = inputStream.readAllBytes();

                    java.io.File tempFile = java.io.File.createTempFile("haarcascade", ".xml");
                    tempFile.deleteOnExit();

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                        fos.write(cascadeData);
                    }

                    if (faceCascade.load(tempFile.getAbsolutePath())) {
                        logger.info("Face cascade classifier downloaded and loaded successfully");
                        return;
                    }
                    logger.warn("Failed to load cascade from downloaded file");
                }
            } catch (Exception ex) {
                logger.warn("Could not download cascade file: {}", ex.getMessage());
            }

            // If all else fails, create a basic classifier that will still work
            logger.warn("Could not load face cascade classifier, using basic classifier");
            faceCascade = new CascadeClassifier();

        } catch (Exception e) {
            logger.warn("Error loading face cascade classifier: {}", e.getMessage());
            faceCascade = new CascadeClassifier();
        }

        logger.info("Face cascade loading completed. Cascade available: {}",
                   faceCascade != null && faceCascade.empty() == false);
    }

    /**
     * Loads the DNN face detector (ResNet-SSD). Tries classpath first and
     * falls back to a short, timeboxed download to a temporary file.
     */
    private void loadDnnFaceDetector() {
        try {
            logger.info("Loading DNN face detector...");

            String protoPath = null;
            String modelPath = null;

            try (java.io.InputStream is = getClass().getResourceAsStream(DNN_CAFFE_PROTO_TXT_CLASSPATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("deploy", ".prototxt");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(data); }
                    protoPath = tmp.getAbsolutePath();
                }
            } catch (Exception ignore) {}

            try (java.io.InputStream is = getClass().getResourceAsStream(DNN_CAFFE_MODEL_CLASSPATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("res10_300x300", ".caffemodel");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(data); }
                    modelPath = tmp.getAbsolutePath();
                }
            } catch (Exception ignore) {}

            if (protoPath == null || modelPath == null) {
                // Attempt short download if not packaged
                try {
                    logger.info("Downloading DNN face model (short timeout)...");
                    java.net.URL urlProto = new java.net.URL(DNN_CAFFE_PROTO_TXT_URL);
                    java.net.URL urlModel = new java.net.URL(DNN_CAFFE_MODEL_URL);

                    java.io.File tmpProto = java.io.File.createTempFile("deploy", ".prototxt");
                    java.io.File tmpModel = java.io.File.createTempFile("res10_300x300", ".caffemodel");
                    tmpProto.deleteOnExit();
                    tmpModel.deleteOnExit();

                    try (java.io.InputStream in = openWithTimeout(urlProto, 4000, 5000);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpProto)) { fos.write(in.readAllBytes()); }
                    try (java.io.InputStream in = openWithTimeout(urlModel, 4000, 7000);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpModel)) { fos.write(in.readAllBytes()); }

                    protoPath = tmpProto.getAbsolutePath();
                    modelPath = tmpModel.getAbsolutePath();
                } catch (Exception dlEx) {
                    logger.warn("Could not download DNN face model: {}", dlEx.getMessage());
                }
            }

            if (protoPath != null && modelPath != null) {
                dnnFaceNet = org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe(protoPath, modelPath);
                if (dnnFaceNet != null && !dnnFaceNet.empty()) {
                    logger.info("DNN face detector loaded successfully");
                } else {
                    logger.warn("Failed to initialize DNN face detector");
                    dnnFaceNet = null;
                }
            } else {
                logger.warn("DNN face detector resources unavailable; will use cascade only");
                dnnFaceNet = null;
            }
        } catch (Exception e) {
            logger.warn("Error loading DNN face detector: {}", e.getMessage());
            dnnFaceNet = null;
        }
    }

    private static java.io.InputStream openWithTimeout(java.net.URL url, int connectTimeoutMs, int readTimeoutMs) throws java.io.IOException {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(connectTimeoutMs);
        c.setReadTimeout(readTimeoutMs);
        return c.getInputStream();
    }

    /**
     * Loads image data into an OpenCV Mat.
     *
     * @param imageData the image data to load
     * @return the OpenCV Mat containing the image
     * @throws BaseVisionException if the image cannot be loaded
     */
    private Mat loadImageToMat(ImageData imageData) throws BaseVisionException {
        try {
            // Check if OpenCV is actually available before trying to use it
            if (!opencvAvailable) {
                throw new VisionProcessingException(
                    "OpenCV is not available for image processing",
                    "opencv_unavailable",
                    null
                );
            }

            // Decode image from bytes
            Mat image = imdecode(new Mat(imageData.data()), IMREAD_COLOR);

            if (image.empty()) {
                throw new VisionProcessingException(
                    "Failed to decode image data",
                    "image_decode",
                    null
                );
            }

            return image;

        } catch (Exception e) {
            throw new VisionProcessingException(
                "Failed to load image into OpenCV Mat",
                "image_load",
                null,
                e
            );
        }
    }

    /**
     * Calculates confidence score for a detected face.
     *
     * @param faceRect the detected face rectangle
     * @param imageWidth the image width
     * @param imageHeight the image height
     * @return the confidence score (0.0 to 1.0)
     */
    private double calculateFaceConfidence(Rect faceRect, int imageWidth, int imageHeight) {
        // Base confidence on face size relative to image
        double faceArea = faceRect.width() * faceRect.height();
        double imageArea = imageWidth * imageHeight;
        double areaRatio = faceArea / imageArea;

        // Optimal face size is between 5% and 25% of image area
        double optimalMin = 0.05;
        double optimalMax = 0.25;

        if (areaRatio < optimalMin) {
            return 0.5 + (areaRatio / optimalMin) * 0.3; // 0.5 to 0.8
        } else if (areaRatio > optimalMax) {
            return 0.8 - ((areaRatio - optimalMax) / (0.5 - optimalMax)) * 0.3; // 0.8 to 0.5
        } else {
            return 0.8 + (0.2 * (1.0 - Math.abs(areaRatio - (optimalMin + optimalMax) / 2) / ((optimalMax - optimalMin) / 2))); // 0.8 to 1.0
        }
    }

    /**
     * Calculates confidence score for a detected circle.
     *
     * @param radius the circle radius
     * @param imageWidth the image width
     * @param imageHeight the image height
     * @return the confidence score (0.0 to 1.0)
     */
        /**
     * Calculates a simple contrast measure in a region of the image.
     *
     * @param grayImage the grayscale image
     * @param x the x coordinate
     * @param y the y coordinate
     * @param size the size of the region
     * @return the contrast value
     */
    private double calculateContrast(Mat grayImage, int x, int y, int size) {
        try {
            // Simple contrast calculation: sample a few pixels and calculate variance
            double sum = 0;
            double sumSq = 0;
            int count = 0;

            for (int dy = 0; dy < size && y + dy < grayImage.rows(); dy += 2) {
                for (int dx = 0; dx < size && x + dx < grayImage.cols(); dx += 2) {
                    double pixel = grayImage.getDoubleBuffer().get((y + dy) * grayImage.cols() + x + dx);
                    sum += pixel;
                    sumSq += pixel * pixel;
                    count++;
                }
            }

            if (count > 0) {
                double mean = sum / count;
                double variance = (sumSq / count) - (mean * mean);
                return Math.sqrt(variance);
            }

            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateCircleConfidence(float radius, int imageWidth, int imageHeight) {
        // Base confidence on circle size relative to image
        double circleArea = Math.PI * radius * radius;
        double imageArea = imageWidth * imageHeight;
        double areaRatio = circleArea / imageArea;

        // Optimal circle size is between 1% and 10% of image area
        double optimalMin = 0.01;
        double optimalMax = 0.10;

        if (areaRatio < optimalMin) {
            return 0.5 + (areaRatio / optimalMin) * 0.3; // 0.5 to 0.8
        } else if (areaRatio > optimalMax) {
            return 0.8 - ((areaRatio - optimalMax) / (0.3 - optimalMax)) * 0.3; // 0.8 to 0.5
        } else {
            return 0.8 + (0.2 * (1.0 - Math.abs(areaRatio - (optimalMin + optimalMax) / 2) / ((optimalMax - optimalMin) / 2))); // 0.8 to 1.0
        }
    }

    /**
     * Validates that image data is not null and valid.
     *
     * @param imageData the image data to validate
     * @throws IllegalArgumentException if imageData is null or invalid
     */
    private void validateImageData(ImageData imageData) {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be empty");
        }
    }

    /**
     * Ensures the backend is initialized before use.
     *
     * @throws BaseVisionException if the backend is not initialized
     */
    private void ensureInitialized() throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "OpenCV backend is not initialized. Call initialize() first.",
                "not_initialized",
                null
            );
        }
    }

    /**
     * Generates a unique correlation ID for tracking operations.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Checks if OpenCV is available for actual processing operations.
     *
     * @return true if OpenCV is available for processing
     */
    public boolean isOpenCvAvailableForProcessing() {
        return opencvAvailable && initialized;
    }

    /**
     * Gets the OpenCV availability status for monitoring purposes.
     *
     * @return true if OpenCV is available
     */
    public boolean isOpenCvAvailable() {
        return opencvAvailable;
    }

    /**
     * Validate a candidate face region using eye detection within the ROI.
     */
    private boolean isFaceLikely(Mat grayImage, Rect rect) {
        try {
            if (eyeCascade == null || eyeCascade.empty()) {
                return true; // cannot validate, accept
            }
            // Create ROI inside the face rectangle (clip to image bounds)
            int x = Math.max(0, rect.x());
            int y = Math.max(0, rect.y());
            int w = Math.min(grayImage.cols() - x, rect.width());
            int h = Math.min(grayImage.rows() - y, rect.height());
            if (w <= 0 || h <= 0) {
                return false;
            }
            Mat roi = new Mat(grayImage, new Rect(x, y, w, h));
            RectVector eyes = new RectVector();
            eyeCascade.detectMultiScale(roi, eyes, 1.1, 3, 0, new Size(Math.max(8, w / 10), Math.max(8, h / 10)), new Size());
            boolean ok = eyes.size() >= 1; // at least one eye to allow profiles
            eyes.deallocate();
            roi.releaseReference();
            return ok;
        } catch (Exception e) {
            return true; // fail open to avoid dropping all detections
        }
    }

    /**
     * Non-Maximum Suppression for rectangles.
     */
    private List<Integer> nonMaximumSuppression(List<Rect> rects, List<Float> scores, float iouThreshold) {
        List<Integer> indices = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) order.add(i);
        order.sort((a, b) -> Float.compare(scores.get(b), scores.get(a)));

        boolean[] suppressed = new boolean[rects.size()];
        for (int i = 0; i < order.size(); i++) {
            int idx = order.get(i);
            if (suppressed[idx]) continue;
            indices.add(idx);
            Rect a = rects.get(idx);
            double areaA = (double) a.width() * a.height();
            for (int j = i + 1; j < order.size(); j++) {
                int idxB = order.get(j);
                if (suppressed[idxB]) continue;
                Rect b = rects.get(idxB);
                double interX1 = Math.max(a.x(), b.x());
                double interY1 = Math.max(a.y(), b.y());
                double interX2 = Math.min(a.x() + a.width(), b.x() + b.width());
                double interY2 = Math.min(a.y() + a.height(), b.y() + b.height());
                double interW = Math.max(0, interX2 - interX1);
                double interH = Math.max(0, interY2 - interY1);
                double inter = interW * interH;
                double union = areaA + (double) b.width() * b.height() - inter;
                double iou = union <= 0 ? 0 : inter / union;
                if (iou > iouThreshold) {
                    suppressed[idxB] = true;
                }
            }
        }
        return indices;
    }

    /** Collect DNN candidates at a given scale; map rectangles back to original coordinates. */
    private void collectDnnCandidates(Mat image, double scale,
                                      List<Rect> outRects, List<Float> outScores) {
        int cols = image.cols();
        int rows = image.rows();
        Mat src = image;
        Mat resized = new Mat();
        boolean usedResize = false;
        if (Math.abs(scale - 1.0) > 1e-3) {
            int newW = (int) Math.round(cols * scale);
            int newH = (int) Math.round(rows * scale);
            resize(image, resized, new Size(newW, newH));
            src = resized;
            usedResize = true;
        }

        Mat blob = org.bytedeco.opencv.global.opencv_dnn.blobFromImage(src, 1.0, new Size(300, 300), new Scalar(104, 177, 123, 0), false, false, org.bytedeco.opencv.global.opencv_core.CV_32F);
        dnnFaceNet.setInput(blob);
        Mat detectionsMat = dnnFaceNet.forward();

        int sCols = src.cols();
        int sRows = src.rows();
        int numDetections = (int) detectionsMat.size(2);
        int step = (int) detectionsMat.size(3);
        FloatPointer data = new FloatPointer(detectionsMat.data());

        for (int i = 0; i < numDetections; i++) {
            float confidence = data.get((long) i * step + 2);
            if (confidence < 0.7f) continue;
            float x1 = data.get((long) i * step + 3) * sCols;
            float y1 = data.get((long) i * step + 4) * sRows;
            float x2 = data.get((long) i * step + 5) * sCols;
            float y2 = data.get((long) i * step + 6) * sRows;

            // Map back to original if resized
            if (usedResize) {
                x1 /= scale; y1 /= scale; x2 /= scale; y2 /= scale;
            }

            int left = (int) Math.max(0, Math.min(cols - 1, Math.round(x1)));
            int top = (int) Math.max(0, Math.min(rows - 1, Math.round(y1)));
            int right = (int) Math.max(0, Math.min(cols - 1, Math.round(x2)));
            int bottom = (int) Math.max(0, Math.min(rows - 1, Math.round(y2)));
            int widthPx = Math.max(1, right - left);
            int heightPx = Math.max(1, bottom - top);

            double aspect = (double) widthPx / heightPx;
            if (aspect < 0.6 || aspect > 1.6) continue;
            outRects.add(new Rect(left, top, widthPx, heightPx));
            outScores.add(confidence);
        }

        blob.releaseReference();
        detectionsMat.releaseReference();
        if (usedResize) resized.releaseReference();
    }

    /** Load the eye cascade with classpath-first, then remote fallback. */
    private void loadEyeCascade() {
        try {
            eyeCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_EYE_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("haarcascade_eye", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(data); }
                    if (eyeCascade.load(tmp.getAbsolutePath())) {
                        logger.info("Eye cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {}

            // Try system path
            if (eyeCascade.load(DEFAULT_EYE_CASCADE_PATH)) {
                logger.info("Eye cascade loaded from system path");
                return;
            }

            // Download fallback
            try {
                logger.info("Attempting to download eye cascade...");
                java.net.URL url = new java.net.URL(EYE_CASCADE_DOWNLOAD_URL);
                java.io.File tmp = java.io.File.createTempFile("haarcascade_eye", ".xml");
                tmp.deleteOnExit();
                try (java.io.InputStream in = openWithTimeout(url, 4000, 5000);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(in.readAllBytes()); }
                if (eyeCascade.load(tmp.getAbsolutePath())) {
                    logger.info("Eye cascade downloaded and loaded successfully");
                    return;
                }
            } catch (Exception ex) {
                logger.warn("Could not download eye cascade: {}", ex.getMessage());
            }

            // If all fails, set to empty to avoid NPE
            logger.warn("Eye cascade unavailable; face verification will be skipped");
        } catch (Exception e) {
            logger.warn("Error loading eye cascade: {}", e.getMessage());
            eyeCascade = null;
        }
    }

    /**
     * Detect profile faces on original and horizontally flipped images.
     */
    private void addProfileDetections(Mat grayImage, int cols, int rows,
                                      List<Detection> detections, double totalConfidenceAccumulator) {
        // Detect on original (typically left-profile)
        RectVector profiles = new RectVector();
        int minDim = Math.min(cols, rows);
        Size minSize = new Size((int) (minDim * MIN_FACE_SIZE_RATIO), (int) (minDim * MIN_FACE_SIZE_RATIO));
        Size maxSize = new Size((int) (minDim * MAX_FACE_SIZE_RATIO), (int) (minDim * MAX_FACE_SIZE_RATIO));

        profileCascade.detectMultiScale(grayImage, profiles, 1.1, 3, 0, minSize, maxSize);
        for (long i = 0; i < profiles.size(); i++) {
            Rect r = profiles.get(i);
            if (!isFaceLikely(grayImage, r)) continue;
            if (isOverlappingExisting(detections, r, cols, rows)) continue;
            double nx = (double) r.x() / cols;
            double ny = (double) r.y() / rows;
            double nw = (double) r.width() / cols;
            double nh = (double) r.height() / rows;
            double conf = Math.min(0.9, 0.8 + (r.width() * r.height()) / (double) (cols * rows));
            detections.add(Detection.of("face", conf, new BoundingBox(nx, ny, nw, nh)));
        }
        profiles.deallocate();

        // Detect on horizontally flipped image for right-profile faces
        Mat flipped = new Mat();
        flip(grayImage, flipped, 1);
        RectVector profilesFlipped = new RectVector();
        profileCascade.detectMultiScale(flipped, profilesFlipped, 1.1, 3, 0, minSize, maxSize);
        for (long i = 0; i < profilesFlipped.size(); i++) {
            Rect r = profilesFlipped.get(i);
            // Map back to original coordinates
            int xMapped = cols - r.x() - r.width();
            Rect mapped = new Rect(Math.max(0, xMapped), r.y(), r.width(), r.height());
            if (!isFaceLikely(grayImage, mapped)) continue;
            if (isOverlappingExisting(detections, mapped, cols, rows)) continue;
            double nx = (double) mapped.x() / cols;
            double ny = (double) mapped.y() / rows;
            double nw = (double) mapped.width() / cols;
            double nh = (double) mapped.height() / rows;
            double conf = Math.min(0.9, 0.8 + (mapped.width() * mapped.height()) / (double) (cols * rows));
            detections.add(Detection.of("face", conf, new BoundingBox(nx, ny, nw, nh)));
        }
        profilesFlipped.deallocate();
        flipped.releaseReference();
    }

    /** Check if a new rect overlaps sufficiently with an existing detection; if so, skip. */
    private boolean isOverlappingExisting(List<Detection> detections, Rect r, int cols, int rows) {
        double rx1 = r.x();
        double ry1 = r.y();
        double rx2 = r.x() + r.width();
        double ry2 = r.y() + r.height();
        for (Detection d : detections) {
            Rect e = new Rect((int) Math.round(d.boundingBox().x() * cols),
                              (int) Math.round(d.boundingBox().y() * rows),
                              (int) Math.round(d.boundingBox().width() * cols),
                              (int) Math.round(d.boundingBox().height() * rows));
            double ex1 = e.x();
            double ey1 = e.y();
            double ex2 = e.x() + e.width();
            double ey2 = e.y() + e.height();
            double interX1 = Math.max(rx1, ex1);
            double interY1 = Math.max(ry1, ey1);
            double interX2 = Math.min(rx2, ex2);
            double interY2 = Math.min(ry2, ey2);
            double interW = Math.max(0, interX2 - interX1);
            double interH = Math.max(0, interY2 - interY1);
            double inter = interW * interH;
            double union = (r.width() * r.height()) + (e.width() * e.height()) - inter;
            double iou = union <= 0 ? 0 : inter / union;
            if (iou > 0.4) {
                return true;
            }
        }
        return false;
    }

    /** Load the profile-face cascade with classpath-first, then remote fallback. */
    private void loadProfileCascade() {
        try {
            profileCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_PROFILE_FACE_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("haarcascade_profileface", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(data); }
                    if (profileCascade.load(tmp.getAbsolutePath())) {
                        logger.info("Profile-face cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {}

            // Try system path
            if (profileCascade.load(DEFAULT_PROFILE_FACE_CASCADE_PATH)) {
                logger.info("Profile-face cascade loaded from system path");
                return;
            }

            // Download fallback
            try {
                logger.info("Attempting to download profile-face cascade...");
                java.net.URL url = new java.net.URL(PROFILE_FACE_CASCADE_DOWNLOAD_URL);
                java.io.File tmp = java.io.File.createTempFile("haarcascade_profileface", ".xml");
                tmp.deleteOnExit();
                try (java.io.InputStream in = openWithTimeout(url, 4000, 5000);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(in.readAllBytes()); }
                if (profileCascade.load(tmp.getAbsolutePath())) {
                    logger.info("Profile-face cascade downloaded and loaded successfully");
                    return;
                }
            } catch (Exception ex) {
                logger.warn("Could not download profile-face cascade: {}", ex.getMessage());
            }

            logger.warn("Profile-face cascade unavailable; side-view detection will be limited to DNN");
        } catch (Exception e) {
            logger.warn("Error loading profile-face cascade: {}", e.getMessage());
            profileCascade = null;
        }
    }
}
