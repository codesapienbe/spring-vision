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

@Component
public class ToolRegistryInspector implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryInspector.class);

    private static final String[] REGISTRY_TYPES = new String[]{
        "org.springframework.ai.tool.ToolRegistry",
        "org.springframework.ai.tools.ToolRegistry",
        "org.springframework.ai.ToolRegistry",
        "org.springframework.ai.core.tool.ToolRegistry"
    };

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
                    // ignore
                }

                if (registryBean != null) {
                    log.info("Found ToolRegistry bean of type {}", registryClassName);
                    // Try several common inspection methods
                    String[] inspectMethods = new String[]{"listRegistered", "listRegisteredTools", "getRegistered", "getRegisteredTools", "getTools", "registered"};
                    for (String m : inspectMethods) {
                        try {
                            Method mm = registryClass.getMethod(m);
                            Object val = mm.invoke(registryBean);
                            if (val instanceof Collection) {
                                log.info("ToolRegistry.{} -> {} entries", m, ((Collection<?>) val).size());
                                ((Collection<?>) val).forEach(item -> log.info(" - {}", item));
                            } else if (val instanceof Map) {
                                log.info("ToolRegistry.{} -> map with {} entries", m, ((Map<?, ?>) val).size());
                                ((Map<?, ?>) val).forEach((k, v) -> log.info(" - {} -> {}", k, v));
                            } else {
                                log.info("ToolRegistry.{} -> {}", m, val);
                            }
                            break;
                        } catch (NoSuchMethodException nsme) {
                            // try next
                        }
                    }
                    return;
                }
            } catch (ClassNotFoundException cnfe) {
                // try next
            } catch (Exception ex) {
                log.warn("Failed to inspect registry {}: {}", registryClassName, ex.getMessage());
            }
        }

        log.debug("No Spring AI ToolRegistry found in context; skipping registry inspection.");
    }
}

