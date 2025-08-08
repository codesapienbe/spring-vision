package com.springvision.core.backend;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.springvision.core.BackendHealthInfo;
import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;

/**
 * DeepFace-based implementation of the VisionBackend interface.
 *
 * <p>This backend integrates with the Python DeepFace library by spawning a
 * sandboxed Python process that runs a minimal DeepFace snippet and returns
 * facial bounding boxes and optional attributes via tab-delimited output.</p>
 */
public class DeepFaceVisionBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(DeepFaceVisionBackend.class);

    /** Backend identifier. */
    public static final String BACKEND_ID = "deepface";
    /** Display name. */
    public static final String DISPLAY_NAME = "DeepFace Vision Backend";
    /** Semantic version of this integration. */
    public static final String VERSION = "0.1.0";

    private static final Duration DEFAULT_PROCESS_TIMEOUT = Duration.ofSeconds(20);

    private String pythonExecutable = "python";
    private String detectorBackend = "retinaface";
    private boolean enforceDetection = false;
    private Duration processTimeout = DEFAULT_PROCESS_TIMEOUT;

    // Analysis/embeddings config
    private boolean analyzeAge;
    private boolean analyzeGender;
    private boolean analyzeEmotion;
    private boolean analyzeRace;
    private boolean generateEmbeddings;
    private boolean normalizeEmbeddings = true;
    private String embeddingModel = "ArcFace";

    private boolean recognitionEnabled;
    private String recognitionGalleryDir;
    private String recognitionMetric = "cosine";
    private int recognitionTopK = 1;

    private boolean initialized;
    private BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private String healthErrorMessage = "Backend not initialized";
    private long lastHealthCheckTime;

    /**
     * Constructs a new DeepFace vision backend with default configuration.
     */
    public DeepFaceVisionBackend() {}

    /**
     * Sets the Python executable to invoke (prefer a virtualenv shim on PATH).
     *
     * @param pythonExecutable the executable name or absolute path
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setPythonExecutable(String pythonExecutable) {
        if (pythonExecutable != null && !pythonExecutable.isBlank()) {
            this.pythonExecutable = pythonExecutable;
        }
        return this;
    }

    /**
     * Sets the detector backend to use (e.g., retinaface, mtcnn, opencv, mediapipe).
     *
     * @param detectorBackend the DeepFace detector backend
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setDetectorBackend(String detectorBackend) {
        if (detectorBackend != null && !detectorBackend.isBlank()) {
            this.detectorBackend = detectorBackend;
        }
        return this;
    }

    /**
     * Sets whether to enforce detection (DeepFace enforce_detection flag).
     *
     * @param enforceDetection true to throw when no face detected
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setEnforceDetection(boolean enforceDetection) {
        this.enforceDetection = enforceDetection;
        return this;
    }

    /**
     * Sets the subprocess timeout duration for DeepFace calls.
     *
     * @param processTimeout the timeout duration, must be positive
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setProcessTimeout(Duration processTimeout) {
        if (processTimeout != null && !processTimeout.isNegative() && !processTimeout.isZero()) {
            this.processTimeout = processTimeout;
        }
        return this;
    }

    /**
     * Enables or disables age analysis per face.
     *
     * @param analyzeAge true to analyze age
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setAnalyzeAge(boolean analyzeAge) {
        this.analyzeAge = analyzeAge;
        return this;
    }

    /**
     * Enables or disables gender analysis per face.
     *
     * @param analyzeGender true to analyze gender
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setAnalyzeGender(boolean analyzeGender) {
        this.analyzeGender = analyzeGender;
        return this;
    }

    /**
     * Enables or disables dominant emotion analysis per face.
     *
     * @param analyzeEmotion true to analyze emotion
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setAnalyzeEmotion(boolean analyzeEmotion) {
        this.analyzeEmotion = analyzeEmotion;
        return this;
    }

    /**
     * Enables or disables race analysis per face.
     *
     * @param analyzeRace true to analyze race
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setAnalyzeRace(boolean analyzeRace) {
        this.analyzeRace = analyzeRace;
        return this;
    }

    /**
     * Enables or disables face embeddings generation per face.
     *
     * @param generateEmbeddings true to generate embeddings
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setGenerateEmbeddings(boolean generateEmbeddings) {
        this.generateEmbeddings = generateEmbeddings;
        return this;
    }

    /**
     * Enables or disables face embeddings normalization.
     *
     * @param normalizeEmbeddings true to normalize embeddings
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setNormalizeEmbeddings(boolean normalizeEmbeddings) {
        this.normalizeEmbeddings = normalizeEmbeddings;
        return this;
    }

    /**
     * Sets the embedding model name (ArcFace, VGG-Face, Facenet, OpenFace, DeepFace, SFace).
     *
     * @param embeddingModel the model name
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setEmbeddingModel(String embeddingModel) {
        if (embeddingModel != null && !embeddingModel.isBlank()) {
            this.embeddingModel = embeddingModel;
        }
        return this;
    }

    /**
     * Enables or disables face recognition.
     *
     * @param enabled true to enable recognition
     * @param galleryDir the directory containing the gallery images
     * @param metric the distance metric for recognition (e.g., "cosine", "euclidean")
     * @param topK the number of best matches to return
     * @return this backend for fluent configuration
     */
    public DeepFaceVisionBackend setRecognition(boolean enabled, String galleryDir, String metric, int topK) {
        this.recognitionEnabled = enabled;
        this.recognitionGalleryDir = galleryDir;
        if (metric != null && !metric.isBlank()) {
            this.recognitionMetric = metric;
        }
        if (topK > 0) {
            this.recognitionTopK = topK;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getBackendId() { return BACKEND_ID; }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    /** {@inheritDoc} */
    @Override
    public String getVersion() { return VERSION; }

    /** {@inheritDoc} */
    @Override
    public Set<DetectionType> getSupportedDetectionTypes() { return Set.of(DetectionType.FACE); }

    /** {@inheritDoc} */
    @Override
    public boolean isHealthy() { return initialized && healthStatus == BackendHealthInfo.HealthStatus.HEALTHY; }

    /** {@inheritDoc} */
    @Override
    public BackendHealthInfo getHealthInfo() {
        long responseTime = System.currentTimeMillis() - lastHealthCheckTime;
        if (isHealthy()) {
            return BackendHealthInfo.healthy(getBackendId(), "DeepFace backend is operational", responseTime);
        }
        return BackendHealthInfo.unhealthy(getBackendId(), "DeepFace backend is not operational", healthErrorMessage, responseTime);
    }

    /**
     * Initializes and probes Python/DeepFace availability.
     */
    @Override
    public void initialize() {
        this.lastHealthCheckTime = System.currentTimeMillis();
        List<String> command = buildProbeCommand();
        try {
            logger.info("DeepFace init probe starting", Map.of(
                "component", BACKEND_ID,
                "pythonExecutable", pythonExecutable
            ));
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(Math.max(3, (int) processTimeout.toSeconds()), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                this.initialized = false;
                this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
                this.healthErrorMessage = "Python/DeepFace probe timed out";
                logger.warn("DeepFace init timeout", Map.of(
                    "component", BACKEND_ID,
                    "timeoutSeconds", processTimeout.toSeconds()
                ));
                return;
            }
            int exit = process.exitValue();
            if (exit == 0) {
                this.initialized = true;
                this.healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
                this.healthErrorMessage = null;
                Map<String, Object> initMeta = new java.util.HashMap<>();
                initMeta.put("component", BACKEND_ID);
                initMeta.put("detectorBackend", detectorBackend);
                initMeta.put("enforceDetection", enforceDetection);
                initMeta.put("analyzeAge", analyzeAge);
                initMeta.put("analyzeGender", analyzeGender);
                initMeta.put("analyzeEmotion", analyzeEmotion);
                initMeta.put("analyzeRace", analyzeRace);
                initMeta.put("generateEmbeddings", generateEmbeddings);
                initMeta.put("normalizeEmbeddings", normalizeEmbeddings);
                initMeta.put("embeddingModel", embeddingModel);
                initMeta.put("recognitionEnabled", recognitionEnabled);
                initMeta.put("recognitionGalleryDir", recognitionGalleryDir);
                initMeta.put("recognitionMetric", recognitionMetric);
                initMeta.put("recognitionTopK", Integer.toString(recognitionTopK));
                initMeta.put("timeoutSeconds", processTimeout.toSeconds());
                logger.info("DeepFace backend initialized", initMeta);
            } else {
                String output = readAll(process.getInputStream());
                this.initialized = false;
                this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
                this.healthErrorMessage = safeTruncate(output, 500);
                logger.warn("DeepFace not available", Map.of(
                    "component", BACKEND_ID,
                    "exitCode", exit
                ));
            }
        } catch (Exception e) {
            this.initialized = false;
            this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            this.healthErrorMessage = e.getMessage();
            logger.warn("DeepFace init failed", Map.of(
                "component", BACKEND_ID,
                "error", e.getClass().getSimpleName()
            ), e);
        } finally {
            this.lastHealthCheckTime = System.currentTimeMillis();
        }
    }

    /** {@inheritDoc} */
    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        validateImageData(imageData);
        ensureInitialized();
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        logger.debug("Starting DeepFace face detection", Map.of(
            "correlation_id", correlationId,
            "component", BACKEND_ID,
            "imageSize", imageData.getSizeInBytes(),
            "detectorBackend", detectorBackend
        ));

        Path tempImage = null;
        try {
            tempImage = writeTempImage(imageData.data());
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (bufferedImage == null) {
                throw new VisionProcessingException("Unsupported image format", "unsupported_image_format", DetectionType.FACE.getCode(), null);
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            List<String> command = buildDetectCommand(tempImage);

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(processTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VisionProcessingException("DeepFace detection timed out", "deepface_timeout", DetectionType.FACE.getCode(), null);
            }
            int exit = process.exitValue();
            String output = readAll(process.getInputStream());
            if (exit != 0) {
                throw new VisionBackendException("DeepFace process failed: exit=" + exit, "deepface_process_error", null, null);
            }

            List<Detection> detections = parseDetections(output, width, height);
            double totalConfidence = detections.stream().mapToDouble(Detection::confidence).sum();

            long processingTime = System.currentTimeMillis() - startTime;
            double averageConfidence = detections.isEmpty() ? 0.0 : totalConfidence / detections.size();

            logger.debug("DeepFace face detection completed", Map.of(
                "correlation_id", correlationId,
                "component", BACKEND_ID,
                "facesDetected", detections.size(),
                "averageConfidence", averageConfidence,
                "processingTimeMs", processingTime
            ));

            return VisionResult.of(DetectionType.FACE, detections, averageConfidence, processingTime);
        } catch (VisionProcessingException | VisionBackendException e) {
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("DeepFace face detection failed", Map.of(
                "correlation_id", correlationId,
                "component", BACKEND_ID,
                "processingTimeMs", processingTime,
                "error", e.getClass().getSimpleName()
            ), e);
            throw new VisionProcessingException("Failed to detect faces using DeepFace", "deepface_face_detection", DetectionType.FACE.getCode(), e);
        } finally {
            if (tempImage != null) {
                try { Files.deleteIfExists(tempImage); } catch (IOException ignored) {}
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException("Object detection is not supported by DeepFace backend");
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new VisionBackendException("DeepFace backend is not initialized", "deepface_not_initialized", null, null);
        }
    }

    private void validateImageData(ImageData imageData) {
        if (imageData == null) throw new IllegalArgumentException("Image data must not be null");
        if (imageData.getSizeInBytes() <= 0) throw new IllegalArgumentException("Image data must not be empty");
        long maxAllowed = 20L * 1024 * 1024; // 20MB
        if (imageData.getSizeInBytes() > maxAllowed) throw new IllegalArgumentException("Image data exceeds maximum allowed size");
    }

    private static Path writeTempImage(byte[] bytes) throws IOException {
        SecureRandom random = new SecureRandom();
        String filename = "sv_df_" + Long.toUnsignedString(random.nextLong()) + ".img";
        Path path = Files.createTempFile(filename, null);
        Files.write(path, bytes);
        return path;
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) { sb.append(line).append('\n'); }
            return sb.toString();
        }
    }

    private static String safeTruncate(String s, int max) { if (s == null) return null; return s.length() <= max ? s : s.substring(0, max); }
    private static int safeParseInt(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static double safeParseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; } }
    private static double clamp01(double v) { return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v); }

    private static double[] parseEmbedding(String s) {
        String[] tokens = s.split(";");
        double[] out = new double[tokens.length];
        for (int i = 0; i < tokens.length; i++) { out[i] = safeParseDouble(tokens[i]); }
        return out;
    }

    // ---------- Refactored helpers: Python command building and script blocks ----------

    private List<String> buildProbeCommand() {
        return List.of(
            pythonExecutable,
            "-c",
            pythonProbeSnippet()
        );
    }

    private List<String> buildDetectCommand(Path imagePath) {
        return List.of(
            pythonExecutable,
            "-c",
            pythonDetectionScript(),
            imagePath.toAbsolutePath().toString(),
            detectorBackend,
            Boolean.toString(enforceDetection),
            Boolean.toString(analyzeAge),
            Boolean.toString(analyzeGender),
            Boolean.toString(analyzeEmotion),
            Boolean.toString(analyzeRace),
            Boolean.toString(generateEmbeddings),
            embeddingModel,
            Boolean.toString(normalizeEmbeddings),
            Boolean.toString(recognitionEnabled),
            recognitionGalleryDir == null ? "" : recognitionGalleryDir,
            recognitionMetric,
            Integer.toString(recognitionTopK)
        );
    }

    private String pythonProbeSnippet() {
        return """
            import importlib,sys
            sys.exit(0 if importlib.util.find_spec('deepface') else 3)
            """;
    }

    private String pythonDetectionScript() {
        return """
import sys, os
from deepface import DeepFace
import cv2

def clamp(val, lo, hi):
    return max(lo, min(hi, val))

def analyze_roi(img, x, y, w, h, an_age, an_gender, an_emotion, an_race):
    age = ''
    gender = ''
    emotion = ''
    race = ''
    if an_age or an_gender or an_emotion or an_race:
        x2 = clamp(x, 0, img.shape[1]); y2 = clamp(y, 0, img.shape[0])
        w2 = clamp(w, 1, img.shape[1]-x2); h2 = clamp(h, 1, img.shape[0]-y2)
        roi = img[y2:y2+h2, x2:x2+w2]
        actions = []
        if an_age: actions.append('age')
        if an_gender: actions.append('gender')
        if an_emotion: actions.append('emotion')
        if an_race: actions.append('race')
        try:
            res = DeepFace.analyze(img_path = roi, actions = actions, enforce_detection = False)
            if isinstance(res, list) and res:
                res = res[0]
            if an_age: age = str(int(res.get('age', '')) if res.get('age') is not None else '')
            if an_gender: gender = str(res.get('gender', '') or res.get('dominant_gender', ''))
            if an_emotion: emotion = str(res.get('dominant_emotion', ''))
            if an_race: race = str(res.get('dominant_race', ''))
        except Exception:
            pass
    return age, gender, emotion, race

def embed_roi(img, x, y, w, h, backend, emb_model, norm_emb):
    try:
        x2 = clamp(x, 0, img.shape[1]); y2 = clamp(y, 0, img.shape[0])
        w2 = clamp(w, 1, img.shape[1]-x2); h2 = clamp(h, 1, img.shape[0]-y2)
        roi = img[y2:y2+h2, x2:x2+w2]
        rep = DeepFace.represent(img_path=roi, detector_backend=backend, enforce_detection=False, model_name=emb_model, normalization=('Base' if not norm_emb else 'Facenet'))
        if isinstance(rep, list) and rep:
            vec = rep[0].get('embedding') or []
        else:
            vec = []
        return ';'.join([str(float(v)) for v in vec])
    except Exception:
        return ''

def recognize_image(image_path, gallery_dir, emb_model, backend, metric):
    try:
        df = DeepFace.find(img_path=image_path, db_path=gallery_dir, model_name=emb_model, detector_backend=backend, enforce_detection=False, distance_metric=metric)
        if isinstance(df, list) and df:
            df = df[0]
        if hasattr(df, 'empty') and not df.empty:
            row = df.iloc[0]
            ident = os.path.basename(str(row.get('identity', '')))
            dist = str(float(row.get(f'{metric}', 0.0)))
            return ident, dist
    except Exception:
        pass
    return '', ''

def main():
    img_path = sys.argv[1]
    backend = sys.argv[2]
    enf = sys.argv[3].lower() == 'true'
    an_age = sys.argv[4].lower() == 'true'
    an_gender = sys.argv[5].lower() == 'true'
    an_emotion = sys.argv[6].lower() == 'true'
    an_race = sys.argv[7].lower() == 'true'
    gen_emb = sys.argv[8].lower() == 'true'
    emb_model = sys.argv[9] if len(sys.argv) > 9 else 'ArcFace'
    norm_emb = sys.argv[10].lower() == 'true' if len(sys.argv) > 10 else True
    recog_enabled = sys.argv[11].lower() == 'true' if len(sys.argv) > 11 else False
    gallery_dir = sys.argv[12] if len(sys.argv) > 12 else ''
    metric = sys.argv[13] if len(sys.argv) > 13 else 'cosine'
    # top_k = int(sys.argv[14]) if len(sys.argv) > 14 else 1  # reserved

    img = cv2.imread(img_path)
    if img is None:
        sys.exit(2)

    faces = DeepFace.extract_faces(img_path=img_path, detector_backend=backend, enforce_detection=enf)
    for f in faces:
        fa = f.get('facial_area') or f.get('region') or {}
        x = int(fa.get('x', 0)); y = int(fa.get('y', 0)); w = int(fa.get('w', 0)); h = int(fa.get('h', 0))
        conf = f.get('confidence') or f.get('score') or 1.0
        try:
            conf = float(conf)
        except Exception:
            conf = 1.0
        age, gender, emotion, race = analyze_roi(img, x, y, w, h, an_age, an_gender, an_emotion, an_race)
        emb = embed_roi(img, x, y, w, h, backend, emb_model, norm_emb) if gen_emb else ''
        ident, dist = recognize_image(img_path, gallery_dir, emb_model, backend, metric) if recog_enabled and gallery_dir else ('', '')
        print(f"{x},{y},{w},{h},{conf}\t{age}\t{gender}\t{emotion}\t{race}\t{emb}\t{ident}\t{dist}")

if __name__ == '__main__':
    main()
            """;
    }

    // ---------- Output parsing ----------

    private List<Detection> parseDetections(String output, int width, int height) throws IOException {
        List<Detection> detections = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Detection detection = parseDetectionLine(line, width, height);
                if (detection != null) {
                    detections.add(detection);
                }
            }
        }
        return detections;
    }

    private Detection parseDetectionLine(String line, int width, int height) {
        String[] fields = line.split("\t");
        if (fields.length < 1) return null;
        String csv = fields[0];
        String[] parts = csv.split(",");
        if (parts.length < 5) return null;
        int x = safeParseInt(parts[0]);
        int y = safeParseInt(parts[1]);
        int w = safeParseInt(parts[2]);
        int h = safeParseInt(parts[3]);
        double conf = safeParseDouble(parts[4]);

        Map<String, Object> attrs = new HashMap<>();
        if (fields.length > 1 && !fields[1].isBlank()) attrs.put("age", safeParseInt(fields[1]));
        if (fields.length > 2 && !fields[2].isBlank()) attrs.put("gender", fields[2]);
        if (fields.length > 3 && !fields[3].isBlank()) attrs.put("emotion", fields[3]);
        if (fields.length > 4 && !fields[4].isBlank()) attrs.put("race", fields[4]);
        if (fields.length > 5 && !fields[5].isBlank()) {
            double[] emb = parseEmbedding(fields[5]);
            attrs.put("embedding_model", embeddingModel);
            attrs.put("embedding", emb);
        }
        if (fields.length > 6 && !fields[6].isBlank()) attrs.put("identity", fields[6]);
        if (fields.length > 7 && !fields[7].isBlank()) attrs.put("identity_distance", safeParseDouble(fields[7]));

        double nx = clamp01((double) x / Math.max(1, width));
        double ny = clamp01((double) y / Math.max(1, height));
        double nw = clamp01((double) w / Math.max(1, width));
        double nh = clamp01((double) h / Math.max(1, height));

        return new Detection("face", conf, new BoundingBox(nx, ny, nw, nh), attrs);
    }
}
