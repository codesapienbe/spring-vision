package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.djl.Device;

/**
 * Unit tests for {@code DjlVisionBackend#loadWithGpuFallback}, the retry-on-CPU
 * mechanism used when a model fails to load on a configured GPU device.
 *
 * <p>Exercises the retry logic in isolation via reflection (both the target method
 * and its {@code ModelLoaderFn} parameter type are private) — deliberately without
 * touching real DJL model loading, since that needs network access this sandbox
 * doesn't have and would conflate "did the retry logic run" with "did the model
 * zoo respond."
 */
class DjlVisionBackendGpuFallbackTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        backend = new DjlVisionBackend();
    }

    @Nested
    @DisplayName("when the configured device is GPU")
    class WhenDeviceIsGpu {

        @Test
        @DisplayName("retries on CPU after a GPU failure, and succeeds")
        void retriesOnCpuAfterGpuFailure() throws Exception {
            setDevice(backend, Device.gpu());
            List<Device> devicesSeen = new ArrayList<>();

            invokeLoadWithGpuFallback(backend, "test model", device -> {
                devicesSeen.add(device);
                if (device.isGpu()) {
                    throw new RuntimeException("simulated CUDA failure");
                }
            });

            assertThat(devicesSeen).hasSize(2);
            assertThat(devicesSeen.get(0).isGpu()).isTrue();
            assertThat(devicesSeen.get(1).isGpu()).isFalse();
            assertThat(getCpuFallbackModels(backend)).containsExactly("test model");
        }

        @Test
        @DisplayName("does not retry when the GPU attempt succeeds")
        void doesNotRetryWhenGpuSucceeds() throws Exception {
            setDevice(backend, Device.gpu());
            AtomicInteger calls = new AtomicInteger();

            invokeLoadWithGpuFallback(backend, "test model", device -> calls.incrementAndGet());

            assertThat(calls.get()).isEqualTo(1);
            assertThat(getCpuFallbackModels(backend)).isEmpty();
        }

        @Test
        @DisplayName("propagates the CPU failure (with the GPU failure suppressed) when both attempts fail")
        void propagatesWhenBothFail() throws Exception {
            setDevice(backend, Device.gpu());

            Throwable thrown = catchInvocationCause(() ->
                invokeLoadWithGpuFallback(backend, "test model", device -> {
                    throw new RuntimeException(device.isGpu() ? "gpu failure" : "cpu failure too");
                }));

            assertThat(thrown).hasMessage("cpu failure too");
            assertThat(thrown.getSuppressed()).hasSize(1);
            assertThat(thrown.getSuppressed()[0]).hasMessage("gpu failure");
            assertThat(getCpuFallbackModels(backend)).isEmpty();
        }
    }

    @Nested
    @DisplayName("when the configured device is CPU")
    class WhenDeviceIsCpu {

        @Test
        @DisplayName("does not retry — a CPU failure is not a GPU-specific problem")
        void doesNotRetryOnCpuFailure() throws Exception {
            setDevice(backend, Device.cpu());
            AtomicInteger calls = new AtomicInteger();

            Throwable thrown = catchInvocationCause(() ->
                invokeLoadWithGpuFallback(backend, "test model", device -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("cpu failure");
                }));

            assertThat(calls.get()).isEqualTo(1);
            assertThat(thrown).hasMessage("cpu failure");
            assertThat(getCpuFallbackModels(backend)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // reflection helpers — loadWithGpuFallback and its ModelLoaderFn parameter
    // type are both private, so tests drive them the same way the rest of this
    // suite already drives other private DjlVisionBackend internals.
    // -----------------------------------------------------------------------

    @FunctionalInterface
    private interface DeviceAction {
        void run(Device device) throws Exception;
    }

    private static void setDevice(DjlVisionBackend target, Device device) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("device");
        f.setAccessible(true);
        f.set(target, device);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> getCpuFallbackModels(DjlVisionBackend target) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("cpuFallbackModels");
        f.setAccessible(true);
        return (Set<String>) f.get(target);
    }

    private static Class<?> modelLoaderFnClass() {
        for (Class<?> nested : DjlVisionBackend.class.getDeclaredClasses()) {
            if ("ModelLoaderFn".equals(nested.getSimpleName())) {
                return nested;
            }
        }
        throw new IllegalStateException("DjlVisionBackend.ModelLoaderFn not found — has it been renamed?");
    }

    private static void invokeLoadWithGpuFallback(DjlVisionBackend target, String label, DeviceAction action)
            throws Exception {
        Class<?> loaderFnClass = modelLoaderFnClass();
        Object proxy = Proxy.newProxyInstance(
            loaderFnClass.getClassLoader(),
            new Class<?>[] {loaderFnClass},
            (proxyInstance, method, args) -> {
                action.run((Device) args[0]);
                return null;
            });

        Method loadWithGpuFallback = DjlVisionBackend.class.getDeclaredMethod(
            "loadWithGpuFallback", String.class, loaderFnClass);
        loadWithGpuFallback.setAccessible(true);
        try {
            loadWithGpuFallback.invoke(target, label, proxy);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Runs the given reflective invocation and returns the exception it threw
     * (unwrapping InvocationTargetException), instead of letting the test fail on
     * an unexpected checked exception.
     */
    private static Throwable catchInvocationCause(ReflectiveCall call) {
        try {
            call.run();
            throw new AssertionError("Expected an exception but none was thrown");
        } catch (Exception e) {
            return e;
        }
    }

    @FunctionalInterface
    private interface ReflectiveCall {
        void run() throws Exception;
    }
}
