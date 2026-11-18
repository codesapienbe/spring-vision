package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies the {@code stdio} profile: it switches the MCP server to the stdio transport with no
 * web server, and {@link SecurityConfig} (JWT validation) only applies when there is one. That it
 * does apply in the servlet server is covered by {@link SecurityConfigTest}.
 */
class StdioProfileTest {

    @Test
    @DisplayName("application-stdio.yml enables the stdio transport and disables the web server")
    void stdioProfileEnablesStdioWithoutWebServer() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("stdio", new ClassPathResource("application-stdio.yml"));

        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).getProperty("spring.ai.mcp.server.stdio")).isEqualTo(true);
        assertThat(sources.get(0).getProperty("spring.main.web-application-type")).isEqualTo("none");
    }

    @Test
    @DisplayName("SecurityConfig is skipped outside a servlet web application")
    void securityConfigSkippedWithoutWebApplication() {
        new ApplicationContextRunner()
                .withUserConfiguration(SecurityConfig.class, KeycloakRealmRoleConverter.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecurityConfig.class);
                });
    }
}
