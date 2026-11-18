package io.github.codesapienbe.springvision.mcp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the {@code mcp} module as an OAuth2 Resource Server: incoming requests to
 * {@code /mcp} must carry a valid JWT from the OIDC issuer named by
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}. The roles in the token are read
 * from {@code spring.vision.mcp.security.roles-claim-path}; which tools they unlock is decided by
 * {@code spring.vision.mcp.security.tool-access} (see {@link ToolAccessPolicy}).
 * Actuator health checks stay open so infrastructure can probe liveness without a token.
 *
 * <p>Applies only to the servlet (Streamable-HTTP) server. The {@code stdio} profile runs no web
 * server, so there is no request to authenticate and this configuration is skipped.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableConfigurationProperties(McpSecurityProperties.class)
public class SecurityConfig {

    private final JwtClaimRolesConverter rolesConverter;

    /**
     * Creates a {@link SecurityConfig}.
     * @param security where the token's roles are found.
     */
    public SecurityConfig(McpSecurityProperties security) {
        this.rolesConverter = new JwtClaimRolesConverter(security.rolesClaimPath());
    }

    /**
     * Defines the security filter chain: public health checks, authenticated everything else,
     * JWTs validated against the issuer's JWKS endpoint, with the token's roles mapped to
     * authorities (see {@link JwtClaimRolesConverter}) for the tool-access checks.
     * @param http the {@link HttpSecurity} to configure.
     * @return the built {@link SecurityFilterChain}.
     * @throws Exception if the security configuration cannot be built.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
