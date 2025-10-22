package io.github.codesapienbe.springvision.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Vision MCP Server Setup CLI Tool
 */
@Command(
    name = "spring-vision-cli",
    mixinStandardHelpOptions = true,
    versionProvider = SpringVisionCliApplication.class,
    description = """
        🌟 Spring Vision MCP Server Setup Tool

        Downloads and sets up the Spring Vision MCP Server JAR file locally
        to avoid network timeouts during startup.
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
        """
)
public class SpringVisionCliApplication implements Callable<Integer>, IVersionProvider {

    private static final Logger logger = LoggerFactory.getLogger(SpringVisionCliApplication.class);

    // Configuration
    private static final String GITHUB_REPO = "codesapienbe/spring-vision";
    private static final String SPRING_VISION_DIR = System.getProperty("user.home") + "/.springvision";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    // ANSI color codes and effects
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String ITALIC = "\u001B[3m";
    private static final String UNDERLINE = "\u001B[4m";
    private static final String BLINK = "\u001B[5m";
    
    // Colors
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    
    // Bright colors
    private static final String BRIGHT_BLACK = "\u001B[90m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_MAGENTA = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";
    
    // Background colors
    private static final String BG_CYAN = "\u001B[46m";
    private static final String BG_MAGENTA = "\u001B[45m";
    
