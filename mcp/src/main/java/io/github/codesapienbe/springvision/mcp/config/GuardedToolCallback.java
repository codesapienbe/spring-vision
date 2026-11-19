package io.github.codesapienbe.springvision.mcp.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.security.access.AccessDeniedException;

import io.github.codesapienbe.springvision.core.djl.ModelStore;
import io.github.codesapienbe.springvision.core.exception.ModelNotReadyException;

/**
 * Wraps a tool so it runs only when the caller's roles are granted it by the
 * {@link ToolAccessPolicy}, and only once the models it needs are in place. Every MCP tool goes through one of these, which keeps access control and the
 * "still setting up" answer in one place instead of in each {@code @Tool} method.
 */
final class GuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolAccessPolicy policy;
    private final Supplier<Set<String>> callerRoles;
    private final ModelStore models;
    private final List<String> neededModels;
    private final Duration modelWait;

    GuardedToolCallback(ToolCallback delegate, ToolAccessPolicy policy, Supplier<Set<String>> callerRoles) {
        this(delegate, policy, callerRoles, ModelStore.classpathOnly(), List.of(), Duration.ZERO);
    }

    /**
     * @param models       where the tool's models come from
     * @param neededModels the models the tool needs ({@link ToolModelRequirements})
     * @param modelWait    how long a call waits for a model still downloading before it answers
     *                     with a "still setting up" error
     */
    GuardedToolCallback(ToolCallback delegate, ToolAccessPolicy policy, Supplier<Set<String>> callerRoles,
            ModelStore models, List<String> neededModels, Duration modelWait) {
        this.delegate = delegate;
        this.policy = policy;
        this.callerRoles = callerRoles;
        this.models = models;
        this.neededModels = List.copyOf(neededModels);
        this.modelWait = modelWait;
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
        requireModels();
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        requireAccess();
        requireModels();
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

    private void requireModels() {
        if (neededModels.isEmpty()) {
            return;
        }
        String tool = delegate.getToolDefinition().name();
        List<ModelStore.ModelStatus> pending;
        try {
            pending = models.awaitReady(neededModels, modelWait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelNotReadyException("Interrupted while waiting for the models '" + tool + "' needs");
        }
        if (!pending.isEmpty()) {
            throw new ModelNotReadyException("spring-vision is still setting up the models '" + tool + "' needs: "
                    + pending.stream().map(ModelStore.ModelStatus::describe).collect(Collectors.joining("; "))
                    + ". Nothing was run. Try the same call again in a minute; setup_status shows the progress.");
        }
    }
}
