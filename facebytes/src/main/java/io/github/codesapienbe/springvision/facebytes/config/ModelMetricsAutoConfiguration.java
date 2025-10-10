package io.github.codesapienbe.springvision.facebytes.config;

import io.github.codesapienbe.springvision.facebytes.models.ModelManager;
import io.github.codesapienbe.springvision.facebytes.utils.Logs;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Auto-configuration that binds the application's Micrometer MeterRegistry into
 * the FaceBytes {@link ModelManager} so model load/inference metrics are exported
 * through the application's metrics backend (Prometheus, etc.).
 * <p>
 * This configuration is conditional and will only be active when a
 * {@link MeterRegistry} bean is present in the Spring context.
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
public class ModelMetricsAutoConfiguration {

    private final MeterRegistry meterRegistry;

    public ModelMetricsAutoConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void bindMeterRegistry() {
        try {
            ModelManager.setMeterRegistry(meterRegistry);
            Logs.info("ModelMetricsAutoConfiguration", "metrics.registry.bound", Map.of("meter_registry", meterRegistry.getClass().getName()));
        } catch (Throwable t) {
            Logs.warn("ModelMetricsAutoConfiguration", "metrics.registry.bind_failed", Map.of("error", t.getClass().getSimpleName()));
        }
    }
}
