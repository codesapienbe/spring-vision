package io.github.codesapienbe.springvision.starter.web.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous processing in the Spring Vision application.
 *
 * <p>This configuration enables async processing and provides a custom executor
 * for vision-related tasks with MDC propagation for logging correlation.</p>
 *
 * <p>⭐ OPTIMIZED: Uses Virtual Threads (Java 21+) for lightweight, scalable async execution.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a new {@link AsyncConfig}.
     */
    public AsyncConfig() {
        // Default constructor
    }

    /**
     * Creates a custom executor for asynchronous vision processing tasks.
     *
     * <p>⭐ OPTIMIZED: Uses Virtual Threads instead of platform threads for better scalability.
     * Virtual threads are lightweight and allow millions of concurrent tasks without the
     * overhead of traditional thread pools.</p>
     *
     * <p>This executor maintains MDC propagation to ensure logging context across async operations.</p>
     *
     * @return the configured executor
     */
    @Bean(name = "visionAsyncExecutor")
    public Executor visionAsyncExecutor() {
        // ⭐ Use Virtual Thread executor for unlimited scalability
        Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Wrap with MDC propagation decorator
        return new MdcPropagatingExecutor(virtualThreadExecutor);
    }

    /**
     * Executor wrapper that propagates MDC context to virtual threads.
     */
    private record MdcPropagatingExecutor(Executor delegate) implements Executor {

        @Override
        public void execute(Runnable command) {
            // Capture MDC context from the current thread
            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            // Wrap command with MDC propagation
            delegate.execute(() -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    command.run();
                } finally {
                    if (previous != null) {
                        MDC.setContextMap(previous);
                    } else {
                        MDC.clear();
                    }
                }
            });
        }
    }
}
