package io.github.codesapienbe.springvision.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.codesapienbe.springvision.core.djl.ModelStore;

/**
 * Reports whether spring-vision has finished setting up: which models are in place and how far
 * each download is. A client calls it when a tool answers that a model is still downloading.
 */
@Component
public class SetupTool {

    private final ModelStore models;

    /**
     * @param models the model store; a classpath-only store when the application defines none
     */
    @Autowired
    public SetupTool(ObjectProvider<ModelStore> models) {
        this.models = models.getIfAvailable(ModelStore::classpathOnly);
    }

    /**
     * @param models the model store to report on
     */
    public SetupTool(ModelStore models) {
        this.models = models;
    }

    /**
     * @return {@code status} ("ready" or "in_progress"), whether downloads are on, and one entry
     *     per model with its state, progress and last error
     */
    @Tool(name = "setup_status", description = "Whether spring-vision has finished setting up: which models are "
        + "ready and how far each download is. Call it when another tool says spring-vision is still setting up, "
        + "then retry that tool once its models are ready.")
    public Map<String, Object> setupStatus() {
        List<ModelStore.ModelStatus> statuses = models.status();
        List<Map<String, Object>> entries = statuses.stream().map(SetupTool::entry).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", statuses.stream().anyMatch(ModelStore.ModelStatus::pending) ? "in_progress" : "ready");
        response.put("downloadsEnabled", models.downloadEnabled());
        response.put("models", entries);
        return response;
    }

    private static Map<String, Object> entry(ModelStore.ModelStatus status) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("model", status.path());
        entry.put("state", status.state().name().toLowerCase());
        entry.put("percent", status.ready() ? 100 : status.percent());
        entry.put("message", status.describe());
        if (status.error() != null) {
            entry.put("error", status.error());
        }
        return entry;
    }
}
