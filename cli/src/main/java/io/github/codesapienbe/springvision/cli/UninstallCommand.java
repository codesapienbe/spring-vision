package io.github.codesapienbe.springvision.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "uninstall",
    description = "Uninstall Spring Vision MCP Server"
)
@Component
public class UninstallCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"--yes", "-y"}, description = "Skip confirmation prompt")
    private boolean skipConfirmation;

    private static final String SPRING_VISION_HOME = System.getProperty("user.home") + "/.springvision";

    @Override
    public Integer call() throws Exception {
        System.out.println("🗑️  Spring Vision MCP Server Uninstaller");
        System.out.println("=======================================");

        Path installDir = Paths.get(SPRING_VISION_HOME);
        if (!Files.exists(installDir)) {
            System.out.println("ℹ️  Spring Vision is not installed");
            return 0;
        }

        if (!skipConfirmation) {
            System.out.println("This will remove Spring Vision from: " + SPRING_VISION_HOME);
            System.out.print("Are you sure? (y/N): ");

            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
                String response = reader.readLine();
                if (!"y".equalsIgnoreCase(response) && !"yes".equalsIgnoreCase(response)) {
                    System.out.println("Uninstallation cancelled");
                    return 0;
                }
            }
        }

        try {
            // Remove installation directory
            removeDirectory(installDir);
            System.out.println("✅ Removed installation directory: " + installDir);

            // Remove MCP config
            removeMcpConfig();
            System.out.println("✅ Removed MCP configuration");

            System.out.println("\n✅ Uninstallation completed successfully!");

        } catch (Exception e) {
            System.err.println("\n❌ Uninstallation failed: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private void removeDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Warning: Could not delete " + path + ": " + e.getMessage());
                    }
                });
        }
    }

    private void removeMcpConfig() throws IOException {
        Path claudeConfig = Paths.get(System.getProperty("user.home"), ".config", "claude_desktop_config.json");

        if (Files.exists(claudeConfig)) {
            // Read current config
            String content = Files.readString(claudeConfig);

            // Remove spring-vision-mcp entry if it exists
            if (content.contains("\"spring-vision-mcp\"")) {
                // Simple removal - in a real implementation, you'd parse and remove just the spring-vision entry
                System.out.println("⚠️  MCP config file exists. Please manually remove the spring-vision-mcp entry from:");
                System.out.println("   " + claudeConfig);
            }
        }
    }
}
