package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;

import io.github.codesapienbe.springvision.mcp.config.McpSecurityProperties.ToolGrant;

class GuardedToolCallbackTest {

    private static final ToolAccessPolicy POLICY =
            new ToolAccessPolicy(List.of(new ToolGrant("user", List.of("count_faces_b"))));

    private ToolCallback delegate;

    @BeforeEach
    void setUp() {
        delegate = mock(ToolCallback.class);
        ToolDefinition definition = DefaultToolDefinition.builder()
                .name("count_faces_b").description("d").inputSchema("{}").build();
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call(any())).thenReturn("ok");
        when(delegate.call(any(), any())).thenReturn("ok-with-context");
    }

    @Test
    @DisplayName("Runs the tool for a granted role")
    void runsForGrantedRole() {
        GuardedToolCallback guarded = new GuardedToolCallback(delegate, POLICY, () -> Set.of("user"));

        assertThat(guarded.call("{}")).isEqualTo("ok");
        assertThat(guarded.call("{}", new ToolContext(Map.of()))).isEqualTo("ok-with-context");
        assertThat(guarded.getToolDefinition().name()).isEqualTo("count_faces_b");
    }

    @Test
    @DisplayName("Denies without running the tool when no role is granted it")
    void deniesWithoutRunning() {
        GuardedToolCallback guarded = new GuardedToolCallback(delegate, POLICY, () -> Set.of("other"));

        assertThatThrownBy(() -> guarded.call("{}"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("count_faces_b");
        assertThatThrownBy(() -> guarded.call("{}", new ToolContext(Map.of())))
                .isInstanceOf(AccessDeniedException.class);
        verify(delegate, never()).call(any());
        verify(delegate, never()).call(any(), any());
    }
}
