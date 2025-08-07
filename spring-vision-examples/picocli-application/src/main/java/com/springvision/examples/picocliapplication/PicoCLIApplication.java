package com.springvision.examples.picocliapplication;

import com.springvision.core.VisionTemplate;
import com.springvision.core.VisionResult;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.exception.VisionProcessingException;
import info.picocli.CommandLine;
import info.picocli.CommandLine.Command;
import info.picocli.CommandLine.Option;
import info.picocli.CommandLine.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * PicoCLI Application for Spring Vision face detection.
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
public class PicoCLIApplication {

    private static final Logger logger = LoggerFactory.getLogger(PicoCLIApplication.class);

    /**
     * Main entry point for the PicoCLI application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        logger.info("Starting Spring Vision PicoCLI Application");
        SpringApplication.run(PicoCLIApplication.class, args);
    }

    /**
     * Main command for face detection operations.
     *
     * <p>This command provides the primary interface for face detection operations,
     * supporting both single file and batch processing modes.</p>
     */
    @Command(
        name = "face-detect",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Perform face detection on image files using Spring Vision",
        subcommands = {
            FaceDetectCommand.class,
            BatchDetectCommand.class,
            HealthCheckCommand.class
        }
    )
    static class FaceDetectApp implements Callable<Integer> {

        @Override
        public Integer call() {
            // Show help if no subcommand is specified
            System.out.println("Spring Vision Face Detection CLI");
            System.out.println("Use --help for more information");
            return 0;
        }
    }

    /**
     * Command for single file face detection.
     *
     * <p>Processes a single image file and outputs detection results in various formats.</p>
     */
    @Command(
        name = "detect",
        description = "Detect faces in a single image file"
    )
    static class FaceDetectCommand implements Callable<Integer> {

        @Parameters(
            index = "0",
            description = "Path to the image file to process",
            arity = "1"
        )
        private String imagePath;

        @Option(
            names = {"-o", "--output"},
            description = "Output format: json, text, csv (default: text)"
        )
        private String outputFormat = "text";

        @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
        )
        private boolean verbose = false;

        @Option(
            names = {"--save-results"},
            description = "Save results to output file"
        )
        private String outputFile;

        private final VisionTemplate visionTemplate;
        private final ApplicationContext applicationContext;

        public FaceDetectCommand(VisionTemplate visionTemplate, ApplicationContext applicationContext) {
            this.visionTemplate = visionTemplate;
            this.applicationContext = applicationContext;
        }

        @Override
        public Integer call() {
            try {
                logger.info("Processing image file: {}", imagePath);

                // Validate input file
                Path path = Paths.get(imagePath);
                if (!Files.exists(path)) {
                    logger.error("Image file not found: {}", imagePath);
                    System.err.println("Error: Image file not found: " + imagePath);
                    return 1;
                }

                if (!Files.isReadable(path)) {
                    logger.error("Image file is not readable: {}", imagePath);
                    System.err.println("Error: Image file is not readable: " + imagePath);
                    return 1;
                }

                // Read image data
                byte[] imageData = Files.readAllBytes(path);
                ImageData image = new ImageData(imageData, path.getFileName().toString());

                // Perform face detection
                logger.info("Starting face detection for image: {}", path.getFileName());
                VisionResult result = visionTemplate.detectFaces(image);

                // Process results
                List<Detection> detections = result.getDetections();

                if (verbose) {
                    logger.info("Detection completed. Found {} faces", detections.size());
                }

                // Output results
                String output = formatResults(detections, outputFormat, path.getFileName().toString());

                if (outputFile != null) {
                    Files.write(Paths.get(outputFile), output.getBytes());
                    logger.info("Results saved to: {}", outputFile);
                    System.out.println("Results saved to: " + outputFile);
                } else {
                    System.out.println(output);
                }

                return 0;

            } catch (VisionProcessingException e) {
                logger.error("Vision processing error: {}", e.getMessage(), e);
                System.err.println("Error: " + e.getMessage());
                return 1;
            } catch (IOException e) {
                logger.error("I/O error: {}", e.getMessage(), e);
                System.err.println("Error: " + e.getMessage());
                return 1;
            } catch (Exception e) {
                logger.error("Unexpected error: {}", e.getMessage(), e);
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }

        /**
         * Format detection results according to the specified output format.
         *
         * @param detections list of detections
         * @param format output format (json, text, csv)
         * @param filename original filename
         * @return formatted output string
         */
        private String formatResults(List<Detection> detections, String format, String filename) {
            return switch (format.toLowerCase()) {
                case "json" -> formatJson(detections, filename);
                case "csv" -> formatCsv(detections, filename);
                case "text", default -> formatText(detections, filename);
            };
        }

        private String formatText(List<Detection> detections, String filename) {
            StringBuilder sb = new StringBuilder();
            sb.append("Face Detection Results for: ").append(filename).append("\n");
            sb.append("=".repeat(50)).append("\n");
            sb.append("Total faces detected: ").append(detections.size()).append("\n\n");

            if (detections.isEmpty()) {
                sb.append("No faces detected in the image.\n");
            } else {
                sb.append("Detected faces:\n");
                for (int i = 0; i < detections.size(); i++) {
                    Detection detection = detections.get(i);
                    sb.append(String.format("  Face %d: Confidence=%.2f%%, BoundingBox=(%d,%d,%d,%d)\n",
                        i + 1,
                        detection.getConfidence() * 100,
                        detection.getBoundingBox().getX(),
                        detection.getBoundingBox().getY(),
                        detection.getBoundingBox().getWidth(),
                        detection.getBoundingBox().getHeight()));
                }
            }

            return sb.toString();
        }

        private String formatJson(List<Detection> detections, String filename) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"filename\": \"").append(filename).append("\",\n");
            sb.append("  \"totalFaces\": ").append(detections.size()).append(",\n");
            sb.append("  \"detections\": [\n");

