package io.github.codesapienbe.springvision.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

/**
 * Inspects the Spring AI {@code ToolRegistry} on application startup and logs the registered tools.
 * This component is useful for debugging and verifying that the correct tools are registered
 * with the Spring AI framework.
 *
 * <p>It listens for the {@link ContextRefreshedEvent} and then attempts to find a bean of type
 * {@code ToolRegistry}. Since the class name and package for {@code ToolRegistry} has changed
 * across different Spring AI versions, this inspector tries a list of known fully-qualified names.</p>
 *
 * <p>If a {@code ToolRegistry} bean is found, it uses reflection to call common methods
 * (e.g., {@code listRegisteredTools}, {@code getTools}) to retrieve and log the names of the
 * registered tool functions.</p>
 */
@Component
public class ToolRegistryInspector implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryInspector.class);

    /**
     * An array of fully-qualified class names for the Spring AI {@code ToolRegistry}.
     * This is necessary to support different versions of the Spring AI library where the
     * package structure may have changed.
     */
    private static final String[] REGISTRY_TYPES = new String[]{
        "org.springframework.ai.tool.ToolRegistry",
        "org.springframework.ai.tools.ToolRegistry",
        "org.springframework.ai.ToolRegistry",
        "org.springframework.ai.core.tool.ToolRegistry"
    };

    /**
     * Default constructor for {@link ToolRegistryInspector}.
     */
    public ToolRegistryInspector() {
        // Default constructor
    }

    /**
     * Handles the {@link ContextRefreshedEvent} to trigger the inspection of the tool registry.
     * This method is called once the Spring application context has been initialized or refreshed.
     *
     * @param event The context refreshed event.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();

        for (String registryClassName : REGISTRY_TYPES) {
            try {
                Class<?> registryClass = Class.forName(registryClassName);
                Object registryBean = null;
                try {
                    registryBean = ctx.getBean(registryClass);
                } catch (Exception ex) {
                    // ignore if bean not found, try next class name
                }

                if (registryBean != null) {
                    log.info("Found ToolRegistry bean of type {}", registryClassName);
                    // Try several common inspection methods to get the list of tools
                    String[] inspectMethods = new String[]{"listRegistered", "listRegisteredTools", "getRegistered", "getRegisteredTools", "getTools", "registered"};
                    for (String m : inspectMethods) {
                        try {
                            Method mm = registryClass.getMethod(m);
                            Object val = mm.invoke(registryBean);
                            if (val instanceof Collection) {
                                log.info("ToolRegistry.{}() -> {} entries", m, ((Collection<?>) val).size());
                                ((Collection<?>) val).forEach(item -> log.info(" - {}", item));
                            } else if (val instanceof Map) {
                                log.info("ToolRegistry.{}() -> map with {} entries", m, ((Map<?, ?>) val).size());
                                ((Map<?, ?>) val).forEach((k, v) -> log.info(" - {} -> {}", k, v));
                            } else {
                                log.info("ToolRegistry.{}() -> {}", m, val);
                            }
                            return; // Stop after the first successful inspection
                        } catch (NoSuchMethodException nsme) {
                            // Method not found, try the next one
                        }
                    }
                    return; // Stop after inspecting the found registry
                }
            } catch (ClassNotFoundException cnfe) {
                // Class not found, try the next one
            } catch (Exception ex) {
                log.warn("Failed to inspect registry {}: {}", registryClassName, ex.getMessage());
            }
        }

        log.debug("No Spring AI ToolRegistry found in context; skipping registry inspection.");
    }
}
