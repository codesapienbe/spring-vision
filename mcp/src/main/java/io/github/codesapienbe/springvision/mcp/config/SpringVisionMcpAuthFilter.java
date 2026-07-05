package io.github.codesapienbe.springvision.mcp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Requires a matching {@code Authorization: Bearer <token>} header on the MCP endpoint.
 * The expected token comes from {@code springvision.mcp.auth.token} (env var
 * {@code SPRING_VISION_MCP_TOKEN}), which must be configured to a non-blank value —
 * per this project's "no data is better than wrong data" rule, refusing to start is
 * preferable to silently exposing an unauthenticated MCP endpoint.
 */
@Component
public class SpringVisionMcpAuthFilter extends OncePerRequestFilter {

    private static final String MCP_PATH_PREFIX = "/mcp";
    private static final String BEARER_PREFIX = "Bearer ";

    private final byte[] expectedTokenBytes;

    public SpringVisionMcpAuthFilter(@Value("${springvision.mcp.auth.token}") String expectedToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new IllegalStateException(
                    "springvision.mcp.auth.token (SPRING_VISION_MCP_TOKEN) must be set to a non-blank value; "
                            + "refusing to start an unauthenticated MCP endpoint.");
        }
        this.expectedTokenBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(MCP_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX) && matchesExpectedToken(header)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid bearer token");
    }

    private boolean matchesExpectedToken(String header) {
        byte[] providedTokenBytes = header.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedTokenBytes, expectedTokenBytes);
    }
}
