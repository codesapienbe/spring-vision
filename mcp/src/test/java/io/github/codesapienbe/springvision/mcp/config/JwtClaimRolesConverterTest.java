package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtClaimRolesConverterTest {

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("t").header("alg", "none").claims(c -> c.putAll(claims)).claim("sub", "s").build();
    }

    private static List<String> authorities(List<String> path, Map<String, Object> claims) {
        return new JwtClaimRolesConverter(path).convert(jwt(claims)).stream()
                .map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("Reads realm roles at realm_access.roles")
    void readsRealmRoles() {
        assertThat(authorities(List.of("realm_access", "roles"),
                Map.of("realm_access", Map.of("roles", List.of("mcp-user", "mcp-admin")))))
                .containsExactly("ROLE_mcp-user", "ROLE_mcp-admin");
    }

    @Test
    @DisplayName("Reads client roles, including a client id with dots")
    void readsClientRoles() {
        assertThat(authorities(List.of("resource_access", "my.app", "roles"),
                Map.of("resource_access", Map.of("my.app", Map.of("roles", List.of("vision"))))))
                .containsExactly("ROLE_vision");
    }

    @Test
    @DisplayName("Reads a flat top-level claim")
    void readsFlatClaim() {
        assertThat(authorities(List.of("roles"), Map.of("roles", List.of("a")))).containsExactly("ROLE_a");
    }

    @Test
    @DisplayName("No roles when the path is missing or does not end in a list")
    void emptyWhenMissingOrNotAList() {
        assertThat(authorities(List.of("realm_access", "roles"), Map.of())).isEmpty();
        assertThat(authorities(List.of("realm_access", "roles"), Map.of("realm_access", "x"))).isEmpty();
        assertThat(authorities(List.of("realm_access", "roles"),
                Map.of("realm_access", Map.of("roles", "mcp-admin")))).isEmpty();
    }
}
