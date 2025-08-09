package com.springvision.core.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;
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
    private static final String DNN_CAFFE_MODEL_URL_ALT =
        "https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/res10_300x300_ssd_iter_140000_fp16.caffemodel";

    /** OpenCV Zoo YuNet and SFace ONNX resources. */
    private static final String YUNET_ONNX_CLASSPATH = "/models/face_detection_yunet_2023mar.onnx";
    private static final String YUNET_ONNX_URL =
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx";
    private static final String SFACE_ONNX_CLASSPATH = "/models/face_recognition_sface_2021dec.onnx";
    private static final String SFACE_ONNX_URL =
        "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx";

    /** Eye cascade to validate candidate faces (reduces rectangular false positives). */
    private static final String DEFAULT_EYE_CASCADE_PATH = "/haarcascade_eye.xml";
    private static final String EYE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml";

    /** Profile-face cascade to detect side-view faces. */
    private static final String DEFAULT_PROFILE_FACE_CASCADE_PATH = "/haarcascade_profileface.xml";
    private static final String PROFILE_FACE_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_profileface.xml";

    /** LBP cascade often performs better across diverse skin tones. */
    private static final String DEFAULT_LBP_CASCADE_PATH = "/lbpcascade_frontalface.xml";
    private static final String LBP_CASCADE_DOWNLOAD_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/lbpcascades/lbpcascade_frontalface.xml";

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
     * Fail-safe guardrail for incoming image payload to avoid memory/DoS issues.
     */
    private static final long MAX_SAFE_IMAGE_BYTES = 50L * 1024 * 1024; // 50MB

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
    private String yuNetModelPath;
    private String sFaceModelPath;

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
            // Guard against overly large images
            if (imageData.getSizeInBytes() > MAX_SAFE_IMAGE_BYTES) {
                throw new VisionProcessingException(
                    "Image exceeds maximum allowed size", "image_too_large", DetectionType.FACE.getCode());
            }
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
            // Adaptive gamma correction based on mean luminance to mitigate color/lighting bias
            try {
                org.bytedeco.opencv.opencv_core.Scalar m = org.bytedeco.opencv.global.opencv_core.mean(grayImage);
                double mu = m.get(0);
                double gamma = 1.0;
                if (mu < 90) gamma = 0.7; // brighten darker images
                else if (mu > 180) gamma = 1.3; // tone down overly bright images
                if (Math.abs(gamma - 1.0) > 0.05) {
                    Mat gammaOut = applyGammaCorrection(grayImage, gamma);
                    grayImage.releaseReference();
                    grayImage = gammaOut;
                }
            } catch (Throwable t) {
                logger.debug("Gamma correction skipped: {}", t.getMessage());
            }
            // Improve robustness for multiple/smaller faces
            equalizeHist(grayImage, grayImage);
            // Apply CLAHE for low-light/contrast images
            try {
                org.bytedeco.opencv.opencv_imgproc.CLAHE clahe =
                    org.bytedeco.opencv.global.opencv_imgproc.createCLAHE(3.0, new Size(8, 8));
                Mat claheOut = new Mat();
                clahe.apply(grayImage, claheOut);
                grayImage.releaseReference();
                grayImage = claheOut;
                clahe.close();
            } catch (Throwable t) {
                logger.debug("CLAHE not applied: {}", t.getMessage());
            }

            // Collect candidates from all detectors for unified NMS
            List<Rect> allRects = new ArrayList<>();
            List<Float> allScores = new ArrayList<>();
            List<Detection> detections = new ArrayList<>();
            double totalConfidence = 0.0;

            // 1) Prefer YuNet if available (best accuracy/recall on small/crowded faces)
            try {
                if (yuNetDetector != null && !yuNetDetector.isNull()) {
                    collectYuNetCandidates(image, allRects, allScores);
                    logger.debug("YuNet stage completed", Map.of(
                        "correlationId", correlationId,
                        "candidates", allRects.size()
                    ));
                }
            } catch (Exception e) {
                logger.warn("YuNet detection failed: {}", e.getMessage());
            }

            // 1b) Then DNN SSD if available
            Net dnn = (dnnFaceNet != null) ? dnnFaceNet.get() : null;
            if (dnn != null && !dnn.empty()) {
                try {
                    List<Rect> candidateRects = new ArrayList<>();
                    List<Float> candidateScores = new ArrayList<>();

                    // Multi-scale: original and upscaled (for small faces)
                    collectDnnCandidates(image, 1.0, dnn, candidateRects, candidateScores);
                    double minDim = Math.min(image.cols(), image.rows());
                    // Prefer two upscales to improve small-face recall, capped for performance
                    double upscale1 = minDim < 900 ? Math.min(2.0, 900.0 / minDim) : 1.0;
                    double upscale2 = minDim < 1200 ? Math.min(3.0, 1200.0 / minDim) : 1.0;
                    if (upscale1 > 1.05) {
                        collectDnnCandidates(image, upscale1, dnn, candidateRects, candidateScores);
                    }
                    if (upscale2 > 1.2) {
                        collectDnnCandidates(image, upscale2, dnn, candidateRects, candidateScores);
                    }

                    allRects.addAll(candidateRects);
                    allScores.addAll(candidateScores);
                    logger.debug("DNN stage completed", Map.of(
                        "correlationId", correlationId,
                        "candidates", candidateRects.size(),
                        "kept", candidateRects.size()
                    ));
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
                    // More sensitive parameters to improve recall in crowded scenes
                    faceCascade.detectMultiScale(grayImage, faces, 1.05, 2, 0, minSize, maxSize);

                    logger.debug("Cascade classifier detected {} faces", faces.size());

                    for (long i = 0; i < faces.size(); i++) {
                        Rect faceRect = faces.get(i);
                        // Score cascades with size-based confidence
                        float c = (float) Math.min(0.95, Math.max(0.5, calculateFaceConfidence(faceRect, image.cols(), image.rows())));
                        allRects.add(new Rect(faceRect.x(), faceRect.y(), faceRect.width(), faceRect.height()));
                        allScores.add(c);
                    }

                    faces.deallocate();
                } catch (Exception e) {
                    logger.warn("Cascade classifier failed, using basic detection: {}", e.getMessage());
                }
            } else {
                logger.debug("Cascade classifier not available, using basic detection");
            }

            // 2b) LBP cascade candidates (complementary to Haar)
            try {
                if (lbpCascade != null && !lbpCascade.empty()) {
                    RectVector facesLbp = new RectVector();
                    int minDim = Math.min(image.cols(), image.rows());
                    Size minSize = new Size((int) (minDim * MIN_FACE_SIZE_RATIO), (int) (minDim * MIN_FACE_SIZE_RATIO));
                    Size maxSize = new Size((int) (minDim * MAX_FACE_SIZE_RATIO), (int) (minDim * MAX_FACE_SIZE_RATIO));
                    lbpCascade.detectMultiScale(grayImage, facesLbp, 1.05, 2, 0, minSize, maxSize);
                    for (long i = 0; i < facesLbp.size(); i++) {
                        Rect r = facesLbp.get(i);
                        allRects.add(new Rect(r.x(), r.y(), r.width(), r.height()));
                        allScores.add(0.75f);
                    }
                    facesLbp.deallocate();
                }
            } catch (Exception e) {
                logger.debug("LBP cascade stage skipped: {}", e.getMessage());
            }

            // 3) Profile-face detection (left profile and mirrored right profile) -> add as candidates
            try {
                if (profileCascade != null && !profileCascade.empty()) {
                    RectVector profiles = new RectVector();
                    int minDim = Math.min(image.cols(), image.rows());
                    Size minSize = new Size((int) (minDim * MIN_FACE_SIZE_RATIO), (int) (minDim * MIN_FACE_SIZE_RATIO));
                    Size maxSize = new Size((int) (minDim * MAX_FACE_SIZE_RATIO), (int) (minDim * MAX_FACE_SIZE_RATIO));
                    profileCascade.detectMultiScale(grayImage, profiles, 1.1, 3, 0, minSize, maxSize);
                    for (long i = 0; i < profiles.size(); i++) {
                        Rect r = profiles.get(i);
                        allRects.add(new Rect(r.x(), r.y(), r.width(), r.height()));
                        allScores.add(0.7f);
                    }
                    profiles.deallocate();

                    Mat flipped = new Mat();
                    flip(grayImage, flipped, 1);
                    RectVector profilesFlipped = new RectVector();
                    profileCascade.detectMultiScale(flipped, profilesFlipped, 1.1, 3, 0, minSize, maxSize);
                    for (long i = 0; i < profilesFlipped.size(); i++) {
                        Rect r = profilesFlipped.get(i);
                        int xMapped = image.cols() - r.x() - r.width();
                        Rect mapped = new Rect(Math.max(0, xMapped), r.y(), r.width(), r.height());
                        allRects.add(mapped);
                        allScores.add(0.7f);
                    }
                    profilesFlipped.deallocate();
                    flipped.releaseReference();
                }
            } catch (Exception e) {
                logger.debug("Profile-face detection skipped due to error: {}", e.getMessage());
            }

            // Final NMS over all candidates
            List<Integer> kept = nonMaximumSuppression(allRects, allScores, 0.5f);
            int cols = image.cols();
            int rows = image.rows();
            for (int idx : kept) {
                Rect r = allRects.get(idx);
                float conf = allScores.get(idx);
                boolean requireEye = Math.min(r.width(), r.height()) >= 24;
                if (requireEye && !isFaceLikely(grayImage, r)) continue;
                double nx = (double) r.x() / cols;
                double ny = (double) r.y() / rows;
                double nw = (double) r.width() / cols;
                double nh = (double) r.height() / rows;
                double confClamped = clamp01(conf);
                if (confClamped != conf) {
                    logger.debug("Confidence out of range adjusted", Map.of(
                        "original", conf,
                        "adjusted", confClamped,
                        "rect", r.toString()
                    ));
                }
                detections.add(Detection.of("face", confClamped, new BoundingBox(nx, ny, nw, nh)));
                totalConfidence += confClamped;
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
            // moved earlier into candidate fusion

            long processingTime = System.currentTimeMillis() - startTime;
            double averageConfidence = detections.isEmpty() ? 0.0 : clamp01(totalConfidence / Math.max(1, detections.size()));

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
            if (imageData.getSizeInBytes() > MAX_SAFE_IMAGE_BYTES) {
                throw new VisionProcessingException(
                    "Image exceeds maximum allowed size", "image_too_large", DetectionType.OBJECT.getCode());
            }
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
                // Prefer loading YuNet first, then DNN SSD as secondary
                loadYuNetDetector();
                loadDnnFaceDetector();
                loadEyeCascade();
                loadProfileCascade();
                loadLbpCascade();
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

            String protoPath = resolveModel(
                DNN_CAFFE_PROTO_TXT_CLASSPATH,
                new String[] { DNN_CAFFE_PROTO_TXT_URL },
                "deploy.prototxt");
            String modelPath = resolveModel(
                DNN_CAFFE_MODEL_CLASSPATH,
                new String[] { DNN_CAFFE_MODEL_URL, DNN_CAFFE_MODEL_URL_ALT },
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
                    } catch (Throwable ignore) {}
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

    /** Gamma correction helper operating in normalized float space. */
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
                x1 /= scale; y1 /= scale; x2 /= scale; y2 /= scale;
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

    /** Load the LBP face cascade with classpath-first, then remote fallback. */
    private void loadLbpCascade() {
        try {
            lbpCascade = new CascadeClassifier();
            // Try classpath
            try (java.io.InputStream is = getClass().getResourceAsStream(DEFAULT_LBP_CASCADE_PATH)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    java.io.File tmp = java.io.File.createTempFile("lbpcascade_frontalface", ".xml");
                    tmp.deleteOnExit();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(data); }
                    if (lbpCascade.load(tmp.getAbsolutePath())) {
                        logger.info("LBP cascade loaded from classpath");
                        return;
                    }
                }
            } catch (Exception ignore) {}

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
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) { fos.write(in.readAllBytes()); }
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

    /** Load YuNet ONNX-based face detector from classpath or short download. */
    private void loadYuNetDetector() {
        try {
            logger.info("Loading YuNet face detector...");
            String modelPath = resolveModel(
                YUNET_ONNX_CLASSPATH,
                new String[] { YUNET_ONNX_URL },
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

    /** Load SFace recognizer for embeddings (optional). */
    private void loadSFaceRecognizer() {
        try {
            logger.info("Loading SFace recognizer...");
            String modelPath = resolveModel(
                SFACE_ONNX_CLASSPATH,
                new String[] { SFACE_ONNX_URL },
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

    /** Collect YuNet candidates at image size; map to rects and scores. */
    private void collectYuNetCandidates(Mat image, List<Rect> outRects, List<Float> outScores) {
        if (yuNetDetector == null || yuNetDetector.isNull()) return;
        try {
            yuNetDetector.setInputSize(new Size(image.cols(), image.rows()));
            Mat det = new Mat();
            yuNetDetector.detect(image, det);
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
                    if (score < 0.5f) continue;
                    int left = Math.max(0, Math.round(x));
                    int top = Math.max(0, Math.round(y));
                    int widthPx = Math.max(1, Math.round(w));
                    int heightPx = Math.max(1, Math.round(h));
                    outRects.add(new Rect(left, top, widthPx, heightPx));
                    outScores.add(score);
                }
            }
            det.releaseReference();
        } catch (Throwable t) {
            logger.debug("YuNet detect failed: {}", t.getMessage());
        }
    }

    /** Compute SFace embedding for a face crop (112x112 BGR). */
    private float[] computeSFaceEmbedding(Mat alignedFace112x112) {
        if (sFaceRecognizer == null) return null;
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
            double norm = 0.0; for (float v : vec) norm += v * v; norm = Math.sqrt(norm);
            if (norm > 0) { for (int i = 0; i < vec.length; i++) vec[i] /= (float) norm; }
            return vec;
        } catch (Throwable t) {
            logger.debug("SFace embedding failed: {}", t.getMessage());
            return null;
        }
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** Resolve model by checking classpath, then a local cache under user home, then downloading to cache from URLs. */
    private String resolveModel(String classpathResource, String[] urls, String cacheFileName) {
        // 1) Classpath
        try (java.io.InputStream is = getClass().getResourceAsStream(classpathResource)) {
            if (is != null) {
                File tmp = File.createTempFile("sv-model-", cacheFileName);
                tmp.deleteOnExit();
                Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tmp.getAbsolutePath();
            }
        } catch (Exception ignore) {}

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
        } catch (Exception ignore) {}
        return null;
    }

    private Path getModelsBaseDir() {
        String userHome = System.getProperty("user.home", ".");
        Path dir = Path.of(userHome, ".spring-vision/models");
        try { Files.createDirectories(dir); } catch (Exception ignore) {}
        return dir;
    }
}
