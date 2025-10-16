package io.github.codesapienbe.springvision.facebytes.models;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.github.codesapienbe.springvision.core.util.OnnxRuntimeGuard;

import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * Minimal ONNX Runtime manager for FaceBytes models.
 * Loads models lazily from env/system properties and exposes a simple inference API.
 */
public final class ModelManager {

    private static final String VGG_ONNX_SYS = "facebytes.vggface.onnx";
    private static final String VGG_ONNX_ENV = "FACEBYTES_VGGFACE_ONNX_PATH";
    private static final String ARCFACE_ONNX_SYS = "facebytes.arcface.onnx";
    private static final String ARCFACE_ONNX_ENV = "FACEBYTES_ARCFACE_ONNX_PATH";
    private static final String FACENET_ONNX_SYS = "facebytes.facenet.onnx";
    private static final String FACENET_ONNX_ENV = "FACEBYTES_FACENET_ONNX_PATH";
    private static final String FACENET512_ONNX_SYS = "facebytes.facenet512.onnx";
    private static final String FACENET512_ONNX_ENV = "FACEBYTES_FACENET512_ONNX_PATH";
    private static final String OPENFACE_ONNX_SYS = "facebytes.openface.onnx";
    private static final String OPENFACE_ONNX_ENV = "FACEBYTES_OPENFACE_ONNX_PATH";
    private static final String SFACE_ONNX_SYS = "facebytes.sface.onnx";
    private static final String SFACE_ONNX_ENV = "FACEBYTES_SFACE_ONNX_PATH";
    private static final String DEEPFACE_ONNX_SYS = "facebytes.deepface.onnx";
    private static final String DEEPFACE_ONNX_ENV = "FACEBYTES_DEEPFACE_ONNX_PATH";

    private static volatile OrtEnvironment env;
    private static volatile OrtSession vggSession;
    private static volatile OrtSession arcfaceSession;
    private static volatile OrtSession facenetSession;
    private static volatile OrtSession facenet512Session;
    private static volatile OrtSession openfaceSession;
    private static volatile OrtSession sfaceSession;
    private static volatile OrtSession deepfaceSession;
    private static final AtomicBoolean initAttempted = new AtomicBoolean(false);
    private static final AtomicBoolean hookInstalled = new AtomicBoolean(false);
    // Micrometer registry (default to simple registry; can be overridden by tests/host app)
    private static volatile MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ModelManager() {
    }

    /**
     * Checks if the VGGFace model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isVggFaceAvailable() {
        ensureInitialized();
        return vggSession != null;
    }

    /**
     * Checks if the ArcFace model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isArcFaceAvailable() {
        ensureInitialized();
        return arcfaceSession != null;
    }

    /**
     * Checks if the Facenet model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isFacenetAvailable() {
        ensureInitialized();
        return facenetSession != null;
    }

    /**
     * Checks if the Facenet512 model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isFacenet512Available() {
        ensureInitialized();
        return facenet512Session != null;
    }

    /**
     * Checks if the OpenFace model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isOpenFaceAvailable() {
        ensureInitialized();
        return openfaceSession != null;
    }

    /**
     * Checks if the SFace model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isSFaceAvailable() {
        ensureInitialized();
        return sfaceSession != null;
    }

    /**
     * Checks if the DeepFace model is available.
     *
     * @return {@code true} if the model is available, {@code false} otherwise.
     */
    public static boolean isDeepFaceAvailable() {
        ensureInitialized();
        return deepfaceSession != null;
    }

    /**
     * Warms up the available models.
     */
    public static void warmupIfAvailable() {
        try {
            ensureInitialized();
            if (vggSession != null) Logs.info("ModelManager", "onnx.vgg.session_ready", Map.of());
            if (arcfaceSession != null) Logs.info("ModelManager", "onnx.arcface.session_ready", Map.of());
            if (facenetSession != null) Logs.info("ModelManager", "onnx.facenet.session_ready", Map.of());
            if (facenet512Session != null) Logs.info("ModelManager", "onnx.facenet512.session_ready", Map.of());
            if (openfaceSession != null) Logs.info("ModelManager", "onnx.openface.session_ready", Map.of());
            if (sfaceSession != null) Logs.info("ModelManager", "onnx.sface.session_ready", Map.of());
            if (deepfaceSession != null) Logs.info("ModelManager", "onnx.deepface.session_ready", Map.of());
        } catch (Throwable t) {
            Logs.warn("ModelManager", "onnx.warmup_failed", Map.of("error", t.getClass().getSimpleName()));
        }
    }

    /**
     * Replace the internal MeterRegistry (for integration with application metrics/Prometheus).
     *
     * @param registry The MeterRegistry to use.
     */
    public static void setMeterRegistry(MeterRegistry registry) {
        if (registry != null) meterRegistry = registry;
    }

    /**
     * Gets the MeterRegistry used by the ModelManager.
     *
     * @return The MeterRegistry.
     */
    public static MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    /**
     * Shuts down the ModelManager and releases all resources.
     */
    public static void shutdown() {
        safeClose();
    }

