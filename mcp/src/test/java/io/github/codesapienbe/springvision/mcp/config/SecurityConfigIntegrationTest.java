package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * End-to-end check that the resource server actually performs OIDC issuer discovery, fetches
 * a real JWKS over HTTP, and verifies a real RS256-signed JWT — as opposed to {@link
 * SecurityConfigTest}, which stubs the {@code JwtDecoder} bean directly and never exercises
 * that discovery/verification path. This is the closest verification achievable without a
 * real Keycloak instance (unavailable in sandboxed CI: pulling its container image is blocked
 * by network egress policy).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {SecurityConfigIntegrationTest.TestAutoConfig.class, SecurityConfig.class,
        KeycloakRealmRoleConverter.class, SecurityConfigIntegrationTest.ProbeController.class})
class SecurityConfigIntegrationTest {

    /**
     * Explicit, minimal autoconfiguration marker so this test boots its own small web
     * context (embedded Tomcat, Spring MVC) without Spring Boot falling back to searching
     * for {@code SpringVisionMcpServerApplication} — which would drag in the DJL-backed
     * beans this test deliberately avoids.
     */
    @EnableAutoConfiguration
    static class TestAutoConfig {
    }

    private static MockWebServer mockOidcServer;
    private static RSAKey rsaJwk;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    static void startMockOidcServer() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        rsaJwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .keyID("test-key-1")
            .build();

        mockOidcServer = new MockWebServer();
        mockOidcServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.startsWith("/.well-known/openid-configuration")) {
                    String issuer = mockOidcServer.url("/").toString().replaceAll("/$", "");
                    return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/jwks\"}");
                }
                if (path != null && path.startsWith("/jwks")) {
                    return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(new JWKSet(rsaJwk.toPublicJWK()).toString());
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        mockOidcServer.start();
    }

    @AfterAll
    static void stopMockOidcServer() throws Exception {
        mockOidcServer.shutdown();
    }

    @DynamicPropertySource
    static void registerIssuerUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> mockOidcServer.url("/").toString().replaceAll("/$", ""));
    }

    private String signToken(List<String> roles) throws Exception {
        String issuer = mockOidcServer.url("/").toString().replaceAll("/$", "");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("test-subject")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 60_000L))
            .claim("realm_access", Map.of("roles", roles))
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(rsaJwk));
        return jwt.serialize();
    }

    private ResponseEntity<String> callMcp(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange("http://localhost:" + port + "/mcp",
            org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    @DisplayName("Rejects requests with no token")
    void rejectsRequestsWithNoToken() {
        assertThat(callMcp(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Rejects a token signed by an untrusted key")
    void rejectsTokenFromUntrustedKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair rogue = generator.generateKeyPair();
        RSAKey rogueJwk = new RSAKey.Builder((RSAPublicKey) rogue.getPublic())
            .privateKey((RSAPrivateKey) rogue.getPrivate())
            .keyID("test-key-1")
            .build();
        String issuer = mockOidcServer.url("/").toString().replaceAll("/$", "");
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("rogue")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 60_000L))
            .claim("realm_access", Map.of("roles", List.of("mcp-admin")))
            .build();
        SignedJWT rogueJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rogueJwk.getKeyID()).build(), claims);
        rogueJwt.sign(new RSASSASigner(rogueJwk));

        assertThat(callMcp(rogueJwt.serialize()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Accepts a token genuinely verified against the discovered JWKS")
    void acceptsTokenVerifiedAgainstRealJwks() throws Exception {
        String token = signToken(List.of("mcp-user"));
        // Spring AI's auto-configured /mcp endpoint (pulled in by @EnableAutoConfiguration)
        // takes precedence over ProbeController's mapping and rejects a bare GET with 400 -
        // a protocol-level detail, not a security one. What this test asserts is that
        // authentication itself succeeded: the request got past Spring Security instead of
        // being rejected with 401/403.
        HttpStatus status = (HttpStatus) callMcp(token).getStatusCode();
        assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Health check stays public with no token")
    void healthCheckStaysPublic() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** Minimal stand-in for the real {@code /mcp} endpoint Spring AI registers. */
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
}
