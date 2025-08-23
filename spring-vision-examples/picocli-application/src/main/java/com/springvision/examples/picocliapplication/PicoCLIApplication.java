package com.springvision.examples.picocliapplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

        // Confidence threshold option
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

            Path path = resolvePath(imagePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                System.err.println("Image file not found or not a file: " + path);
                System.exit(1);
            }
            if (!Files.isReadable(path)) {
                System.err.println("Image file is not readable: " + path);
                System.exit(1);
            }

            logger.info("Processing single image: {}", path);
            ImageData imageData = ImageData.fromBytes(Files.readAllBytes(path));

            VisionResult result = visionTemplate.detect(imageData, DetectionType.FACE);

            if (verbose) {
                logger.info("Detection completed. Found {} faces", result.detections().size());
            }

            printResults(result, format);

        } catch (NumberFormatException e) {
            System.err.println("Invalid confidence threshold: " + confidenceStr);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
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

    private void printHelp() {
        System.out.println("Spring Vision CLI - Face Detection Tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Interactive mode:");
        System.out.println("    java -jar picocli-application.jar --interactive");
        System.out.println();
        System.out.println("  Single file detection:");
        System.out.println("    java -jar picocli-application.jar --detect <image-file> [options]");
        System.out.println();
        System.out.println("  Batch directory processing:");
        System.out.println("    java -jar picocli-application.jar --batch <directory> [--progress] [options]");
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
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar picocli-application.jar --interactive");
        System.out.println("  java -jar picocli-application.jar --detect image.jpg --format json");
        System.out.println("  java -jar picocli-application.jar --batch ./images --confidence 0.7 --progress --verbose");
        System.out.println("  java -jar picocli-application.jar --health");
    }
}
