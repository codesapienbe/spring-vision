package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link KeycloakRealmRoleConverter}.
 */
class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("test-subject");
    }

    @Test
    @DisplayName("Maps realm_access.roles to ROLE_* authorities")
    void mapsRealmRolesToRoleAuthorities() {
        Jwt jwt = baseJwt()
            .claim("realm_access", Map.of("roles", java.util.List.of("mcp-user", "mcp-admin")))
            .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_mcp-user", "ROLE_mcp-admin");
    }

    @Test
    @DisplayName("Returns no authorities when realm_access claim is absent")
    void returnsEmptyWhenRealmAccessClaimMissing() {
        Jwt jwt = baseJwt().build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("Returns no authorities when realm_access has no roles array")
    void returnsEmptyWhenRolesFieldMissing() {
        Jwt jwt = baseJwt()
            .claim("realm_access", Map.of("other", "value"))
            .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
