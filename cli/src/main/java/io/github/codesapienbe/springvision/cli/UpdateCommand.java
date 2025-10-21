package io.github.codesapienbe.springvision.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "update",
    description = "Update Spring Vision MCP Server to the latest version"
)
@Component
public class UpdateCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"--version", "-v"}, description = "Specific version to update to")
    private String version = "latest";

    private static final String SPRING_VISION_HOME = System.getProperty("user.home") + "/.springvision";

    @Override
    public Integer call() throws Exception {
        System.out.println("🔄 Spring Vision Update");
        System.out.println("=======================");

        Path installDir = Paths.get(SPRING_VISION_HOME);
        if (!Files.exists(installDir)) {
            System.out.println("❌ Spring Vision is not installed");
            System.out.println("   Install first with: java -jar spring-vision-installer.jar install");
            return 1;
        }

        try {
            System.out.println("📦 Updating Spring Vision MCP Server...");

            // For now, we'll copy from the current build
            // In a real updater, this would download from GitHub releases
            updateFromLocalBuild();

            System.out.println("\n✅ Update completed successfully!");
            System.out.println("🔄 Restart any running Spring Vision instances to use the new version");

        } catch (Exception e) {
            System.err.println("\n❌ Update failed: " + e.getMessage());
            return 1;
        }

        return 0;
    }

    private void updateFromLocalBuild() throws IOException {
        // Copy the updated MCP JAR from target directory
        Path sourceJar = Paths.get("mcp/target/mcp-0.0.1.jar");
        Path targetJar = Paths.get(SPRING_VISION_HOME, "spring-vision-mcp.jar");

        if (!Files.exists(sourceJar)) {
            throw new RuntimeException("Updated MCP JAR not found at: " + sourceJar + ". Please build the project first with 'mvn clean install'");
        }

        Files.copy(sourceJar, targetJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📦 Updated JAR file: " + targetJar);

        // Copy the updated run.java script
        Path sourceRun = Paths.get("run.java");
        Path targetRun = Paths.get(SPRING_VISION_HOME, "run.java");
        Files.copy(sourceRun, targetRun, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("📄 Updated run script: " + targetRun);

        // Update the run.sh script if it exists
        Path runScript = Paths.get(SPRING_VISION_HOME, "run.sh");
        if (Files.exists(runScript)) {
            // The run.sh script should already be up to date, but let's recreate it to be safe
            String runScriptContent = """
                #!/bin/bash
                # Spring Vision MCP Server Runner

                # Get the directory where this script is located
                SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

                # Run the MCP server
                java -jar "$SCRIPT_DIR/spring-vision-mcp.jar" "$@"
                """;

            Files.writeString(runScript, runScriptContent);
            System.out.println("📝 Updated run.sh script: " + runScript);
        }
    }
}
