package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.djl.ModelStore;
import io.github.codesapienbe.springvision.mcp.SetupTool;
import io.github.codesapienbe.springvision.mcp.VisionTool;

/**
 * Pins the tool access shipped in {@code application.yml}: every granted name is a real tool,
 * {@code mcp-admin} gets every tool, and {@code mcp-user} gets everything except the sensitive set.
 */
class DefaultToolAccessTest {

    private static final Set<String> ADMIN_ONLY = Set.of(
            "extract_face_embeddings_b", "extract_face_embeddings_u", "extract_face_embeddings_b_validated",
            "recognize_identity_card_b", "recognize_identity_card_u",
            "recognize_driver_license_b", "recognize_driver_license_u",
            "recognize_license_plate_b", "recognize_license_plate_u",
            "detect_nsfw_b", "detect_nsfw_u", "detect_deepfake_b", "detect_deepfake_u",
            "detect_threats_b", "detectThreats", "authenticate_access_b", "authenticateAccess",
            "verify_faces_between_urls", "verify_faces_between_bytes",
            "lookup_faces_in_dataset_u", "lookup_faces_in_dataset_b",
            "store_identity_u", "list_identities", "delete_identity");

    private static McpSecurityProperties security;
    private static Set<String> toolNames;

    @BeforeAll
    static void loadShippedConfiguration() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        security = new Binder(ConfigurationPropertySources.from(sources))
                .bind("spring.vision.mcp.security", McpSecurityProperties.class)
                .get();
        VisionTool visionTool = new VisionTool(new VisionTemplate(mock(VisionBackend.class)));
        // Building the provider validates that every granted name is a real tool.
        SetupTool setupTool = new SetupTool(ModelStore.classpathOnly());
        toolNames = Arrays.stream(new ToolCallbackConfiguration()
                .tools(visionTool, setupTool, security, false, ModelStore.classpathOnly(), Duration.ZERO)
                .getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Test
    @DisplayName("mcp-admin may call every tool")
    void adminMayCallEveryTool() {
        ToolAccessPolicy policy = new ToolAccessPolicy(security.toolAccess());
        assertThat(toolNames).hasSize(69).contains("setup_status").allMatch(tool -> policy.allows(Set.of("mcp-admin"), tool));
    }

    @Test
    @DisplayName("mcp-user may call everything except the sensitive tools")
    void userMayCallAllButSensitiveTools() {
        ToolAccessPolicy policy = new ToolAccessPolicy(security.toolAccess());
        Set<String> denied = toolNames.stream()
                .filter(tool -> !policy.allows(Set.of("mcp-user"), tool))
                .collect(Collectors.toUnmodifiableSet());
        assertThat(denied).isEqualTo(ADMIN_ONLY);
    }

    @Test
    @DisplayName("Roles come from realm_access.roles, and the stdio caller is mcp-admin")
    void claimPathAndStdioRoles() {
        assertThat(security.rolesClaimPath()).containsExactly("realm_access", "roles");
        assertThat(security.stdioRoles()).containsExactly("mcp-admin");
    }
}
