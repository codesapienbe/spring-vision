package io.github.codesapienbe.springvision.core.error;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.core.logging.VisionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Comprehensive error handling and recovery mechanism for the Spring Vision framework.
 *
 * <p>This class provides centralized error handling with retry mechanisms, circuit breaker
 * patterns, graceful degradation, and comprehensive error categorization and reporting.</p>
 *
 * <p>The error handler supports multiple recovery strategies and provides detailed
 * error analytics for monitoring and debugging.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    // Error tracking
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong retryableErrors = new AtomicLong(0);
    private final AtomicLong nonRetryableErrors = new AtomicLong(0);
    private final AtomicLong circuitBreakerTrips = new AtomicLong(0);

    // Circuit breaker state
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // Error categorization
    private final Map<String, AtomicLong> errorCountsByType = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCountsByComponent = new ConcurrentHashMap<>();

    // Default retry configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    private static final double DEFAULT_RETRY_MULTIPLIER = 2.0;

    /**
     * Executes an operation with retry logic and error handling.
     */
    public <T> T executeWithRetry(String component, String operation, Supplier<T> operationSupplier)
        throws BaseVisionException {
        return executeWithRetry(component, operation, operationSupplier, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * Executes an operation with custom retry configuration.
     */
    public <T> T executeWithRetry(String component, String operation, Supplier<T> operationSupplier,
                                  int maxRetries, long initialDelayMs) throws BaseVisionException {

        Exception lastException = null;
        long delayMs = initialDelayMs;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                // Check circuit breaker
                CircuitBreaker circuitBreaker = getCircuitBreaker(component);
                if (circuitBreaker.isOpen()) {
                    throw new VisionProcessingException("Circuit breaker is open for component: " + component);
                }

                // Execute operation
                T result = operationSupplier.get();

                // Record success
                circuitBreaker.recordSuccess();
                logSuccess(component, operation, attempt);

                return result;

            } catch (Exception e) {
                lastException = e;
                totalErrors.incrementAndGet();

                // Categorize error
                categorizeError(component, e);

                // Check if error is retryable and if we have retries left
                if (!isRetryableError(e) || attempt > maxRetries) {
                    nonRetryableErrors.incrementAndGet();
                    getCircuitBreaker(component).recordFailure();
                    logFinalError(component, operation, e, attempt);
                    throw wrapException(e);
                }

                retryableErrors.incrementAndGet();
                getCircuitBreaker(component).recordFailure();
                logRetryableError(component, operation, e, attempt, maxRetries, delayMs);

                // Wait before retry (only if we're not on the last attempt)
                if (attempt <= maxRetries) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new VisionProcessingException("Operation interrupted", ie);
                    }

                    // Exponential backoff
                    delayMs = (long) (delayMs * DEFAULT_RETRY_MULTIPLIER);
                }
            }
        }

        // This should never be reached, but just in case
        throw wrapException(lastException);
    }

    /**
     * Executes an operation with circuit breaker pattern.
     */
    public <T> T executeWithCircuitBreaker(String component, String operation, Supplier<T> operationSupplier)
        throws BaseVisionException {

        CircuitBreaker circuitBreaker = getCircuitBreaker(component);

        if (circuitBreaker.isOpen()) {
            circuitBreakerTrips.incrementAndGet();
            throw new VisionProcessingException("Circuit breaker is open for component: " + component);
        }

        try {
            T result = operationSupplier.get();
            circuitBreaker.recordSuccess();
            return result;

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            totalErrors.incrementAndGet();
            categorizeError(component, e);
            throw wrapException(e);
        }
    }

    /**
     * Executes an operation with graceful degradation.
     */
    public <T> T executeWithGracefulDegradation(String component, String operation,
                                                Supplier<T> primaryOperation,
                                                Function<Exception, T> fallbackOperation) {

        try {
            return executeWithRetry(component, operation, primaryOperation);

        } catch (Exception e) {
            logger.warn("Primary operation failed, using fallback: component={}, operation={}, error={}",
                component, operation, e.getMessage());

            try {
                return fallbackOperation.apply(e);

            } catch (Exception fallbackError) {
                logger.error("Fallback operation also failed: component={}, operation={}, error={}",
                    component, operation, fallbackError.getMessage());
                throw new RuntimeException("Both primary and fallback operations failed", fallbackError);
            }
        }
    }

    /**
     * Handles errors with custom error handler.
     */
    public void handleError(String component, String operation, Exception error, ErrorHandlerCallback callback) {
        totalErrors.incrementAndGet();
        categorizeError(component, error);

        try {
            callback.onError(component, operation, error);
        } catch (Exception callbackError) {
            logger.error("Error handler callback failed: component={}, operation={}, error={}",
                component, operation, callbackError.getMessage());
        }
    }

    /**
     * Gets error statistics.
     */
    public ErrorStatistics getErrorStatistics() {
        return ErrorStatistics.builder()
            .totalErrors(totalErrors.get())
            .retryableErrors(retryableErrors.get())
            .nonRetryableErrors(nonRetryableErrors.get())
            .circuitBreakerTrips(circuitBreakerTrips.get())
            .errorCountsByType(new ConcurrentHashMap<>(errorCountsByType))
            .errorCountsByComponent(new ConcurrentHashMap<>(errorCountsByComponent))
            .circuitBreakerStates(getCircuitBreakerStates())
            .build();
    }

    /**
     * Resets error statistics.
     */
    public void resetErrorStatistics() {
        totalErrors.set(0);
        retryableErrors.set(0);
        nonRetryableErrors.set(0);
        circuitBreakerTrips.set(0);
        errorCountsByType.clear();
        errorCountsByComponent.clear();

        // Reset circuit breakers
        circuitBreakers.values().forEach(CircuitBreaker::reset);
    }

    /**
     * Checks if an error is retryable.
     */
    private boolean isRetryableError(Exception error) {
        if (error instanceof BaseVisionException) {
            return ((BaseVisionException) error).isRetryable();
        }

        // Network-related errors are typically retryable
        String errorMessage = error.getMessage().toLowerCase();
        return errorMessage.contains("timeout") ||
            errorMessage.contains("connection") ||
            errorMessage.contains("network") ||
            errorMessage.contains("temporary") ||
            errorMessage.contains("unavailable");
    }

    /**
     * Categorizes errors for analytics.
     */
    private void categorizeError(String component, Exception error) {
        // Count by component
        errorCountsByComponent.computeIfAbsent(component, k -> new AtomicLong()).incrementAndGet();

        // Count by error type
        String errorType = error.getClass().getSimpleName();
        errorCountsByType.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Wraps exceptions in framework exceptions.
     */
    private BaseVisionException wrapException(Exception error) {
        if (error instanceof BaseVisionException) {
            return (BaseVisionException) error;
        }

        return new VisionProcessingException("Operation failed: " + error.getMessage(), error);
    }

    /**
     * Gets or creates a circuit breaker for a component.
     */
    private CircuitBreaker getCircuitBreaker(String component) {
        return circuitBreakers.computeIfAbsent(component, k -> new CircuitBreaker());
    }

    /**
     * Gets circuit breaker states.
     */
    private Map<String, String> getCircuitBreakerStates() {
        Map<String, String> states = new ConcurrentHashMap<>();
        circuitBreakers.forEach((component, breaker) ->
            states.put(component, breaker.isOpen() ? "OPEN" : "CLOSED"));
        return states;
    }

    /**
     * Logs successful operation.
     */
    private void logSuccess(String component, String operation, int attempt) {
        Map<String, Object> successData = Map.of(
            "attempt", attempt,
            "success", true
        );

        VisionLogger.info(component, "Operation completed successfully", successData);
    }

    /**
     * Logs retryable error.
     */
    private void logRetryableError(String component, String operation, Exception error,
                                   int attempt, int maxRetries, long delayMs) {
        Map<String, Object> errorData = Map.of(
            "attempt", attempt,
            "max_retries", maxRetries,
            "retry_delay_ms", delayMs,
            "error_type", error.getClass().getSimpleName(),
            "error_message", error.getMessage(),
            "retryable", true
        );

        VisionLogger.warn(component, "Retryable error occurred, will retry", errorData);
    }

    /**
     * Logs final error after all retries.
     */
    private void logFinalError(String component, String operation, Exception error, int attempt) {
        Map<String, Object> errorData = Map.of(
            "attempt", attempt,
            "error_type", error.getClass().getSimpleName(),
            "error_message", error.getMessage(),
            "retryable", false
        );

        VisionLogger.error(component, "Operation failed after all retries", error, errorData);
    }

    /**
     * Circuit breaker implementation.
     */
    private static class CircuitBreaker {
        private static final int FAILURE_THRESHOLD = 5;
        private static final long TIMEOUT_MS = 60000; // 1 minute

        private volatile CircuitState state = CircuitState.CLOSED;
        private volatile long failureCount = 0;
        private volatile long lastFailureTime = 0;

        public boolean isOpen() {
            if (state == CircuitState.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > TIMEOUT_MS) {
                    state = CircuitState.HALF_OPEN;
                }
            }
            return state == CircuitState.OPEN;
        }

        public void recordSuccess() {
            if (state == CircuitState.HALF_OPEN) {
                state = CircuitState.CLOSED;
                failureCount = 0;
            }
        }

        public void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();

            if (failureCount >= FAILURE_THRESHOLD) {
                state = CircuitState.OPEN;
            }
        }

        public void reset() {
            state = CircuitState.CLOSED;
            failureCount = 0;
            lastFailureTime = 0;
        }

        private enum CircuitState {
            CLOSED, OPEN, HALF_OPEN
        }
    }

    /**
     * Error handler callback interface.
     */
    @FunctionalInterface
    public interface ErrorHandlerCallback {
        void onError(String component, String operation, Exception error);
    }

    /**
     * Error statistics data class.
     */
    public static class ErrorStatistics {
        private final long totalErrors;
        private final long retryableErrors;
        private final long nonRetryableErrors;
        private final long circuitBreakerTrips;
        private final Map<String, AtomicLong> errorCountsByType;
        private final Map<String, AtomicLong> errorCountsByComponent;
        private final Map<String, String> circuitBreakerStates;

        private ErrorStatistics(Builder builder) {
            this.totalErrors = builder.totalErrors;
            this.retryableErrors = builder.retryableErrors;
            this.nonRetryableErrors = builder.nonRetryableErrors;
            this.circuitBreakerTrips = builder.circuitBreakerTrips;
            this.errorCountsByType = builder.errorCountsByType;
            this.errorCountsByComponent = builder.errorCountsByComponent;
            this.circuitBreakerStates = builder.circuitBreakerStates;
        }

        // Getters
        public long getTotalErrors() {
            return totalErrors;
        }

        public long getRetryableErrors() {
            return retryableErrors;
        }

        public long getNonRetryableErrors() {
            return nonRetryableErrors;
        }

        public long getCircuitBreakerTrips() {
            return circuitBreakerTrips;
        }

        public Map<String, AtomicLong> getErrorCountsByType() {
            return errorCountsByType;
        }

        public Map<String, AtomicLong> getErrorCountsByComponent() {
            return errorCountsByComponent;
        }

        public Map<String, String> getCircuitBreakerStates() {
            return circuitBreakerStates;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long totalErrors;
            private long retryableErrors;
            private long nonRetryableErrors;
            private long circuitBreakerTrips;
            private Map<String, AtomicLong> errorCountsByType;
            private Map<String, AtomicLong> errorCountsByComponent;
            private Map<String, String> circuitBreakerStates;

            public Builder totalErrors(long totalErrors) {
                this.totalErrors = totalErrors;
                return this;
            }

            public Builder retryableErrors(long retryableErrors) {
                this.retryableErrors = retryableErrors;
                return this;
            }

            public Builder nonRetryableErrors(long nonRetryableErrors) {
                this.nonRetryableErrors = nonRetryableErrors;
                return this;
            }

            public Builder circuitBreakerTrips(long circuitBreakerTrips) {
                this.circuitBreakerTrips = circuitBreakerTrips;
                return this;
            }

            public Builder errorCountsByType(Map<String, AtomicLong> errorCountsByType) {
                this.errorCountsByType = errorCountsByType;
                return this;
            }

            public Builder errorCountsByComponent(Map<String, AtomicLong> errorCountsByComponent) {
                this.errorCountsByComponent = errorCountsByComponent;
                return this;
            }

            public Builder circuitBreakerStates(Map<String, String> circuitBreakerStates) {
                this.circuitBreakerStates = circuitBreakerStates;
                return this;
            }

            public ErrorStatistics build() {
                return new ErrorStatistics(this);
            }
        }
    }
}
