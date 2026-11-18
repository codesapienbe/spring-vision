package io.github.codesapienbe.springvision.mcp.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A minimal MCP Streamable-HTTP client: JSON-RPC over POST, answers as JSON or as server-sent
 * events, and the {@code Mcp-Session-Id} header carried between calls.
 */
public final class StreamableHttpMcpClient {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SESSION_HEADER = "Mcp-Session-Id";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final URI endpoint;

    /**
     * @param baseUrl the server root, e.g. {@code http://localhost:8080}.
     */
    public StreamableHttpMcpClient(String baseUrl) {
        this.endpoint = URI.create(baseUrl + "/mcp");
    }

    /**
     * One HTTP exchange.
     *
     * @param status the HTTP status.
     * @param sessionId the session header, if the server sent one.
     * @param messages the JSON-RPC messages in the body.
     */
    public record Exchange(int status, String sessionId, List<JsonNode> messages) {

        /** Copies the messages. */
        public Exchange {
            messages = List.copyOf(messages);
        }
    }

    /**
     * Sends one JSON-RPC message.
     * @param token the bearer token, or {@code null} for none.
     * @param sessionId the session, or {@code null} before {@code initialize}.
     * @param message the JSON-RPC message.
     * @return the exchange.
     */
    public Exchange post(String token, String sessionId, Map<String, Object> message) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(write(message)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (sessionId != null) {
            request.header(SESSION_HEADER, sessionId);
        }
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Exchange(response.statusCode(), response.headers().firstValue(SESSION_HEADER).orElse(null),
                    parse(response.body()));
        } catch (IOException e) {
            throw new IllegalStateException("MCP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    /**
     * Runs {@code initialize} and {@code notifications/initialized}.
     * @param token the bearer token, or {@code null}.
     * @return the exchange of the {@code initialize} call; on success it carries the session.
     */
    public Exchange initialize(String token) {
        Exchange init = post(token, null, Map.of("jsonrpc", "2.0", "id", 0, "method", "initialize",
                "params", Map.of("protocolVersion", "2025-06-18", "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "e2e", "version", "0"))));
        if (init.status() == 200) {
            post(token, init.sessionId(), Map.of("jsonrpc", "2.0", "method", "notifications/initialized"));
        }
        return init;
    }

    /**
     * Opens a session, failing the test if the server refuses.
     * @param token the bearer token.
     * @return the session.
     */
    public Session open(String token) {
        Exchange init = initialize(token);
        if (init.status() != 200) {
            throw new AssertionError("initialize returned HTTP " + init.status());
        }
        return new Session(token, init.sessionId());
    }

    /** An initialized MCP session over HTTP. */
    public final class Session {

        private final String token;
        private final String sessionId;
        private final AtomicInteger nextId = new AtomicInteger(1);

        private Session(String token, String sessionId) {
            this.token = token;
            this.sessionId = sessionId;
        }

        /**
         * Sends a request and returns its {@code result}.
         * @param method the JSON-RPC method.
         * @param params its params.
         * @return the result node.
         */
        public JsonNode request(String method, Map<String, Object> params) {
            int id = nextId.getAndIncrement();
            Exchange exchange = post(token, sessionId,
                    Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params));
            return exchange.messages().stream()
                    .filter(message -> message.path("id").asInt(-1) == id)
                    .findFirst()
                    .map(message -> message.path("result"))
                    .orElseThrow(() -> new AssertionError("No answer to " + method + " (HTTP " + exchange.status() + ")"));
        }

        /** @return the names of the tools the server lists. */
        public List<String> toolNames() {
            return request("tools/list", Map.of()).path("tools").findValuesAsText("name");
        }

        /**
         * Calls a tool with no arguments.
         * @param tool the tool name.
         * @return the outcome.
         */
        public ToolResult call(String tool) {
            return ToolResult.of(request("tools/call", Map.of("name", tool, "arguments", Map.of())));
        }
    }

    private static String write(Map<String, Object> message) {
        try {
            return JSON.writeValueAsString(message);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<JsonNode> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            if (body.trim().startsWith("{")) {
                return List.of(JSON.readTree(body));
            }
            return body.lines()
                    .filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring("data:".length()).trim())
                    .filter(data -> !data.isEmpty())
                    .map(StreamableHttpMcpClient::readTree)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unparseable MCP response: " + body, e);
        }
    }

    private static JsonNode readTree(String json) {
        try {
            return JSON.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("Unparseable MCP event: " + json, e);
        }
    }
}
