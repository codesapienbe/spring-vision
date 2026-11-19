package io.github.codesapienbe.springvision.mcp.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.djl.ModelStore;
import io.github.codesapienbe.springvision.mcp.SetupTool;
import io.github.codesapienbe.springvision.mcp.VisionTool;

/**
 * Configuration for registering Spring AI Tool Callbacks.
 * This class registers any @Tool-annotated beans with Spring AI's MethodToolCallbackProvider,
 * each wrapped so it only runs for a caller whose roles are granted it in
 * {@code spring.vision.mcp.security.tool-access}.
 */
@Configuration
@EnableConfigurationProperties(McpSecurityProperties.class)
public class ToolCallbackConfiguration {

    /**
     * Default constructor for {@link ToolCallbackConfiguration}.
     */
    public ToolCallbackConfiguration() {
        // Default constructor
    }

    /**
     * Registers the VisionTool's {@code @Tool} methods, each guarded by the tool-access policy.
     * The MCP autoconfiguration picks up the provider and exposes the tools to MCP clients.
     * @param visionTool The VisionTool bean, which may be null.
     * @param setupTool the {@code setup_status} tool, which may be null.
     * @param security the configured tool grants and stdio roles.
     * @param stdioTransport whether the server runs over stdio, where there is no token and the
     *     caller has the configured stdio roles.
     * @param modelStore where the tools' models come from.
     * @param modelWait how long a call waits for a model still downloading
     *     ({@code spring.vision.mcp.model-wait}).
     * @return A ToolCallbackProvider with the guarded tools.
     * @throws IllegalStateException if the tool grants name a tool that does not exist.
     */
    @Bean
    public ToolCallbackProvider tools(@Nullable VisionTool visionTool, @Nullable SetupTool setupTool,
            McpSecurityProperties security, @Value("${spring.ai.mcp.server.stdio:false}") boolean stdioTransport,
            ObjectProvider<ModelStore> modelStore, @Value("${spring.vision.mcp.model-wait:5s}") Duration modelWait) {
        return tools(visionTool, setupTool, security, stdioTransport,
                modelStore.getIfAvailable(ModelStore::classpathOnly), modelWait);
    }

    /** The tools without {@code setup_status} and with a classpath-only model store. */
    ToolCallbackProvider tools(@Nullable VisionTool visionTool, McpSecurityProperties security, boolean stdioTransport) {
        return tools(visionTool, null, security, stdioTransport, ModelStore.classpathOnly(), Duration.ZERO);
    }

    ToolCallbackProvider tools(@Nullable VisionTool visionTool, @Nullable SetupTool setupTool,
            McpSecurityProperties security, boolean stdioTransport, ModelStore modelStore, Duration modelWait) {
        if (visionTool == null) {
            return ToolCallbackProvider.from(List.of());
        }
        Object[] toolObjects = Arrays.stream(new Object[] {visionTool, setupTool}).filter(Objects::nonNull).toArray();
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder().toolObjects(toolObjects).build()
                .getToolCallbacks();
        ToolAccessPolicy policy = new ToolAccessPolicy(security.toolAccess());
        Set<String> unknown = policy.unknownTools(Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet()));
        if (!unknown.isEmpty()) {
            throw new IllegalStateException(
                    "spring.vision.mcp.security.tool-access grants tools that do not exist: " + unknown);
        }
        Set<String> stdioRoles = Set.copyOf(security.stdioRoles());
        Supplier<Set<String>> callerRoles = stdioTransport
                ? () -> stdioRoles
                : ToolCallbackConfiguration::rolesFromSecurityContext;
        return ToolCallbackProvider.from(Arrays.stream(callbacks)
                .map(callback -> (ToolCallback) new GuardedToolCallback(callback, policy, callerRoles, modelStore,
                        ToolModelRequirements.modelsFor(callback.getToolDefinition().name()), modelWait))
                .toList());
    }

    /**
     * The roles of the authenticated HTTP caller: its {@code ROLE_*} authorities without the prefix.
     * Read on the thread that runs the tool, which is the request thread.
     */
    static Set<String> rolesFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(JwtClaimRolesConverter.ROLE_PREFIX))
                .map(authority -> authority.substring(JwtClaimRolesConverter.ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Bean
    public VectorService inMemoryVectorService() {
        return new io.github.codesapienbe.springvision.mcp.config.InMemoryVectorService();
    }
}
