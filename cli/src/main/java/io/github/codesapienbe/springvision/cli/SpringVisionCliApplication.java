package io.github.codesapienbe.springvision.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Help.Ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

/**
 * Spring Vision MCP Server Setup CLI Tool
 *
 * This tool downloads the latest Spring Vision MCP Server JAR file
 * and sets it up locally to avoid network timeouts during startup.
 */
@Command(
    name = "spring-vision-cli",
    mixinStandardHelpOptions = true,
    version = "Spring Vision CLI 0.0.3",
    description = """
        🌟 Spring Vision MCP Server Setup Tool

        Downloads and sets up the Spring Vision MCP Server JAR file locally
        to avoid network timeouts during startup.

        This tool replaces the old build.sh script with a modern,
        user-friendly command-line interface.
        """,
    headerHeading = "@|bold,green 🌟 Spring Vision MCP Server Setup|@%n%n",
    synopsisHeading = "%n@|bold,cyan USAGE:|@%n%n",
    descriptionHeading = "%n@|bold,cyan DESCRIPTION:|@%n%n",
    optionListHeading = "%n@|bold,cyan OPTIONS:|@%n%n",
    footerHeading = "%n@|bold,yellow EXAMPLES:|@%n",
    footer = """
        @|bold Run setup (downloads latest version):|@
          $ spring-vision-cli

        @|bold Force re-download:|@
          $ spring-vision-cli --force

        @|bold Show version:|@
          $ spring-vision-cli --version

        @|bold Show help:|@
          $ spring-vision-cli --help
        """
)
public class SpringVisionCliApplication implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SpringVisionCliApplication.class);

    // Configuration
    private static final String GITHUB_REPO = "codesapienbe/spring-vision";
    private static final String SPRING_VISION_DIR = System.getProperty("user.home") + "/.springvision";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    // ANSI color codes for output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    @Option(
        names = {"-f", "--force"},
        description = "Force re-download even if JAR already exists"
    )
    private boolean forceDownload = false;

    @Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose output"
    )
    private boolean verbose = false;

    @Option(
        names = {"--no-color"},
        description = "Disable colored output"
    )
    private boolean noColor = false;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SpringVisionCliApplication())
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        printBanner();

        try {
            // Check if JBang is installed
            if (!checkJBang()) {
                printError("JBang is not installed. Please install it first:");
                printInfo("  curl -Ls https://sh.jbang.dev | bash -s - app setup");
                printInfo("  or visit: https://www.jbang.dev/download/");
                return 1;
            }

            // Create Spring Vision directory
            if (!createDirectory()) {
                return 1;
            }

            // Get latest release information
            ReleaseInfo releaseInfo = getLatestRelease();
            if (releaseInfo == null) {
                return 1;
            }

            // Download JAR file
            if (!downloadJar(releaseInfo)) {
                return 1;
            }

            // Print setup information
            printSetupInfo(releaseInfo);

            printSuccess("✨ Setup completed successfully!");
            return 0;

        } catch (Exception e) {
            printError("Setup failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void printBanner() {
        println();
        printColor(BOLD + CYAN, "╔════════════════════════════════════════════════════════════════╗");
        printColor(BOLD + CYAN, "║                    🌟 Spring Vision CLI 🌟                    ║");
        printColor(BOLD + CYAN, "║              MCP Server Setup & Management Tool               ║");
        printColor(BOLD + CYAN, "╚════════════════════════════════════════════════════════════════╝");
        println();
    }

    private boolean checkJBang() {
        printInfo("🔍 Checking JBang installation...");
        try {
            ProcessBuilder pb = new ProcessBuilder("jbang", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                printSuccess("JBang is installed ✓");
                return true;
            }
        } catch (Exception e) {
            // JBang not found
        }
        return false;
    }

    private boolean createDirectory() {
        printInfo("📁 Creating Spring Vision directory: " + SPRING_VISION_DIR);
        try {
            Path dir = Paths.get(SPRING_VISION_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                printSuccess("Directory created: " + SPRING_VISION_DIR);
            } else {
                printInfo("Directory already exists: " + SPRING_VISION_DIR);
            }
            return true;
        } catch (IOException e) {
            printError("Failed to create directory: " + e.getMessage());
            return false;
        }
    }

    private ReleaseInfo getLatestRelease() {
        printInfo("📡 Fetching latest release information from GitHub...");

        try {
            URI uri = URI.create(GITHUB_API_URL);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                printError("Failed to fetch release info. HTTP " + responseCode);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonNode rootNode = objectMapper.readTree(response.toString());

                // Extract version from tag_name (remove 'v' prefix if present)
                String tagName = rootNode.get("tag_name").asText();
                String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                printSuccess("Latest version: " + version);

                // Find MCP JAR download URL
                JsonNode assets = rootNode.get("assets");
                if (assets != null && assets.isArray()) {
                    for (JsonNode asset : assets) {
                        String name = asset.get("name").asText();
                        if (name.contains("mcp-") && name.endsWith(".jar")) {
                            String downloadUrl = asset.get("browser_download_url").asText();
                            long size = asset.get("size").asLong();

                            printInfo("Found MCP JAR: " + name);
                            printInfo("Download URL: " + downloadUrl);
                            printInfo("Size: " + formatFileSize(size));

                            return new ReleaseInfo(version, downloadUrl, name, size);
                        }
                    }
                }

                printError("No MCP JAR found in release assets");
                return null;
            }
        } catch (Exception e) {
            printError("Failed to fetch release information: " + e.getMessage());
            return null;
        }
    }

    private boolean downloadJar(ReleaseInfo releaseInfo) {
        Path jarPath = Paths.get(SPRING_VISION_DIR, releaseInfo.jarName);

        // Check if JAR exists
        if (Files.exists(jarPath) && !forceDownload) {
            printInfo("JAR file already exists: " + jarPath);
            printInfo("Use --force to re-download");
            return true;
        }

        if (forceDownload && Files.exists(jarPath)) {
            printInfo("🔄 Force download enabled - removing existing JAR");
            try {
                Files.delete(jarPath);
            } catch (IOException e) {
                printError("Failed to remove existing JAR: " + e.getMessage());
                return false;
            }
        }

        printInfo("⬇️  Downloading Spring Vision MCP Server v" + releaseInfo.version);
        printInfo("   This may take a few minutes due to the large file size (~" +
                 formatFileSize(releaseInfo.size) + ")...");

        try {
            URI uri = URI.create(releaseInfo.downloadUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            long fileSize = conn.getContentLengthLong();
            if (fileSize <= 0) {
                fileSize = releaseInfo.size; // fallback to GitHub reported size
            }

            try (var inputStream = conn.getInputStream()) {
                // Create progress tracking stream
                ProgressInputStream progressStream = new ProgressInputStream(inputStream, fileSize);
                Files.copy(progressStream, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Verify download
            if (!Files.exists(jarPath)) {
                printError("Download failed - JAR file not found after download");
                return false;
            }

            long finalSize = Files.size(jarPath);
            if (finalSize < 500000000) { // Less than 500MB
                printError("Downloaded file seems too small (" + formatFileSize(finalSize) + "). Download may have failed.");
                Files.deleteIfExists(jarPath);
                return false;
            }

            printSuccess("Download completed successfully! (" + formatFileSize(finalSize) + ")");
            printSuccess("JAR saved to: " + jarPath);

            return true;

        } catch (Exception e) {
            printError("Download failed: " + e.getMessage());
            // Clean up partial download
            try {
                Files.deleteIfExists(jarPath);
            } catch (IOException cleanupError) {
                logger.warn("Failed to clean up partial download", cleanupError);
            }
            return false;
        }
    }

    private void printSetupInfo(ReleaseInfo releaseInfo) {
        println();
        printColor(BOLD + GREEN, "╔════════════════════════════════════════════════════════════════╗");
        printColor(BOLD + GREEN, "║                       🎉 Setup Complete!                      ║");
        printColor(BOLD + GREEN, "╚════════════════════════════════════════════════════════════════╝");
        println();

        Path jarPath = Paths.get(SPRING_VISION_DIR, releaseInfo.jarName);
        printColor(BOLD + CYAN, "MCP Server JAR:");
        printColor(CYAN, "  " + jarPath);
        println();

        printColor(BOLD + CYAN, "Version:");
        printColor(CYAN, "  " + releaseInfo.version);
        println();

        printColor(BOLD + YELLOW, "To run the MCP server:");
        printColor(YELLOW, "  jbang " + jarPath);
        println();

        printColor(BOLD + YELLOW, "To update your MCP client config (~/.cursor/mcp.json):");
        printColor(MAGENTA, "{");
        printColor(MAGENTA, ' ' + "\"mcpServers\": {");
        printColor(MAGENTA, "    \"spring-vision\": {");
        printColor(MAGENTA, "      \"command\": \"jbang\",");
        printColor(MAGENTA, "      \"args\": [\"" + jarPath + "\"]");
        printColor(MAGENTA, "    }");
        printColor(MAGENTA, "  }");
        printColor(MAGENTA, "}");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // Helper methods for colored output
    private void printInfo(String message) {
        printColor(BLUE, "ℹ️  " + message);
    }

    private void printSuccess(String message) {
        printColor(GREEN, "✅ " + message);
    }

    private void printWarn(String message) {
        printColor(YELLOW, "⚠️  " + message);
    }

    private void printError(String message) {
        printColor(RED, "❌ " + message);
    }

    private void printColor(String color, String message) {
        if (noColor) {
            System.out.println(message);
        } else {
            System.out.println(color + message + RESET);
        }
    }

    private void println() {
        System.out.println();
    }

    /**
     * Release information container
     */
    private static class ReleaseInfo {
        final String version;
        final String downloadUrl;
        final String jarName;
        final long size;

        ReleaseInfo(String version, String downloadUrl, String jarName, long size) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.jarName = jarName;
            this.size = size;
        }
    }

    /**
     * Progress tracking input stream for download progress
     */
    private class ProgressInputStream extends java.io.FilterInputStream {
        private final long totalSize;
        private long bytesRead = 0;
        private long lastReportedProgress = 0;

        protected ProgressInputStream(java.io.InputStream in, long totalSize) {
            super(in);
            this.totalSize = totalSize;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                reportProgress();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytes = super.read(b, off, len);
            if (bytes > 0) {
                bytesRead += bytes;
                reportProgress();
            }
            return bytes;
        }

        private void reportProgress() {
            if (totalSize > 0) {
                long progress = (bytesRead * 100) / totalSize;
                if (progress >= lastReportedProgress + 5) { // Report every 5%
                    lastReportedProgress = progress;
                    printInfo("Download progress: " + progress + "% (" +
                             formatFileSize(bytesRead) + " / " + formatFileSize(totalSize) + ")");
                }
            }
        }
    }
}
