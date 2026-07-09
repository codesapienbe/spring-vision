package io.github.codesapienbe.springvision.mcp.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies {@link SecurityConfig}'s authorization rules in isolation, without needing a
 * running Keycloak instance: a stubbed {@link JwtDecoder} bean stands in for the real one,
 * since the {@code jwt()} test post-processor pre-seeds an authenticated security context
 * and never actually invokes the decoder.
 */
@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class, SecurityConfigTest.ProbeController.class,
        SecurityConfigTest.StubJwtDecoderConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to /mcp is rejected")
    void unauthenticatedRequestToMcpIsRejected() throws Exception {
        mockMvc.perform(get("/mcp")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Request to /mcp with a valid JWT is allowed through")
    void authenticatedRequestWithJwtIsAllowed() throws Exception {
        mockMvc.perform(get("/mcp").with(jwt())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Health check is public, no token required")
    void actuatorHealthIsPublicWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/mcp")
        String mcp() {
            return "ok";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }
    }

    @TestConfiguration
    static class StubJwtDecoderConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-subject")
                    .build();
        }
    }
}
