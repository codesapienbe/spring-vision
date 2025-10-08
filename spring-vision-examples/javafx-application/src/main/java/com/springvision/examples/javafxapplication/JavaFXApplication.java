package com.springvision.examples.javafxapplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.springvision.core.config.VisionProperties;
import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.backend.OpenCvVisionBackend;
import com.springvision.core.exception.VisionProcessingException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * JavaFX Application for Spring Vision face detection.
 *
 * <p>This application provides a desktop GUI interface for performing face detection
 * on image files using the Spring Vision framework. It features a modern, user-friendly
 * interface with drag-and-drop support, real-time processing, and visual result display.</p>
 *
 * <p>The application integrates with Spring Boot's autoconfiguration to automatically
 * configure the vision backend and provides a rich desktop experience for face detection tasks.</p>
 *
 * @author Spring Vision Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class JavaFXApplication {

    private static final Logger logger = LoggerFactory.getLogger(JavaFXApplication.class);

    /**
     * JavaFX Application class that handles the GUI.
     */
    public static class FaceDetectionApp extends Application {

        private static volatile VisionTemplate providedVisionTemplate;
        private static volatile VisionProperties providedVisionProperties;

        private VisionTemplate visionTemplate;
        private VisionProperties visionProperties;
        private Stage primaryStage;
        private ImageView imageView;
        private VBox resultArea;
        private ProgressIndicator progressIndicator;
        private Label statusLabel;
        private Button detectButton;
        private Button clearButton;
        private Pane overlayPane;
        private ComboBox<String> backendComboBox;

        private static final int CONNECT_TIMEOUT_MS = 8000;
        private static final int READ_TIMEOUT_MS = 15000;
        private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024; // 50MB

        public static void provideVisionTemplate(VisionTemplate template) {
            providedVisionTemplate = template;
        }

        public static void provideVisionProperties(VisionProperties properties) {
            providedVisionProperties = properties;
        }

        @Override
        public void start(Stage primaryStage) {
            this.primaryStage = primaryStage;
            this.visionTemplate = getVisionTemplate();
            this.visionProperties = providedVisionProperties; // may be null if autoconfig not used

            setupUI();
            setupEventHandlers();
            setupDragAndDrop();

            primaryStage.setTitle("Spring Vision - Face Detection");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();
        }

        /**
         * Get the VisionTemplate from Spring context.
         * This is a simplified approach - in a real application, you'd use proper dependency injection.
         */
        private VisionTemplate getVisionTemplate() {
            if (providedVisionTemplate == null) {
                logger.warn("VisionTemplate was not provided before launching JavaFX UI. Detection will not work.");
            }
            return providedVisionTemplate;
        }

        /**
         * Set up the main user interface.
         */
        private void setupUI() {
            // Main container
            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #f5f5f5;");

            // Top toolbar
            HBox toolbar = createToolbar();
            root.setTop(toolbar);

            // Center content area
            SplitPane contentArea = new SplitPane();
            contentArea.setDividerPositions(0.7);

            // Left side - image display
            VBox imageContainer = createImageContainer();
            contentArea.getItems().add(imageContainer);

            // Right side - results and controls
            VBox controlPanel = createControlPanel();
            contentArea.getItems().add(controlPanel);

            root.setCenter(contentArea);

            // Status bar
            HBox statusBar = createStatusBar();
            root.setBottom(statusBar);

            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setScene(scene);
        }

        /**
         * Create the top toolbar with main controls.
         */
        private HBox createToolbar() {
            HBox toolbar = new HBox(10);
            toolbar.setPadding(new Insets(10));
            toolbar.setAlignment(Pos.CENTER_LEFT);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

            Button openButton = new Button("Open Image");
            openButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: bold;");
            openButton.setOnAction(e -> openImageFile());

            Button openUrlButton = new Button("Open URL");
            openUrlButton.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white; -fx-font-weight: bold;");
            openUrlButton.setOnAction(e -> openImageUrl());

            detectButton = new Button("Detect Faces");
            detectButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;");
            detectButton.setDisable(true);

            clearButton = new Button("Clear");
            clearButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;");
            clearButton.setOnAction(e -> clearResults());

            // Backend selector - only show backends available on the classpath
            backendComboBox = new ComboBox<>();
            java.util.List<String> backendOptions = new java.util.ArrayList<>();
            backendOptions.add("opencv");
            if (classExists("com.springvision.backend.facebytes.FaceBytesVisionBackend")) backendOptions.add("facebytes");
            if (classExists("com.springvision.backend.deepface.DeepFaceVisionBackend")) backendOptions.add("deepface");
            if (classExists("com.springvision.backend.compreface.CompreFaceVisionBackend")) backendOptions.add("compreface");
            backendComboBox.getItems().addAll(backendOptions);
            String currentBackend = visionTemplate != null ? visionTemplate.getBackendId() : backendOptions.get(0);
            backendComboBox.getSelectionModel().select(currentBackend);
            backendComboBox.setOnAction(e -> onBackendChanged());

            toolbar.getChildren().addAll(openButton, openUrlButton, detectButton, clearButton, new Label("Backend:"), backendComboBox);
            return toolbar;
        }

        private void onBackendChanged() {
            String selected = backendComboBox.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isBlank()) {
                return;
            }
            try {
                switchBackend(selected.trim().toLowerCase());
                statusLabel.setText("Backend switched to: " + visionTemplate.getBackendId());
            } catch (Exception ex) {
                logger.error("Failed to switch backend: {}", ex.getMessage(), ex);
                showError("Backend Switch Failed", "Could not switch backend: " + ex.getMessage());
                // Revert selection to current
                backendComboBox.getSelectionModel().select(visionTemplate.getBackendId());
            }
        }

        private void switchBackend(String backendId) throws Exception {
            logger.info("Switching vision backend: {}", Map.of("component", "JavaFXApplication", "backendId", backendId));

            VisionBackend backend;
            switch (backendId) {
                case "facebytes": {
                    try {
                        Class<?> cls = Class.forName("com.springvision.backend.facebytes.FaceBytesVisionBackend");
                        Object inst = cls.getDeclaredConstructor().newInstance();
                        logger.info("FaceBytes backend initialized successfully: {}", Map.of("component", "JavaFXApplication"));
                        backend = (VisionBackend) inst;
                    } catch (ClassNotFoundException cnf) {
                        throw new Exception("FaceBytes backend not available on classpath. Add spring-vision-facebytes module.", cnf);
                    }
                    break;
                }

                case "opencv": {
                    OpenCvVisionBackend ocv = new OpenCvVisionBackend();
                    ocv.initialize();
                    logger.info("OpenCV backend initialized successfully: {}", Map.of("component", "JavaFXApplication"));
                    backend = ocv;
                    break;
                }
                case "deepface": {
                    // Use DeepFace HTTP API (optional module)
                    try {
                        Class<?> cls = Class.forName("com.springvision.backend.deepface.DeepFaceVisionBackend");
                        Object inst = cls.getDeclaredConstructor(String.class).newInstance("http://localhost:5000");
                        try {
                            cls.getMethod("initialize").invoke(inst);
                        } catch (NoSuchMethodException ignored) {
                            // initialize method may not be present; ignore
                        }
                        logger.info("DeepFace backend initialized successfully: {}", Map.of("component", "JavaFXApplication"));
                        backend = (VisionBackend) inst;
                    } catch (ClassNotFoundException cnf) {
                        throw new Exception("DeepFace backend not available on classpath. Add spring-vision-deepface module.", cnf);
                    }
                    break;
                }
                case "compreface": {
                    // Use CompreFace HTTP API (optional module)
                    try {
                        Class<?> cls = Class.forName("com.springvision.backend.compreface.CompreFaceVisionBackend");
                        Object inst = cls.getDeclaredConstructor(String.class).newInstance("http://localhost:8000");
                        try {
                            cls.getMethod("initialize").invoke(inst);
                        } catch (NoSuchMethodException ignored) {
                            // initialize method may not be present; ignore
                        }
                        logger.info("CompreFace backend initialized successfully: {}", Map.of("component", "JavaFXApplication"));
                        backend = (VisionBackend) inst;
                    } catch (ClassNotFoundException cnf) {
                        throw new Exception("CompreFace backend not available on classpath. Add spring-vision-compreface module.", cnf);
                    }
                    break;
                }
                default: {
                    // Fallback to OpenCV if nothing matched -- keep default last
                    OpenCvVisionBackend ocv = new OpenCvVisionBackend();
                    ocv.initialize();
                    logger.info("Fallback to OpenCV backend initialized: {}", Map.of("component", "JavaFXApplication"));
                    backend = ocv;
                    break;
                }
            }
            this.visionTemplate = new VisionTemplate(backend);
            logger.info("Vision template created with backend: {}", Map.of(
                "component", "JavaFXApplication",
                "backendId", backend.getBackendId(),
                "backendName", backend.getDisplayName()
            ));
        }

        // Helper to check presence of optional backend classes on the classpath.
        // Using Class.forName avoids compile-time coupling to optional modules.
        private boolean classExists(String fqcn) {
            if (fqcn == null || fqcn.isBlank()) return false;
            try {
                Class.forName(fqcn);
                return true;
            } catch (Throwable t) {
                // Be defensive: ClassNotFoundException, NoClassDefFoundError, LinkageError, etc.
                return false;
            }
        }

        /**
         * Create the image display container.
         */
        private VBox createImageContainer() {
            VBox container = new VBox(10);
            container.setPadding(new Insets(10));
            container.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            Label titleLabel = new Label("Image Preview");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // Image view with overlay and scroll pane
            imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 3;");

            overlayPane = new Pane();
            overlayPane.setPickOnBounds(false);
            overlayPane.setMouseTransparent(true);

            StackPane imageStack = new StackPane(imageView, overlayPane);
            imageStack.setStyle("-fx-background-color: transparent;");

            // Bind the ImageView size to the container so it fits available space
            imageView.fitWidthProperty().bind(imageStack.widthProperty());
            imageView.fitHeightProperty().bind(imageStack.heightProperty());

            ScrollPane scrollPane = new ScrollPane(imageStack);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: transparent;");

            // Drop zone label
            Label dropLabel = new Label("Drag and drop an image here or click 'Open Image'");
            dropLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
            dropLabel.setAlignment(Pos.CENTER);

            container.getChildren().addAll(titleLabel, scrollPane, dropLabel);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            return container;
        }

        /**
         * Create the control panel for results and settings.
         */
        private VBox createControlPanel() {
            VBox panel = new VBox(10);
            panel.setPadding(new Insets(10));
            panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            Label titleLabel = new Label("Detection Results");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // Progress indicator
            progressIndicator = new ProgressIndicator();
            progressIndicator.setVisible(false);
            progressIndicator.setMaxSize(50, 50);

            // Status label
            statusLabel = new Label("Ready to detect faces");
            statusLabel.setStyle("-fx-text-fill: #666666;");

            // Results area
            resultArea = new VBox(5);
            resultArea.setPadding(new Insets(10));
            resultArea.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 3;");

            ScrollPane resultsScroll = new ScrollPane(resultArea);
            resultsScroll.setFitToWidth(true);
            VBox.setVgrow(resultsScroll, Priority.ALWAYS);

            panel.getChildren().addAll(titleLabel, progressIndicator, statusLabel, resultsScroll);
            return panel;
        }

        /**
         * Create the status bar.
         */
        private HBox createStatusBar() {
            HBox statusBar = new HBox(10);
            statusBar.setPadding(new Insets(5, 10, 5, 10));
            statusBar.setAlignment(Pos.CENTER_LEFT);
            statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");

            Label statusLabel = new Label("Spring Vision Face Detection v1.0.0");
            statusLabel.setStyle("-fx-text-fill: #666666;");

            statusBar.getChildren().add(statusLabel);
            return statusBar;
        }

        /**
         * Set up event handlers for buttons and controls.
         */
        private void setupEventHandlers() {
            detectButton.setOnAction(e -> detectFaces());
        }

        /**
         * Set up drag and drop functionality.
         */
        private void setupDragAndDrop() {
            imageView.setOnDragOver(event -> {
                if (event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                }
                event.consume();
            });

            imageView.setOnDragDropped(event -> {
                var db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    if (!files.isEmpty()) {
                        loadImage(files.get(0));
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        /**
         * Open image file using file chooser.
         */
        private void openImageFile() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Image File");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                loadImage(selectedFile);
            }
        }

        private void openImageUrl() {
            TextInputDialog dialog = new TextInputDialog("https://example.com/image.jpg");
            dialog.setTitle("Open Image from URL");
            dialog.setHeaderText("Enter a public HTTP/HTTPS image URL");
            dialog.setContentText("URL:");
            dialog.showAndWait().ifPresent(url -> {
                try {
                    byte[] bytes = downloadImageBytes(url.trim());
                    loadImageFromBytes(bytes);
                    // Store bytes for detection path
                    imageView.setUserData(bytes);
                    detectButton.setDisable(false);
                    statusLabel.setText("Image loaded from URL");
                    clearResults();
                } catch (IllegalArgumentException iae) {
                    showError("Invalid URL", iae.getMessage());
                } catch (IOException ioe) {
                    showError("Download Failed", "Could not fetch image: " + ioe.getMessage());
                } catch (Exception ex) {
                    showError("Error", ex.getMessage());
                }
            });
        }

        private void loadImageFromBytes(byte[] data) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                Image image = new Image(bais);
                if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new IOException("Unsupported or corrupt image content");
                }
                imageView.setImage(image);
            }
        }

        /**
         * Load and display an image file.
         */
        private void loadImage(File file) {
            try {
                logger.info("Loading image file: {}", file.getAbsolutePath());

                // Load image for display
                Image image = new Image(file.toURI().toString());
                imageView.setImage(image);

                // Store file reference for detection
                imageView.setUserData(file);

                // Enable detect button
                detectButton.setDisable(false);
                statusLabel.setText("Image loaded: " + file.getName());

                // Clear previous results
                clearResults();

            } catch (Exception e) {
                logger.error("Error loading image", Map.of(
                    "component", "JavaFXApplication",
                    "fileName", file.getName(),
                    "fileSize", file.length(),
                    "errorType", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage()
                ), e);
                showError("Error loading image", "Could not load the selected image file: " + e.getMessage());
            }
        }

        /**
         * Perform face detection on the loaded image.
         */
        private void detectFaces() {
            File imageFile = (File) imageView.getUserData();
            if (imageFile == null || !imageFile.exists()) {
                // If not a file, try bytes from URL load
                Object ud = imageView.getUserData();
                if (!(ud instanceof byte[] bytes) || bytes.length == 0) {
                    showError("No Image", "Please load an image first.");
                    return;
                }
            }

            if (visionTemplate == null) {
                showError("Configuration Error", "Vision engine is not initialized. Please restart the application.");
                return;
            }

            // Show progress
            progressIndicator.setVisible(true);
            detectButton.setDisable(true);
            statusLabel.setText("Detecting faces...");
            clearResults();

            // Generate correlation ID for this detection request
            String correlationId = java.util.UUID.randomUUID().toString();

            // Perform detection asynchronously
            CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    logger.info("Starting face detection", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "fileName", imageFile != null ? imageFile.getName() : "<url-bytes>",
                        "fileSize", imageFile != null ? imageFile.length() : (long) ((byte[]) imageView.getUserData()).length,
                        "backendId", visionTemplate.getBackend().getBackendId()
                    ));

                    // Read image data
                    byte[] imageDataBytes;
                    if (imageFile != null && imageFile.exists()) {
                        imageDataBytes = Files.readAllBytes(imageFile.toPath());
                    } else {
                        imageDataBytes = (byte[]) imageView.getUserData();
                    }
                    ImageData image = ImageData.fromBytes(imageDataBytes);

                    // Perform detection
                    com.springvision.core.DetectionQuery query = new com.springvision.core.DetectionQuery.Builder()
                        .type(com.springvision.core.DetectionType.FACE)
                        .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
                        .build();
                    VisionResult result = visionTemplate.detect(image, query);
                    long processingTime = System.currentTimeMillis() - startTime;

                    logger.info("Face detection completed", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "detectionCount", result.detections().size(),
                        "processingTimeMs", processingTime,
                        "backendId", visionTemplate.getBackend().getBackendId()
                    ));
                    return result;

                } catch (VisionProcessingException e) {
                    logger.error("Vision processing error", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "errorType", "VisionProcessingException",
                        "errorMessage", e.getMessage()
                    ), e);
                    throw new RuntimeException("Vision processing failed: " + e.getMessage(), e);
                } catch (IOException e) {
                    logger.error("I/O error during face detection", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "errorType", "IOException",
                        "errorMessage", e.getMessage(),
                        "fileName", imageFile != null ? imageFile.getName() : "<url-bytes>"
                    ), e);
                    throw new RuntimeException("Could not read image: " + e.getMessage(), e);
                } catch (Exception e) {
                    logger.error("Unexpected error during face detection", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "errorType", e.getClass().getSimpleName(),
                        "errorMessage", e.getMessage()
                    ), e);
                    throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
                }
            }).thenAcceptAsync(result -> {
                Platform.runLater(() -> displayResults(result));
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    logger.error("Detection failed in UI thread", Map.of(
                        "component", "JavaFXApplication",
                        "correlationId", correlationId,
                        "errorType", throwable.getClass().getSimpleName(),
                        "errorMessage", throwable.getMessage()
                    ), throwable);
                    showError("Detection Failed", throwable.getMessage());
                    resetUI();
                });
                return null;
            });
        }

        private byte[] downloadImageBytes(String urlString) throws IOException {
            if (!isHttpUrl(urlString)) {
                throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
            }
            URL url = URI.create(urlString).toURL();
            validatePublicHost(url);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "spring-vision-javafx-example/1.0");
            String ct = conn.getContentType();
            if (ct != null && !ct.toLowerCase().startsWith("image/")) {
                throw new IllegalArgumentException("URL does not point to an image content type");
            }
            int contentLength = conn.getContentLength();
            if (contentLength > 0 && contentLength > MAX_DOWNLOAD_BYTES) {
                throw new IllegalArgumentException("Remote content too large");
            }
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int read; int total = 0;
                while ((read = in.read(buf)) != -1) {
                    total += read;
                    if (total > MAX_DOWNLOAD_BYTES) {
                        throw new IllegalArgumentException("Download exceeds size limit");
                    }
                    baos.write(buf, 0, read);
                }
                return baos.toByteArray();
            }
        }

        private boolean isHttpUrl(String s) {
            String v = s == null ? "" : s.trim().toLowerCase();
            return v.startsWith("http://") || v.startsWith("https://");
        }

        private void validatePublicHost(URL url) throws IOException {
            String host = url.getHost();
            if (host == null || host.isBlank()) throw new IOException("Invalid URL host");
            InetAddress[] addrs;
            try {
                addrs = InetAddress.getAllByName(host);
            } catch (Exception e) {
                throw new IOException("Failed to resolve host: " + host);
            }
            for (InetAddress a : addrs) {
                if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                    throw new IOException("Refusing to connect to non-public address: " + a.getHostAddress());
                }
            }
        }

        /**
         * Display detection results in the UI.
         */
        private void displayResults(VisionResult result) {
            List<Detection> detections = result.detections();

            // Update status
            statusLabel.setText(String.format("Detection completed: %d faces found", detections.size()));

            // Display results
            if (detections.isEmpty()) {
                Label noFacesLabel = new Label("No faces detected in the image.");
                noFacesLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                resultArea.getChildren().add(noFacesLabel);
            } else {
                // Add summary
                Label summaryLabel = new Label(String.format("Detected %d face(s):", detections.size()));
                summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                resultArea.getChildren().add(summaryLabel);

                // Add individual detections
                for (int i = 0; i < detections.size(); i++) {
                    Detection detection = detections.get(i);
                    VBox detectionBox = createDetectionBox(i + 1, detection);
                    resultArea.getChildren().add(detectionBox);
                }

                // Draw bounding boxes on image
                drawBoundingBoxes(detections);
            }

            // Reset UI
            progressIndicator.setVisible(false);
            detectButton.setDisable(false);
        }

        /**
         * Create a visual representation of a detection result.
         */
        private VBox createDetectionBox(int index, Detection detection) {
            VBox box = new VBox(5);
            box.setPadding(new Insets(8));
            box.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-radius: 3;");

            Label titleLabel = new Label(String.format("Face %d", index));
            titleLabel.setStyle("-fx-font-weight: bold;");

            Label confidenceLabel = new Label(String.format("Confidence: %.2f%%", detection.confidence() * 100));
            confidenceLabel.setStyle("-fx-text-fill: #1976d2;");

            int pctX = (int) Math.round(detection.boundingBox().x() * 100);
            int pctY = (int) Math.round(detection.boundingBox().y() * 100);
            int pctW = (int) Math.round(detection.boundingBox().width() * 100);
            int pctH = (int) Math.round(detection.boundingBox().height() * 100);
            Label bboxLabel = new Label(String.format("Position: %d%%, %d%%  Size: %d%% × %d%%", pctX, pctY, pctW, pctH));
            bboxLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

            box.getChildren().addAll(titleLabel, confidenceLabel, bboxLabel);
            return box;
        }

        /**
         * Draw bounding boxes on the image view.
         */
        private void drawBoundingBoxes(List<Detection> detections) {
            // Clear existing overlays
            overlayPane.getChildren().clear();

            // Compute displayed image metrics and letterbox offsets
            DisplayMetrics metrics = computeDisplayMetrics();
            if (metrics.displayedWidth <= 0 || metrics.displayedHeight <= 0) {
                return;
            }

            for (Detection detection : detections) {
                var bb = detection.boundingBox();
                double x = metrics.offsetX + (bb.x() * metrics.displayedWidth);
                double y = metrics.offsetY + (bb.y() * metrics.displayedHeight);
                double w = bb.width() * metrics.displayedWidth;
                double h = bb.height() * metrics.displayedHeight;

                Rectangle rect = new Rectangle(x, y, w, h);
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.RED);
                rect.setStrokeWidth(2);
                overlayPane.getChildren().add(rect);
            }
        }

        /**
         * Clear all results and reset the display.
         */
        private void clearResults() {
            resultArea.getChildren().clear();
            overlayPane.getChildren().clear();
            statusLabel.setText("Ready to detect faces");
        }

        /**
         * Calculates displayed image size and letterbox offsets within the overlay container.
         */
        private DisplayMetrics computeDisplayMetrics() {
            Image img = imageView.getImage();
            if (img == null) {
                return new DisplayMetrics(0, 0, 0, 0);
            }

            // Actual displayed size of the ImageView (after preserveRatio scaling)
            double displayedWidth = imageView.getBoundsInParent().getWidth();
            double displayedHeight = imageView.getBoundsInParent().getHeight();

            // Overlay/container size (StackPane layer)
            double containerWidth = overlayPane.getWidth() > 0 ? overlayPane.getWidth() : overlayPane.getBoundsInParent().getWidth();
            double containerHeight = overlayPane.getHeight() > 0 ? overlayPane.getHeight() : overlayPane.getBoundsInParent().getHeight();

            // Compute letterbox offsets to center the image within the container
            double offsetX = Math.max(0, (containerWidth - displayedWidth) / 2.0);
            double offsetY = Math.max(0, (containerHeight - displayedHeight) / 2.0);

            return new DisplayMetrics(displayedWidth, displayedHeight, offsetX, offsetY);
        }

        private record DisplayMetrics(double displayedWidth, double displayedHeight, double offsetX, double offsetY) {
        }

        /**
         * Reset the UI to its initial state.
         */
        private void resetUI() {
            progressIndicator.setVisible(false);
            detectButton.setDisable(false);
            statusLabel.setText("Ready to detect faces");
        }

        /**
         * Show an error dialog.
         */
        private void showError(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    /**
     * Command line runner that launches the JavaFX application.
     */
    @Component
    static class JavaFXCommandRunner implements CommandLineRunner {

        private final VisionTemplate visionTemplate;
        private final ApplicationContext applicationContext;
        private final VisionProperties visionProperties;

        public JavaFXCommandRunner(VisionTemplate visionTemplate, ApplicationContext applicationContext,
                                   ObjectProvider<VisionProperties> visionPropertiesProvider) {
            this.visionTemplate = visionTemplate;
            this.applicationContext = applicationContext;
            this.visionProperties = visionPropertiesProvider.getIfAvailable();
        }

        @Override
        public void run(String... args) throws Exception {
            logger.info("Launching JavaFX application");

            // Provide VisionTemplate and (optional) VisionProperties to JavaFX UI before launching
            FaceDetectionApp.provideVisionTemplate(visionTemplate);
            if (visionProperties != null) {
                FaceDetectionApp.provideVisionProperties(visionProperties);
            }

            Application.launch(FaceDetectionApp.class, args);
        }
    }
}
