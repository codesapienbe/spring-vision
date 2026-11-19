package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.codesapienbe.springvision.core.djl.ModelSpec;
import io.github.codesapienbe.springvision.core.djl.ModelStore;

class SetupToolTest {

    @Test
    @DisplayName("Reports ready with every model at 100 % when all are in place")
    void readyWhenEverythingIsInPlace(@TempDir Path directory) throws Exception {
        Files.createDirectories(directory.resolve("a"));
        Files.write(directory.resolve("a/model.onnx"), new byte[10]);
        ModelSpec spec = ModelSpec.file("a/model.onnx", "http://127.0.0.1:1/a", "0".repeat(64), 10);
        try (ModelStore store = new ModelStore(directory, List.of(spec), true)) {
            Map<String, Object> status = new SetupTool(store).setupStatus();

            assertThat(status).containsEntry("status", "ready").containsEntry("downloadsEnabled", true);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> models = (List<Map<String, Object>>) status.get("models");
            assertThat(models).singleElement()
                    .satisfies(model -> assertThat(model).containsEntry("state", "ready").containsEntry("percent", 100));
        }
    }

    @Test
    @DisplayName("Reports in_progress while a model is still on its way")
    void inProgressWhileDownloading(@TempDir Path directory) {
        ModelSpec spec = ModelSpec.file("b/model.onnx", "http://127.0.0.1:1/b", "0".repeat(64), 10);
        try (ModelStore store = new ModelStore(directory, List.of(spec), true)) {
            store.startBackgroundDownloads();

            assertThat(new SetupTool(store).setupStatus()).containsEntry("status", "in_progress");
        }
    }
}