            for (int i = 0; i < detections.size(); i++) {
                Detection detection = detections.get(i);
                sb.append("    {\n");
                sb.append("      \"index\": ").append(i + 1).append(",\n");
                sb.append("      \"confidence\": ").append(String.format("%.4f", detection.getConfidence())).append(",\n");
                sb.append("      \"boundingBox\": {\n");
                sb.append("        \"x\": ").append(detection.getBoundingBox().getX()).append(",\n");
                sb.append("        \"y\": ").append(detection.getBoundingBox().getY()).append(",\n");
                sb.append("        \"width\": ").append(detection.getBoundingBox().getWidth()).append(",\n");
                sb.append("        \"height\": ").append(detection.getBoundingBox().getHeight()).append("\n");
                sb.append("      }\n");
                sb.append("    }").append(i < detections.size() - 1 ? "," : "").append("\n");
            }

            sb.append("  ]\n");
            sb.append("}\n");
            return sb.toString();
        }

        private String formatCsv(List<Detection> detections, String filename) {
            StringBuilder sb = new StringBuilder();
            sb.append("filename,face_index,confidence,x,y,width,height\n");

            for (int i = 0; i < detections.size(); i++) {
                Detection detection = detections.get(i);
                sb.append(String.format("\"%s\",%d,%.4f,%d,%d,%d,%d\n",
                    filename,
                    i + 1,
                    detection.getConfidence(),
                    detection.getBoundingBox().getX(),
                    detection.getBoundingBox().getY(),
                    detection.getBoundingBox().getWidth(),
                    detection.getBoundingBox().getHeight()));
            }

            return sb.toString();
        }
    }

    /**
     * Command for batch face detection.
     *
     * <p>Processes multiple image files in a directory and outputs batch results.</p>
     */
    @Command(
        name = "batch",
        description = "Detect faces in multiple image files (batch processing)"
    )
    static class BatchDetectCommand implements Callable<Integer> {

        @Parameters(
            index = "0",
            description = "Directory containing image files to process",
            arity = "1"
        )
        private String directoryPath;

        @Option(
            names = {"-p", "--pattern"},
            description = "File pattern to match (default: *.jpg,*.jpeg,*.png)"
        )
        private String filePattern = "*.{jpg,jpeg,png}";

        @Option(
            names = {"-o", "--output"},
            description = "Output format: json, text, csv (default: text)"
        )
        private String outputFormat = "text";

        @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
        )
        private boolean verbose = false;

        @Option(
            names = {"--save-results"},
            description = "Save results to output file"
        )
        private String outputFile;

        private final VisionTemplate visionTemplate;

        public BatchDetectCommand(VisionTemplate visionTemplate) {
            this.visionTemplate = visionTemplate;
        }

        @Override
        public Integer call() {
            // TODO: Implement batch processing
            System.out.println("Batch processing not yet implemented");
            return 0;
        }
    }

    /**
     * Command for health check.
     *
     * <p>Checks the health and status of the vision backend.</p>
     */
    @Command(
        name = "health",
        description = "Check the health status of the vision backend"
    )
    static class HealthCheckCommand implements Callable<Integer> {

        private final VisionTemplate visionTemplate;

        public HealthCheckCommand(VisionTemplate visionTemplate) {
            this.visionTemplate = visionTemplate;
        }

        @Override
        public Integer call() {
            try {
                // TODO: Implement health check
                System.out.println("Health check not yet implemented");
                return 0;
            } catch (Exception e) {
                logger.error("Health check failed: {}", e.getMessage(), e);
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Command line runner that executes the PicoCLI commands.
     */
    @Component
    static class PicoCLICommandRunner implements CommandLineRunner {

        private final VisionTemplate visionTemplate;
        private final ApplicationContext applicationContext;

        public PicoCLICommandRunner(VisionTemplate visionTemplate, ApplicationContext applicationContext) {
            this.visionTemplate = visionTemplate;
            this.applicationContext = applicationContext;
        }

        @Override
        public void run(String... args) throws Exception {
            // Create command instances with dependencies
            FaceDetectCommand detectCommand = new FaceDetectCommand(visionTemplate, applicationContext);
            BatchDetectCommand batchCommand = new BatchDetectCommand(visionTemplate);
            HealthCheckCommand healthCommand = new HealthCheckCommand(visionTemplate);

            // Create command line interface
            CommandLine commandLine = new CommandLine(new FaceDetectApp());
            commandLine.addSubcommand("detect", detectCommand);
            commandLine.addSubcommand("batch", batchCommand);
            commandLine.addSubcommand("health", healthCommand);

            // Execute the command
            int exitCode = commandLine.execute(args);

            // Exit the application
            System.exit(exitCode);
        }
    }
}
