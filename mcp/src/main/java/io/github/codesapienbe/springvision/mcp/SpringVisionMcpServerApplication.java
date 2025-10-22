package io.github.codesapienbe.springvision.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Main application class for Spring Vision MCP Server using stdio transport.
 * This server provides computer vision operations via the Model Context Protocol (MCP)
 * using standard input/output for communication with MCP clients.
 */
@SpringBootApplication
public class SpringVisionMcpServerApplication {

    /**
     * Default constructor for {@link SpringVisionMcpServerApplication}.
     */
    public SpringVisionMcpServerApplication() {
        // Default constructor
    }

    /**
     * Main entry point for the Spring Vision MCP Server application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringVisionMcpServerApplication.class);
        // Ensure the DJL repository cache dir system property is set early from application.yml
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            Environment env = event.getEnvironment();
            String modelCache = env.getProperty("spring.vision.djl.model-cache-dir");

            // Fallback to system property or default user home
            if (modelCache == null || modelCache.isBlank()) {
                modelCache = System.getProperty("ai.djl.repository.cache.dir");
            }
            if (modelCache == null || modelCache.isBlank()) {
                modelCache = System.getProperty("user.home") + "/.djl.ai";
            }

            // Ensure directory exists and set system property before DJL initializes
            try {
                Path cachePath = Paths.get(modelCache);
                Files.createDirectories(cachePath);
                System.setProperty("ai.djl.repository.cache.dir", cachePath.toAbsolutePath().toString());
                System.out.println("[startup] Set ai.djl.repository.cache.dir=" + cachePath.toAbsolutePath());

                // Attempt to extract bundled models from classpath to the cache (best-effort).
                extractBundledModelsToCache(cachePath);

            } catch (IOException e) {
                System.err.println("[startup] Failed to prepare DJL cache dir: " + e.getMessage());
            }
        });
        app.run(args);
    }

    private static void extractBundledModelsToCache(Path cachePath) {
        // Look for manifest.properties under /models in classpath
        try (InputStream in = SpringVisionMcpServerApplication.class.getClassLoader().getResourceAsStream("models/manifest.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                List<String> resources = new ArrayList<>();
                for (Object k : p.keySet()) {
                    String val = p.getProperty((String) k);
                    if (val != null && val.startsWith("models/")) {
                        resources.add(val);
                    }
                }
                for (String res : resources) {
                    extractResourceToCache(res, cachePath);
                }
                return;
            }
        } catch (IOException e) {
            System.err.println("[startup] Failed to read models/manifest.properties: " + e.getMessage());
        }

        // Fallback: try known zip names (best-effort)
        String[] known = {"retinaface.zip", "object_detection.zip", "pose_estimation.zip", "action_recognition.zip", "semantic_segmentation.zip", "instance_segmentation.zip", "face_recognition.zip", "face_detection_yunet_2023mar.onnx"};
        for (String k : known) {
            extractResourceToCache("models/" + k, cachePath);
        }
    }

    private static void extractResourceToCache(String resourcePath, Path cachePath) {
        try (InputStream in = SpringVisionMcpServerApplication.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return; // not bundled
            System.out.println("[startup] Found bundled model resource: " + resourcePath + " - extracting to cache");
            if (resourcePath.toLowerCase().endsWith(".zip")) {
                try (ZipInputStream zis = new ZipInputStream(in)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        Path out = cachePath.resolve(entry.getName());
                        Files.createDirectories(out.getParent());
                        Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        zis.closeEntry();
                    }
                }
            } else {
                // Copy non-zip (e.g., .onnx) directly into cache under its filename
                String fileName = Paths.get(resourcePath).getFileName().toString();
                Path out = cachePath.resolve(fileName);
                Files.createDirectories(out.getParent());
                Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[startup] Failed to extract bundled model " + resourcePath + ": " + e.getMessage());
        }
    }
}
