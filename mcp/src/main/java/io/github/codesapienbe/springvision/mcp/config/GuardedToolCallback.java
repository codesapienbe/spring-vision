package io.github.codesapienbe.springvision.mcp.config;

import java.util.Set;
import java.util.function.Supplier;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.security.access.AccessDeniedException;

/**
 * Wraps a tool so it runs only when the caller's roles are granted it by the
 * {@link ToolAccessPolicy}. Every MCP tool goes through one of these, which keeps access control
 * in one place instead of in each {@code @Tool} method.
 */
final class GuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolAccessPolicy policy;
    private final Supplier<Set<String>> callerRoles;

    GuardedToolCallback(ToolCallback delegate, ToolAccessPolicy policy, Supplier<Set<String>> callerRoles) {
        this.delegate = delegate;
        this.policy = policy;
        this.callerRoles = callerRoles;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        requireAccess();
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        requireAccess();
        return delegate.call(toolInput, toolContext);
    }

    private void requireAccess() {
        String tool = delegate.getToolDefinition().name();
        Set<String> roles = callerRoles.get();
        if (!policy.allows(roles, tool)) {
            throw new AccessDeniedException("Tool '" + tool + "' is not granted to the caller's roles " + roles
                    + " (spring.vision.mcp.security.tool-access)");
        }
    }
}
