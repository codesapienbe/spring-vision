package com.deepface.models;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.deepface.config.DeepFaceConfig;
import com.deepface.utils.Logs;

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

    private static volatile OrtEnvironment env;
    private static volatile OrtSession vggSession;
    private static final AtomicBoolean initAttempted = new AtomicBoolean(false);
    private static final AtomicBoolean hookInstalled = new AtomicBoolean(false);

    private ModelManager() {}

    public static boolean isVggFaceAvailable() {
        ensureInitialized();
        return vggSession != null;
    }

    public static void warmupIfAvailable() {
        try {
            ensureInitialized();
            if (vggSession != null) {
                Logs.info("ModelManager", "onnx.vgg.session_ready", Map.of());
            }
        } catch (Throwable t) {
            Logs.warn("ModelManager", "onnx.vgg.warmup_failed", Map.of("error", t.getClass().getSimpleName()));
        }
    }

    public static void shutdown() {
        safeClose();
    }

    public static float[] runVggFaceEmbedding(float[] nchw, long[] shape) throws OrtException {
        ensureInitialized();
        if (vggSession == null) {
            throw new OrtException("VGGFace ONNX session is not available");
        }
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            OrtSession.Result res = vggSession.run(Collections.singletonMap(inputName(vggSession), input));
            float[] out = flattenFirstFloat(res);
            res.close();
            return out;
        }
    }

    private static void ensureInitialized() {
        if (initAttempted.compareAndSet(false, true)) {
            try {
                String path = DeepFaceConfig.current().vggOnnxPath();
                if (path == null || path.isBlank()) {
                    path = System.getProperty(VGG_ONNX_SYS);
                }
                if (path == null || path.isBlank()) {
                    path = System.getenv(VGG_ONNX_ENV);
                }
                if (path == null || path.isBlank()) {
                    Logs.info("ModelManager", "onnx.vgg.path_missing", Map.of("enabled", false));
                    installShutdownHook();
                    return;
                }
                File f = new File(path);
                if (!f.exists() || !f.isFile()) {
                    Logs.warn("ModelManager", "onnx.vgg.file_not_found", Map.of("path", path));
                    installShutdownHook();
                    return;
                }
                env = OrtEnvironment.getEnvironment();
                vggSession = env.createSession(path, new OrtSession.SessionOptions());
                Logs.info("ModelManager", "onnx.vgg.loaded", Map.of("path", path));
                installShutdownHook();
            } catch (Throwable t) {
                Logs.warn("ModelManager", "onnx.vgg.init_failed", Map.of("error", t.getClass().getSimpleName()));
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
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }

    private static void safeClose() {
        try { if (vggSession != null) vggSession.close(); } catch (Throwable ignored) {}
        try { if (env != null) env.close(); } catch (Throwable ignored) {}
        vggSession = null;
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
