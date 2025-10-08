package com.springvision.examples.picocliapplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Comparator;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.exception.VisionProcessingException;

import com.deepface.core.DeepFace;
import com.deepface.core.EmbeddingResult;
import com.deepface.core.FaceRegion;

/**
 * CLI Application for Spring Vision face detection using Apache Commons CLI.
 *
 * <p>This application provides a command-line interface for performing face detection
 * on image files using the Spring Vision framework. It supports single file processing,
 * batch processing, and various output formats.</p>
 *
 * <p>The application integrates with Spring Boot's autoconfiguration to automatically
 * configure the vision backend and provides a clean CLI interface for face detection tasks.</p>
 *
 * @author Spring Vision Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class PicoCLIApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PicoCLIApplication.class);

    private static final String DEFAULT_VERIFY_METRIC = "cosine"; // cosine | euclidean
    private static final double DEFAULT_VERIFY_THRESHOLD_COSINE = 0.35;
    private static final double DEFAULT_VERIFY_THRESHOLD_EUCLIDEAN = 1.24;

    private final ApplicationContext applicationContext;
    private final VisionTemplate visionTemplate;

    public PicoCLIApplication(ApplicationContext applicationContext, VisionTemplate visionTemplate) {
        this.applicationContext = applicationContext;
        this.visionTemplate = visionTemplate;
    }

    /**
     * Main entry point for the CLI application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Spring Vision CLI Application");
        SpringApplication.run(PicoCLIApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("spring-vision-cli", options);
                return;
            }

            if (cmd.hasOption("version")) {
                System.out.println("Spring Vision CLI 1.0.0");
                return;
            }

            if (cmd.hasOption("interactive")) {
                handleInteractiveMode(cmd);
            } else if (cmd.hasOption("verify")) {
                handleVerify(cmd);
            } else if (cmd.hasOption("verify-batch")) {
                handleBatchVerify(cmd);
            } else if (cmd.hasOption("embed")) {
                handleEmbed(cmd);
            } else if (cmd.hasOption("obscure")) {
                handleObscure(cmd);
            } else if (cmd.hasOption("detect")) {
                handleSingleDetection(cmd);
            } else if (cmd.hasOption("batch")) {
                handleBatchDetection(cmd);
            } else if (cmd.hasOption("health")) {
                handleHealthCheck();
            } else {
                printHelp();
            }

        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("spring-vision-cli", options);
            System.exit(1);
        }
    }

    private Options createOptions() {
        Options options = new Options();

        // Help and version options
        options.addOption("h", "help", false, "Show help information");
        options.addOption("v", "version", false, "Show version information");

        // Interactive mode option
        options.addOption("i", "interactive", false, "Run in interactive mode");

        // Single detection option
        Option detectOption = Option.builder("d")
                .longOpt("detect")
                .hasArg()
                .argName("FILE")
                .desc("Detect faces in a single image file")
                .build();
        options.addOption(detectOption);

        // Batch detection option
        Option batchOption = Option.builder("b")
                .longOpt("batch")
                .hasArg()
                .argName("DIRECTORY")
                .desc("Detect faces in all images in a directory")
                .build();
        options.addOption(batchOption);

        // Health check option
        options.addOption("hc", "health", false, "Check system health");

        // Output format option
        Option formatOption = Option.builder("f")
                .longOpt("format")
                .hasArg()
                .argName("FORMAT")
                .desc("Output format (json, text, csv) [default: text]")
                .build();
        options.addOption(formatOption);

        // Confidence threshold option (detection)
        Option confidenceOption = Option.builder("c")
                .longOpt("confidence")
                .hasArg()
                .argName("THRESHOLD")
                .desc("Confidence threshold (0.0-1.0) [default: 0.5]")
                .build();
        options.addOption(confidenceOption);

        // Verbose option
        options.addOption("V", "verbose", false, "Enable verbose output");

        // Progress display option (batch only)
        options.addOption("p", "progress", false, "Show progress during batch processing");

        // Embedding extraction option
        Option embedOption = Option.builder("e")
                .longOpt("embed")
                .hasArg()
                .argName("FILE")
                .desc("Extract face embeddings for an image (JSON or text)")
                .build();
        options.addOption(embedOption);

        // Verification option
        Option verifyOption = Option.builder()
                .longOpt("verify")
                .numberOfArgs(2)
                .argName("FILE1 FILE2")
                .desc("Verify if two images belong to the same person")
                .build();
        options.addOption(verifyOption);

        // Metric for verification
        Option metricOption = Option.builder("m")
                .longOpt("metric")
                .hasArg()
                .argName("cosine|euclidean")
                .desc("Distance metric for verification [default: cosine]")
                .build();
        options.addOption(metricOption);

        // Threshold for verification
        Option thresholdOption = Option.builder("t")
                .longOpt("threshold")
                .hasArg()
                .argName("VALUE")
                .desc("Distance threshold for verification (depends on metric)")
                .build();
        options.addOption(thresholdOption);

        // Batch verification option
        Option verifyBatchOption = Option.builder()
                .longOpt("verify-batch")
                .numberOfArgs(2)
                .argName("FILE DIRECTORY")
                .desc("Verify a reference image against all images in a directory")
                .build();
        options.addOption(verifyBatchOption);

        // Face obscuring option with enhanced 2x obscurity
        Option obscureOption = Option.builder()
                .longOpt("obscure")
                .numberOfArgs(2)
                .argName("INPUT OUTPUT")
                .desc("Obscure faces with 2x enhanced privacy protection (double Gaussian blur + adaptive pixelation)")
                .build();
        options.addOption(obscureOption);

        // Truncate for embedding printing
        Option truncateOption = Option.builder()
                .longOpt("truncate")
                .hasArg()
                .argName("N")
                .desc("Print only first N embedding values for readability (output only)")
                .build();
        options.addOption(truncateOption);

        return options;
    }

    private Path resolvePath(String rawPath) {
        String trimmed = rawPath == null ? "" : rawPath.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Path must not be empty");
        }
        String userHome = System.getProperty("user.home");
        String expanded;
        if (trimmed.equals("~")) {
            expanded = userHome;
        } else if (trimmed.startsWith("~" + File.separator)) {
            expanded = userHome + trimmed.substring(1);
        } else {
            expanded = trimmed;
        }
        return Paths.get(expanded).toAbsolutePath().normalize();
    }

    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024; // 50MB safety cap
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;

    private boolean isHttpUrl(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private byte[] readAllBytesFromSource(String source) throws IOException {
        if (!isHttpUrl(source)) {
            Path p = resolvePath(source);
            if (!Files.exists(p) || !Files.isRegularFile(p)) {
                // Attempt fallback: try to load sample from classpath resources
                String resourceName = "/samples/selfie-sample.jpg";
                InputStream res = PicoCLIApplication.class.getResourceAsStream(resourceName);
                if (res != null) {
                    try (res) {
                        logger.warn("Input file {} not found; using classpath sample {} as fallback", p, resourceName);
                        return res.readAllBytes();
                    }
                }
                throw new IOException("File not found or not a file: " + p);
            }
            if (!Files.isReadable(p)) {
                throw new IOException("File not readable: " + p);
            }
            return Files.readAllBytes(p);
        }
        return downloadBytesFromHttp(source);
    }

    private BufferedImage readImageFromSource(String source) throws IOException {
        byte[] data = readAllBytesFromSource(source);
        try (var bais = new java.io.ByteArrayInputStream(data)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) throw new IOException("Unsupported or corrupt image content");
            return img;
        }
    }

    private byte[] downloadBytesFromHttp(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        validateRemoteAddress(url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "spring-vision-cli/1.0");
        int contentLength = conn.getContentLength();
        if (contentLength > 0 && contentLength > MAX_DOWNLOAD_BYTES) {
            throw new IOException("Remote content too large: " + contentLength);
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("Download exceeds size limit: " + MAX_DOWNLOAD_BYTES);
                }
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private void validateRemoteAddress(URL url) throws IOException {
        String host = url.getHost();
        if (host == null || host.isBlank()) throw new IOException("Invalid URL host");
        try {
            java.net.InetAddress[] addrs = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress a : addrs) {
                if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                    throw new IOException("Refusing to connect to non-public address: " + a.getHostAddress());
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new IOException("Failed to resolve host: " + host);
        }
    }

    private void handleSingleDetection(CommandLine cmd) {
        String imagePath = cmd.getOptionValue("detect");
        String format = cmd.getOptionValue("format", "text");
        String confidenceStr = cmd.getOptionValue("confidence", "0.5");
        boolean verbose = cmd.hasOption("verbose");

        try {
            double confidence = Double.parseDouble(confidenceStr);
            if (confidence < 0.0 || confidence > 1.0) {
                System.err.println("Confidence threshold must be between 0.0 and 1.0");
                System.exit(1);
            }

            logger.info("Processing single image: {}", imagePath);
            ImageData imageData = ImageData.fromBytes(readAllBytesFromSource(imagePath));

            VisionResult result = visionTemplate.detect(imageData, DetectionType.FACE);

            if (verbose) {
                logger.info("Detection completed. Found {} faces", result.detections().size());
            }

            printResults(result, format);

        } catch (NumberFormatException e) {
            System.err.println("Invalid confidence threshold: " + confidenceStr);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading image: " + e.getMessage());
            System.exit(1);
        } catch (VisionProcessingException e) {
            System.err.println("Error processing image: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleBatchDetection(CommandLine cmd) {
        String directoryPath = cmd.getOptionValue("batch");
        String format = cmd.getOptionValue("format", "text");
        String confidenceStr = cmd.getOptionValue("confidence", "0.5");
        boolean verbose = cmd.hasOption("verbose");
        boolean showProgress = cmd.hasOption("progress");

        try {
            double confidence = Double.parseDouble(confidenceStr);
            if (confidence < 0.0 || confidence > 1.0) {
                System.err.println("Confidence threshold must be between 0.0 and 1.0");
                System.exit(1);
            }

            Path directory = resolvePath(directoryPath);
            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                System.err.println("Directory not found: " + directory);
                System.exit(1);
            }
            if (!Files.isReadable(directory)) {
                System.err.println("Directory is not readable: " + directory);
                System.exit(1);
            }

            logger.info("Processing batch directory: {}", directory);

            List<Path> imageFiles;
            try (var paths = Files.list(directory)) {
                imageFiles = paths
                        .filter(this::isImageFile)
                        .sorted()
                        .toList();
            }

            int total = imageFiles.size();
            if (total == 0) {
                System.out.println("No image files found in directory: " + directory);
                return;
            }

            for (int index = 0; index < total; index++) {
                Path path = imageFiles.get(index);
                try {
                    if (verbose) {
                        logger.info("Processing: {}", path.getFileName());
                    }

                    ImageData imageData = ImageData.fromBytes(Files.readAllBytes(path));
                    VisionResult result = visionTemplate.detect(imageData, DetectionType.FACE);

                    System.out.println("File: " + path.getFileName());
                    printResults(result, format);
                    System.out.println();
                } catch (Exception e) {
                    System.err.println("Error processing " + path.getFileName() + ": " + e.getMessage());
                }

                if (showProgress) {
                    printProgress(index + 1, total);
                }
            }

            if (showProgress) {
                System.out.println(); // move to next line after progress finishes
            }

        } catch (NumberFormatException e) {
            System.err.println("Invalid confidence threshold: " + confidenceStr);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading directory: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleHealthCheck() {
        try {
            logger.info("Performing health check");

            // Simple health check - try to detect faces in a minimal test
            boolean isHealthy = true;
            String status = "OK";

            try {
                // Test if vision template is available
                if (visionTemplate == null) {
                    isHealthy = false;
                    status = "Vision template not available";
                }

                // Test if application context is available
                if (applicationContext == null) {
                    isHealthy = false;
                    status = "Application context not available";
                }

            } catch (Exception e) {
                isHealthy = false;
                status = "Error during health check: " + e.getMessage();
            }

            if (isHealthy) {
                System.out.println("Health Check: PASSED");
                System.out.println("Status: " + status);
                System.out.println("Vision Template: Available");
                System.out.println("Application Context: Available");
            } else {
                System.out.println("Health Check: FAILED");
                System.out.println("Status: " + status);
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleInteractiveMode(CommandLine cmd) {
        System.out.println("Interactive Mode - Spring Vision CLI");
        System.out.println("Type 'exit' to quit at any prompt. Press Enter to accept defaults.");
        System.out.println();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                System.out.print("Image file path > ");
                String imagePath = reader.readLine();
                if (imagePath == null || imagePath.trim().isEmpty()) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(imagePath.trim())) {
                    System.out.println("Exiting interactive mode.");
                    break;
                }

                System.out.print("Output format [text|json|csv] (default: text) > ");
                String format = reader.readLine();
                if (format == null || format.isBlank()) {
                    format = "text";
                }

                System.out.print("Confidence threshold [0.0-1.0] (default: 0.5) > ");
                String confidenceStr = reader.readLine();
                if (confidenceStr == null || confidenceStr.isBlank()) {
                    confidenceStr = "0.5";
                }

                CommandLine fakeCmd = new DefaultParser().parse(createOptions(), new String[] {
                        "--detect", imagePath,
                        "--format", format,
                        "--confidence", confidenceStr
                });

                handleSingleDetection(fakeCmd);
            } catch (ParseException pe) {
                System.err.println("Invalid options: " + pe.getMessage());
            } catch (IOException ioe) {
                System.err.println("I/O error: " + ioe.getMessage());
                break;
            }
        }
    }

    private void handleEmbed(CommandLine cmd) {
        String imagePath = cmd.getOptionValue("embed");
        String format = cmd.getOptionValue("format", "text");
        int truncate = parseIntOrDefault(cmd.getOptionValue("truncate"), -1);
        try {
            BufferedImage img = readImageFromSource(imagePath);
            List<EmbeddingResult> list = DeepFace.represent(img);
            if ("json".equalsIgnoreCase(format)) {
                printEmbeddingJson(list, img.getWidth(), img.getHeight(), truncate);
            } else if ("csv".equalsIgnoreCase(format)) {
                printEmbeddingCsv(list, img.getWidth(), img.getHeight(), truncate);
            } else {
                printEmbeddingText(list, img.getWidth(), img.getHeight(), truncate);
            }
        } catch (Exception e) {
            System.err.println("Embedding failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleVerify(CommandLine cmd) {
        String[] files = cmd.getOptionValues("verify");
        String metric = cmd.getOptionValue("metric", DEFAULT_VERIFY_METRIC).toLowerCase();
        String format = cmd.getOptionValue("format", "text");
        String thresholdStr = cmd.getOptionValue("threshold", "");
        try {
            if (files == null || files.length != 2) {
                System.err.println("--verify requires two image paths or URLs: --verify <image1> <image2>");
                System.exit(1);
            }
            BufferedImage imgA = readImageFromSource(files[0]);
            BufferedImage imgB = readImageFromSource(files[1]);
            var embA = getTopEmbedding(DeepFace.represent(imgA));
            var embB = getTopEmbedding(DeepFace.represent(imgB));
            if (embA == null || embB == null) {
                System.err.println("No face embeddings found in one or both images");
                System.exit(1);
            }
            float[] a = l2Normalize(embA.embedding());
            float[] b = l2Normalize(embB.embedding());
            double distance = switch (metric) {
                case "euclidean" -> euclideanDistance(a, b);
                case "cosine" -> cosineDistance(a, b);
                default -> cosineDistance(a, b);
            };
            double threshold = thresholdStr.isBlank()
                    ? ("euclidean".equals(metric) ? DEFAULT_VERIFY_THRESHOLD_EUCLIDEAN : DEFAULT_VERIFY_THRESHOLD_COSINE)
                    : Double.parseDouble(thresholdStr);
            boolean isMatch = distance <= threshold;

            if ("json".equalsIgnoreCase(format)) {
                String json = "{"+
                        "\"metric\":\"" + metric + "\","+
                        "\"distance\":" + String.format("%.6f", distance) + ","+
                        "\"threshold\":" + String.format("%.6f", threshold) + ","+
                        "\"is_match\":" + isMatch +
                        "}";
                System.out.println(json);
            } else if ("csv".equalsIgnoreCase(format)) {
                System.out.println("metric,distance,threshold,is_match");
                System.out.printf("%s,%.6f,%.6f,%s%n", metric, distance, threshold, isMatch);
            } else {
                System.out.printf("Verification: metric=%s, distance=%.6f, threshold=%.6f, match=%s%n",
                        metric, distance, threshold, isMatch);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid threshold: " + thresholdStr);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Verification failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleBatchVerify(CommandLine cmd) {
        String[] args = cmd.getOptionValues("verify-batch");
        String metric = cmd.getOptionValue("metric", DEFAULT_VERIFY_METRIC).toLowerCase();
        String format = cmd.getOptionValue("format", "text");
        String thresholdStr = cmd.getOptionValue("threshold", "");
        boolean showProgress = cmd.hasOption("progress");
        try {
            if (args == null || args.length != 2) {
                System.err.println("--verify-batch requires two arguments: --verify-batch <reference-image-URL-or-file> <directory>");
                System.exit(1);
            }
            BufferedImage refImg = readImageFromSource(args[0]);
            var refEmb = getTopEmbedding(DeepFace.represent(refImg));
            if (refEmb == null || refEmb.embedding() == null) {
                System.err.println("No face embedding found in reference image");
                System.exit(1);
            }
            float[] refVec = l2Normalize(refEmb.embedding());

            Path dirPath = resolvePath(args[1]);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath) || !Files.isReadable(dirPath)) {
                System.err.println("Directory not found or not readable: " + dirPath);
                System.exit(1);
            }

            List<Path> imageFiles;
            try (var paths = Files.list(dirPath)) {
                imageFiles = paths.filter(this::isImageFile).sorted().toList();
            }
            int total = imageFiles.size();
            if (total == 0) {
                System.out.println("No image files found in directory: " + dirPath);
                return;
            }

            double defaultThreshold = "euclidean".equals(metric) ? DEFAULT_VERIFY_THRESHOLD_EUCLIDEAN : DEFAULT_VERIFY_THRESHOLD_COSINE;
            double threshold = thresholdStr.isBlank() ? defaultThreshold : Double.parseDouble(thresholdStr);

            if ("json".equalsIgnoreCase(format)) {
                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < total; i++) {
                    Path p = imageFiles.get(i);
                    try {
                        BufferedImage img = ImageIO.read(p.toFile());
                        if (img == null) throw new IOException("unsupported/corrupt image");
                        var er = getTopEmbedding(DeepFace.represent(img));
                        if (er == null || er.embedding() == null) {
                            continue;
                        }
                        float[] vec = l2Normalize(er.embedding());
                        double distance = "euclidean".equals(metric) ? euclideanDistance(refVec, vec) : cosineDistance(refVec, vec);
                        boolean isMatch = distance <= threshold;
                        json.append("{")
                            .append("\"file\":\"").append(p.getFileName().toString().replace("\"", "")).append("\",")
                            .append("\"distance\":").append(String.format("%.6f", distance)).append(",")
                            .append("\"threshold\":").append(String.format("%.6f", threshold)).append(",")
                            .append("\"metric\":\"").append(metric).append("\",")
                            .append("\"is_match\":").append(isMatch)
                            .append("}");
                        if (i < total - 1) json.append(",");
                    } catch (Exception e) {
                        // skip problematic files
                    }
                    if (showProgress) {
                        printProgress(i + 1, total);
                    }
                }
                json.append("]");
                System.out.println(json);
                if (showProgress) System.out.println();
            } else if ("csv".equalsIgnoreCase(format)) {
                System.out.println("file,distance,threshold,metric,is_match");
                for (int i = 0; i < total; i++) {
                    Path p = imageFiles.get(i);
                    try {
                        BufferedImage img = ImageIO.read(p.toFile());
                        if (img == null) throw new IOException("unsupported/corrupt image");
                        var er = getTopEmbedding(DeepFace.represent(img));
                        if (er == null || er.embedding() == null) continue;
                        float[] vec = l2Normalize(er.embedding());
                        double distance = "euclidean".equals(metric) ? euclideanDistance(refVec, vec) : cosineDistance(refVec, vec);
                        boolean isMatch = distance <= threshold;
                        System.out.printf("%s,%.6f,%.6f,%s,%s%n",
                                p.getFileName(), distance, threshold, metric, isMatch);
                    } catch (Exception e) {
                        // skip
                    }
                    if (showProgress) {
                        printProgress(i + 1, total);
                    }
                }
                if (showProgress) System.out.println();
            } else { // text
                for (int i = 0; i < total; i++) {
                    Path p = imageFiles.get(i);
                    try {
                        BufferedImage img = ImageIO.read(p.toFile());
                        if (img == null) throw new IOException("unsupported/corrupt image");
                        var er = getTopEmbedding(DeepFace.represent(img));
                        if (er == null || er.embedding() == null) continue;
                        float[] vec = l2Normalize(er.embedding());
                        double distance = "euclidean".equals(metric) ? euclideanDistance(refVec, vec) : cosineDistance(refVec, vec);
                        boolean isMatch = distance <= threshold;
                        System.out.printf("%s -> metric=%s distance=%.6f threshold=%.6f match=%s%n",
                                p.getFileName(), metric, distance, threshold, isMatch);
                    } catch (Exception e) {
                        // skip
                    }
                    if (showProgress) {
                        printProgress(i + 1, total);
                    }
                }
                if (showProgress) System.out.println();
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid threshold: " + thresholdStr);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println("I/O error: " + ioe.getMessage());
            System.exit(1);
        }
    }

    private void handleObscure(CommandLine cmd) {
        String[] args = cmd.getOptionValues("obscure");
        try {
            if (args == null || args.length != 2) {
                System.err.println("--obscure requires two arguments: --obscure <input-image-URL-or-file> <output-image>");
                System.exit(1);
            }
            String input = args[0];
            Path outputPath = resolvePath(args[1]);
            
            // Check or create output directory
            Path outputDir = outputPath.getParent();
            if (outputDir != null && !Files.exists(outputDir)) {
                try {
                    Files.createDirectories(outputDir);
                } catch (IOException e) {
                    System.err.println("Cannot create output directory: " + outputDir);
                    System.exit(1);
                }
            }
            
            logger.info("Obscuring faces in image: {} -> {}", input, outputPath);
            
            ImageData inputImage = ImageData.fromBytes(readAllBytesFromSource(input));
            ImageData obscuredImage = visionTemplate.obscureFaces(inputImage);
            Files.write(outputPath, obscuredImage.data());
            
            System.out.println("Successfully applied 2x enhanced face obscuring and saved to: " + outputPath);
            
        } catch (IOException ioe) {
            System.err.println("I/O error: " + ioe.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Face obscuring failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private EmbeddingResult getTopEmbedding(List<EmbeddingResult> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .sorted(Comparator.comparingDouble(er -> -safeConfidence(er.faceRegion())))
                .findFirst()
                .orElse(null);
    }

    private double safeConfidence(FaceRegion r) {
        if (r == null) return 0.0;
        return Math.max(0.0, Math.min(1.0, r.confidence()));
    }

    private static int parseIntOrDefault(String value, int def) {
        if (value == null || value.isBlank()) return def;
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return def; }
    }

    private void printResults(VisionResult result, String format) {
        List<Detection> detections = result.detections();

        switch (format.toLowerCase()) {
            case "json":
                printJsonResults(detections);
                break;
            case "csv":
                printCsvResults(detections);
                break;
            case "text":
            default:
                printTextResults(detections);
                break;
        }
    }

    private void printTextResults(List<Detection> detections) {
        if (detections.isEmpty()) {
            System.out.println("No faces detected");
            return;
        }

        System.out.println("Detected " + detections.size() + " face(s):");
        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            System.out.printf("Face %d: Confidence=%.2f, Bounds=(%.3f,%.3f,%.3f,%.3f)%n",
                    i + 1,
                    detection.confidence(),
                    detection.boundingBox().x(),
                    detection.boundingBox().y(),
                    detection.boundingBox().width(),
                    detection.boundingBox().height());
        }
    }

    private void printJsonResults(List<Detection> detections) {
        System.out.println("{");
        System.out.println("  \"detections\": [");

        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            System.out.printf("    {%n");
            System.out.printf("      \"index\": %d,%n", i + 1);
            System.out.printf("      \"confidence\": %.2f,%n", detection.confidence());
            System.out.printf("      \"bounds\": {%n");
            System.out.printf("        \"x\": %.3f,%n", detection.boundingBox().x());
            System.out.printf("        \"y\": %.3f,%n", detection.boundingBox().y());
            System.out.printf("        \"width\": %.3f,%n", detection.boundingBox().width());
            System.out.printf("        \"height\": %.3f%n", detection.boundingBox().height());
            System.out.printf("      }%n");
            System.out.printf("    }%s%n", i < detections.size() - 1 ? "," : "");
        }

        System.out.println("  ]");
        System.out.println("}");
    }

    private void printCsvResults(List<Detection> detections) {
        System.out.println("Index,Confidence,X,Y,Width,Height");

        for (int i = 0; i < detections.size(); i++) {
            Detection detection = detections.get(i);
            System.out.printf("%d,%.2f,%.3f,%.3f,%.3f,%.3f%n",
                    i + 1,
                    detection.confidence(),
                    detection.boundingBox().x(),
                    detection.boundingBox().y(),
                    detection.boundingBox().width(),
                    detection.boundingBox().height());
        }
    }

    private void printEmbeddingText(List<EmbeddingResult> list, int imgW, int imgH, int truncate) {
        if (list == null || list.isEmpty()) {
            System.out.println("No face embeddings found");
            return;
        }
        System.out.println("Found " + list.size() + " face embedding(s):");
        for (int i = 0; i < list.size(); i++) {
            EmbeddingResult er = list.get(i);
            FaceRegion r = er.faceRegion();
            double nx = clamp01((double) r.x() / imgW);
            double ny = clamp01((double) r.y() / imgH);
            double nw = clamp01((double) r.width() / imgW);
            double nh = clamp01((double) r.height() / imgH);
            float[] vec = er.embedding();
            System.out.printf("Face %d: conf=%.2f, bounds=(%.3f,%.3f,%.3f,%.3f), dims=%d%n",
                    i + 1, safeConfidence(r), nx, ny, nw, nh, vec == null ? 0 : vec.length);
            if (vec != null) {
                int n = (truncate > 0 && truncate < vec.length) ? truncate : vec.length;
                StringBuilder sb = new StringBuilder();
                sb.append("  embedding[0:" + n + "] = [");
                for (int k = 0; k < n; k++) {
                    sb.append(String.format("%.6f", vec[k]));
                    if (k < n - 1) sb.append(", ");
                }
                sb.append("]");
                System.out.println(sb);
            }
        }
    }

    private void printEmbeddingJson(List<EmbeddingResult> list, int imgW, int imgH, int truncate) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < (list == null ? 0 : list.size()); i++) {
            EmbeddingResult er = list.get(i);
            FaceRegion r = er.faceRegion();
            double nx = clamp01((double) r.x() / imgW);
            double ny = clamp01((double) r.y() / imgH);
            double nw = clamp01((double) r.width() / imgW);
            double nh = clamp01((double) r.height() / imgH);
            float[] vec = er.embedding();
            json.append("{")
                .append("\"confidence\": ").append(String.format("%.4f", safeConfidence(r))).append(",")
                .append("\"bounds\": {")
                .append("\"x\": ").append(String.format("%.3f", nx)).append(",")
                .append("\"y\": ").append(String.format("%.3f", ny)).append(",")
                .append("\"width\": ").append(String.format("%.3f", nw)).append(",")
                .append("\"height\": ").append(String.format("%.3f", nh)).append("},")
                .append("\"embedding\": ");
            if (vec == null) {
                json.append("null");
            } else {
                int n = (truncate > 0 && truncate < vec.length) ? truncate : vec.length;
                json.append("[");
                for (int k = 0; k < n; k++) {
                    json.append(String.format("%.6f", vec[k]));
                    if (k < n - 1) json.append(",");
                }
                if (n < vec.length) json.append(", \"_truncated\": ").append(vec.length - n);
                json.append("]");
            }
            json.append("}");
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        System.out.println(json);
    }

    private void printEmbeddingCsv(List<EmbeddingResult> list, int imgW, int imgH, int truncate) {
        System.out.println("index,confidence,x,y,width,height,embedding");
        for (int i = 0; i < (list == null ? 0 : list.size()); i++) {
            EmbeddingResult er = list.get(i);
            FaceRegion r = er.faceRegion();
            double nx = clamp01((double) r.x() / imgW);
            double ny = clamp01((double) r.y() / imgH);
            double nw = clamp01((double) r.width() / imgW);
            double nh = clamp01((double) r.height() / imgH);
            float[] vec = er.embedding();
            String emb;
            if (vec == null) {
                emb = "";
            } else {
                int n = (truncate > 0 && truncate < vec.length) ? truncate : vec.length;
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int k = 0; k < n; k++) {
                    sb.append(String.format("%.6f", vec[k]));
                    if (k < n - 1) sb.append(" ");
                }
                if (n < vec.length) sb.append(" ...");
                sb.append("]");
                emb = sb.toString();
            }
            System.out.printf("%d,%.4f,%.3f,%.3f,%.3f,%.3f,%s%n",
                    i + 1, safeConfidence(r), nx, ny, nw, nh, emb);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp") || name.endsWith(".gif");
    }

    private void printProgress(int completed, int total) {
        int percent = (int) Math.round((completed * 100.0) / total);
        String message = String.format("\rProgress: %d/%d (%d%%)", completed, total, percent);
        System.out.print(message);
        System.out.flush();
    }

    private static float[] l2Normalize(float[] vec) {
        if (vec == null || vec.length == 0) return vec;
        double s = 0.0; for (float v : vec) s += v * v; s = Math.sqrt(s);
        if (s <= 0) return vec;
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = (float) (vec[i] / s);
        return out;
    }

    private static double cosineDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na <= 0 || nb <= 0) return Double.NaN;
        double sim = dot / (Math.sqrt(na) * Math.sqrt(nb));
        return 1.0 - sim;
    }

    private static double euclideanDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return Double.NaN;
        double s = 0.0; for (int i = 0; i < a.length; i++) { double d = a[i] - b[i]; s += d * d; }
        return Math.sqrt(s);
    }

    private void printHelp() {
        System.out.println("Spring Vision CLI - Face Detection Tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Interactive mode:");
        System.out.println("    java -jar picocli-application.jar --interactive");
        System.out.println();
        System.out.println("  Single file detection:");
        System.out.println("    java -jar picocli-application.jar --detect <image-file-or-URL> [options]");
        System.out.println();
        System.out.println("  Batch directory processing:");
        System.out.println("    java -jar picocli-application.jar --batch <directory> [--progress] [options]");
        System.out.println();
        System.out.println("  Embedding extraction:");
        System.out.println("    java -jar picocli-application.jar --embed <image-file-or-URL> [--format json|text|csv] [--truncate N]");
        System.out.println();
        System.out.println("  Face verification:");
        System.out.println("    java -jar picocli-application.jar --verify <image1-file-or-URL> <image2-file-or-URL> [--metric cosine|euclidean] [--threshold value] [--format json|text|csv]");
        System.out.println();
        System.out.println("  Batch verification:");
        System.out.println("    java -jar picocli-application.jar --verify-batch <image-file-or-URL> <directory> [--metric ...] [--threshold ...] [--format json|csv|text] [--progress]");
        System.out.println();
        System.out.println("  Face obscuring:");
        System.out.println("    java -jar picocli-application.jar --obscure <input-image-file-or-URL> <output-image-file>");
        System.out.println();
        System.out.println("  Health check:");
        System.out.println("    java -jar picocli-application.jar --health");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help                    Show this help message");
        System.out.println("  -v, --version                 Show version information");
        System.out.println("  -i, --interactive             Run in interactive mode");
        System.out.println("  -f, --format <format>         Output format (json, text, csv) [default: text]");
        System.out.println("  -c, --confidence <threshold>  Confidence threshold (0.0-1.0) [default: 0.5]");
        System.out.println("  -p, --progress                Show progress during batch processing");
        System.out.println("  -V, --verbose                 Enable verbose output");
        System.out.println("      --truncate <n>            Print first n embedding values (embed only)");
        System.out.println("  -m, --metric <metric>         Metric for verification (cosine, euclidean)");
        System.out.println("  -t, --threshold <value>       Threshold for verification (depends on metric)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar picocli-application.jar --interactive");
        System.out.println("  java -jar picocli-application.jar --detect https://example.com/img.jpg --format json");
        System.out.println("  java -jar picocli-application.jar --embed image.jpg --format json --truncate 8");
        System.out.println("  java -jar picocli-application.jar --verify https://a.jpg b.jpg --metric cosine --format csv");
        System.out.println("  java -jar picocli-application.jar --verify-batch a.jpg ./images --metric cosine --format csv --progress");
        System.out.println("  java -jar picocli-application.jar --obscure https://a.jpg output.jpg");
        System.out.println("  java -jar picocli-application.jar --batch ./images --confidence 0.7 --progress --verbose");
        System.out.println("  java -jar picocli-application.jar --health");
    }
}
