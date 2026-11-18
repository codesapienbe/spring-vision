package io.github.codesapienbe.springvision.mcp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication;

/**
 * Tool access configured for an identity provider with its own role names and claim layout,
 * end to end over HTTP: roles read from a client-role claim whose client id contains dots, and
 * grants that replace the shipped ones entirely.
 */
@SpringBootTest(classes = {SpringVisionMcpServerApplication.class, StubBackendConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.vision.djl.online-classifier.enabled=false",
            "spring.vision.mcp.security.roles-claim-path=resource_access,my.app.client,roles",
            "spring.vision.mcp.security.tool-access[0].role=vision-reader",
            "spring.vision.mcp.security.tool-access[0].tools=scan_barcode_b,extract_text_b",
            "spring.vision.mcp.security.tool-access[1].role=vision-operator",
            "spring.vision.mcp.security.tool-access[1].tools=*"
        })
class McpHttpCustomRolesIntegrationTest {

    private static TestOidcIssuer issuer;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startIssuer() throws Exception {
        issuer = TestOidcIssuer.start();
    }

    @AfterAll
    static void stopIssuer() throws IOException {
        issuer.close();
    }

    @DynamicPropertySource
    static void issuerUri(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer.uri());
    }

    private StreamableHttpMcpClient.Session session(Map<String, Object> claims) {
        return new StreamableHttpMcpClient("http://localhost:" + port).open(issuer.token(claims));
    }

    private static Map<String, Object> clientRoles(String clientId, String... roles) {
        return Map.of("resource_access", Map.of(clientId, Map.of("roles", List.of(roles))));
    }

    @Test
    @DisplayName("A configured role gets exactly its configured tools")
    void readerRole() {
        StreamableHttpMcpClient.Session reader = session(clientRoles("my.app.client", "vision-reader"));

        assertThat(reader.call("scan_barcode_b").denied()).isFalse();
        assertThat(reader.call("extract_text_b").denied()).isFalse();
        assertThat(reader.call("count_faces_b").denied()).isTrue();
        assertThat(reader.call("list_identities").denied()).isTrue();
    }

    @Test
    @DisplayName("\"*\" grants every tool")
    void operatorRole() {
        StreamableHttpMcpClient.Session operator = session(clientRoles("my.app.client", "vision-operator"));
        assertThat(operator.call("list_identities").denied()).isFalse();
    }

    @Test
    @DisplayName("Roles at any other claim path are ignored")
    void otherClaimPathsIgnored() {
        StreamableHttpMcpClient.Session realmRole =
                session(Map.of("realm_access", Map.of("roles", List.of("vision-operator", "mcp-admin"))));
        StreamableHttpMcpClient.Session otherClient = session(clientRoles("another-client", "vision-operator"));

        assertThat(realmRole.call("scan_barcode_b").denied()).isTrue();
        assertThat(otherClient.call("scan_barcode_b").denied()).isTrue();
    }

    @Test
    @DisplayName("The shipped grants are replaced, not merged")
    void shippedGrantsReplaced() {
        StreamableHttpMcpClient.Session shippedRole = session(clientRoles("my.app.client", "mcp-admin"));
        assertThat(shippedRole.call("scan_barcode_b").denied()).isTrue();
    }
}
