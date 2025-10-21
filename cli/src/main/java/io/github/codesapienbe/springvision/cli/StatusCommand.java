package io.github.codesapienbe.springvision.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "status",
    description = "Check Spring Vision installation status"
)
@Component
public class StatusCommand implements Callable<Integer> {

    private static final String SPRING_VISION_HOME = System.getProperty("user.home") + "/.springvision";

    @Override
    public Integer call() throws Exception {
        System.out.println("📊 Spring Vision Installation Status");
        System.out.println("===================================");

        Path installDir = Paths.get(SPRING_VISION_HOME);

        if (!Files.exists(installDir)) {
            System.out.println("❌ Spring Vision is not installed");
            System.out.println("   Install with: java -jar spring-vision-installer.jar install");
            return 1;
        }

        System.out.println("✅ Spring Vision is installed");
        System.out.println("   Location: " + SPRING_VISION_HOME);

        // Check for JAR file
        Path jarFile = installDir.resolve("spring-vision-mcp.jar");
        if (Files.exists(jarFile)) {
            try {
                long size = Files.size(jarFile);
                System.out.println("✅ JAR file: " + jarFile + " (" + formatSize(size) + ")");
            } catch (IOException e) {
                System.out.println("⚠️  JAR file: " + jarFile + " (size unknown)");
            }
        } else {
            System.out.println("❌ JAR file missing: " + jarFile);
        }

        // Check for run script
        Path runScript = installDir.resolve("run.sh");
        if (Files.exists(runScript)) {
            System.out.println("✅ Run script: " + runScript);
        } else {
            System.out.println("❌ Run script missing: " + runScript);
        }

        // Check for run.java
        Path runJava = installDir.resolve("run.java");
        if (Files.exists(runJava)) {
            System.out.println("✅ JBang script: " + runJava);
        } else {
            System.out.println("❌ JBang script missing: " + runJava);
        }

        // Check MCP configuration
        checkMcpConfig();

        // Show usage instructions
        System.out.println("\n🚀 Usage:");
        System.out.println("   ~/.springvision/run.sh                    # Run MCP server");
        System.out.println("   jbang ~/.springvision/run.java           # Run with JBang");

        return 0;
    }

    private void checkMcpConfig() {
        Path claudeConfig = Paths.get(System.getProperty("user.home"), ".config", "claude_desktop_config.json");

        if (Files.exists(claudeConfig)) {
            try {
                String content = Files.readString(claudeConfig);
                if (content.contains("spring-vision-mcp")) {
                    System.out.println("✅ MCP config: Configured in Claude Desktop");
                } else {
                    System.out.println("⚠️  MCP config: File exists but spring-vision-mcp not configured");
                }
            } catch (IOException e) {
                System.out.println("⚠️  MCP config: Could not read config file");
            }
        } else {
            System.out.println("❌ MCP config: Not configured for Claude Desktop");
            System.out.println("   Run 'install' command to set up MCP configuration");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
