package com.deepface.models;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.deepface.config.DeepFaceConfig;
import com.deepface.utils.Logs;
import com.springvision.core.util.OnnxRuntimeGuard;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private ModelManager() {}

    public static boolean isVggFaceAvailable() { ensureInitialized(); return vggSession != null; }
    public static boolean isArcFaceAvailable() { ensureInitialized(); return arcfaceSession != null; }
    public static boolean isFacenetAvailable() { ensureInitialized(); return facenetSession != null; }
    public static boolean isFacenet512Available() { ensureInitialized(); return facenet512Session != null; }
    public static boolean isOpenFaceAvailable() { ensureInitialized(); return openfaceSession != null; }
    public static boolean isSFaceAvailable() { ensureInitialized(); return sfaceSession != null; }
    public static boolean isDeepFaceAvailable() { ensureInitialized(); return deepfaceSession != null; }

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

    public static void shutdown() { safeClose(); }

    public static float[] runVggFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (vggSession == null) throw new OrtException("VGGFace ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = vggSession.run(Collections.singletonMap(inputName(vggSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runArcFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (arcfaceSession == null) throw new OrtException("ArcFace ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = arcfaceSession.run(Collections.singletonMap(inputName(arcfaceSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runFacenetEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (facenetSession == null) throw new OrtException("Facenet ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = facenetSession.run(Collections.singletonMap(inputName(facenetSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runFacenet512Embedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (facenet512Session == null) throw new OrtException("Facenet512 ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = facenet512Session.run(Collections.singletonMap(inputName(facenet512Session), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runOpenFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (openfaceSession == null) throw new OrtException("OpenFace ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = openfaceSession.run(Collections.singletonMap(inputName(openfaceSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runSFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (sfaceSession == null) throw new OrtException("SFace ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = sfaceSession.run(Collections.singletonMap(inputName(sfaceSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
        }
    }

    public static float[] runDeepFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (deepfaceSession == null) throw new OrtException("DeepFace ONNX session is not available");
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = deepfaceSession.run(Collections.singletonMap(inputName(deepfaceSession), input));
            float[] out = flattenFirstFloat(res); res.close(); return out;
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
                if (vggPath == null || vggPath.isBlank()) { vggPath = System.getProperty(VGG_ONNX_SYS); }
                if (vggPath == null || vggPath.isBlank()) { vggPath = System.getenv(VGG_ONNX_ENV); }
                String arcPath = DeepFaceConfig.current().arcFaceOnnxPath();
                if (arcPath == null || arcPath.isBlank()) { arcPath = System.getProperty(ARCFACE_ONNX_SYS); }
                if (arcPath == null || arcPath.isBlank()) { arcPath = System.getenv(ARCFACE_ONNX_ENV); }
                String fnPath = DeepFaceConfig.current().facenetOnnxPath();
                if (fnPath == null || fnPath.isBlank()) { fnPath = System.getProperty(FACENET_ONNX_SYS); }
                if (fnPath == null || fnPath.isBlank()) { fnPath = System.getenv(FACENET_ONNX_ENV); }
                String fn512Path = DeepFaceConfig.current().facenet512OnnxPath();
                if (fn512Path == null || fn512Path.isBlank()) { fn512Path = System.getProperty(FACENET512_ONNX_SYS); }
                if (fn512Path == null || fn512Path.isBlank()) { fn512Path = System.getenv(FACENET512_ONNX_ENV); }
                String ofPath = DeepFaceConfig.current().openfaceOnnxPath();
                if (ofPath == null || ofPath.isBlank()) { ofPath = System.getProperty(OPENFACE_ONNX_SYS); }
                if (ofPath == null || ofPath.isBlank()) { ofPath = System.getenv(OPENFACE_ONNX_ENV); }
                String sfPath = DeepFaceConfig.current().sfaceOnnxPath();
                if (sfPath == null || sfPath.isBlank()) { sfPath = System.getProperty(SFACE_ONNX_SYS); }
                if (sfPath == null || sfPath.isBlank()) { sfPath = System.getenv(SFACE_ONNX_ENV); }
                String dfPath = DeepFaceConfig.current().deepfaceOnnxPath();
                if (dfPath == null || dfPath.isBlank()) { dfPath = System.getProperty(DEEPFACE_ONNX_SYS); }
                if (dfPath == null || dfPath.isBlank()) { dfPath = System.getenv(DEEPFACE_ONNX_ENV); }
                if (vggPath == null && arcPath == null && fnPath == null && fn512Path == null && ofPath == null && sfPath == null && dfPath == null) {
                    Logs.info("ModelManager", "onnx.path_missing", Map.of("enabled", false));
                    installShutdownHook();
                    return;
                }
                env = (OrtEnvironment) OnnxRuntimeGuard.createEnvironment();
                if (vggPath != null && !vggPath.isBlank()) {
                    File f = new File(vggPath);
                    if (f.exists() && f.isFile()) { vggSession = env.createSession(vggPath, new OrtSession.SessionOptions()); Logs.info("ModelManager", "onnx.vgg.loaded", Map.of("path", vggPath)); }
                    else { Logs.warn("ModelManager", "onnx.vgg.file_not_found", Map.of("path", vggPath)); }
                }
                if (arcPath != null && !arcPath.isBlank()) {
                    File f2 = new File(arcPath);
                    if (f2.exists() && f2.isFile()) { arcfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, arcPath); Logs.info("ModelManager", "onnx.arcface.loaded", Map.of("path", arcPath)); }
                    else { Logs.warn("ModelManager", "onnx.arcface.file_not_found", Map.of("path", arcPath)); }
                }
                if (fnPath != null && !fnPath.isBlank()) {
                    File f3 = new File(fnPath);
                    if (f3.exists() && f3.isFile()) { facenetSession = (OrtSession) OnnxRuntimeGuard.createSession(env, fnPath); Logs.info("ModelManager", "onnx.facenet.loaded", Map.of("path", fnPath)); }
                    else { Logs.warn("ModelManager", "onnx.facenet.file_not_found", Map.of("path", fnPath)); }
                }
                if (fn512Path != null && !fn512Path.isBlank()) {
                    File f4 = new File(fn512Path);
                    if (f4.exists() && f4.isFile()) { facenet512Session = (OrtSession) OnnxRuntimeGuard.createSession(env, fn512Path); Logs.info("ModelManager", "onnx.facenet512.loaded", Map.of("path", fn512Path)); }
                    else { Logs.warn("ModelManager", "onnx.facenet512.file_not_found", Map.of("path", fn512Path)); }
                }
                if (ofPath != null && !ofPath.isBlank()) {
                    File f5 = new File(ofPath);
                    if (f5.exists() && f5.isFile()) { openfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, ofPath); Logs.info("ModelManager", "onnx.openface.loaded", Map.of("path", ofPath)); }
                    else { Logs.warn("ModelManager", "onnx.openface.file_not_found", Map.of("path", ofPath)); }
                }
                if (sfPath != null && !sfPath.isBlank()) {
                    File f6 = new File(sfPath);
                    if (f6.exists() && f6.isFile()) { sfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, sfPath); Logs.info("ModelManager", "onnx.sface.loaded", Map.of("path", sfPath)); }
                    else { Logs.warn("ModelManager", "onnx.sface.file_not_found", Map.of("path", sfPath)); }
                }
                if (dfPath != null && !dfPath.isBlank()) {
                    File f7 = new File(dfPath);
                    if (f7.exists() && f7.isFile()) { deepfaceSession = (OrtSession) OnnxRuntimeGuard.createSession(env, dfPath); Logs.info("ModelManager", "onnx.deepface.loaded", Map.of("path", dfPath)); }
                    else { Logs.warn("ModelManager", "onnx.deepface.file_not_found", Map.of("path", dfPath)); }
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
                    } catch (Throwable ignored) {}
                }, "facebytes-onnx-shutdown"));
            } catch (Throwable ignored) { }
        }
    }

    private static void safeClose() {
        try { if (vggSession != null) vggSession.close(); } catch (Throwable ignored) {}
        try { if (arcfaceSession != null) arcfaceSession.close(); } catch (Throwable ignored) {}
        try { if (facenetSession != null) facenetSession.close(); } catch (Throwable ignored) {}
        try { if (facenet512Session != null) facenet512Session.close(); } catch (Throwable ignored) {}
        try { if (openfaceSession != null) openfaceSession.close(); } catch (Throwable ignored) {}
        try { if (sfaceSession != null) sfaceSession.close(); } catch (Throwable ignored) {}
        try { if (deepfaceSession != null) deepfaceSession.close(); } catch (Throwable ignored) {}
        try { if (env != null) env.close(); } catch (Throwable ignored) {}
        vggSession = null;
        arcfaceSession = null;
        facenetSession = null;
        facenet512Session = null;
        openfaceSession = null;
        sfaceSession = null;
        deepfaceSession = null;
        env = null;
    }

    private static String inputName(OrtSession session) { return session.getInputNames().iterator().next(); }

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
