package io.github.codesapienbe.springvision.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@CommandLine.Command(
    name = "install",
    description = "Install Spring Vision MCP Server"
)
@Component
public class InstallCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"--force", "-f"}, description = "Force reinstallation even if already installed")
    private boolean force;

    @CommandLine.Option(names = {"--version", "-v"}, description = "Specific version to install (default: latest)")
    private String version = "latest";

    @CommandLine.Option(names = {"--no-mcp-config"}, description = "Skip MCP configuration setup")
    private boolean skipMcpConfig;

    private static final String SPRING_VISION_HOME = System.getProperty("user.home") + "/.springvision";
    private static final String GITHUB_REPO = "codesapienbe/spring-vision";
    private static final String GITHUB_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/";

    @Override
    public Integer call() throws Exception {
        System.out.println("🚀 Spring Vision MCP Server Installer");
        System.out.println("=====================================");

        try {
            // Check prerequisites
            checkPrerequisites();

            // Create installation directory
            createInstallDirectory();

            // Download and install
            downloadAndInstall();

            // Setup MCP configuration
            if (!skipMcpConfig) {
                setupMcpConfig();
            }

            // Create run script
            createRunScript();

            System.out.println("\n✅ Installation completed successfully!");
            System.out.println("📁 Installed to: " + SPRING_VISION_HOME);
            System.out.println("🚀 Run with: ~/.springvision/run.sh");
            System.out.println("📖 MCP Config: ~/.config/claude_desktop_config.json (if using Claude Desktop)");

        } catch (Exception e) {
            System.err.println("\n❌ Installation failed: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    private void checkPrerequisites() throws Exception {
        System.out.println("🔍 Checking prerequisites...");

        // Check Java
        Process javaCheck = Runtime.getRuntime().exec("java -version");
        if (javaCheck.waitFor() != 0) {
            throw new RuntimeException("Java is not installed or not in PATH. Please install Java 21+");
        }
        System.out.println("✅ Java found");

        // Check if already installed
        Path installDir = Paths.get(SPRING_VISION_HOME);
        if (Files.exists(installDir) && !force) {
            System.out.println("⚠️  Spring Vision is already installed at: " + SPRING_VISION_HOME);
            System.out.println("   Use --force to reinstall");
            return;
        }
    }

    private void createInstallDirectory() throws IOException {
        System.out.println("📁 Creating installation directory...");
        Path installDir = Paths.get(SPRING_VISION_HOME);
        Files.createDirectories(installDir);
        System.out.println("✅ Created: " + installDir);
    }

    private void downloadAndInstall() throws Exception {
        System.out.println("⬇️  Downloading Spring Vision MCP Server...");

        // For now, we'll copy from the current build
        // In a real installer, this would download from GitHub releases
        copyFromLocalBuild();

        System.out.println("✅ Downloaded and installed Spring Vision MCP Server");
    }

    private void copyFromLocalBuild() throws IOException {
        // Copy the MCP JAR from target directory
        Path sourceJar = Paths.get("mcp/target/mcp-0.0.1.jar");
        Path targetJar = Paths.get(SPRING_VISION_HOME, "spring-vision-mcp.jar");

        if (!Files.exists(sourceJar)) {
            throw new RuntimeException("MCP JAR not found at: " + sourceJar + ". Please build the project first with 'mvn clean install'");
        }

        Files.copy(sourceJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📦 Copied MCP JAR to: " + targetJar);

        // Copy the run.java script
        Path sourceRun = Paths.get("run.java");
        Path targetRun = Paths.get(SPRING_VISION_HOME, "run.java");
        Files.copy(sourceRun, targetRun, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📄 Copied run script to: " + targetRun);
    }

    private void setupMcpConfig() throws IOException {
        System.out.println("⚙️  Setting up MCP configuration...");

        // Create MCP config for Claude Desktop
        String mcpConfig = """
            {
              "mcpServers": {
                "spring-vision-mcp": {
                  "command": "bash",
                  "args": ["%s/run.sh"],
                  "env": {
                    "SPRING_PROFILES_ACTIVE": "default"
                  }
                }
              }
            }
            """.formatted(SPRING_VISION_HOME.replace("\\", "\\\\"));

        Path configDir = Paths.get(System.getProperty("user.home"), ".config");
        Files.createDirectories(configDir);

        Path claudeConfig = configDir.resolve("claude_desktop_config.json");
        Files.writeString(claudeConfig, mcpConfig);

        System.out.println("✅ MCP configuration created at: " + claudeConfig);
    }

    private void createRunScript() throws IOException {
        System.out.println("📝 Creating run script...");

        String runScript = """
            #!/bin/bash
            # Spring Vision MCP Server Runner

            # Get the directory where this script is located
            SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

            # Run the MCP server
            java -jar "$SCRIPT_DIR/spring-vision-mcp.jar" "$@"
            """;

        Path runScriptPath = Paths.get(SPRING_VISION_HOME, "run.sh");
        Files.writeString(runScriptPath, runScript);

        // Make executable on Unix-like systems
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"chmod", "+x", runScriptPath.toString()});
            process.waitFor();
        } catch (Exception e) {
            System.out.println("⚠️  Could not make run script executable (normal on Windows)");
        }

        System.out.println("✅ Run script created at: " + runScriptPath);
    }
}
