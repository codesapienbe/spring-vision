package io.github.codesapienbe.springvision.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection-guarded helper for interacting with ONNX Runtime without introducing a hard compile-time
 * dependency. Provides convenience methods to check availability and to create/close environments and sessions.
 * <p>
 * This utility is intentionally small and defensive: callers should always verify {@link #isAvailable()} before
 * attempting to create sessions. Methods return raw Object instances from the ONNX runtime API to avoid
 * compile-time coupling.
 */
public final class OnnxRuntimeGuard {
    private static final Logger logger = LoggerFactory.getLogger(OnnxRuntimeGuard.class);

    private static final boolean AVAILABLE;
    private static final Class<?> ORT_ENV_CLASS;
    private static final Class<?> ORT_SESSION_CLASS;
    private static final Class<?> SESSION_OPTIONS_CLASS;

    static {
        Class<?> env = null;
        Class<?> session = null;
        Class<?> sessionOptions = null;
        boolean available = false;
        try {
            env = Class.forName("ai.onnxruntime.OrtEnvironment");
            session = Class.forName("ai.onnxruntime.OrtSession");
            // Session options is a nested class; best-effort lookup
            try {
                sessionOptions = Class.forName("ai.onnxruntime.OrtSession$SessionOptions");
            } catch (ClassNotFoundException ignored) {
                sessionOptions = null;
            }
            available = true;
        } catch (Throwable t) {
            logger.info("component=OnnxRuntimeGuard message=onnx.unavailable reason={} ", t.getClass().getSimpleName());
            available = false;
        }
        ORT_ENV_CLASS = env;
        ORT_SESSION_CLASS = session;
        SESSION_OPTIONS_CLASS = sessionOptions;
        AVAILABLE = available;
    }

    private OnnxRuntimeGuard() {
        // utility
    }

    /**
     * Returns true if ONNX Runtime classes are present on the classpath.
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Create an OrtEnvironment instance via reflection. Caller must ensure ONNX runtime is available.
     *
     * @return raw OrtEnvironment instance
     */
    public static Object createEnvironment() {
        if (!AVAILABLE) {
            throw new IllegalStateException("ONNX Runtime is not available on classpath");
        }
        try {
            Method getEnv = ORT_ENV_CLASS.getMethod("getEnvironment");
            Object env = getEnv.invoke(null);
            logger.info("component=OnnxRuntimeGuard message=environment.created");
            return env;
        } catch (Throwable t) {
            logger.error("component=OnnxRuntimeGuard message=environment.create_failed cause={}", t.getClass().getSimpleName(), t);
            throw new IllegalStateException("Failed to create OrtEnvironment", t);
        }
    }

    /**
     * Create an OrtSession from an existing OrtEnvironment and model path. This method attempts to call
     * a suitable createSession overload via reflection. Returns the raw OrtSession object.
     *
     * @param environment raw OrtEnvironment instance (obtained from {@link #createEnvironment()})
     * @param modelPath   file-system path to the ONNX model
     * @return raw OrtSession instance
     * @throws Exception when session creation fails or ONNX runtime is unavailable
     */
    public static Object createSession(Object environment, String modelPath) throws Exception {
        if (!AVAILABLE) {
            throw new IllegalStateException("ONNX Runtime is not available on classpath");
        }
        if (environment == null) throw new IllegalArgumentException("environment must not be null");
        if (modelPath == null) throw new IllegalArgumentException("modelPath must not be null");

        // Try to find a createSession(String) method first
        try {
            Method createSessionSimple = ORT_ENV_CLASS.getMethod("createSession", String.class);
            Object session = createSessionSimple.invoke(environment, modelPath);
            logger.info("component=OnnxRuntimeGuard message=session.created method=singleArg path={}", modelPath);
            return session;
        } catch (NoSuchMethodException ignored) {
            // fallback to other overloads
        }

        // Try createSession(String, SessionOptions) if SessionOptions is available
        if (SESSION_OPTIONS_CLASS != null) {
            try {
                Method createSessionWithOptions = ORT_ENV_CLASS.getMethod("createSession", String.class, SESSION_OPTIONS_CLASS);
                Constructor<?> optionsCtor = SESSION_OPTIONS_CLASS.getDeclaredConstructor();
                optionsCtor.setAccessible(true);
                Object options = optionsCtor.newInstance();
                Object session = createSessionWithOptions.invoke(environment, modelPath, options);
                logger.info("component=OnnxRuntimeGuard message=session.created method=withOptions path={}", modelPath);
                return session;
            } catch (NoSuchMethodException ignored) {
                // continue to error below
            }
        }

        // As a last resort, enumerate methods named createSession and try to invoke the first compatible one
        for (Method m : ORT_ENV_CLASS.getMethods()) {
            if (!"createSession".equals(m.getName())) continue;
            Class<?>[] params = m.getParameterTypes();
            try {
                if (params.length == 1 && params[0].equals(String.class)) {
                    Object session = m.invoke(environment, modelPath);
                    logger.info("component=OnnxRuntimeGuard message=session.created method=reflective.path={}", modelPath);
                    return session;
                }
                if (params.length == 2 && params[0].equals(String.class)) {
                    // create a null for the second param (best-effort)
                    Object session = m.invoke(environment, modelPath, (Object) null);
                    logger.info("component=OnnxRuntimeGuard message=session.created method=reflectiveNull path={}", modelPath);
                    return session;
                }
            } catch (Throwable t) {
                // try next
            }
        }

        throw new IllegalStateException("No compatible createSession method found on OrtEnvironment");
    }

    /**
     * Close a raw OrtSession instance if possible. This method is best-effort and will swallow exceptions
     * after logging to avoid impacting caller flows.
     *
     * @param session raw OrtSession instance
     */
    public static void closeSessionQuietly(Object session) {
        if (session == null) return;
        if (!AVAILABLE) return;
        try {
            Method close = ORT_SESSION_CLASS.getMethod("close");
            close.invoke(session);
            logger.info("component=OnnxRuntimeGuard message=session.closed");
        } catch (Throwable t) {
            logger.warn("component=OnnxRuntimeGuard message=session.close_failed cause={} ", t.getClass().getSimpleName());
        }
    }

    /**
     * Close a raw OrtEnvironment instance if the API exposes a close/shutdown method. Swallows exceptions.
     *
     * @param environment raw OrtEnvironment instance
     */
    public static void closeEnvironmentQuietly(Object environment) {
        if (environment == null) return;
        if (!AVAILABLE) return;
        try {
            Method close = ORT_ENV_CLASS.getMethod("close");
            close.invoke(environment);
            logger.info("component=OnnxRuntimeGuard message=environment.closed");
        } catch (Throwable t) {
            logger.warn("component=OnnxRuntimeGuard message=environment.close_failed cause={} ", t.getClass().getSimpleName());
        }
    }
}
