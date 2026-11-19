package io.github.codesapienbe.springvision.mcp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication;

/**
 * The MCP server over Streamable HTTP, end to end: the real application context, the real
 * security filter chain and MCP transport, real issuer discovery and RS256 signature checks
 * against a local issuer, and the tool-access policy shipped in {@code application.yml}. Only
 * the vision backend is a stub, so no model is loaded.
 */
@SpringBootTest(classes = {SpringVisionMcpServerApplication.class, StubBackendConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.vision.djl.online-classifier.enabled=false")
class McpHttpEndToEndIntegrationTest {

    private static final String USER_TOOL = "scan_barcode_b";
    private static final List<String> ADMIN_TOOLS =
            List.of("list_identities", "recognize_license_plate_b", "extract_face_embeddings_b", "detect_deepfake_b");

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

    private StreamableHttpMcpClient client() {
        return new StreamableHttpMcpClient("http://localhost:" + port);
    }

    @Nested
    @DisplayName("Authentication")
    class Authentication {

        @Test
        @DisplayName("No token: 401")
        void noToken() {
            assertThat(client().initialize(null).status()).isEqualTo(401);
        }

        @Test
        @DisplayName("Malformed token: 401")
        void malformedToken() {
            assertThat(client().initialize("not-a-jwt").status()).isEqualTo(401);
        }

        @Test
        @DisplayName("Token signed with a key the issuer does not publish: 401")
        void untrustedKey() throws Exception {
            assertThat(client().initialize(issuer.untrustedKeyToken()).status()).isEqualTo(401);
        }

        @Test
        @DisplayName("Expired token: 401")
        void expired() {
            assertThat(client().initialize(issuer.expiredToken()).status()).isEqualTo(401);
        }

        @Test
        @DisplayName("Token from another issuer: 401")
        void foreignIssuer() {
            assertThat(client().initialize(issuer.foreignIssuerToken()).status()).isEqualTo(401);
        }

        @Test
        @DisplayName("Valid token: the MCP session opens")
        void validToken() {
            StreamableHttpMcpClient.Exchange init = client().initialize(issuer.realmRolesToken("mcp-user"));
            assertThat(init.status()).isEqualTo(200);
            assertThat(init.sessionId()).isNotBlank();
            assertThat(init.messages()).anySatisfy(message ->
                    assertThat(message.path("result").path("serverInfo").path("name").asText()).isEqualTo("spring-vision"));
        }

        @Test
        @DisplayName("The health check needs no token")
        void healthIsPublic() throws Exception {
            HttpResponse<String> health = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Tool access from the shipped application.yml")
    class ToolAccess {

        @Test
        @DisplayName("Every caller sees the whole tool list; access is checked on call")
        void listsEveryTool() {
            assertThat(client().open(issuer.realmRolesToken("mcp-user")).toolNames()).hasSize(69).contains(USER_TOOL, "setup_status")
                    .containsAll(ADMIN_TOOLS);
        }

        @Test
        @DisplayName("mcp-user runs general tools and is denied the sensitive ones")
        void userRole() {
            StreamableHttpMcpClient.Session session = client().open(issuer.realmRolesToken("mcp-user"));

            assertThat(session.call(USER_TOOL).denied()).isFalse();
            assertThat(ADMIN_TOOLS).allSatisfy(tool -> {
                ToolResult result = session.call(tool);
                assertThat(result.denied()).as(tool).isTrue();
                assertThat(result.text()).contains("'" + tool + "'", "[mcp-user]");
            });
        }

        @Test
        @DisplayName("mcp-admin runs every tool")
        void adminRole() {
            StreamableHttpMcpClient.Session session = client().open(issuer.realmRolesToken("mcp-admin"));

            assertThat(session.call(USER_TOOL).denied()).isFalse();
            assertThat(ADMIN_TOOLS).allSatisfy(tool -> assertThat(session.call(tool).denied()).as(tool).isFalse());
        }

        @Test
        @DisplayName("A token without roles, or with unknown roles, runs nothing")
        void noOrUnknownRoles() {
            StreamableHttpMcpClient.Session noRoles = client().open(issuer.token(Map.of()));
            StreamableHttpMcpClient.Session unknown = client().open(issuer.realmRolesToken("someone-else"));

            assertThat(noRoles.call(USER_TOOL).denied()).isTrue();
            assertThat(unknown.call(USER_TOOL).denied()).isTrue();
            assertThat(unknown.call("list_identities").denied()).isTrue();
        }

        @Test
        @DisplayName("Any one granted role is enough")
        void anyGrantedRole() {
            StreamableHttpMcpClient.Session session = client().open(issuer.realmRolesToken("someone-else", "mcp-admin"));
            assertThat(session.call("list_identities").denied()).isFalse();
        }

        @Test
        @DisplayName("A denial is a tool error; the session keeps working")
        void denialKeepsSession() {
            StreamableHttpMcpClient.Session session = client().open(issuer.realmRolesToken("mcp-user"));

            ToolResult denied = session.call("list_identities");
            assertThat(denied.error()).isTrue();
            assertThat(denied.denied()).isTrue();
            assertThat(session.call(USER_TOOL).denied()).isFalse();
        }

        @Test
        @DisplayName("Each session is checked against its own token")
        void sessionsAreIndependent() {
            StreamableHttpMcpClient.Session user = client().open(issuer.realmRolesToken("mcp-user"));
            StreamableHttpMcpClient.Session admin = client().open(issuer.realmRolesToken("mcp-admin"));

            assertThat(user.call("list_identities").denied()).isTrue();
            assertThat(admin.call("list_identities").denied()).isFalse();
            assertThat(user.call("list_identities").denied()).isTrue();
        }
    }
}
