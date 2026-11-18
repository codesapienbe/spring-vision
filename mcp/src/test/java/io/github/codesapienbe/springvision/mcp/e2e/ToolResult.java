package io.github.codesapienbe.springvision.mcp.e2e;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The outcome of an MCP {@code tools/call}: whether the server flagged it as an error, and its text.
 *
 * @param error the result's {@code isError}.
 * @param text the first text content.
 */
public record ToolResult(boolean error, String text) {

    private static final String DENIED = "is not granted to the caller's roles";

    static ToolResult of(JsonNode result) {
        return new ToolResult(result.path("isError").asBoolean(false),
                result.path("content").path(0).path("text").asText(""));
    }

    /** @return whether the tool-access check refused the call. */
    public boolean denied() {
        return error && text.contains(DENIED);
    }
}
