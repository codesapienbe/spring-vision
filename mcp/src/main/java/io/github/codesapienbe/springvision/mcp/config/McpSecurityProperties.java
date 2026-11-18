package io.github.codesapienbe.springvision.mcp.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Who may call which MCP tool, bound from {@code spring.vision.mcp.security}.
 *
 * <p>The server does not run an identity provider of its own. Over HTTP it validates JWTs from
 * whichever OIDC issuer {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} names, reads
 * the caller's roles from {@link #rolesClaimPath()}, and allows a tool only when one of those roles
 * is granted it in {@link #toolAccess()}. Over stdio there is no token; the local caller gets
 * {@link #stdioRoles()}.</p>
 *
 * @param rolesClaimPath JWT claim path to the list of role names, one element per nesting level,
 *     e.g. {@code [realm_access, roles]} or {@code [resource_access, my-client, roles]}.
 * @param toolAccess which tools each role may call; {@code "*"} grants every tool. A tool that no
 *     role of the caller is granted is denied.
 * @param stdioRoles the roles the caller has over the stdio transport.
 */
@ConfigurationProperties("spring.vision.mcp.security")
public record McpSecurityProperties(List<String> rolesClaimPath, List<ToolGrant> toolAccess, List<String> stdioRoles) {

    /**
     * Copies every list, so later changes to the bound collections cannot change the policy.
     */
    public McpSecurityProperties {
        rolesClaimPath = rolesClaimPath == null || rolesClaimPath.isEmpty()
                ? List.of("realm_access", "roles") : List.copyOf(rolesClaimPath);
        toolAccess = toolAccess == null ? List.of() : List.copyOf(toolAccess);
        stdioRoles = stdioRoles == null ? List.of() : List.copyOf(stdioRoles);
    }

    /**
     * The tools one role may call.
     *
     * @param role the role name as it appears in the token, without a {@code ROLE_} prefix.
     * @param tools MCP tool names, or {@code "*"} for every tool.
     */
    public record ToolGrant(String role, List<String> tools) {

        /**
         * Copies the tool list.
         */
        public ToolGrant {
            tools = tools == null ? List.of() : List.copyOf(tools);
        }
    }
}
