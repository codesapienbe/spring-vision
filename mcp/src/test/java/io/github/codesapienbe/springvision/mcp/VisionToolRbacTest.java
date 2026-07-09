package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;

/**
 * Verifies the {@code mcp-admin} role gate on {@link VisionTool}'s sensitive tools.
 *
 * <p>Enforcement is a manual {@link SecurityContextHolder} check ({@code requireAdminRole()})
 * rather than {@code @PreAuthorize}, because Spring AI's {@code MethodToolCallbackProvider}
 * does not reliably enforce method-security AOP on {@code @Tool} methods (see
 * spring-projects/spring-ai#2356, #3272) — these tests exercise the actual mechanism used.
 */
class VisionToolRbacTest {

    private static final String ADMIN_AUTHORITY = "ROLE_mcp-admin";
    private static final String USER_AUTHORITY = "ROLE_mcp-user";

    private VisionTool visionTool;

    @BeforeEach
    void setUp() {
        VisionBackend backend = mock(VisionBackend.class);
        VisionTemplate template = new VisionTemplate(backend);
        visionTool = new VisionTool(template);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("requireAdminRole()")
    class RequireAdminRole {

        @Test
        @DisplayName("Denies when there is no authentication at all")
        void deniesWhenNoAuthentication() {
            SecurityContextHolder.clearContext();
            assertThatThrownBy(() -> visionTool.requireAdminRole())
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Denies an authenticated caller without the mcp-admin role")
        void deniesWhenAuthenticatedWithoutAdminRole() {
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("plain-user", "n/a", USER_AUTHORITY));
            assertThatThrownBy(() -> visionTool.requireAdminRole())
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Allows a caller with the mcp-admin role")
        void allowsWhenAuthenticatedWithAdminRole() {
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-user", "n/a", ADMIN_AUTHORITY));
            assertThatCode(() -> visionTool.requireAdminRole()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Admin-tier tool wiring")
    class AdminTierToolWiring {

        @Test
        @DisplayName("listIdentities is denied without the mcp-admin role")
        void listIdentitiesDeniedWithoutAdminRole() {
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("plain-user", "n/a", USER_AUTHORITY));
            assertThatThrownBy(() -> visionTool.listIdentities())
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("listIdentities proceeds past the auth check with the mcp-admin role")
        void listIdentitiesProceedsPastAuthCheckWithAdminRole() {
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-user", "n/a", ADMIN_AUTHORITY));
            // The mock VisionBackend isn't a DjlVisionBackend, so it fails later with
            // VisionUnsupportedException - proving requireAdminRole() did not block the call.
            assertThatThrownBy(() -> visionTool.listIdentities())
                .isInstanceOf(VisionUnsupportedException.class);
        }
    }

    @Nested
    @DisplayName("General-tier tools stay accessible without the mcp-admin role")
    class GeneralTierToolsUnaffected {

        @Test
        @DisplayName("countFaces is not gated by requireAdminRole")
        void countFacesIsNotGatedByAdminRole() {
            SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("plain-user", "n/a", USER_AUTHORITY));
            // No admin role is present; if this were denied, it would surface as
            // AccessDeniedException specifically, which it must not.
            assertThatThrownBy(() -> visionTool.countFaces("not-a-real-url"))
                .isNotInstanceOf(AccessDeniedException.class);
        }
    }
}
