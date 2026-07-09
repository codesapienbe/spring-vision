package io.github.codesapienbe.springvision.mcp.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Maps Keycloak's nested {@code realm_access.roles} claim to Spring Security
 * {@code ROLE_*} authorities. Spring Security's default
 * {@code JwtGrantedAuthoritiesConverter} only reads a flat {@code scope}/{@code scp}
 * claim and has no knowledge of Keycloak's realm-role structure, so this replaces it
 * (wired into {@link SecurityConfig} via a {@code JwtAuthenticationConverter}).
 */
@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    /**
     * Default constructor for {@link KeycloakRealmRoleConverter}.
     */
    public KeycloakRealmRoleConverter() {
        // Default constructor
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null || !(realmAccess.get(ROLES_CLAIM) instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .map(String::valueOf)
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .collect(Collectors.toList());
    }
}
