package io.github.codesapienbe.springvision.mcp.e2e;

import java.util.Set;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.VisionBackend;

/**
 * A backend with no capabilities, so the end-to-end tests start the real MCP server without
 * loading any model. Every tool that reaches it fails with {@code VisionUnsupportedException};
 * the tests use that to tell "the access check let the call through" apart from "denied".
 */
@TestConfiguration(proxyBeanMethods = false)
public class StubBackendConfiguration {

    @Bean
    VisionBackend stubVisionBackend() {
        return new VisionBackend() {
            @Override
            public String getBackendId() {
                return "e2e-stub";
            }

            @Override
            public String getDisplayName() {
                return "End-to-end test stub";
            }

            @Override
            public String getVersion() {
                return "0";
            }

            @Override
            public Set<DetectionType> getSupportedDetectionTypes() {
                return Set.of();
            }

            @Override
            public boolean isHealthy() {
                return true;
            }

            @Override
            public BackendHealthInfo getHealthInfo() {
                return BackendHealthInfo.healthy("e2e-stub", "stub", 0);
            }
        };
    }
}
