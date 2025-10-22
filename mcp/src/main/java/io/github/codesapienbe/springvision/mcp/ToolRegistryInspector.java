package io.github.codesapienbe.springvision.mcp;

import net.logstash.logback.argument.StructuredArguments;
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
 * across different Spring AI versions, this inspector tries a list of known fully qualified names.</p>
 *
 * <p>If a {@code ToolRegistry} bean is found, it uses reflection to call common methods
 * (e.g., {@code listRegisteredTools}, {@code getTools}) to retrieve and log the names of the
 * registered tool functions.</p>
 */
@Component
public class ToolRegistryInspector implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryInspector.class);

    /**
     * An array of fully qualified class names for the Spring AI {@code ToolRegistry}.
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
        if (event == null) {
            log.warn("Received null ContextRefreshedEvent",
                StructuredArguments.keyValue("event", "null_event_received"));
            return;
        }
        ApplicationContext ctx = event.getApplicationContext();

        for (String registryClassName : REGISTRY_TYPES) {
            try {
                Class<?> registryClass = Class.forName(registryClassName);
                Object registryBean = null;
                try {
                    registryBean = ctx.getBean(registryClass);
                } catch (Exception ex) {
                    // ignore if bean not found, try the next class name
                }

                if (registryBean != null) {
                    log.info("Found ToolRegistry bean",
                        StructuredArguments.keyValue("event", "tool_registry_found"),
                        StructuredArguments.keyValue("registry_type", registryClassName)
                    );
                    // Try several common inspection methods to get the list of tools
                    String[] inspectMethods = new String[]{"listRegistered", "listRegisteredTools", "getRegistered", "getRegisteredTools", "getTools", "registered"};
                    for (String m : inspectMethods) {
                        try {
                            Method mm = registryClass.getMethod(m);
                            Object val = mm.invoke(registryBean);
                            if (val instanceof Collection) {
                                Collection<?> tools = (Collection<?>) val;
                                log.info("ToolRegistry inspection",
                                    StructuredArguments.keyValue("event", "tool_registry_inspection"),
                                    StructuredArguments.keyValue("method", m),
                                    StructuredArguments.keyValue("tool_count", tools.size()),
                                    StructuredArguments.keyValue("tools", tools)
                                );
                            } else if (val instanceof Map) {
                                Map<?, ?> toolMap = (Map<?, ?>) val;
                                log.info("ToolRegistry inspection",
                                    StructuredArguments.keyValue("event", "tool_registry_inspection"),
                                    StructuredArguments.keyValue("method", m),
                                    StructuredArguments.keyValue("entry_count", toolMap.size()),
                                    StructuredArguments.keyValue("tools", toolMap)
                                );
                            } else {
                                log.info("ToolRegistry inspection",
                                    StructuredArguments.keyValue("event", "tool_registry_inspection"),
                                    StructuredArguments.keyValue("method", m),
                                    StructuredArguments.keyValue("result", val)
                                );
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
                log.warn("Failed to inspect registry",
                    StructuredArguments.keyValue("event", "tool_registry_inspection_failed"),
                    StructuredArguments.keyValue("registry_type", registryClassName),
                    StructuredArguments.keyValue("error", ex.getMessage())
                );
            }
        }

        log.debug("No Spring AI ToolRegistry found",
            StructuredArguments.keyValue("event", "tool_registry_not_found"),
            StructuredArguments.keyValue("message", "Skipping registry inspection")
        );
    }
}