    // Unicode characters
    private static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final String[] DOTS_FRAMES = {"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};
    private static final String[] PROGRESS_BLOCKS = {" ", "▏", "▎", "▍", "▌", "▋", "▊", "▉", "█"};
    
    // Cursor control
    private static final String CLEAR_LINE = "\r\u001B[K";
    private static final String SAVE_CURSOR = "\u001B[s";
    private static final String RESTORE_CURSOR = "\u001B[u";
    private static final String HIDE_CURSOR = "\u001B[?25l";
    private static final String SHOW_CURSOR = "\u001B[?25h";

    @Option(names = {"-f", "--force"}, description = "Force re-download even if JAR already exists")
    private boolean forceDownload = false;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    @Option(names = {"--no-color"}, description = "Disable colored output")
    private boolean noColor = false;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SpringVisionCliApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public String[] getVersion() throws Exception {
        try {
            // Try to read version from VERSION file in the working directory
            java.nio.file.Path versionPath = java.nio.file.Paths.get("VERSION");
            if (java.nio.file.Files.exists(versionPath)) {
                String version = java.nio.file.Files.readString(versionPath).trim();
                return new String[]{"Spring Vision CLI " + version};
            }
            // Fallback to reading from classpath resource
            try (var inputStream = getClass().getClassLoader().getResourceAsStream("VERSION")) {
                if (inputStream != null) {
                    String version = new String(inputStream.readAllBytes()).trim();
                    return new String[]{"Spring Vision CLI " + version};
                }
            }
            return new String[]{"Spring Vision CLI (version unknown)"};
        } catch (Exception e) {
            return new String[]{"Spring Vision CLI (version unknown)"};
        }
    }

    @Override
    public Integer call() throws Exception {
        printWelcomeBanner();

        try {
            if (!checkJBang()) {
                printError("JBang is not installed. Please install it first:");
                printBox("  curl -Ls https://sh.jbang.dev | bash -s - app setup\n  or visit: https://www.jbang.dev/download/", YELLOW);
                return 1;
            }

            if (!createDirectory()) {
                return 1;
            }

            ReleaseInfo releaseInfo = getLatestRelease();
            if (releaseInfo == null) {
                return 1;
            }

            if (!downloadJar(releaseInfo)) {
                return 1;
            }

            printSetupInfo(releaseInfo);
            printSuccessBanner();
            return 0;

        } catch (Exception e) {
            printError("Setup failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private void printWelcomeBanner() {
        println();
        String bannerText = """
            ███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗     ██╗   ██╗██╗███████╗██╗ ██████╗ ███╗   ██╗
            ██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝     ██║   ██║██║██╔════╝██║██╔═══██╗████╗  ██║
            ███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗    ██║   ██║██║███████╗██║██║   ██║██╔██╗ ██║
            ╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║    ╚██╗ ██╔╝██║╚════██║██║██║   ██║██║╚██╗██║
            ███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝     ╚████╔╝ ██║███████║██║╚██████╔╝██║ ╚████║
            ╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝       ╚═══╝  ╚═╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝
            """;
        String[] banner = bannerText.trim().split("\n");
        
        for (int i = 0; i < banner.length; i++) {
            String color = (i % 2 == 0) ? BRIGHT_CYAN : CYAN;
            printColor(BOLD + color, "           " + banner[i]);
        }
        
        println();
        printCentered(BOLD + BRIGHT_MAGENTA + "🚀 MCP Server Setup & Management Tool 🚀");
        printCentered(DIM + BRIGHT_BLACK + "v0.0.4");
        println();
        printGradientLine("═", 100);
        println();
    }

    private void printSuccessBanner() {
        println();
        printGradientLine("═", 100);
        println();

        String successArtText = """
               _____ _    _  _____ _____ ______  _____ _____
              / ____| |  | |/ ____/ ____|  ____|/ ____/ ____|
             | (___ | |  | | |   | |    | |__  | (___| (___
              \\___ \\| |  | | |   | |    |  __|  \\___ \\\\___ \\
              ____) | |__| | |___| |____| |____ ____) |___) |
             |_____/ \\____/ \\_____\\_____|______|_____/_____/
            """;
        String[] successArt = successArtText.trim().split("\n");
        
        for (String line : successArt) {
            printColor(BOLD + BRIGHT_GREEN, "           " + line);
        }
        
        println();
        printCentered(BRIGHT_GREEN + "✨ " + BOLD + "SETUP COMPLETED SUCCESSFULLY!" + RESET + BRIGHT_GREEN + " ✨");
        println();
        printGradientLine("═", 100);
        println();
    }

    private boolean checkJBang() {
        Spinner spinner = new Spinner("Checking JBang installation");
        spinner.start();
        
        try {
            Thread.sleep(500); // Dramatic pause
            ProcessBuilder pb = new ProcessBuilder("jbang", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                spinner.success("JBang is installed and ready!");
                return true;
            }
        } catch (Exception e) {
            // JBang not found
        }
        
        spinner.fail("JBang not found!");
        return false;
    }

    private boolean createDirectory() {
        Spinner spinner = new Spinner("Creating Spring Vision directory");
        spinner.start();
        
        try {
            Thread.sleep(300);
            Path dir = Paths.get(SPRING_VISION_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                spinner.success("Directory created: " + BRIGHT_CYAN + SPRING_VISION_DIR);
            } else {
                spinner.success("Directory ready: " + BRIGHT_CYAN + SPRING_VISION_DIR);
            }
            return true;
        } catch (Exception e) {
            spinner.fail("Failed to create directory: " + e.getMessage());
            return false;
        }
    }

    private ReleaseInfo getLatestRelease() {
        Spinner spinner = new Spinner("Fetching latest release from GitHub");
        spinner.start();

        try {
            Thread.sleep(500);
            URI uri = URI.create(GITHUB_API_URL);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                spinner.fail("Failed to fetch release info. HTTP " + responseCode);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonNode rootNode = objectMapper.readTree(response.toString());
                String tagName = rootNode.get("tag_name").asText();
                String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                JsonNode assets = rootNode.get("assets");
                if (assets != null && assets.isArray()) {
                    for (JsonNode asset : assets) {
                        String name = asset.get("name").asText();
                        if (name.contains("mcp-") && name.endsWith(".jar")) {
                            String downloadUrl = asset.get("browser_download_url").asText();
                            long size = asset.get("size").asLong();

                            spinner.success("Found version " + BRIGHT_MAGENTA + "v" + version + RESET + GREEN + " (" + formatFileSize(size) + ")");
                            
                            println();
                            printBox("📦 " + name + "\n🔗 " + downloadUrl, CYAN);
                            println();

                            return new ReleaseInfo(version, downloadUrl, name, size);
                        }
                    }
                }

                spinner.fail("No MCP JAR found in release assets");
                return null;
            }
        } catch (Exception e) {
            spinner.fail("Failed to fetch release information: " + e.getMessage());
            return null;
        }
    }

    private boolean downloadJar(ReleaseInfo releaseInfo) {
        Path jarPath = Paths.get(SPRING_VISION_DIR, releaseInfo.jarName);

        if (Files.exists(jarPath) && !forceDownload) {
            printInfo("JAR file already exists!");
            printBox("💾 " + jarPath + "\n💡 Use --force to re-download", YELLOW);
            return true;
        }

        if (forceDownload && Files.exists(jarPath)) {
            Spinner spinner = new Spinner("Removing existing JAR");
            spinner.start();
            try {
                Thread.sleep(300);
                Files.delete(jarPath);
                spinner.success("Existing JAR removed");
            } catch (IOException | InterruptedException e) {
                spinner.fail("Failed to remove existing JAR: " + e.getMessage());
                return false;
            }
        }

        println();
        printColor(BOLD + BRIGHT_CYAN, "╔══════════════════════════════════════════════════════════════════════════════════════════════════╗");
        printColor(BOLD + BRIGHT_CYAN, "║                                  ⬇️  DOWNLOADING MCP SERVER  ⬇️                                  ║");
        printColor(BOLD + BRIGHT_CYAN, "╚══════════════════════════════════════════════════════════════════════════════════════════════════╝");
        println();

        try {
            URI uri = URI.create(releaseInfo.downloadUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            long fileSize = conn.getContentLengthLong();
            if (fileSize <= 0) {
                fileSize = releaseInfo.size;
            }

            try (var inputStream = conn.getInputStream()) {
                UnicodeProgressInputStream progressStream = new UnicodeProgressInputStream(inputStream, fileSize);
                Files.copy(progressStream, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(jarPath)) {
                printError("Download failed - JAR file not found after download");
                return false;
            }

            long finalSize = Files.size(jarPath);
            if (finalSize < 500000000) {
                printError("Downloaded file seems too small (" + formatFileSize(finalSize) + ")");
                Files.deleteIfExists(jarPath);
                return false;
            }

            println();
            println();
            printColor(BOLD + BRIGHT_GREEN, "✓ Download completed successfully! (" + formatFileSize(finalSize) + ")");
            printColor(BRIGHT_BLACK, "  └─ Saved to: " + jarPath);
            println();

            return true;

        } catch (Exception e) {
            printError("Download failed: " + e.getMessage());
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
        Path jarPath = Paths.get(SPRING_VISION_DIR, releaseInfo.jarName);
        
        printGradientLine("═", 100);
        println();
        printCentered(BOLD + BRIGHT_MAGENTA + "🎯 SETUP INFORMATION 🎯");
        println();
        printGradientLine("═", 100);
        println();

        printInfoLine("Version", releaseInfo.version, BRIGHT_MAGENTA);
        printInfoLine("Location", jarPath.toString(), BRIGHT_CYAN);
        printInfoLine("Size", formatFileSize(releaseInfo.size), BRIGHT_YELLOW);
        
        println();
        printBox(BOLD + "🚀 To run the MCP server:" + RESET + "\n\n  " + BRIGHT_GREEN + "jbang " + jarPath + RESET, CYAN);
        println();
        
        String mcpConfig = """
            {
              "mcpServers": {
                "spring-vision": {
                  "command": "jbang",
                  "args": ["%s"]
                }
              }
            }
            """.formatted(jarPath);

        printBox(BOLD + "⚙️  Add to MCP client config (~/.cursor/mcp.json):" + RESET + "\n\n" +
                 BRIGHT_BLACK + mcpConfig.strip() + RESET, MAGENTA);
        println();
    }

    private void printInfoLine(String label, String value, String color) {
        String formatted = String.format("  %-12s %s", 
            BOLD + BRIGHT_WHITE + label + ":" + RESET, 
            color + value + RESET);
        println(formatted);
    }

    private void printBox(String content, String borderColor) {
        String[] lines = content.split("\n");
        int maxLength = 0;
        for (String line : lines) {
            int length = stripAnsi(line).length();
            if (length > maxLength) maxLength = length;
        }
        
        maxLength = Math.min(maxLength + 4, 96);
        
        printColor(borderColor, "  ╔" + "═".repeat(maxLength) + "╗");
        for (String line : lines) {
            int padding = maxLength - stripAnsi(line).length() - 2;
            printColor(borderColor, "  ║ " + RESET + line + " ".repeat(Math.max(0, padding)) + " " + borderColor + "║");
        }
        printColor(borderColor, "  ╚" + "═".repeat(maxLength) + "╝");
    }

    private void printGradientLine(String character, int length) {
        String[] colors = {BRIGHT_CYAN, CYAN, BRIGHT_BLUE, BLUE, BRIGHT_MAGENTA, MAGENTA};
        StringBuilder line = new StringBuilder("  ");
        int segmentSize = length / colors.length;

        for (int i = 0; i < colors.length; i++) {
            line.append(colors[i]).append(character.repeat(segmentSize));
        }
        println(line.toString() + RESET);
    }

    private void printCentered(String text) {
        int padding = (100 - stripAnsi(text).length()) / 2;
        println(" ".repeat(Math.max(0, padding)) + text + RESET);
    }

    private String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void printInfo(String message) {
        printColor(BRIGHT_BLUE, "  ℹ️  " + message);
    }

    private void printError(String message) {
        printColor(BOLD + BRIGHT_RED, "  ❌ " + message);
    }

    private void printColor(String color, String message) {
        if (noColor) {
            System.out.println(stripAnsi(message));
        } else {
            System.out.println(color + message + RESET);
        }
    }

    private void println() {
        System.out.println();
    }

    private void println(String message) {
        System.out.println(message);
    }

    /**
     * Animated spinner class
     */
    private class Spinner {
        private final String message;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicInteger frameIndex = new AtomicInteger(0);
        private ScheduledExecutorService executor;

        Spinner(String message) {
            this.message = message;
        }

        void start() {
            running.set(true);
            System.out.print(HIDE_CURSOR);
            
            executor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "spinner-animation");
                thread.setDaemon(true);
                return thread;
            });
            
            executor.scheduleAtFixedRate(() -> {
                if (running.get()) {
                    int index = frameIndex.getAndIncrement() % SPINNER_FRAMES.length;
                    String frame = SPINNER_FRAMES[index];
                    System.out.print(CLEAR_LINE + "  " + BRIGHT_CYAN + frame + RESET + " " + message + "...");
                }
            }, 0, 80, TimeUnit.MILLISECONDS);
        }

        void success(String finalMessage) {
            stop();
            System.out.println(CLEAR_LINE + "  " + BRIGHT_GREEN + "✓" + RESET + " " + finalMessage);
        }

        void fail(String finalMessage) {
            stop();
            System.out.println(CLEAR_LINE + "  " + BRIGHT_RED + "✗" + RESET + " " + finalMessage);
        }

        void stop() {
            running.set(false);
            if (executor != null) {
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.print(SHOW_CURSOR);
        }
    }

    /**
     * Unicode progress bar input stream
     */
    private class UnicodeProgressInputStream extends java.io.FilterInputStream {
        private final long totalSize;
        private long bytesRead = 0;
        private long lastUpdateTime = System.currentTimeMillis();
        private long startTime = System.currentTimeMillis();

        protected UnicodeProgressInputStream(java.io.InputStream in, long totalSize) {
            super(in);
            this.totalSize = totalSize;
            System.out.print(HIDE_CURSOR);
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                updateProgress();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytes = super.read(b, off, len);
            if (bytes > 0) {
                bytesRead += bytes;
                updateProgress();
            }
            if (bytes == -1) {
                System.out.print(SHOW_CURSOR);
            }
            return bytes;
        }

        private void updateProgress() {
            long now = System.currentTimeMillis();
            if (now - lastUpdateTime < 100) return; // Update every 100ms
            lastUpdateTime = now;

            double progress = (double) bytesRead / totalSize;
            int barWidth = 50;
            
            // Calculate progress bar
            double progressChars = progress * barWidth;
            int fullBlocks = (int) progressChars;
            int partialBlock = (int) ((progressChars - fullBlocks) * 8);
            
            StringBuilder bar = new StringBuilder();
            bar.append(BRIGHT_GREEN);
            bar.append("█".repeat(fullBlocks));
            if (fullBlocks < barWidth && partialBlock > 0) {
                bar.append(PROGRESS_BLOCKS[partialBlock]);
            }
            bar.append(RESET).append(DIM);
            bar.append("░".repeat(Math.max(0, barWidth - fullBlocks - 1)));
            bar.append(RESET);

            // Calculate speed and ETA
            long elapsed = (now - startTime) / 1000;
            double speed = elapsed > 0 ? (double) bytesRead / elapsed : 0;
            long remaining = speed > 0 ? (long) ((totalSize - bytesRead) / speed) : 0;

            String progressText = String.format(
                "%s  %s %s %3.0f%% %s │ %s %s/%s %s │ %s %s/s %s │ %s ETA: %s",
                CLEAR_LINE,
                BRIGHT_CYAN + "⚡" + RESET,
                bar,
                progress * 100,
                RESET,
                BRIGHT_YELLOW,
                formatFileSize(bytesRead),
                formatFileSize(totalSize),
                RESET,
                BRIGHT_MAGENTA,
                formatFileSize((long) speed),
                RESET,
                BRIGHT_BLUE,
                formatTime(remaining) + RESET
            );

            System.out.print(progressText);
        }

        private String formatTime(long seconds) {
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

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
}
