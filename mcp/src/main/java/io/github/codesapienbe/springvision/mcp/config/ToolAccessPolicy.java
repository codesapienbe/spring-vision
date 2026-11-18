package io.github.codesapienbe.springvision.mcp.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Decides whether a caller holding some roles may call a tool, from the grants in
 * {@link McpSecurityProperties#toolAccess()}. Anything not granted is denied.
 */
public final class ToolAccessPolicy {

    /** Grants every tool. */
    public static final String ALL_TOOLS = "*";

    private final Map<String, Set<String>> toolsByRole;

    /**
     * Creates the policy.
     * @param grants the configured grants; a role listed more than once gets the union.
     */
    public ToolAccessPolicy(List<McpSecurityProperties.ToolGrant> grants) {
        this.toolsByRole = Map.copyOf(grants.stream().collect(Collectors.groupingBy(
                McpSecurityProperties.ToolGrant::role,
                Collectors.flatMapping(grant -> grant.tools().stream(), Collectors.toUnmodifiableSet()))));
    }

    /**
     * Whether any of the roles is granted the tool.
     * @param roles the caller's role names.
     * @param toolName the MCP tool name.
     * @return {@code true} if allowed.
     */
    public boolean allows(Collection<String> roles, String toolName) {
        return roles.stream()
                .map(toolsByRole::get)
                .filter(Objects::nonNull)
                .anyMatch(tools -> tools.contains(ALL_TOOLS) || tools.contains(toolName));
    }

    /**
     * The granted tool names that are not among the known tools, so a typo fails at start-up
     * instead of silently denying a tool.
     * @param knownTools the names of the registered tools.
     * @return the unknown names, sorted; empty when every name is known.
     */
    public Set<String> unknownTools(Set<String> knownTools) {
        return toolsByRole.values().stream()
                .flatMap(Set::stream)
                .filter(tool -> !ALL_TOOLS.equals(tool) && !knownTools.contains(tool))
                .collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), Set::copyOf));
    }
}