    /**
     * Runs the VGGFace model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runVggFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (vggSession == null) throw new OrtException("VGGFace ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = vggSession.run(Collections.singletonMap(inputName(vggSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "vggface").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "vggface", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "vggface"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "vggface"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the ArcFace model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runArcFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (arcfaceSession == null) throw new OrtException("ArcFace ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = arcfaceSession.run(Collections.singletonMap(inputName(arcfaceSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "arcface").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "arcface", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "arcface"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "arcface"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the Facenet model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runFacenetEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (facenetSession == null) throw new OrtException("Facenet ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = facenetSession.run(Collections.singletonMap(inputName(facenetSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "facenet").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "facenet", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "facenet"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "facenet"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the Facenet512 model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runFacenet512Embedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (facenet512Session == null) throw new OrtException("Facenet512 ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = facenet512Session.run(Collections.singletonMap(inputName(facenet512Session), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "facenet512").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "facenet512", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "facenet512"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "facenet512"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the OpenFace model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runOpenFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (openfaceSession == null) throw new OrtException("OpenFace ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = openfaceSession.run(Collections.singletonMap(inputName(openfaceSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "openface").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "openface", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "openface"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "openface"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the SFace model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runSFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (sfaceSession == null) throw new OrtException("SFace ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = sfaceSession.run(Collections.singletonMap(inputName(sfaceSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "sface").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "sface", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "sface"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "sface"));
            throw new OrtException(t.getMessage());
        }
    }

    /**
     * Runs the DeepFace model to get an embedding.
     *
     * @param nchw  The input tensor in NCHW format.
     * @param shape The shape of the input tensor.
     * @return The embedding as a float array.
     * @throws OrtException if the inference fails.
     */
    public static float[] runDeepFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (deepfaceSession == null) throw new OrtException("DeepFace ONNX session is not available");
        long t0 = System.nanoTime();
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = deepfaceSession.run(Collections.singletonMap(inputName(deepfaceSession), input));
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            Timer.builder("facebytes.model.inference.time").tag("model", "deepface").register(meterRegistry).record(durationMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelInference", "inference.completed", Map.of("model", "deepface", "duration_ms", durationMs));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        } catch (OrtException e) {
            Logs.error("ModelInference", "inference.failed", e, Map.of("model", "deepface"));
            throw e;
        } catch (Throwable t) {
            Logs.error("ModelInference", "inference.failed", t, Map.of("model", "deepface"));
            throw new OrtException(t.getMessage());
        }
    }

    private static void ensureInitialized() {
        if (initAttempted.compareAndSet(false, true)) {
            try {
                // If ONNX usage is disabled via configuration, skip initialization
                if (!DeepFaceConfig.current().onnxEnabled()) {
                    Logs.info("ModelManager", "onnx.disabled", Map.of("enabled", false));
                    installShutdownHook();
                    return;
                }

                // Guard: ensure ONNX runtime classes are present before attempting to create an environment
                if (!OnnxRuntimeGuard.isAvailable()) {
                    Logs.info("ModelManager", "onnx.runtime_unavailable", Map.of("available", false));
                    installShutdownHook();
                    return;
                }

                env = (OrtEnvironment) OnnxRuntimeGuard.createEnvironment();

                // Load models from classpath - simple and direct
                loadModelFromClasspath("arcface.onnx", "/models/facebytes/arcface.onnx", s -> arcfaceSession = s);
                loadModelFromClasspath("sface.onnx", "/models/facebytes/sface.onnx", s -> sfaceSession = s);
                loadModelFromClasspath("vggface.onnx", "/models/facebytes/vggface.onnx", s -> vggSession = s);
                loadModelFromClasspath("facenet128.onnx", "/models/facebytes/facenet128.onnx", s -> facenetSession = s);
                loadModelFromClasspath("facenet512.onnx", "/models/facebytes/facenet512.onnx", s -> facenet512Session = s);
                loadModelFromClasspath("openface.onnx", "/models/facebytes/openface.onnx", s -> openfaceSession = s);
                loadModelFromClasspath("deepface.onnx", "/models/facebytes/deepface.onnx", s -> deepfaceSession = s);

                if (vggSession == null && arcfaceSession == null && facenetSession == null &&
                    facenet512Session == null && openfaceSession == null && sfaceSession == null && deepfaceSession == null) {
                    Logs.warn("ModelManager", "onnx.no_models_found", Map.of(
                        "message", "No ONNX models found in classpath",
                        "expected_location", "classpath:/models/facebytes/*.onnx",
                        "action", "Run: mvn clean install to download models during build"
                    ));
                }

                installShutdownHook();
            } catch (Throwable t) {
                Logs.error("ModelManager", "init.failed", t, Map.of());
                installShutdownHook();
            }
        }
    }

    /**
     * Load a model from classpath resource.
     */
    private static void loadModelFromClasspath(String modelName, String classpathPath, java.util.function.Consumer<OrtSession> sessionSetter) {
        try {
            InputStream is = ModelManager.class.getResourceAsStream(classpathPath);
            if (is == null) {
                Logs.debug("ModelManager", "classpath_resource_not_found", Map.of("path", classpathPath, "model", modelName));
                return;
            }

            // Extract to temp file (ONNX Runtime requires file path, not stream)
            Path tempFile = java.nio.file.Files.createTempFile("spring-vision-", "-" + modelName);
            java.nio.file.Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();
            tempFile.toFile().deleteOnExit();

            String tempPath = tempFile.toAbsolutePath().toString();

            Logs.info("ModelLoader", "model.load.start", Map.of("model", modelName, "classpath", classpathPath));
            long t0 = System.nanoTime();
            OrtSession session = (OrtSession) OnnxRuntimeGuard.createSession(env, tempPath);
            long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            sessionSetter.accept(session);

            Timer.builder("facebytes.model.load.time").tag("model", modelName).register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
            Logs.info("ModelLoader", "model.load.complete", Map.of("model", modelName, "load_time_ms", loadMs));

        } catch (Exception e) {
            Logs.warn("ModelManager", "model_load_failed", Map.of("model", modelName, "error", e.getMessage()));
        }
    }

    /**
     * Resolves model path from configuration or classpath.
     * Priority: 1) System property, 2) Environment variable, 3) Classpath resource
     */
    private static String resolveModelPath(String modelKey, Map<String, String> classpathModels) {
        // Check system property and environment variable first
        String envKey = "FACEBYTES_" + modelKey.replace(".onnx", "").toUpperCase() + "_ONNX_PATH";
        String sysKey = "facebytes." + modelKey.replace(".onnx", "") + ".onnx";

        String configured = System.getProperty(sysKey);
        if (configured == null) configured = System.getenv(envKey);

        if (configured != null && !configured.isBlank()) {
            File f = new File(configured);
            if (f.exists() && f.isFile()) {
                return configured;
            }
            Logs.warn("ModelManager", "configured_path_invalid", Map.of("model", modelKey, "path", configured));
        }

        // Try to extract classpath resource to temp file
        String classpathLocation = classpathModels.get(modelKey);
        if (classpathLocation != null) {
            return extractClasspathResource(classpathLocation, modelKey);
        }

        return null;
    }

    /**
     * Extracts a classpath resource to a temporary file for ONNX Runtime to load.
     */
    private static String extractClasspathResource(String classpathPath, String modelKey) {
        try {
            InputStream is = ModelManager.class.getResourceAsStream(classpathPath);
            if (is == null) {
                Logs.warn("ModelManager", "classpath_resource_not_found", Map.of("path", classpathPath, "model", modelKey));
                return null;
            }

            // Extract to temp file (ONNX Runtime requires file path)
            Path tempDir = java.nio.file.Files.createTempDirectory("spring-vision-facebytes");
            Path tempFile = tempDir.resolve(modelKey);
            java.nio.file.Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            is.close();

            // Mark for deletion on exit
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();

            Logs.info("ModelManager", "classpath_resource_extracted", Map.of(
                "model", modelKey,
                "classpath", classpathPath,
                "temp_path", tempFile.toAbsolutePath().toString()
            ));

            return tempFile.toAbsolutePath().toString();
        } catch (Exception e) {
            Logs.error("ModelManager", "classpath_extraction_failed", e, Map.of("path", classpathPath, "model", modelKey));
            return null;
        }
    }

    private static void installShutdownHook() {
        if (hookInstalled.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        safeClose();
                        Logs.info("ModelManager", "onnx.resources_closed", Map.of());
                    } catch (Throwable ignored) {
                    }
                }, "facebytes-onnx-shutdown"));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void safeClose() {
        try {
            if (vggSession != null) vggSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (arcfaceSession != null) arcfaceSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (facenetSession != null) facenetSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (facenet512Session != null) facenet512Session.close();
        } catch (Throwable ignored) {
        }
        try {
            if (openfaceSession != null) openfaceSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (sfaceSession != null) sfaceSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (deepfaceSession != null) deepfaceSession.close();
        } catch (Throwable ignored) {
        }
        try {
            if (env != null) env.close();
        } catch (Throwable ignored) {
        }
        vggSession = null;
        arcfaceSession = null;
        facenetSession = null;
        facenet512Session = null;
        openfaceSession = null;
        sfaceSession = null;
        deepfaceSession = null;
        env = null;
    }

    private static String inputName(OrtSession session) {
        return session.getInputNames().iterator().next();
    }

    private static float[] flattenFirstFloat(OrtSession.Result res) throws OrtException {
        for (Map.Entry<String, OnnxValue> e : res) {
            OnnxValue ov = e.getValue();
            if (ov instanceof OnnxTensor t) {
                Object v = t.getValue();
                if (v instanceof float[] a) return a;
                if (v instanceof float[][] a2) return a2[0];
                if (v instanceof float[][][] a3) return a3[0][0];
            }
        }
        throw new OrtException("Unsupported ONNX output type");
    }
}
