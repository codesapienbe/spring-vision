package io.github.codesapienbe.springvision.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the {@code mcp} module as an OAuth2 Resource Server: incoming requests to
 * {@code /mcp} must carry a valid JWT issued by the configured Keycloak realm (see
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} and {@code keycloak/README.md}).
 * Actuator health checks stay open so infrastructure can probe liveness without a token.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Default constructor for {@link SecurityConfig}.
     */
    public SecurityConfig() {
        // Default constructor
    }

    /**
     * Defines the security filter chain: public health checks, authenticated everything else,
     * JWTs validated against Keycloak's JWKS endpoint.
     * @param http the {@link HttpSecurity} to configure.
     * @return the built {@link SecurityFilterChain}.
     * @throws Exception if the security configuration cannot be built.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
