package io.github.codesapienbe.springvision.mcp.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps the role names found at a configurable JWT claim path to {@code ROLE_*} authorities.
 * Spring Security's default converter only reads a flat {@code scope}/{@code scp} claim; identity
 * providers nest roles differently (Keycloak realm roles sit at {@code realm_access.roles}, client
 * roles at {@code resource_access.<client>.roles}), so the path comes from
 * {@link McpSecurityProperties#rolesClaimPath()}.
 */
public class JwtClaimRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    static final String ROLE_PREFIX = "ROLE_";

    private final List<String> claimPath;

    /**
     * Creates the converter.
     * @param claimPath one claim name per nesting level, e.g. {@code [realm_access, roles]}.
     */
    public JwtClaimRolesConverter(List<String> claimPath) {
        this.claimPath = List.copyOf(claimPath);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object node = jwt.getClaims();
        for (String name : claimPath) {
            if (!(node instanceof Map<?, ?> claims)) {
                return List.of();
            }
            node = claims.get(name);
        }
        if (!(node instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .map(String::valueOf)
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .toList();
    }
}
