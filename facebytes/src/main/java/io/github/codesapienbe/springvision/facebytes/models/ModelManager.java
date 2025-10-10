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
import java.nio.FloatBuffer;
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

    public static boolean isVggFaceAvailable() {
        ensureInitialized();
        return vggSession != null;
    }

    public static boolean isArcFaceAvailable() {
        ensureInitialized();
        return arcfaceSession != null;
    }

    public static boolean isFacenetAvailable() {
        ensureInitialized();
        return facenetSession != null;
    }

    public static boolean isFacenet512Available() {
        ensureInitialized();
        return facenet512Session != null;
    }

    public static boolean isOpenFaceAvailable() {
        ensureInitialized();
        return openfaceSession != null;
    }

    public static boolean isSFaceAvailable() {
        ensureInitialized();
        return sfaceSession != null;
    }

    public static boolean isDeepFaceAvailable() {
        ensureInitialized();
        return deepfaceSession != null;
    }

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
     */
    public static void setMeterRegistry(MeterRegistry registry) {
        if (registry != null) meterRegistry = registry;
    }

    public static MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public static void shutdown() {
        safeClose();
    }

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

                String vggPath = DeepFaceConfig.current().vggOnnxPath();
                if (vggPath == null || vggPath.isBlank()) {
                    vggPath = System.getProperty(VGG_ONNX_SYS);
                }
                if (vggPath == null || vggPath.isBlank()) {
                    vggPath = System.getenv(VGG_ONNX_ENV);
                }
                // Try resolving or downloading the default model into the cache when not explicitly configured
                if (vggPath == null || vggPath.isBlank()) {
                    try {
                        vggPath = ModelDownloader.resolveOrDownload(null, "vggface.onnx");
                        if (vggPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "vggface", "path", vggPath));
                    } catch (Throwable ignored) {
                    }
                }
                String arcPath = DeepFaceConfig.current().arcFaceOnnxPath();
                if (arcPath == null || arcPath.isBlank()) {
                    arcPath = System.getProperty(ARCFACE_ONNX_SYS);
                }
                if (arcPath == null || arcPath.isBlank()) {
                    arcPath = System.getenv(ARCFACE_ONNX_ENV);
                }
                if (arcPath == null || arcPath.isBlank()) {
                    try {
                        arcPath = ModelDownloader.resolveOrDownload(null, "arcface.onnx");
                        if (arcPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "arcface", "path", arcPath));
                    } catch (Throwable ignored) {
                    }
                }
                String fnPath = DeepFaceConfig.current().facenetOnnxPath();
                if (fnPath == null || fnPath.isBlank()) {
                    fnPath = System.getProperty(FACENET_ONNX_SYS);
                }
                if (fnPath == null || fnPath.isBlank()) {
                    fnPath = System.getenv(FACENET_ONNX_ENV);
                }
                if (fnPath == null || fnPath.isBlank()) {
                    try {
                        fnPath = ModelDownloader.resolveOrDownload(null, "facenet128.onnx");
                        if (fnPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "facenet", "path", fnPath));
                    } catch (Throwable ignored) {
                    }
                }
                String fn512Path = DeepFaceConfig.current().facenet512OnnxPath();
                if (fn512Path == null || fn512Path.isBlank()) {
                    fn512Path = System.getProperty(FACENET512_ONNX_SYS);
                }
                if (fn512Path == null || fn512Path.isBlank()) {
                    fn512Path = System.getenv(FACENET512_ONNX_ENV);
                }
                if (fn512Path == null || fn512Path.isBlank()) {
                    try {
                        fn512Path = ModelDownloader.resolveOrDownload(null, "facenet512.onnx");
                        if (fn512Path != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "facenet512", "path", fn512Path));
                    } catch (Throwable ignored) {
                    }
                }
                String ofPath = DeepFaceConfig.current().openfaceOnnxPath();
                if (ofPath == null || ofPath.isBlank()) {
                    ofPath = System.getProperty(OPENFACE_ONNX_SYS);
                }
                if (ofPath == null || ofPath.isBlank()) {
                    ofPath = System.getenv(OPENFACE_ONNX_ENV);
                }
                if (ofPath == null || ofPath.isBlank()) {
                    try {
                        ofPath = ModelDownloader.resolveOrDownload(null, "openface.onnx");
                        if (ofPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "openface", "path", ofPath));
                    } catch (Throwable ignored) {
                    }
                }
                String sfPath = DeepFaceConfig.current().sfaceOnnxPath();
                if (sfPath == null || sfPath.isBlank()) {
                    sfPath = System.getProperty(SFACE_ONNX_SYS);
                }
                if (sfPath == null || sfPath.isBlank()) {
                    sfPath = System.getenv(SFACE_ONNX_ENV);
                }
                if (sfPath == null || sfPath.isBlank()) {
                    try {
                        sfPath = ModelDownloader.resolveOrDownload(null, "sface.onnx");
                        if (sfPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "sface", "path", sfPath));
                    } catch (Throwable ignored) {
                    }
                }
                String dfPath = DeepFaceConfig.current().deepfaceOnnxPath();
                if (dfPath == null || dfPath.isBlank()) {
                    dfPath = System.getProperty(DEEPFACE_ONNX_SYS);
                }
                if (dfPath == null || dfPath.isBlank()) {
                    dfPath = System.getenv(DEEPFACE_ONNX_ENV);
                }
                if (dfPath == null || dfPath.isBlank()) {
                    try {
                        dfPath = ModelDownloader.resolveOrDownload(null, "deepface.onnx");
                        if (dfPath != null)
                            Logs.info("ModelManager", "model.resolved", Map.of("model", "deepface", "path", dfPath));
                    } catch (Throwable ignored) {
                    }
                }
                if (vggPath == null && arcPath == null && fnPath == null && fn512Path == null && ofPath == null && sfPath == null && dfPath == null) {
                    Logs.info("ModelManager", "onnx.path_missing", Map.of("enabled", false));
                    installShutdownHook();
                    return;
                }
                env = (OrtEnvironment) OnnxRuntimeGuard.createEnvironment();
                // Load VGG
                if (vggPath != null && !vggPath.isBlank()) {
                    File f = new File(vggPath);
                    if (f.exists() && f.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "vggface", "path", vggPath));
                        long t0 = System.nanoTime();
                        vggSession = env.createSession(vggPath, new OrtSession.SessionOptions());
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "vggface").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "vggface", "path", vggPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "vggface", "path", vggPath));
                    }
                }
                if (arcPath != null && !arcPath.isBlank()) {
                    File f2 = new File(arcPath);
                    if (f2.exists() && f2.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "arcface", "path", arcPath));
                        long t0 = System.nanoTime();
                        arcfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, arcPath);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "arcface").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "arcface", "path", arcPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "arcface", "path", arcPath));
                    }
                }
                if (fnPath != null && !fnPath.isBlank()) {
                    File f3 = new File(fnPath);
                    if (f3.exists() && f3.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "facenet", "path", fnPath));
                        long t0 = System.nanoTime();
                        facenetSession = (OrtSession) OnnxRuntimeGuard.createSession(env, fnPath);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "facenet").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "facenet", "path", fnPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "facenet", "path", fnPath));
                    }
                }
                if (fn512Path != null && !fn512Path.isBlank()) {
                    File f4 = new File(fn512Path);
                    if (f4.exists() && f4.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "facenet512", "path", fn512Path));
                        long t0 = System.nanoTime();
                        facenet512Session = (OrtSession) OnnxRuntimeGuard.createSession(env, fn512Path);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "facenet512").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "facenet512", "path", fn512Path, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "facenet512", "path", fn512Path));
                    }
                }
                if (ofPath != null && !ofPath.isBlank()) {
                    File f5 = new File(ofPath);
                    if (f5.exists() && f5.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "openface", "path", ofPath));
                        long t0 = System.nanoTime();
                        openfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, ofPath);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "openface").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "openface", "path", ofPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "openface", "path", ofPath));
                    }
                }
                if (sfPath != null && !sfPath.isBlank()) {
                    File f6 = new File(sfPath);
                    if (f6.exists() && f6.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "sface", "path", sfPath));
                        long t0 = System.nanoTime();
                        sfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, sfPath);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "sface").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "sface", "path", sfPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "sface", "path", sfPath));
                    }
                }
                if (dfPath != null && !dfPath.isBlank()) {
                    File f7 = new File(dfPath);
                    if (f7.exists() && f7.isFile()) {
                        Logs.info("ModelLoader", "model.load.start", Map.of("model", "deepface", "path", dfPath));
                        long t0 = System.nanoTime();
                        deepfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, dfPath);
                        long loadMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                        Timer.builder("facebytes.model.load.time").tag("model", "deepface").register(meterRegistry).record(loadMs, TimeUnit.MILLISECONDS);
                        Logs.info("ModelLoader", "model.load.complete", Map.of("model", "deepface", "path", dfPath, "load_time_ms", loadMs));
                    } else {
                        Logs.warn("ModelLoader", "model.file_not_found", Map.of("model", "deepface", "path", dfPath));
                    }
                }
                installShutdownHook();
            } catch (Throwable t) {
                Logs.warn("ModelManager", "onnx.init_failed", Map.of("error", t.getClass().getSimpleName()));
                safeClose();
                installShutdownHook();
            }
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
