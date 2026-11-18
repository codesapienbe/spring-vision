package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.mcp.config.McpSecurityProperties.ToolGrant;

class ToolAccessPolicyTest {

    private final ToolAccessPolicy policy = new ToolAccessPolicy(List.of(
            new ToolGrant("admin", List.of(ToolAccessPolicy.ALL_TOOLS)),
            new ToolGrant("user", List.of("count_faces_b")),
            new ToolGrant("user", List.of("extract_text_b"))));

    @Test
    @DisplayName("A role is allowed exactly the tools it is granted")
    void allowsGrantedTools() {
        assertThat(policy.allows(Set.of("user"), "count_faces_b")).isTrue();
        assertThat(policy.allows(Set.of("user"), "list_identities")).isFalse();
    }

    @Test
    @DisplayName("A role listed twice gets the union of its grants")
    void mergesRepeatedRoles() {
        assertThat(policy.allows(Set.of("user"), "extract_text_b")).isTrue();
    }

    @Test
    @DisplayName("\"*\" grants every tool")
    void wildcardGrantsEverything() {
        assertThat(policy.allows(Set.of("admin"), "list_identities")).isTrue();
    }

    @Test
    @DisplayName("Unknown roles and no roles are denied")
    void deniesUnknownOrNoRoles() {
        assertThat(policy.allows(Set.of("someone-else"), "count_faces_b")).isFalse();
        assertThat(policy.allows(Set.of(), "count_faces_b")).isFalse();
    }

    @Test
    @DisplayName("Any one granted role is enough")
    void anyGrantedRoleSuffices() {
        assertThat(policy.allows(Set.of("someone-else", "user"), "count_faces_b")).isTrue();
    }

    @Test
    @DisplayName("Reports granted names that match no known tool, ignoring \"*\"")
    void reportsUnknownTools() {
        assertThat(policy.unknownTools(Set.of("count_faces_b"))).containsExactly("extract_text_b");
        assertThat(policy.unknownTools(Set.of("count_faces_b", "extract_text_b"))).isEmpty();
    }
}
