package com.springvision.core.backend;

import com.springvision.core.BackendHealthInfo;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe, stability-enhanced wrapper around {@link OpenCvVisionBackend}.
 *
 * <p>This backend preserves the existing OpenCV-based workflow and semantics while
 * adding production-grade stability features:
 * <ul>
 *   <li>Initialization and detection timeouts</li>
 *   <li>Circuit breaker for failure isolation</li>
 *   <li>Thread-safe state management with read/write locks</li>
 *   <li>Background maintenance for memory pressure checks</li>
 *   <li>Lightweight metrics for success/failure rates</li>
 * </ul>
 *
 * <p>All detection calls are delegated to the underlying {@link OpenCvVisionBackend}
 * to ensure no change in business logic or observable behavior. This class can be
 * optionally wired where additional resilience is desired without changing defaults.</p>
 */
public final class StableOpenCvVisionBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(StableOpenCvVisionBackend.class);

    private static final String BACKEND_ID = "opencv-stable";
    private static final String DISPLAY_NAME = "OpenCV Vision Backend (Stable Wrapper)";

    private static final Duration INITIALIZATION_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration DETECTION_TIMEOUT = Duration.ofSeconds(30);

    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final ExecutorService detectionExecutor;
    private final ScheduledExecutorService maintenanceExecutor;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    private final AtomicLong totalDetections = new AtomicLong(0);
    private final AtomicLong failedDetections = new AtomicLong(0);

    private final CircuitBreaker circuitBreaker;
    private final MemoryManager memoryManager;

    private volatile BackendState state = BackendState.UNINITIALIZED;

    private final OpenCvVisionBackend delegate;

    /**
     * Creates a new stability-enhanced backend wrapping a fresh {@link OpenCvVisionBackend}.
     */
    public StableOpenCvVisionBackend() {
        this(new OpenCvVisionBackend());
    }

    /**
     * Creates a new stability-enhanced backend wrapping the provided delegate.
     *
     * @param delegate the underlying {@link OpenCvVisionBackend}; must not be null
     */
    public StableOpenCvVisionBackend(OpenCvVisionBackend delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate backend must not be null");
        this.detectionExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            new NamedThreadFactory("opencv-stable-detect")
        );
        this.maintenanceExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("opencv-stable-maint"));
        this.circuitBreaker = new CircuitBreaker("opencv-detection", 0.5, Duration.ofMinutes(1));
        this.memoryManager = new MemoryManager(Runtime.getRuntime().maxMemory() / 2); // conservative guardrail

        scheduleMaintenance();
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
        // Delegate version to keep reporting consistent with underlying OpenCV implementation
        return delegate.getVersion();
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return delegate.getSupportedDetectionTypes();
    }

    @Override
    public boolean isHealthy() {
        stateLock.readLock().lock();
        try {
            return state == BackendState.READY && !shutdownRequested.get() && delegate.isHealthy();
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long start = System.currentTimeMillis();
        try {
            long total = totalDetections.get();
            long failed = failedDetections.get();
            double successRate = total > 0 ? (1.0 - ((double) failed / total)) : 1.0;
            String message = String.format("Stable wrapper healthy - Success rate: %.2f%%, Total: %d", successRate * 100, total);

            if (isHealthy()) {
                return BackendHealthInfo.healthy(getBackendId(), message, System.currentTimeMillis() - start);
            }
            return BackendHealthInfo.unhealthy(getBackendId(), "Backend not ready", "State: " + state, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return BackendHealthInfo.unhealthy(getBackendId(), "Health check failed", e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public void initialize() throws BaseVisionException {
        stateLock.writeLock().lock();
        try {
            if (state != BackendState.UNINITIALIZED) {
                logger.warn("Stable backend already initialized, current state: {}", state);
                return;
            }
            state = BackendState.INITIALIZING;
            Future<?> initFuture = detectionExecutor.submit(() -> {
                try {
                    delegate.initialize();
                } catch (BaseVisionException e) {
                    throw new CompletionException(e);
                } catch (RuntimeException e) {
                    throw e;
                }
            });
            try {
                initFuture.get(INITIALIZATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                state = BackendState.READY;
                logger.info("Stable OpenCV backend initialized successfully");
            } catch (TimeoutException te) {
                state = BackendState.FAILED;
                throw new VisionBackendException("Initialization timeout", "init_timeout", null, te);
            } catch (ExecutionException ee) {
                state = BackendState.FAILED;
                Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                if (cause instanceof BaseVisionException bve) {
                    throw bve;
                }
                throw new VisionBackendException("Initialization failed", "init_error", null, cause);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                state = BackendState.FAILED;
                throw new VisionBackendException("Initialization interrupted", "init_interrupted", null, ie);
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");
        if (!circuitBreaker.canExecute()) {
            throw new VisionProcessingException("Circuit breaker is open", "circuit_open", DetectionType.FACE.getCode());
        }
        return executeWithTimeout(() -> delegate.detectFaces(imageData), DETECTION_TIMEOUT, DetectionType.FACE);
    }

    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");
        if (!circuitBreaker.canExecute()) {
            throw new VisionProcessingException("Circuit breaker is open", "circuit_open", DetectionType.OBJECT.getCode());
        }
        return executeWithTimeout(() -> delegate.detectObjects(imageData), DETECTION_TIMEOUT, DetectionType.OBJECT);
    }

    @Override
    public void shutdown() throws BaseVisionException {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return;
        }
        logger.info("Shutting down StableOpenCvVisionBackend");
        try {
            maintenanceExecutor.shutdown();
            detectionExecutor.shutdown();
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
            if (!detectionExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                detectionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VisionBackendException("Shutdown interrupted", "shutdown_interrupt", null, e);
        } finally {
            delegate.shutdown();
            state = BackendState.SHUTDOWN;
        }
    }

    private void validateState() throws VisionBackendException {
        stateLock.readLock().lock();
        try {
            if (shutdownRequested.get()) {
                throw new VisionBackendException("Backend is shutting down", "shutdown_in_progress", null);
            }
            if (state != BackendState.READY) {
                throw new VisionBackendException("Backend not ready", "backend_not_ready", null);
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    private VisionResult executeWithTimeout(Callable<VisionResult> task, Duration timeout, DetectionType detectionType)
        throws BaseVisionException {
        Future<VisionResult> future = detectionExecutor.submit(() -> {
            memoryManager.checkMemoryAndCleanup();
            return task.call();
        });
        try {
            VisionResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            totalDetections.incrementAndGet();
            circuitBreaker.recordSuccess();
            return result;
        } catch (TimeoutException e) {
            failedDetections.incrementAndGet();
            circuitBreaker.recordFailure();
            future.cancel(true);
            throw new VisionProcessingException("Operation timeout", "timeout", detectionType.getCode(), e);
        } catch (ExecutionException e) {
            failedDetections.incrementAndGet();
            circuitBreaker.recordFailure();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof BaseVisionException bve) {
                throw bve;
            }
            throw new VisionProcessingException("Execution failed", "execution_error", detectionType.getCode(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedDetections.incrementAndGet();
            circuitBreaker.recordFailure();
            throw new VisionProcessingException("Execution interrupted", "execution_interrupt", detectionType.getCode(), e);
        }
    }

    private void scheduleMaintenance() {
        maintenanceExecutor.scheduleAtFixedRate(() -> {
            try {
                memoryManager.checkMemoryAndCleanup();
                logger.debug("Stable backend memory check completed");
            } catch (Exception e) {
                logger.warn("Memory check failed: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private enum BackendState { UNINITIALIZED, INITIALIZING, READY, FAILED, SHUTDOWN }

    /**
     * Simple circuit breaker suitable for local isolation of repeated failures.
     */
    private static final class CircuitBreaker {
        private final String name;
        private final double failureThreshold;
        private final Duration openTimeout;
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private volatile State state = State.CLOSED;
        private volatile Instant lastFailureTime = Instant.EPOCH;

        private enum State { CLOSED, OPEN, HALF_OPEN }

        CircuitBreaker(String name, double failureThreshold, Duration openTimeout) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.openTimeout = openTimeout;
        }

        boolean canExecute() {
            if (state == State.CLOSED) {
                return true;
            }
            if (state == State.OPEN) {
                if (Instant.now().isAfter(lastFailureTime.plus(openTimeout))) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            return true; // HALF_OPEN permits trial
        }

        void recordSuccess() {
            successCount.incrementAndGet();
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                failureCount.set(0);
            }
        }

        void recordFailure() {
            failureCount.incrementAndGet();
            lastFailureTime = Instant.now();
            long total = failureCount.get() + successCount.get();
            if (total >= 10 && (double) failureCount.get() / Math.max(1, total) > failureThreshold) {
                state = State.OPEN;
            }
        }
    }

    /**
     * Conservative memory guard to prevent excessive heap pressure.
     */
    private static final class MemoryManager {
        private final long maxMemoryBytes;

        MemoryManager(long maxMemoryBytes) {
            this.maxMemoryBytes = Math.max(128L * 1024 * 1024, maxMemoryBytes); // at least 128MB
        }

        void checkMemoryAndCleanup() throws VisionProcessingException {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            if (used > maxMemoryBytes) {
                System.gc();
                long rechecked = runtime.totalMemory() - runtime.freeMemory();
                if (rechecked > maxMemoryBytes) {
                    throw new VisionProcessingException("Memory usage exceeded limit", "memory_limit_exceeded", null);
                }
            }
        }
    }

    /**
     * Named daemon thread factory for executors.
     */
    private static final class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final AtomicLong counter = new AtomicLong(0);

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = Objects.requireNonNull(namePrefix);
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = defaultFactory.newThread(r);
            t.setName(namePrefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, ex) -> logger.error("Uncaught in {}: {}", thread.getName(), ex.getMessage(), ex));
            return t;
        }
    }
}
