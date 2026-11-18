package io.github.codesapienbe.springvision.mcp.e2e;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * A local OIDC issuer for the end-to-end tests: serves discovery and JWKS over real HTTP and
 * signs RS256 tokens, so the server under test does genuine issuer discovery and signature checks.
 */
public final class TestOidcIssuer implements AutoCloseable {

    private final MockWebServer server = new MockWebServer();
    private final RSAKey signingKey;

    private TestOidcIssuer(RSAKey signingKey) {
        this.signingKey = signingKey;
    }

    /**
     * Starts an issuer on a free loopback port.
     * @return the running issuer.
     * @throws IOException if the server cannot start.
     * @throws JOSEException if the key cannot be generated.
     */
    public static TestOidcIssuer start() throws IOException, JOSEException {
        TestOidcIssuer issuer = new TestOidcIssuer(new RSAKeyGenerator(2048).keyID("e2e-key").generate());
        issuer.server.setDispatcher(issuer.dispatcher());
        issuer.server.start();
        return issuer;
    }

    /** @return the issuer URI, as the {@code iss} claim and the server's issuer-uri carry it. */
    public String uri() {
        return server.url("/").toString().replaceAll("/$", "");
    }

    /**
     * A valid token from this issuer.
     * @param extraClaims claims to add, such as the role claim.
     * @return the serialized JWT.
     */
    public String token(Map<String, Object> extraClaims) {
        return sign(signingKey, uri(), new Date(System.currentTimeMillis() + 300_000L), extraClaims);
    }

    /**
     * A token with Keycloak-style realm roles at {@code realm_access.roles}.
     * @param roles the role names.
     * @return the serialized JWT.
     */
    public String realmRolesToken(String... roles) {
        return token(Map.of("realm_access", Map.of("roles", java.util.List.of(roles))));
    }

    /** @return a token that expired a minute ago. */
    public String expiredToken() {
        return sign(signingKey, uri(), new Date(System.currentTimeMillis() - 60_000L),
                Map.of("realm_access", Map.of("roles", java.util.List.of("mcp-admin"))));
    }

    /** @return a token whose {@code iss} is not this issuer. */
    public String foreignIssuerToken() {
        return sign(signingKey, "https://other-issuer.example", new Date(System.currentTimeMillis() + 300_000L),
                Map.of("realm_access", Map.of("roles", java.util.List.of("mcp-admin"))));
    }

    /**
     * A token claiming this issuer but signed with a key the issuer does not publish.
     * @return the serialized JWT.
     * @throws JOSEException if the rogue key cannot be generated.
     */
    public String untrustedKeyToken() throws JOSEException {
        RSAKey rogue = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();
        return sign(rogue, uri(), new Date(System.currentTimeMillis() + 300_000L),
                Map.of("realm_access", Map.of("roles", java.util.List.of("mcp-admin"))));
    }

    private static String sign(RSAKey key, String issuer, Date expiry, Map<String, Object> extraClaims) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("e2e-subject")
                .issueTime(new Date(System.currentTimeMillis() - 120_000L))
                .expirationTime(expiry);
        extraClaims.forEach(claims::claim);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                claims.build());
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the test token", e);
        }
        return jwt.serialize();
    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.startsWith("/.well-known/openid-configuration")) {
                    return json("{\"issuer\":\"" + uri() + "\",\"jwks_uri\":\"" + uri() + "/jwks\","
                            + "\"subject_types_supported\":[\"public\"]}");
                }
                if (path.startsWith("/jwks")) {
                    return json(new JWKSet(signingKey.toPublicJWK()).toString());
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private static MockResponse json(String body) {
        return new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body);
    }

    @Override
    public void close() throws IOException {
        server.shutdown();
    }
}
