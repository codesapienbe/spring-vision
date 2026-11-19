package io.github.codesapienbe.springvision.mcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.djl.ModelCatalog;
import io.github.codesapienbe.springvision.core.djl.ModelSpec;
import io.github.codesapienbe.springvision.mcp.VisionTool;

class ToolModelRequirementsTest {

    @Test
    @DisplayName("Every listed tool exists, so a renamed tool cannot silently lose its model check")
    void listsOnlyRealTools() {
        VisionTool visionTool = new VisionTool(new VisionTemplate(mock(VisionBackend.class)));
        Set<String> tools = Arrays.stream(MethodToolCallbackProvider.builder().toolObjects(visionTool).build()
                .getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());

        assertThat(tools).containsAll(ToolModelRequirements.BY_TOOL.keySet());
    }

    @Test
    @DisplayName("Every required model is one the store can download")
    void requiresOnlyCatalogModels() {
        Set<String> catalog = ModelCatalog.all().stream().map(ModelSpec::path).collect(Collectors.toUnmodifiableSet());

        assertThat(ToolModelRequirements.BY_TOOL.values().stream().flatMap(List::stream)).allMatch(catalog::contains);
    }

    @Test
    @DisplayName("Tools that use no downloaded model need nothing")
    void modelFreeToolsNeedNothing() {
        assertThat(ToolModelRequirements.modelsFor("scan_barcode_b")).isEmpty();
        assertThat(ToolModelRequirements.modelsFor("count_faces_u")).isEmpty();
        assertThat(ToolModelRequirements.modelsFor("detect_emotions_u")).containsExactly(ModelCatalog.EMOTION);
    }
}
