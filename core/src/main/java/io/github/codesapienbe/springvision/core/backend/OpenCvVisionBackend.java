package io.github.codesapienbe.springvision.core.backend;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.codesapienbe.springvision.core.capabilities.AnnotationCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceVerificationCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceLookupCapability;
import jakarta.annotation.PreDestroy;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
// Custom implementations to replace IntVector and FloatVector
// These classes don't exist in current OpenCV JavaCV version
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
 * @see VisionBackend
 * @see VisionTemplate
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.opencv")
@ConditionalOnProperty(prefix = "spring.vision.opencv", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenCvVisionBackend implements VisionBackend, FaceDetectionCapability, ObjectDetectionCapability,
    AnnotationCapability, FaceVerificationCapability, FaceLookupCapability {

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
        "https://raw.githubusercontent.com/opencv_3rdparty/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel";
    private static final String DNN_CAFFE_MODEL_URL_ALT =
        "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel";

    /**
     * OpenCV Zoo YuNet and SFace ONNX resources.
     */
    private static final String YUNET_ONNX_CLASSPATH = "/models/face_detection_yunet_2023mar.onnx";
    private static final String YUNET_ONNX_URL =
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx";
    private static final String SFACE_ONNX_CLASSPATH = "/models/face_recognition_sface_2021dec.onnx";
    private static final String SFACE_ONNX_URL =
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx";

    /**
     * Eye cascade to validate candidate faces (reduces rectangular false positives).
     */
    private static final String DEFAULT_EYE_CASCADE_PATH = "/haarcascade_eye.xml";
    private static final String EYE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml";

    /**
     * Profile-face cascade to detect side-view faces.
     */
    private static final String DEFAULT_PROFILE_FACE_CASCADE_PATH = "/haarcascade_profileface.xml";
    private static final String PROFILE_FACE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_profileface.xml";

    /**
     * LBP cascade often performs better across diverse skin tones.
     */
    private static final String DEFAULT_LBP_CASCADE_PATH = "/lbpcascade_frontalface.xml";
    private static final String LBP_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/lbpcascades/lbpcascade_frontalface.xml";

    /**
     * Default confidence threshold for detections.
     */
    @SuppressWarnings("unused")
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
     * Fail-safe guardrail for incoming image payload to avoid memory/DoS issues.
     */
    @SuppressWarnings("unused")
    private static final long MAX_SAFE_IMAGE_BYTES = 50L * 1024 * 1024; // 50MB
    /**
     * Additional fail-safe for very large images that may cause native OOM during
     * OpenCV operations regardless of byte size (e.g., highly compressed but huge resolution).
     * Values chosen conservatively for desktop usage while keeping good accuracy.
     */
    @SuppressWarnings("unused")
    private static final int MAX_SAFE_DIMENSION = 4000;     // Cap either width or height to 4K

    // Configuration properties
    private boolean enabled = true;
    private double confidenceThreshold = 0.8;
    private int maxDetections = 10;
    private boolean enableAutoDownload = true;
    private int downloadTimeoutSeconds = 30;
    private String modelPath = "~/.spring-vision/models/opencv";
    private int maxPoolSize = 5;
    private int poolTimeoutSeconds = 60;

    /**
     * Face detection cascade classifier.
     */
    private CascadeClassifier faceCascade;
    private ThreadLocal<Net> dnnFaceNet;
    private CascadeClassifier eyeCascade;
    private CascadeClassifier profileCascade;
    private CascadeClassifier lbpCascade;
    private FaceDetectorYN yuNetDetector;
    private Object sFaceRecognizer; // Loaded via reflection to avoid hard dependency

    // Cached DNN resource paths for ThreadLocal initialization
    private String dnnProtoPath;
    private String dnnModelPath;
    @SuppressWarnings("unused")
    private String yuNetModelPath;
    @SuppressWarnings("unused")
    private String sFaceModelPath;

    // AnnotationCapability - delegate to existing face-specific methods for now
    @Override
    public ImageData obscure(ImageData imageData, java.util.function.Predicate<io.github.codesapienbe.springvision.core.Detection> filter)
        throws io.github.codesapienbe.springvision.core.exception.BaseVisionException {
        // Reuse existing implementation
        return obscureFaces(imageData);
    }

    @Override
    public ImageData annotate(ImageData imageData, io.github.codesapienbe.springvision.core.AnnotationRequest request)
        throws io.github.codesapienbe.springvision.core.exception.BaseVisionException {
        if (request.getAction() == io.github.codesapienbe.springvision.core.AnnotationRequest.Action.OBSCURE) {
            return obscureFaces(imageData);
        }
        if (request.getAction() == io.github.codesapienbe.springvision.core.AnnotationRequest.Action.MARK) {
            return markFaces(imageData);
        }
        if (request.getAction() == io.github.codesapienbe.springvision.core.AnnotationRequest.Action.TAG) {
            return tagFaces(imageData, request.getLabel());
        }
        return imageData;
    }

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

    private boolean dnnDownloadWarnLogged = false;
    private boolean sfaceInitWarnLogged = false;

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
        this.lbpCascade = null;
        this.yuNetDetector = null;
        this.sFaceRecognizer = null;
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
        return Set.of(DetectionType.FACE, DetectionType.OBJECT, DetectionType.BARCODE);
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
    public List<Detection> detectFaces(ImageData imageData) {
        return performFaceDetection(imageData);
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        return performObjectDetection(imageData);
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
                boolean cascadesAvailable = tryLoadFaceCascade();
                // Prefer loading YuNet first, then DNN SSD as secondary
                loadYuNetDetector();
                loadDnnFaceDetector();
                if (cascadesAvailable) {
                    loadEyeCascade();
                    loadProfileCascade();
                    loadLbpCascade();
                } else {
                    logger.warn("Skipping Haar/LBP cascades due to unavailable native highgui/GTK in headless environment");
                }
                loadSFaceRecognizer();
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
                try (Mat testMat = new Mat()) {
                    testMat.releaseReference();
                }
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

            String protoPath = resolveModel(
                DNN_CAFFE_PROTO_TXT_CLASSPATH,
                new String[]{DNN_CAFFE_PROTO_TXT_URL},
                "deploy.prototxt");
            String modelPath = resolveModel(
                DNN_CAFFE_MODEL_CLASSPATH,
                new String[]{DNN_CAFFE_MODEL_URL, DNN_CAFFE_MODEL_URL_ALT},
                "res10_300x300_ssd_iter_140000_fp16.caffemodel");

            if (protoPath != null && modelPath != null) {
                // cache paths and provide thread-local networks for thread safety
                this.dnnProtoPath = protoPath;
                this.dnnModelPath = modelPath;
                this.dnnFaceNet = ThreadLocal.withInitial(() -> {
                    Net net = org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe(dnnProtoPath, dnnModelPath);
                    try {
                        net.setPreferableBackend(org.bytedeco.opencv.global.opencv_dnn.DNN_BACKEND_OPENCV);
                        net.setPreferableTarget(org.bytedeco.opencv.global.opencv_dnn.DNN_TARGET_CPU);
                    } catch (Throwable ignore) {
                    }
                    return net;
                });
                Net test = this.dnnFaceNet.get();
                if (test != null && !test.empty()) {
                    logger.info("DNN face detector loaded successfully (thread-local)");
                } else {
                    logger.warn("Failed to initialize DNN face detector");
                    this.dnnFaceNet = null;
                }
            } else {
                if (!dnnDownloadWarnLogged) {
                    logger.info("DNN face detector resources unavailable; using YuNet/cascades only");
                    dnnDownloadWarnLogged = true;
                }
                dnnFaceNet = null;
            }
        } catch (Exception e) {
            if (!dnnDownloadWarnLogged) {
                logger.info("Error loading DNN face detector; using YuNet/cascades only: {}", e.getMessage());
                dnnDownloadWarnLogged = true;
            }
            dnnFaceNet = null;
        }
    }

    /**
     * Gamma correction helper operating in normalized float space.
     */
    private Mat applyGammaCorrection(Mat grayImage, double gamma) {
        try {
            Mat norm = new Mat();
            grayImage.convertTo(norm, org.bytedeco.opencv.global.opencv_core.CV_32F, 1.0 / 255.0, 0.0);
            org.bytedeco.opencv.global.opencv_core.pow(norm, gamma, norm);
            Mat out = new Mat();
            norm.convertTo(out, org.bytedeco.opencv.global.opencv_core.CV_8U, 255.0, 0.0);
            norm.releaseReference();
            return out;
        } catch (Throwable t) {
            logger.debug("Gamma correction failed: {}", t.getMessage());
            return grayImage;
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
     * @param faceRect    the detected face rectangle
     * @param imageWidth  the image width
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
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param size      the size of the region
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

    /**
     * Collect DNN candidates at a given scale; map rectangles back to original coordinates.
     */
    private void collectDnnCandidates(Mat image, double scale, Net net,
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
        synchronized (net) { // Net is not thread-safe
            net.setInput(blob);
        }
        Mat detectionsMat = net.forward();

        int sCols = src.cols();
        int sRows = src.rows();
        int numDetections = (int) detectionsMat.size(2);
        int step = (int) detectionsMat.size(3);
        FloatPointer data = new FloatPointer(detectionsMat.data());

        for (int i = 0; i < numDetections; i++) {
            float confidence = data.get((long) i * step + 2);
            if (confidence < 0.5f) continue;
            float x1 = data.get((long) i * step + 3) * sCols;
            float y1 = data.get((long) i * step + 4) * sRows;
            float x2 = data.get((long) i * step + 5) * sCols;
            float y2 = data.get((long) i * step + 6) * sRows;

            // Map back to original if resized
            if (usedResize) {
                x1 /= scale;
                y1 /= scale;
                x2 /= scale;
                y2 /= scale;
            }

            int left = (int) Math.max(0, Math.min(cols - 1, Math.round(x1)));
            int top = (int) Math.max(0, Math.min(rows - 1, Math.round(y1)));
            int right = (int) Math.max(0, Math.min(cols - 1, Math.round(x2)));
            int bottom = (int) Math.max(0, Math.min(rows - 1, Math.round(y2)));
            int widthPx = Math.max(1, right - left);
            int heightPx = Math.max(1, bottom - top);

            double aspect = (double) widthPx / heightPx;
            if (aspect < 0.5 || aspect > 1.8) continue;
            outRects.add(new Rect(left, top, widthPx, heightPx));
            outScores.add(confidence);
        }

        blob.releaseReference();
        detectionsMat.releaseReference();
        if (usedResize) resized.releaseReference();
    }

    /**
     * Load the eye cascade with classpath-first, then remote fallback.
     */
    private void loadEyeCascade() {
        try {
            eyeCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_EYE_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("haarcascade_eye", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                        fos.write(data);
                    }
                    if (eyeCascade.load(tmp.getAbsolutePath())) {
                        logger.info("Eye cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {
            }

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
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                    fos.write(in.readAllBytes());
                }
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
     * Load the LBP face cascade with classpath-first, then remote fallback.
     */
    private void loadLbpCascade() {
        try {
            lbpCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_LBP_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("lbpcascade_frontalface", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                        fos.write(data);
                    }
                    if (lbpCascade.load(tmp.getAbsolutePath())) {
                        logger.info("LBP cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {
            }

            // Try system path
            if (lbpCascade.load(DEFAULT_LBP_CASCADE_PATH)) {
                logger.info("LBP cascade loaded from system path");
                return;
            }

            // Download fallback
            try {
                logger.info("Attempting to download LBP face cascade...");
                java.net.URL url = new java.net.URL(LBP_CASCADE_DOWNLOAD_URL);
                java.io.File tmp = java.io.File.createTempFile("lbpcascade_frontalface", ".xml");
                tmp.deleteOnExit();
                try (java.io.InputStream in = openWithTimeout(url, 4000, 5000);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                    fos.write(in.readAllBytes());
                }
                if (lbpCascade.load(tmp.getAbsolutePath())) {
                    logger.info("LBP cascade downloaded and loaded successfully");
                    return;
                }
            } catch (Exception ex) {
                logger.warn("Could not download LBP cascade: {}", ex.getMessage());
            }

            logger.warn("LBP cascade unavailable; fallback to Haar/DNN only");
        } catch (Exception e) {
            logger.warn("Error loading LBP cascade: {}", e.getMessage());
            lbpCascade = null;
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
            detections.add(new Detection("face", conf, new BoundingBox(nx, ny, nw, nh), java.util.Map.of(
                "category", io.github.codesapienbe.springvision.core.DetectionCategory.FACE.name()
            )));
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
            detections.add(new Detection("face", conf, new BoundingBox(nx, ny, nw, nh), java.util.Map.of(
                "category", io.github.codesapienbe.springvision.core.DetectionCategory.FACE.name()
            )));
        }
        profilesFlipped.deallocate();
        flipped.releaseReference();
    }

    /**
     * Check if a new rect overlaps sufficiently with an existing detection; if so, skip.
     */
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

    /**
     * Load the profile-face cascade with classpath-first, then remote fallback.
     */
    private void loadProfileCascade() {
        try {
            profileCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_PROFILE_FACE_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("haarcascade_profileface", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                        fos.write(data);
                    }
                    if (profileCascade.load(tmp.getAbsolutePath())) {
                        logger.info("Profile-face cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {
            }

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
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                    fos.write(in.readAllBytes());
                }
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

    /**
     * Load YuNet ONNX-based face detector from classpath or short download.
     */
    private void loadYuNetDetector() {
        try {
            logger.info("Loading YuNet face detector...");
            String modelPath = resolveModel(
                YUNET_ONNX_CLASSPATH,
                new String[]{YUNET_ONNX_URL},
                "face_detection_yunet_2023mar.onnx");

            if (modelPath != null) {
                this.yuNetModelPath = modelPath;
                // Create with default thresholds; input size set per image later
                yuNetDetector = FaceDetectorYN.create(modelPath, "", new Size(320, 320), 0.6f, 0.3f, 5000,
                    org.bytedeco.opencv.global.opencv_dnn.DNN_BACKEND_OPENCV,
                    org.bytedeco.opencv.global.opencv_dnn.DNN_TARGET_CPU);
                if (yuNetDetector == null || yuNetDetector.isNull()) {
                    logger.warn("Failed to initialize YuNet detector");
                    yuNetDetector = null;
                } else {
                    logger.info("YuNet face detector loaded successfully");
                }
            } else {
                logger.warn("YuNet model unavailable; skipping YuNet detector");
                yuNetDetector = null;
            }
        } catch (Throwable t) {
            logger.warn("Error loading YuNet: {}", t.getMessage());
            yuNetDetector = null;
        }
    }

    /**
     * Load SFace recognizer for embeddings (optional).
     */
    private void loadSFaceRecognizer() {
        try {
            logger.info("Loading SFace recognizer...");
            String modelPath = resolveModel(
                SFACE_ONNX_CLASSPATH,
                new String[]{SFACE_ONNX_URL},
                "face_recognition_sface_2021dec.onnx");

            if (modelPath != null) {
                this.sFaceModelPath = modelPath;
                try {
                    Class<?> cls = Class.forName("org.bytedeco.opencv.opencv_face.FaceRecognizerSF");
                    java.lang.reflect.Method create = cls.getMethod("create", String.class, String.class);
                    this.sFaceRecognizer = create.invoke(null, modelPath, "");
                } catch (Throwable t) {
                    if (!sfaceInitWarnLogged) {
                        logger.info("SFace recognizer not available; embedding features disabled: {}", t.getMessage());
                        sfaceInitWarnLogged = true;
                    }
                    this.sFaceRecognizer = null;
                }
                if (this.sFaceRecognizer == null) {
                    if (!sfaceInitWarnLogged) {
                        logger.info("SFace recognizer initialization failed; continuing without embeddings");
                        sfaceInitWarnLogged = true;
                    }
                } else {
                    logger.info("SFace recognizer loaded successfully");
                }
            } else {
                if (!sfaceInitWarnLogged) {
                    logger.info("SFace model unavailable; skipping recognizer");
                    sfaceInitWarnLogged = true;
                }
                sFaceRecognizer = null;
            }
        } catch (Throwable t) {
            if (!sfaceInitWarnLogged) {
                logger.info("Error loading SFace; continuing without embeddings: {}", t.getMessage());
                sfaceInitWarnLogged = true;
            }
            sFaceRecognizer = null;
        }
    }

    /**
     * Collect YuNet candidates at given scale; refine boxes using landmarks and map back to original image.
     */
    private void collectYuNetCandidates(Mat image, double scale, List<Rect> outRects, List<Float> outScores) {
        if (yuNetDetector == null || yuNetDetector.isNull()) return;
        try {
            Mat src = image;
            Mat resized = new Mat();
            boolean usedResize = false;
            if (Math.abs(scale - 1.0) > 1e-3) {
                int newW = (int) Math.round(image.cols() * scale);
                int newH = (int) Math.round(image.rows() * scale);
                resize(image, resized, new Size(newW, newH));
                src = resized;
                usedResize = true;
            }
            yuNetDetector.setInputSize(new Size(src.cols(), src.rows()));
            Mat det = new Mat();
            yuNetDetector.detect(src, det);
            if (!det.empty()) {
                int rows = (int) det.size(0);
                int step = (int) det.size(1); // expected 15
                FloatPointer dp = new FloatPointer(det.data());
                for (int i = 0; i < rows; i++) {
                    float x = dp.get((long) i * step + 0);
                    float y = dp.get((long) i * step + 1);
                    float w = dp.get((long) i * step + 2);
                    float h = dp.get((long) i * step + 3);
                    float score = dp.get((long) i * step + 4);
                    if (score < 0.35f) continue; // lower threshold for recall; duplicates removed by NMS
                    // Expand bbox moderately around center to include forehead/chin
                    double cx = x + w / 2.0;
                    double cy = y + h / 2.0;
                    double padX = w * 0.08;
                    double padY = h * 0.12;
                    double left = Math.max(0, cx - (w / 2.0 + padX));
                    double top = Math.max(0, cy - (h / 2.0 + padY));
                    double right = Math.min(src.cols() - 1, cx + (w / 2.0 + padX));
                    double bottom = Math.min(src.rows() - 1, cy + (h / 2.0 + padY));

                    int leftI = (int) Math.round(left);
                    int topI = (int) Math.round(top);
                    int widthPx = Math.max(1, (int) Math.round(right - left));
                    int heightPx = Math.max(1, (int) Math.round(bottom - top));

                    // Map back to original if resized
                    if (usedResize) {
                        leftI = (int) Math.round(leftI / scale);
                        topI = (int) Math.round(topI / scale);
                        widthPx = (int) Math.max(1, Math.round(widthPx / scale));
                        heightPx = (int) Math.max(1, Math.round(heightPx / scale));
                    }

                    outRects.add(new Rect(leftI, topI, widthPx, heightPx));
                    outScores.add(score);
                }
            }
            det.releaseReference();
            if (usedResize) resized.releaseReference();
        } catch (Throwable t) {
            logger.debug("YuNet detect failed: {}", t.getMessage());
        }
    }

    /**
     * Compute SFace embedding for a face crop (112x112 BGR).
     */
    private float[] computeSFaceEmbedding(Mat alignedFace112x112) {
        // Primary path: use OpenCV FaceRecognizerSF when available
        if (sFaceRecognizer != null) {
            try {
                Mat embedding = new Mat();
                Class<?> cls = Class.forName("org.bytedeco.opencv.opencv_face.FaceRecognizerSF");
                java.lang.reflect.Method feature = cls.getMethod("feature", Mat.class, Mat.class);
                feature.invoke(sFaceRecognizer, alignedFace112x112, embedding);
                int len = (int) embedding.total();
                float[] vec = new float[len];
                embedding.data().asByteBuffer().asFloatBuffer().get(vec);
                embedding.releaseReference();
                // L2 normalize
                double norm = 0.0;
                for (float v : vec) norm += v * v;
                norm = Math.sqrt(norm);
                if (norm > 0) {
                    for (int i = 0; i < vec.length; i++) vec[i] /= (float) norm;
                }
                return vec;
            } catch (Throwable t) {
                logger.debug("SFace embedding failed (OpenCV): {}", t.getMessage());
                // fall through to fallback attempt
            }
        }

        // Fallback: try to use FaceBytes ModelManager via reflection (no compile-time dependency)
        try {
            Class<?> mm = Class.forName("io.github.codesapienbe.springvision.facebytes.models.ModelManager");
            java.lang.reflect.Method isAvailable = mm.getMethod("isSFaceAvailable");
            Boolean available = (Boolean) isAvailable.invoke(null);
            if (available != null && available) {
                // Convert Mat to NCHW float array expected by ModelManager
                float[] nchw = matToNCHWFloat(alignedFace112x112);
                long[] shape = new long[]{1, 3, alignedFace112x112.rows(), alignedFace112x112.cols()};
                java.lang.reflect.Method run = mm.getMethod("runSFaceEmbedding", float[].class, long[].class);
                Object result = run.invoke(null, nchw, shape);
                if (result instanceof float[] arr && arr.length > 0) {
                    // ModelManager already returns normalized embedding; return directly
                    return arr;
                }
            }
        } catch (ClassNotFoundException cnf) {
            logger.debug("FaceBytes ModelManager not present for SFace fallback: {}", cnf.getMessage());
        } catch (Throwable t) {
            logger.debug("SFace fallback via ModelManager failed: {}", t.getMessage());
        }

        return null;
    }

    /**
     * Convert a BGR Mat to a NCHW float array suitable for ONNX-style inference.
     * The Mat is expected to be HxW with 3 channels (BGR). Values are converted to float
     * in range [0,255]. Caller is responsible for matching model expected normalization.
     */
    private float[] matToNCHWFloat(Mat mat) {
        int h = mat.rows();
        int w = mat.cols();
        int channels = mat.channels();
        if (channels < 3) {
            // expand single channel to 3 channels by duplication
            channels = 3;
        }
        float[] out = new float[channels * h * w];
        try {
            // Read byte data from Mat; OpenCV Mat stores BGR by default
            org.bytedeco.javacpp.BytePointer bp = (org.bytedeco.javacpp.BytePointer) mat.data();
            int stride = w * mat.channels();
            byte[] rowBuf = new byte[stride];
            int idxB = 0;
            int idxG = h * w;
            int idxR = 2 * h * w;
            for (int y = 0; y < h; y++) {
                bp.position((long) y * stride);
                bp.get(rowBuf);
                int p = 0;
                for (int x = 0; x < w; x++) {
                    int b = rowBuf[p++] & 0xFF;
                    int g = rowBuf[p++] & 0xFF;
                    int r = rowBuf[p++] & 0xFF;
                    out[idxB++] = (float) b;
                    out[idxG++] = (float) g;
                    out[idxR++] = (float) r;
                }
            }
        } catch (Throwable t) {
            logger.debug("Failed to convert Mat to NCHW float: {}", t.getMessage());
            return new float[3 * h * w];
        }
        return out;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Clamp normalized bounding box coordinates and return a valid box or null if degenerate.
     */
    private static BoundingBox clampAndCreateBox(double nx, double ny, double nw, double nh) {
        nx = clamp01(nx);
        ny = clamp01(ny);
        nw = Math.max(0.0, Math.min(nw, 1.0 - nx));
        nh = Math.max(0.0, Math.min(nh, 1.0 - ny));
        if (nw <= 0.0 || nh <= 0.0) {
            return null;
        }
        return new BoundingBox(nx, ny, nw, nh);
    }

    /**
     * Resolve model by checking classpath, then a local cache under user home, then downloading to cache from URLs.
     */
    private String resolveModel(String classpathResource, String[] urls, String cacheFileName) {
        // 1) Classpath
        try (java.io.InputStream is = getClass().getResourceAsStream(classpathResource)) {
            if (is != null) {
                File tmp = File.createTempFile("sv-model-", cacheFileName);
                tmp.deleteOnExit();
                Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tmp.getAbsolutePath();
            }
        } catch (Exception ignore) {
        }

        // 2) User cache
        try {
            Path base = getModelsBaseDir();
            Path cached = base.resolve(cacheFileName);
            if (Files.exists(cached)) {
                return cached.toAbsolutePath().toString();
            }
            // 3) Download to cache
            for (String u : urls) {
                try {
                    java.net.URL url = new java.net.URL(u);
                    try (java.io.InputStream in = openWithTimeout(url, 6000, 15000)) {
                        Files.copy(in, cached, StandardCopyOption.REPLACE_EXISTING);
                        return cached.toAbsolutePath().toString();
                    }
                } catch (Exception ignore) { /* try next */ }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private Path getModelsBaseDir() {
        String userHome = System.getProperty("user.home", ".");
        Path dir = Path.of(userHome, ".spring-vision/models");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignore) {
        }
        return dir;
    }

    /**
     * Compute dynamic min/max face area ratios from top N confident candidates; robust to outliers.
     */
    private static double[] computeDynamicAreaBounds(List<Rect> rects, List<Float> scores, int cols, int rows) {
        int n = rects.size();
        java.util.List<Integer> order = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        order.sort((a, b) -> Float.compare(scores.get(b), scores.get(a)));
        int top = Math.min(3, n);
        double[] areas = new double[top];
        for (int i = 0; i < top; i++) {
            Rect r = rects.get(order.get(i));
            areas[i] = (double) r.width() * r.height() / ((double) cols * rows);
        }
        java.util.Arrays.sort(areas);
        double median = areas[top / 2];
        // Derive permissive bounds that suppress extreme outliers (e.g., huge false boxes) but keep small faces
        double minArea = Math.min(0.01, median * 0.02);    // keep very small faces
        double maxArea = Math.max(0.06, median * 1.8);     // drop overly large boxes
        // Clamp to safe global limits
        minArea = Math.max(0.00005, Math.min(minArea, 0.05));
        maxArea = Math.max(minArea * 3.0, Math.min(maxArea, 0.9));
        return new double[]{minArea, maxArea};
    }

    /**
     * Apply a lightweight Multi-Scale Retinex on a grayscale image to normalize illumination
     * while preserving edges. Returns an 8-bit grayscale Mat. Any failure returns the input.
     */
    private Mat applyMultiScaleRetinexGray(Mat grayImage) {
        try {
            // Convert to float in [0,1]
            Mat inputFloat = new Mat();
            grayImage.convertTo(inputFloat, org.bytedeco.opencv.global.opencv_core.CV_32F, 1.0 / 255.0, 0.0);

            // log(I + eps)
            double eps = 1e-6;
            Mat epsMat = new Mat(inputFloat.size(), inputFloat.type(), new Scalar(eps));
            org.bytedeco.opencv.global.opencv_core.add(inputFloat, epsMat, inputFloat);
            Mat logI = new Mat();
            org.bytedeco.opencv.global.opencv_core.log(inputFloat, logI);

            // Accumulate SSR across multiple Gaussian scales
            int[] kernelSizes = new int[]{15, 80, 250};
            Mat acc = new Mat(logI.size(), logI.type(), new Scalar(0));
            for (int k : kernelSizes) {
                Mat blurred = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur(inputFloat, blurred, new Size(k, k), 0);
                org.bytedeco.opencv.global.opencv_core.add(blurred, epsMat, blurred);
                Mat logB = new Mat();
                org.bytedeco.opencv.global.opencv_core.log(blurred, logB);
                Mat ssr = new Mat();
                org.bytedeco.opencv.global.opencv_core.subtract(logI, logB, ssr);
                org.bytedeco.opencv.global.opencv_core.add(acc, ssr, acc);
                ssr.releaseReference();
                logB.releaseReference();
                blurred.releaseReference();
            }
            epsMat.releaseReference();
            // Average the accumulation
            Mat scaleMat = new Mat(acc.size(), acc.type(), new Scalar(1.0 / kernelSizes.length));
            org.bytedeco.opencv.global.opencv_core.multiply(acc, scaleMat, acc);
            scaleMat.releaseReference();

            // Normalize to 0..255 and convert to 8U
            Mat out = new Mat();
            org.bytedeco.opencv.global.opencv_core.normalize(
                acc,
                out,
                0,
                255,
                org.bytedeco.opencv.global.opencv_core.NORM_MINMAX,
                org.bytedeco.opencv.global.opencv_core.CV_8U,
                new Mat()
            );

            // Cleanup
            acc.releaseReference();
            logI.releaseReference();
            inputFloat.releaseReference();

            return out;
        } catch (Throwable t) {
            logger.debug("Retinex processing failed: {}", t.getMessage());
            return grayImage;
        }
    }

    /**
     * Compute a mild quality factor based on blur (Laplacian variance) and illumination (mean intensity).
     * Returns a factor in approximately [0.85, 1.08] to avoid drastic behavioral changes.
     */
    private double computeQualityFactor(Mat grayImage, Rect rect) {
        try {
            if (grayImage == null || grayImage.empty() || rect == null) return 1.0;
            int x = Math.max(0, rect.x());
            int y = Math.max(0, rect.y());
            int w = Math.min(grayImage.cols() - x, rect.width());
            int h = Math.min(grayImage.rows() - y, rect.height());
            if (w <= 0 || h <= 0) return 1.0;

            Mat roi = new Mat(grayImage, new Rect(x, y, w, h));

            // Blur score via Laplacian variance
            Mat lap = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.Laplacian(roi, lap, org.bytedeco.opencv.global.opencv_core.CV_64F);
            Mat meanMat = new Mat();
            Mat stddevMat = new Mat();
            org.bytedeco.opencv.global.opencv_core.meanStdDev(lap, meanMat, stddevMat);
            double std = 0.0;
            if (!stddevMat.empty()) {
                java.nio.DoubleBuffer db = stddevMat.getDoubleBuffer();
                if (db != null && db.remaining() > 0) {
                    std = db.get(0);
                }
            }
            double variance = std * std;

            // Normalize variance to [0,1] with soft threshold ~100
            double blurScore = Math.min(1.0, variance / 100.0);

            // Illumination score: prefer mid-tones around ~128
            double avg = org.bytedeco.opencv.global.opencv_core.mean(roi).get(0);
            double illumScore = 1.0 - Math.min(1.0, Math.abs(avg - 128.0) / 128.0);
            // Smooth floor to avoid zeroing
            illumScore = 0.6 + 0.4 * illumScore; // maps [0,1] -> [0.6,1.0]

            // Combine
            double combined = 0.5 * blurScore + 0.5 * illumScore; // [0.3,1.0]
            // Map to mild factor range
            double factor = 0.85 + 0.23 * combined; // ~[0.85,1.08]

            // Cleanup
            lap.releaseReference();
            meanMat.releaseReference();
            stddevMat.releaseReference();
            roi.releaseReference();

            return factor;
        } catch (Throwable t) {
            return 1.0;
        }
    }

    /**
     * Attempts to load the face cascade but tolerates environments where native GUI deps are missing.
     *
     * @return true if cascades are available; false if skipped due to missing native dependencies
     */
    private boolean tryLoadFaceCascade() {
        try {
            loadFaceCascade();
            return true;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            // HighGUI/GTK may be missing in headless environments; proceed without cascades
            logger.warn("Face cascade unavailable (native dependency missing): {}", e.getMessage());
            faceCascade = null;
            return false;
        } catch (Throwable t) {
            // Catch-all to avoid failing backend initialization
            logger.warn("Failed to load face cascade: {}", t.getMessage());
            faceCascade = null;
            return false;
        }
    }

    public ImageData obscureFaces(ImageData imageData) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be null or empty");
        }

        try {
            // Decode image to Mat
            org.bytedeco.javacpp.BytePointer rawPointer = new org.bytedeco.javacpp.BytePointer(imageData.data());
            Mat buffer = new Mat(1, (int) imageData.getSizeInBytes(), org.bytedeco.opencv.global.opencv_core.CV_8U, rawPointer);
            Mat image = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buffer, IMREAD_COLOR);
            if (image == null || image.empty()) {
                rawPointer.deallocate();
                buffer.releaseReference();
                throw new VisionProcessingException("Failed to decode image data", "obscure", "face");
            }
            buffer.releaseReference();
            rawPointer.deallocate();

            // Detect faces
            List<Detection> faces = detectFaces(imageData);

            if (faces.isEmpty()) {
                logger.debug("No faces detected for obscuring");
                // Return original image if no faces found
                return imageData;
            }

            // Apply blur to each detected face
            for (Detection face : faces) {
                BoundingBox bbox = face.boundingBox();
                if (bbox != null) {
                    // Convert normalized coordinates to pixel coordinates
                    int x = (int) (bbox.x() * image.cols());
                    int y = (int) (bbox.y() * image.rows());
                    int width = (int) (bbox.width() * image.cols());
                    int height = (int) (bbox.height() * image.rows());

                    // Ensure coordinates are within image bounds
                    x = Math.max(0, Math.min(x, image.cols() - 1));
                    y = Math.max(0, Math.min(y, image.rows() - 1));
                    width = Math.min(width, image.cols() - x);
                    height = Math.min(height, image.rows() - y);

                    if (width > 0 && height > 0) {
                        // Extract face region
                        Rect faceRect = new Rect(x, y, width, height);
                        Mat faceRegion = new Mat(image, faceRect);

                        // Apply enhanced obscuring: 2x stronger blur + pixelation
                        Mat blurredFace = new Mat();
                        Mat doubleBlurredFace = new Mat();

                        // First pass: Heavy Gaussian blur (2x larger kernel size)
                        org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur(
                            faceRegion,
                            blurredFace,
                            new Size(61, 61), // Increased from 31x31 to 61x61 for 2x obscurity
                            0.0, 0.0,
                            org.bytedeco.opencv.global.opencv_core.BORDER_DEFAULT
                        );

                        // Second pass: Apply additional blur for compound effect
                        org.bytedeco.opencv.global.opencv_imgproc.GaussianBlur(
                            blurredFace,
                            doubleBlurredFace,
                            new Size(31, 31), // Secondary blur for extra obscurity
                            0.0, 0.0,
                            org.bytedeco.opencv.global.opencv_core.BORDER_DEFAULT
                        );

                        // Optional: Add pixelation effect for maximum obscurity
                        Mat pixelated = new Mat();
                        int pixelSize = Math.max(8, Math.min(width, height) / 12); // Adaptive pixel size

                        // Downscale then upscale for pixelation effect
                        org.bytedeco.opencv.global.opencv_imgproc.resize(
                            doubleBlurredFace,
                            pixelated,
                            new Size(Math.max(1, width / pixelSize), Math.max(1, height / pixelSize)),
                            0, 0,
                            org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR
                        );
                        org.bytedeco.opencv.global.opencv_imgproc.resize(
                            pixelated,
                            doubleBlurredFace,
                            new Size(width, height),
                            0, 0,
                            org.bytedeco.opencv.global.opencv_imgproc.INTER_NEAREST // Use nearest neighbor for blocky effect
                        );

                        // Copy heavily obscured face back to original image
                        doubleBlurredFace.copyTo(faceRegion);

                        // Cleanup all intermediate matrices
                        faceRegion.releaseReference();
                        blurredFace.releaseReference();
                        doubleBlurredFace.releaseReference();
                        pixelated.releaseReference();
                    }
                }
            }

            // Encode the modified image back to bytes
            org.bytedeco.javacpp.BytePointer outBuffer = new org.bytedeco.javacpp.BytePointer();
            boolean ok = org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", image, outBuffer);
            if (!ok || outBuffer.isNull() || outBuffer.limit() == 0) {
                image.releaseReference();
                outBuffer.deallocate();
                throw new VisionProcessingException("Failed to encode obscured image", "obscure", "face");
            }
            long length = outBuffer.limit();
            byte[] obscuredImageBytes = new byte[(int) length];
            outBuffer.get(obscuredImageBytes);

            // Cleanup
            image.releaseReference();
            outBuffer.deallocate();

            logger.info("Successfully obscured {} faces in image", faces.size());
            return ImageData.fromBytes(obscuredImageBytes, "image/jpeg");

        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionBackendException("Failed to obscure faces: " + e.getMessage(), e);
        }
    }

    /**
     * Draw a textual tag above each detected face.
     */
    public ImageData tagFaces(ImageData imageData, String tag) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be null or empty");
        }
        if (tag == null) {
            throw new IllegalArgumentException("Tag must not be null");
        }
        String safeTag = tag.strip();
        if (safeTag.length() > 255) {
            safeTag = safeTag.substring(0, 255);
        }
        if (safeTag.isEmpty()) {
            // Nothing to render; return original image
            return imageData;
        }

        try {
            // Decode image to Mat
            org.bytedeco.javacpp.BytePointer rawPointer = new org.bytedeco.javacpp.BytePointer(imageData.data());
            Mat buffer = new Mat(1, (int) imageData.getSizeInBytes(), org.bytedeco.opencv.global.opencv_core.CV_8U, rawPointer);
            Mat image = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buffer, IMREAD_COLOR);
            if (image == null || image.empty()) {
                rawPointer.deallocate();
                buffer.releaseReference();
                throw new VisionProcessingException("Failed to decode image data", "tag", "face");
            }
            buffer.releaseReference();
            rawPointer.deallocate();

            // Detect faces
            List<Detection> faces = detectFaces(imageData);

            if (faces.isEmpty()) {
                logger.debug("No faces detected for tagging");
                return imageData;
            }

            // Choose font and scale based on image size
            int imgW = image.cols();
            int imgH = image.rows();
            int minDim = Math.max(1, Math.min(imgW, imgH));
            double fontScale = Math.max(0.4, Math.min(2.5, minDim / 600.0));
            int thickness = Math.max(1, (int) Math.round(fontScale * 2));
            int baseline[] = new int[1];

            for (Detection face : faces) {
                BoundingBox bbox = face.boundingBox();
                if (bbox == null) {
                    continue;
                }
                int x = (int) Math.round(bbox.x() * imgW);
                int y = (int) Math.round(bbox.y() * imgH);
                int width = (int) Math.round(bbox.width() * imgW);
                int height = (int) Math.round(bbox.height() * imgH);

                x = Math.max(0, Math.min(x, imgW - 1));
                y = Math.max(0, Math.min(y, imgH - 1));
                width = Math.min(width, imgW - x);
                height = Math.min(height, imgH - y);
                if (width <= 0 || height <= 0) {
                    continue;
                }

                // Calculate text size
                org.bytedeco.opencv.opencv_core.Size textSize = org.bytedeco.opencv.global.opencv_imgproc.getTextSize(
                    safeTag,
                    org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                    fontScale,
                    thickness,
                    baseline
                );

                // Position above the face rectangle
                int textX = Math.max(0, Math.min(x, imgW - textSize.width()));
                int textY = y - 8; // small margin above
                if (textY - textSize.height() < 0) {
                    // If above goes out of frame, place inside the top edge of face rect
                    textY = y + Math.min(height - 4, textSize.height() + 4);
                }

                // Draw background rectangle for readability
                int rectX = textX - 2;
                int rectY = textY - textSize.height() - 2;
                int rectW = textSize.width() + 4;
                int rectH = textSize.height() + 4;

                rectX = Math.max(0, rectX);
                rectY = Math.max(0, rectY);
                if (rectX + rectW > imgW) rectW = imgW - rectX;
                if (rectY + rectH > imgH) rectH = imgH - rectY;

                if (rectW > 0 && rectH > 0) {
                    org.bytedeco.opencv.global.opencv_imgproc.rectangle(
                        image,
                        new org.bytedeco.opencv.opencv_core.Rect(rectX, rectY, rectW, rectH),
                        new Scalar(0, 0, 0, 0),
                        org.bytedeco.opencv.global.opencv_imgproc.FILLED,
                        0,
                        0
                    );
                }

                // Draw the text in white
                org.bytedeco.opencv.global.opencv_imgproc.putText(
                    image,
                    safeTag,
                    new org.bytedeco.opencv.opencv_core.Point(textX, textY),
                    org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                    fontScale,
                    new Scalar(255, 255, 255, 0),
                    thickness,
                    org.bytedeco.opencv.global.opencv_imgproc.LINE_AA,
                    false
                );
            }

            // Encode back to bytes
            org.bytedeco.javacpp.BytePointer outBuffer = new org.bytedeco.javacpp.BytePointer();
            boolean ok = org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", image, outBuffer);
            if (!ok || outBuffer.isNull() || outBuffer.limit() == 0) {
                image.releaseReference();
                outBuffer.deallocate();
                throw new VisionProcessingException("Failed to encode tagged image", "tag", "face");
            }
            long length = outBuffer.limit();
            byte[] taggedBytes = new byte[(int) length];
            outBuffer.get(taggedBytes);

            image.releaseReference();
            outBuffer.deallocate();

            logger.info("Tagged {} faces in image", faces.size());
            return ImageData.fromBytes(taggedBytes, "image/jpeg");
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionBackendException("Failed to tag faces: " + e.getMessage(), e);
        }
    }

    /**
     * Draw a colored rectangle around each detected face.
     */
    public ImageData markFaces(ImageData imageData) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be null or empty");
        }

        try {
            // Decode image to Mat
            org.bytedeco.javacpp.BytePointer rawPointer = new org.bytedeco.javacpp.BytePointer(imageData.data());
            Mat buffer = new Mat(1, (int) imageData.getSizeInBytes(), org.bytedeco.opencv.global.opencv_core.CV_8U, rawPointer);
            Mat image = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buffer, IMREAD_COLOR);
            if (image == null || image.empty()) {
                rawPointer.deallocate();
                buffer.releaseReference();
                throw new VisionProcessingException("Failed to decode image data", "mark", "face");
            }
            buffer.releaseReference();
            rawPointer.deallocate();

            // Detect faces
            List<Detection> faces = detectFaces(imageData);

            if (faces.isEmpty()) {
                logger.debug("No faces detected for marking");
                return imageData;
            }

            int imgW = image.cols();
            int imgH = image.rows();
            java.util.Random rng = new java.util.Random(0x5F3759DFL ^ System.currentTimeMillis());

            for (Detection face : faces) {
                BoundingBox bbox = face.boundingBox();
                if (bbox == null) {
                    continue;
                }
                int x = (int) Math.round(bbox.x() * imgW);
                int y = (int) Math.round(bbox.y() * imgH);
                int width = (int) Math.round(bbox.width() * imgW);
                int height = (int) Math.round(bbox.height() * imgH);

                x = Math.max(0, Math.min(x, imgW - 1));
                y = Math.max(0, Math.min(y, imgH - 1));
                width = Math.min(width, imgW - x);
                height = Math.min(height, imgH - y);
                if (width <= 0 || height <= 0) {
                    continue;
                }

                // Random distinct color with minimum brightness for visibility
                int r = 64 + rng.nextInt(192);
                int g = 64 + rng.nextInt(192);
                int b = 64 + rng.nextInt(192);
                Scalar color = new Scalar(b, g, r, 0); // OpenCV uses BGR

                int thickness = Math.max(2, Math.min(6, Math.round(Math.min(imgW, imgH) / 400.0f)));
                org.bytedeco.opencv.global.opencv_imgproc.rectangle(
                    image,
                    new org.bytedeco.opencv.opencv_core.Rect(x, y, width, height),
                    color,
                    thickness,
                    org.bytedeco.opencv.global.opencv_imgproc.LINE_AA,
                    0
                );
            }

            // Encode back to bytes
            org.bytedeco.javacpp.BytePointer outBuffer = new org.bytedeco.javacpp.BytePointer();
            boolean ok = org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", image, outBuffer);
            if (!ok || outBuffer.isNull() || outBuffer.limit() == 0) {
                image.releaseReference();
                outBuffer.deallocate();
                throw new VisionProcessingException("Failed to encode marked image", "mark", "face");
            }
            long length = outBuffer.limit();
            byte[] markedBytes = new byte[(int) length];
            outBuffer.get(markedBytes);

            image.releaseReference();
            outBuffer.deallocate();

            logger.info("Marked {} faces in image", faces.size());
            return ImageData.fromBytes(markedBytes, "image/jpeg");
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionBackendException("Failed to mark faces: " + e.getMessage(), e);
        }
    }

    /**
     * Performs actual face detection using enhanced multi-detector fusion.
     * This method combines YuNet, DNN-SSD, and Haar cascade results for maximum accuracy.
     */
    private List<Detection> performFaceDetection(ImageData imageData) {
        if (imageData == null || imageData.isEmpty()) {
            logger.warn("Image data is null or empty, returning empty detection list");
            return Collections.emptyList();
        }

        if (!isHealthy()) {
            logger.warn("Backend is not healthy, returning empty detection list");
            return Collections.emptyList();
        }

        try {
            // Decode image to Mat
            org.bytedeco.javacpp.BytePointer rawPointer = new org.bytedeco.javacpp.BytePointer(imageData.data());
            Mat buffer = new Mat(1, (int) imageData.getSizeInBytes(), org.bytedeco.opencv.global.opencv_core.CV_8U, rawPointer);
            Mat image = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buffer, IMREAD_COLOR);

            if (image == null || image.empty()) {
                rawPointer.deallocate();
                buffer.releaseReference();
                logger.warn("Failed to decode image data");
                return Collections.emptyList();
            }

            buffer.releaseReference();
            rawPointer.deallocate();

            // Enhanced multi-detector fusion for maximum accuracy
            List<Detection> fusedDetections = performMultiDetectorFusion(image);

            image.releaseReference();

            logger.info("Detected {} faces using enhanced multi-detector fusion", fusedDetections.size());
            return fusedDetections;

        } catch (Exception e) {
            logger.error("Face detection failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Enhanced multi-detector fusion combining YuNet, DNN-SSD, and Haar cascade.
     * Uses consensus voting and quality assessment for improved accuracy.
     */
    private List<Detection> performMultiDetectorFusion(Mat image) {
        List<CandidateDetection> allCandidates = new ArrayList<>();

        // 1. YuNet Detection (highest accuracy)
        List<CandidateDetection> yunetCandidates = detectWithYuNet(image);
        allCandidates.addAll(yunetCandidates);
        logger.debug("YuNet detected {} candidates", yunetCandidates.size());

        // 2. DNN-SSD Detection (good for challenging conditions)
        List<CandidateDetection> dnnCandidates = detectWithDNN(image);
        allCandidates.addAll(dnnCandidates);
        logger.debug("DNN-SSD detected {} candidates", dnnCandidates.size());

        // 3. Haar Cascade Detection (fallback for edge cases)
        List<CandidateDetection> haarCandidates = detectWithHaarCascade(image);
        allCandidates.addAll(haarCandidates);
        logger.debug("Haar Cascade detected {} candidates", haarCandidates.size());

        // 4. Fusion and consensus-based filtering
        List<Detection> fusedResults = fuseDetectionCandidates(image, allCandidates);

        logger.debug("Fusion produced {} final detections from {} candidates",
            fusedResults.size(), allCandidates.size());

        return fusedResults;
    }

    /**
     * Candidate detection structure for fusion processing.
     */
    private static class CandidateDetection {
        final Rect rect;
        final float confidence;
        final String detector;
        final Map<String, Object> attributes;
        double qualityScore;

        CandidateDetection(Rect rect, float confidence, String detector, Map<String, Object> attributes) {
            this.rect = rect;
            this.confidence = confidence;
            this.detector = detector;
            this.attributes = new HashMap<>(attributes);
            this.qualityScore = 0.0;
        }
    }

    /**
     * YuNet face detection with multi-scale processing.
     */
    private List<CandidateDetection> detectWithYuNet(Mat image) {
        List<CandidateDetection> candidates = new ArrayList<>();

        if (yuNetDetector == null || yuNetDetector.isNull()) {
            return candidates;
        }

        try {
            List<Rect> rects = new ArrayList<>();
            List<Float> scores = new ArrayList<>();

            // Multi-scale detection for better coverage
            double[] scales = {1.0, 0.9, 1.1, 0.8, 1.2};
            for (double scale : scales) {
                collectYuNetCandidates(image, scale, rects, scores);
            }

            // Convert to candidates with quality assessment
            Mat grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);

            for (int i = 0; i < rects.size(); i++) {
                Rect rect = rects.get(i);
                float score = scores.get(i);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("detector_confidence", score);
                attributes.put("scale_factor", "multi");

                CandidateDetection candidate = new CandidateDetection(rect, score, "yunet", attributes);
                candidate.qualityScore = assessFaceQuality(grayImage, rect);
                candidates.add(candidate);
            }

            grayImage.releaseReference();

        } catch (Exception e) {
            logger.debug("YuNet detection failed: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * DNN-SSD face detection with enhanced preprocessing.
     */
    private List<CandidateDetection> detectWithDNN(Mat image) {
        List<CandidateDetection> candidates = new ArrayList<>();

        if (dnnFaceNet == null) {
            return candidates;
        }

        try {
            Net net = dnnFaceNet.get();
            if (net == null || net.empty()) {
                return candidates;
            }

            List<Rect> rects = new ArrayList<>();
            List<Float> scores = new ArrayList<>();

            // Multi-scale DNN detection
            double[] scales = {1.0, 0.85, 1.15};
            for (double scale : scales) {
                collectDnnCandidates(image, scale, net, rects, scores);
            }

            // Convert to candidates with quality assessment
            Mat grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);

            for (int i = 0; i < rects.size(); i++) {
                Rect rect = rects.get(i);
                float score = scores.get(i);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("detector_confidence", score);
                attributes.put("model_type", "resnet_ssd");

                CandidateDetection candidate = new CandidateDetection(rect, score, "dnn_ssd", attributes);
                candidate.qualityScore = assessFaceQuality(grayImage, rect);
                candidates.add(candidate);
            }

            grayImage.releaseReference();

        } catch (Exception e) {
            logger.debug("DNN-SSD detection failed: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * Haar cascade face detection with quality validation.
     */
    private List<CandidateDetection> detectWithHaarCascade(Mat image) {
        List<CandidateDetection> candidates = new ArrayList<>();

        if (faceCascade == null || faceCascade.isNull()) {
            return candidates;
        }

        try {
            Mat grayImage = new Mat();
            cvtColor(image, grayImage, COLOR_BGR2GRAY);

            // Apply histogram equalization for better detection
            Mat equalizedGray = new Mat();
            equalizeHist(grayImage, equalizedGray);

            RectVector faces = new RectVector();

            // Multi-scale Haar cascade detection
            faceCascade.detectMultiScale(equalizedGray, faces, 1.1, 3, 0,
                new Size(30, 30), new Size());

            for (int i = 0; i < faces.size(); i++) {
                Rect rect = faces.get(i);

                // Validate with eye detection for higher precision
                if (!isFaceLikely(grayImage, rect)) {
                    continue;
                }

                float confidence = 0.75f; // Base confidence for Haar cascade

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("detector_confidence", confidence);
                attributes.put("validated_with_eyes", true);

                CandidateDetection candidate = new CandidateDetection(rect, confidence, "haar_cascade", attributes);
                candidate.qualityScore = assessFaceQuality(grayImage, rect);
                candidates.add(candidate);
            }

            faces.deallocate();
            grayImage.releaseReference();
            equalizedGray.releaseReference();

        } catch (Exception e) {
            logger.debug("Haar cascade detection failed: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * Advanced face quality assessment for recognition accuracy.
     * Returns a score from 0.0 (poor) to 1.0 (excellent).
     */
    private double assessFaceQuality(Mat grayImage, Rect faceRect) {
        try {
            // Extract face region
            int x = Math.max(0, faceRect.x());
            int y = Math.max(0, faceRect.y());
            int w = Math.min(grayImage.cols() - x, faceRect.width());
            int h = Math.min(grayImage.rows() - y, faceRect.height());

            if (w <= 0 || h <= 0) {
                return 0.0;
            }

            Mat faceRegion = new Mat(grayImage, new Rect(x, y, w, h));

            // 1. Blur assessment (Laplacian variance)
            double blurScore = computeBlurScore(faceRegion);

            // 2. Resolution assessment
            double resolutionScore = computeResolutionScore(w, h);

            // 3. Illumination assessment
            double illuminationScore = computeIlluminationScore(faceRegion);

            // 4. Pose assessment (frontal vs profile)
            double poseScore = computePoseScore(faceRegion);

            // 5. Aspect ratio assessment (face shape validity)
            double aspectScore = computeAspectScore(w, h);

            // Combine scores with weighted importance for recognition
            double qualityScore =
                0.3 * blurScore +       // Most important for recognition
                    0.25 * resolutionScore + // Critical for feature extraction
                    0.2 * illuminationScore + // Important for consistency
                    0.15 * poseScore +      // Affects recognition accuracy
                    0.1 * aspectScore;      // Basic validity check

            faceRegion.releaseReference();

            return Math.max(0.0, Math.min(1.0, qualityScore));

        } catch (Exception e) {
            logger.debug("Quality assessment failed: {}", e.getMessage());
            return 0.5; // Default moderate quality
        }
    }

    /**
     * Compute blur score using Laplacian variance.
     */
    private double computeBlurScore(Mat faceRegion) {
        try {
            Mat laplacian = new Mat();
            org.bytedeco.opencv.global.opencv_imgproc.Laplacian(faceRegion, laplacian,
                org.bytedeco.opencv.global.opencv_core.CV_64F);

            Mat meanMat = new Mat();
            Mat stddevMat = new Mat();
            org.bytedeco.opencv.global.opencv_core.meanStdDev(laplacian, meanMat, stddevMat);

            double variance = 0.0;
            if (!stddevMat.empty()) {
                java.nio.DoubleBuffer db = stddevMat.getDoubleBuffer();
                if (db != null && db.remaining() > 0) {
                    double stddev = db.get(0);
                    variance = stddev * stddev;
                }
            }

            // Normalize variance to [0,1] - higher is less blurry
            double blurScore = Math.min(1.0, variance / 500.0); // Threshold tuned for faces

            laplacian.releaseReference();
            meanMat.releaseReference();
            stddevMat.releaseReference();

            return blurScore;

        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * Compute resolution score based on face size.
     */
    private double computeResolutionScore(int width, int height) {
        int minDimension = Math.min(width, height);

        // Optimal face size for recognition: 112x112 or larger
        if (minDimension >= 112) return 1.0;
        if (minDimension >= 80) return 0.8;
        if (minDimension >= 64) return 0.6;
        if (minDimension >= 48) return 0.4;
        if (minDimension >= 32) return 0.2;
        return 0.1;
    }

    /**
     * Compute illumination score for even lighting.
     */
    private double computeIlluminationScore(Mat faceRegion) {
        try {
            double meanIntensity = org.bytedeco.opencv.global.opencv_core.mean(faceRegion).get(0);

            // Optimal range: 80-180 (avoiding very dark or very bright)
            double optimal = 128.0;
            double deviation = Math.abs(meanIntensity - optimal) / optimal;
            double illuminationScore = Math.max(0.0, 1.0 - deviation);

            return illuminationScore;

        } catch (Exception e) {
            return 0.5;
        }
    }

    /**
     * Compute pose score (frontal faces score higher).
     */
    private double computePoseScore(Mat faceRegion) {
        try {
            int width = faceRegion.cols();
            int height = faceRegion.rows();

            // Simple symmetry check for frontal pose
            Mat leftHalf = new Mat(faceRegion, new Rect(0, 0, width / 2, height));
            Mat rightHalf = new Mat(faceRegion, new Rect(width / 2, 0, width / 2, height));

            // Flip right half for comparison
            Mat rightFlipped = new Mat();
            flip(rightHalf, rightFlipped, 1);

            // Compare histograms using our custom vector implementations
            Mat leftHist = new Mat();
            Mat rightHist = new Mat();

            // Use our custom histogram calculation function
            calculateHistogram(leftHalf, new Mat(), leftHist, new IntVector(256), new FloatVector(0, 256));
            calculateHistogram(rightFlipped, new Mat(), rightHist, new IntVector(256), new FloatVector(0, 256));

            double correlation = org.bytedeco.opencv.global.opencv_imgproc.compareHist(leftHist, rightHist,
                org.bytedeco.opencv.global.opencv_imgproc.HISTCMP_CORREL);

            leftHalf.releaseReference();
            rightHalf.releaseReference();
            rightFlipped.releaseReference();
            leftHist.releaseReference();
            rightHist.releaseReference();

            return Math.max(0.0, Math.min(1.0, correlation));

        } catch (Exception e) {
            return 0.7; // Default good pose score
        }
    }

    /**
     * Compute aspect ratio score for face shape validity.
     */
    private double computeAspectScore(int width, int height) {
        double aspectRatio = (double) width / height;

        // Typical face aspect ratios: 0.7 to 1.3
        if (aspectRatio >= 0.7 && aspectRatio <= 1.3) return 1.0;
        if (aspectRatio >= 0.6 && aspectRatio <= 1.5) return 0.8;
        if (aspectRatio >= 0.5 && aspectRatio <= 1.8) return 0.5;
        return 0.2;
    }

    /**
     * Fuse detection candidates using consensus voting and quality ranking.
     */
    private List<Detection> fuseDetectionCandidates(Mat image, List<CandidateDetection> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Group overlapping candidates
        List<List<CandidateDetection>> groups = groupOverlappingCandidates(candidates);

        // 2. Select best candidate from each group
        List<Detection> finalDetections = new ArrayList<>();

        for (List<CandidateDetection> group : groups) {
            CandidateDetection bestCandidate = selectBestCandidate(group);

            if (bestCandidate != null && bestCandidate.qualityScore > 0.3) { // Quality threshold
                BoundingBox bbox = clampAndCreateBox(
                    (double) bestCandidate.rect.x() / image.cols(),
                    (double) bestCandidate.rect.y() / image.rows(),
                    (double) bestCandidate.rect.width() / image.cols(),
                    (double) bestCandidate.rect.height() / image.rows()
                );

                if (bbox != null) {
                    Map<String, Object> attributes = new HashMap<>(bestCandidate.attributes);
                    attributes.put("quality_score", bestCandidate.qualityScore);
                    attributes.put("consensus_votes", group.size());
                    attributes.put("primary_detector", bestCandidate.detector);

                    // Combine confidence with quality for final score and clamp to [0,1]
                    double finalConfidence = (bestCandidate.confidence + bestCandidate.qualityScore) / 2.0;
                    finalConfidence = clamp01(finalConfidence);

                    finalDetections.add(new Detection(
                        DetectionType.FACE.getCode(),
                        finalConfidence,
                        bbox,
                        attributes
                    ));
                }
            }
        }

        // 3. Sort by confidence and limit results
        finalDetections.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));

        return finalDetections;
    }

    /**
     * Group overlapping candidates based on IoU threshold.
     */
    private List<List<CandidateDetection>> groupOverlappingCandidates(List<CandidateDetection> candidates) {
        List<List<CandidateDetection>> groups = new ArrayList<>();
        boolean[] assigned = new boolean[candidates.size()];

        for (int i = 0; i < candidates.size(); i++) {
            if (assigned[i]) continue;

            List<CandidateDetection> group = new ArrayList<>();
            group.add(candidates.get(i));
            assigned[i] = true;

            // Find all overlapping candidates
            for (int j = i + 1; j < candidates.size(); j++) {
                if (assigned[j]) continue;

                if (calculateIoU(candidates.get(i).rect, candidates.get(j).rect) > 0.5) {
                    group.add(candidates.get(j));
                    assigned[j] = true;
                }
            }

            groups.add(group);
        }

        return groups;
    }

    /**
     * Calculate Intersection over Union (IoU) between two rectangles.
     */
    private double calculateIoU(Rect rect1, Rect rect2) {
        double x1 = Math.max(rect1.x(), rect2.x());
        double y1 = Math.max(rect1.y(), rect2.y());
        double x2 = Math.min(rect1.x() + rect1.width(), rect2.x() + rect2.width());
        double y2 = Math.min(rect1.y() + rect1.height(), rect2.y() + rect2.height());

        if (x2 <= x1 || y2 <= y1) return 0.0;

        double intersection = (x2 - x1) * (y2 - y1);
        double area1 = rect1.width() * rect1.height();
        double area2 = rect2.width() * rect2.height();
        double union = area1 + area2 - intersection;

        return union > 0 ? intersection / union : 0.0;
    }

    /**
     * Select the best candidate from a group based on combined score.
     */
    private CandidateDetection selectBestCandidate(List<CandidateDetection> group) {
        if (group.isEmpty()) return null;

        CandidateDetection best = null;
        double bestScore = -1.0;

        for (CandidateDetection candidate : group) {
            // Combined score: detector priority + confidence + quality
            double detectorWeight = getDetectorWeight(candidate.detector);
            double combinedScore = detectorWeight * 0.4 + candidate.confidence * 0.3 + candidate.qualityScore * 0.3;

            if (combinedScore > bestScore) {
                bestScore = combinedScore;
                best = candidate;
            }
        }

        return best;
    }

    /**
     * Get detector weight for fusion priority.
     */
    private double getDetectorWeight(String detector) {
        switch (detector) {
            case "yunet":
                return 1.0;      // Highest priority - most accurate
            case "dnn_ssd":
                return 0.8;    // Second priority - good for difficult cases
            case "haar_cascade":
                return 0.6; // Fallback - reliable but less accurate
            default:
                return 0.5;
        }
    }

    /**
     * Custom IntVector implementation for histogram calculation.
     * Replaces the missing IntVector class from OpenCV JavaCV.
     */
    private record IntVector(int... data) {

        public IntVector(int size) {
            this(new int[size]);
        }

        public int get(int index) {
            if (index >= 0 && index < data.length) {
                return data[index];
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
        }

        public void set(int index, int value) {
            if (index >= 0 && index < data.length) {
                data[index] = value;
            } else {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
            }
        }

        public int size() {
            return data.length;
        }

        @Override
        public int[] data() {
            return data.clone(); // Return copy for safety
        }

        @Override
        public String toString() {
            return "IntVector{" + Arrays.toString(data) + "}";
        }
    }

    /**
     * Custom FloatVector implementation for histogram calculation.
     * Replaces the missing FloatVector class from OpenCV JavaCV.
     */
    private record FloatVector(float... data) {

        public FloatVector(double... values) {
            this(new float[values.length]);
            for (int i = 0; i < values.length; i++) {
                this.data[i] = (float) values[i];
            }
        }

        public FloatVector(int size) {
            this(new float[size]);
        }

        public float get(int index) {
            if (index >= 0 && index < data.length) {
                return data[index];
            }
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
        }

        public void set(int index, float value) {
            if (index >= 0 && index < data.length) {
                data[index] = value;
            } else {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + data.length);
            }
        }

        public int size() {
            return data.length;
        }

        @Override
        public float[] data() {
            return data.clone(); // Return copy for safety
        }

        @Override
        public String toString() {
            return "FloatVector{" + Arrays.toString(data) + "}";
        }
    }

    /**
     * Custom histogram calculation function that works with our custom vector classes.
     * This replicates the functionality of calcHist with proper parameter handling.
     */
    private void calculateHistogram(Mat image, Mat mask, Mat histogram, IntVector histSize, FloatVector ranges) {
        try {
            // Create histogram size array
            int[] histSizeArray = new int[histSize.size()];
            for (int i = 0; i < histSize.size(); i++) {
                histSizeArray[i] = histSize.get(i);
            }

            // Create ranges array
            float[] rangesArray = new float[ranges.size()];
            for (int i = 0; i < ranges.size(); i++) {
                rangesArray[i] = ranges.get(i);
            }

            // Use OpenCV's calcHist with proper parameter arrays
            org.bytedeco.opencv.global.opencv_imgproc.calcHist(
                image,
                1, // number of images
                new int[]{0}, // channels
                mask,
                histogram,
                1, // histogram dimensionality
                histSizeArray,
                rangesArray
            );
        } catch (Exception e) {
            logger.debug("Histogram calculation failed: {}", e.getMessage());
        }
    }

    /**
     * Performs actual object detection (placeholder for now).
     * OpenCV backend primarily focuses on face detection
     */
    private List<Detection> performObjectDetection(ImageData imageData) {
        if (imageData == null || imageData.isEmpty()) {
            logger.warn("Image data is null or empty for object detection");
            return Collections.emptyList();
        }

        // OpenCV backend doesn't currently support object detection
        // This would typically use YOLO or other object detection models
        logger.debug("Object detection not implemented in OpenCV backend");
        return Collections.emptyList();
    }

    // Implementation of EmbeddingCapability interface, not VisionBackend
    public java.util.List<float[]> extractEmbeddings(io.github.codesapienbe.springvision.core.ImageData imageData) throws io.github.codesapienbe.springvision.core.exception.BaseVisionException {
        // Use SFace when available; otherwise fallback to default FaceBytes-based embeddings
        if (this.sFaceRecognizer == null) {
            return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
        }
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("Image data must not be null or empty");
        }
        try {
            // Decode image to Mat
            org.bytedeco.javacpp.BytePointer rawPointer = new org.bytedeco.javacpp.BytePointer(imageData.data());
            Mat buffer = new Mat(1, (int) imageData.getSizeInBytes(), org.bytedeco.opencv.global.opencv_core.CV_8U, rawPointer);
            Mat image = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buffer, IMREAD_COLOR);
            if (image == null || image.empty()) {
                rawPointer.deallocate();
                buffer.releaseReference();
                throw new VisionProcessingException("Failed to decode image data", "embeddings", "face");
            }
            buffer.releaseReference();
            rawPointer.deallocate();

            // Detect faces (reuse existing detection pipeline)
            java.util.List<Detection> faces = detectFaces(imageData);
            java.util.List<float[]> vectors = new java.util.ArrayList<>(Math.max(1, faces.size()));

            if (faces.isEmpty()) {
                // Fallback to default if SFace available but no faces found by our detector
                image.releaseReference();
                return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
            }

            for (Detection face : faces) {
                BoundingBox bbox = face.boundingBox();
                if (bbox == null) continue;
                int x = (int) Math.round(bbox.x() * image.cols());
                int y = (int) Math.round(bbox.y() * image.rows());
                int w = (int) Math.round(bbox.width() * image.cols());
                int h = (int) Math.round(bbox.height() * image.rows());
                x = Math.max(0, Math.min(x, image.cols() - 1));
                y = Math.max(0, Math.min(y, image.rows() - 1));
                w = Math.min(w, image.cols() - x);
                h = Math.min(h, image.rows() - y);
                if (w <= 0 || h <= 0) continue;

                // Crop and resize to 112x112 as expected by SFace
                Mat roi = new Mat(image, new Rect(x, y, w, h));
                Mat resized = new Mat();
                org.bytedeco.opencv.global.opencv_imgproc.resize(roi, resized, new Size(112, 112));

                float[] vec = computeSFaceEmbedding(resized);
                if (vec != null && vec.length > 0) {
                    vectors.add(vec);
                }

                roi.releaseReference();
                resized.releaseReference();
            }

            image.releaseReference();

            if (vectors.isEmpty()) {
                // As a robust fallback, use FaceBytes embeddings
                return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
            }
            return vectors;
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            // Fallback to default embeddings on any unexpected error to avoid breaking the API
            return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
        }
    }

    // New methods: verify and nearest-lookup

    @Override
    public boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        try {
            java.util.List<float[]> ea = extractEmbeddings(a);
            java.util.List<float[]> eb = extractEmbeddings(b);
            if (ea == null || eb == null || ea.isEmpty() || eb.isEmpty()) {
                return false;
            }
            float[] va = ea.get(0);
            float[] vb = eb.get(0);
            if (va == null || vb == null) return false;
            double dist;
            if ("euclidean".equalsIgnoreCase(metric)) {
                dist = euclideanDistance(va, vb);
            } else {
                dist = cosineDistance(va, vb);
            }
            return dist <= threshold;
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            // Fall back to default verification implementation
            return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.defaultVerify(a, b, metric, threshold);
        }
    }

    @Override
    public java.util.List<Integer> findNearestEmbeddings(ImageData probeImage, float[] probeEmbedding, java.util.List<float[]> galleryEmbeddings, String metric, int topK) throws BaseVisionException {
        if ((probeImage == null && probeEmbedding == null) || galleryEmbeddings == null || galleryEmbeddings.isEmpty()) {
            throw new IllegalArgumentException("Probe and gallery must be provided");
        }
        try {
            float[] probe = probeEmbedding;
            if (probe == null) {
                java.util.List<float[]> pe = extractEmbeddings(probeImage);
                if (pe == null || pe.isEmpty()) {
                    throw new VisionProcessingException("Failed to extract probe embedding", "findNearest", null);
                }
                probe = pe.getFirst();
            }
            // Normalize for cosine; for euclidean keep original
            float[] probeNorm = probe.clone();
            boolean useEuclidean = "euclidean".equalsIgnoreCase(metric);
            if (!useEuclidean) probeNorm = l2NormalizeLocal(probeNorm);

            java.util.List<java.util.Map.Entry<Integer, Double>> list = new java.util.ArrayList<>();
            for (int i = 0; i < galleryEmbeddings.size(); i++) {
                float[] g = galleryEmbeddings.get(i);
                if (g == null) continue;
                float[] gNorm = g.clone();
                if (!useEuclidean) gNorm = l2NormalizeLocal(gNorm);
                double dist = useEuclidean ? euclideanDistance(probeNorm, gNorm) : cosineDistance(probeNorm, gNorm);
                if (Double.isNaN(dist)) continue;
                list.add(new java.util.AbstractMap.SimpleEntry<>(i, dist));
            }
            list.sort(java.util.Comparator.comparingDouble(java.util.Map.Entry::getValue));
            int k = Math.max(0, Math.min(topK, list.size()));
            java.util.List<Integer> out = new java.util.ArrayList<>(k);
            for (int i = 0; i < k; i++) out.add(list.get(i).getKey());
            return out;
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            // Delegate to EmbeddingSupport as a robust fallback
            return io.github.codesapienbe.springvision.core.util.EmbeddingSupport.findNearest(probeImage, probeEmbedding, galleryEmbeddings, metric, topK);
        }
    }

    /**
     * Convenience: search a gallery of ImageData and return top-K matching indices.
     */
    public java.util.List<Integer> findNearestInGallery(ImageData probeImage, java.util.List<ImageData> galleryImages, String metric, int topK) throws BaseVisionException {
        if (probeImage == null || galleryImages == null || galleryImages.isEmpty()) {
            throw new IllegalArgumentException("Probe and gallery must not be null/empty");
        }
        java.util.List<float[]> galleryEmb = new java.util.ArrayList<>();
        for (ImageData gi : galleryImages) {
            try {
                java.util.List<float[]> e = extractEmbeddings(gi);
                if (e != null && !e.isEmpty()) galleryEmb.add(e.getFirst());
            } catch (Exception ignore) {
                // skip images that fail to produce embeddings
            }
        }
        return findNearestEmbeddings(probeImage, null, galleryEmb, metric, topK);
    }

    // Local helper functions for distances and normalization (kept private to avoid changing EmbeddingSupport API)
    private static float[] l2NormalizeLocal(float[] vec) {
        if (vec == null || vec.length == 0) return vec;
        double s = 0.0;
        for (float v : vec) s += v * v;
        s = Math.sqrt(s);
        if (s <= 0) return vec;
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = (float) (vec[i] / s);
        return out;
    }

    private static double cosineDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na <= 0 || nb <= 0) return Double.NaN;
        double sim = dot / (Math.sqrt(na) * Math.sqrt(nb));
        return 1.0 - sim;
    }

    private static double euclideanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }

    /**
     * Destroys resources and performs cleanup before shutdown.
     */
    @PreDestroy
    public void cleanup() {
        try {
            logger.info("Cleaning up OpenCV backend resources");

            // Release face cascade
            if (faceCascade != null) {
                faceCascade.close();
                faceCascade = null;
            }

            // Release frame converter
            if (frameConverter != null) {
                frameConverter.close();
                frameConverter = null;
            }

            // Release DNN resources
            if (dnnFaceNet != null) {
                dnnFaceNet = null;
            }

            // Release YuNet detector
            if (yuNetDetector != null) {
                yuNetDetector.close();
                yuNetDetector = null;
            }

            // Release SFace recognizer
            if (sFaceRecognizer != null) {
                sFaceRecognizer = null;
            }

            logger.info("OpenCV backend resources cleaned up successfully");
        } catch (Exception e) {
            logger.warn("Error during OpenCV backend cleanup: {}", e.getMessage());
        }
    }
}
