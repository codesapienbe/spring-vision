package com.springvision.examples.vaadinapplication.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Main view for the Vaadin face detection application.
 *
 * <p>This view provides a user interface for uploading images and performing face detection
 * using the Spring Vision framework. It includes file upload, image preview with overlay,
 * result display, and error handling.</p>
 */
@Push
@Route("")
public class MainView extends VerticalLayout {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String API_BASE_URL = BASE_URL + "/api/vision";
    private static final String FACE_DETECTION_ENDPOINT = API_BASE_URL + "/detect/faces";
    private static final String HEALTH_ENDPOINT = API_BASE_URL + "/health";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Div resultsContainer = new Div();
    private final Div statusContainer = new Div();

    // Preview/overlay components
    private final Div previewWrapper = new Div();
    private final Image previewImage = new Image();
    private byte[] lastUploadedImageBytes;

    private final IntegerField confidenceSlider = new IntegerField("Min confidence (%)");

    // Add missing UI component declarations
    private final Checkbox showLabelsCheckbox = new Checkbox("Show labels", true);
    private final Select<String> boxColorSelect = new Select<>();
    private final IntegerField boxLineWidth = new IntegerField("Line width");

    // Export state and links
    private String lastResultsJson;
    private String lastResultsCsv;
    private Anchor jsonDownloadLink;
    private Anchor csvDownloadLink;

    // Batch detection state
    private final List<UploadedItem> batchFiles = new ArrayList<>();
    private Button batchDetectButton;
    private ProgressBar batchProgressBar;
    private Paragraph batchProgressLabel;

    /**
     * Constructs the main view with all UI components.
     */
    public MainView() {
        setupLayout();
        setupHeader();
        setupControls();
        setupUploadArea();
        setupPreviewArea();
        setupResultsArea();
        setupStatusArea();
        checkHealthStatus();
    }

    private void setupLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        addClassName(LumoUtility.Background.CONTRAST_5);
    }

    private void setupHeader() {
        H1 header = new H1("Spring Vision - Vaadin Face Detection");
        header.getStyle().set("text-align", "center");
        header.getStyle().set("font-size", "2rem");
        header.getStyle().set("font-weight", "bold");
        add(header);

        Paragraph description = new Paragraph(
            "Upload an image to detect faces using the Spring Vision framework with Vaadin interface."
        );
        description.getStyle().set("text-align", "center");
        description.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(description);
    }

    private void setupControls() {
        HorizontalLayout controls = new HorizontalLayout();
        controls.setAlignItems(Alignment.CENTER);
        controls.getStyle().set("margin-top", "0.5rem");
        Paragraph label = new Paragraph();
        label.getStyle().set("margin", "0 0.5rem 0 0");
        confidenceSlider.setMin(0);
        confidenceSlider.setMax(100);
        confidenceSlider.setStepButtonsVisible(true);
        confidenceSlider.setValue(0);
        confidenceSlider.setWidth("160px");
        controls.setWidth("100%");
        controls.getStyle().set("flex-wrap", "wrap");
        boxColorSelect.setLabel("Box color");
boxColorSelect.setItems("red", "green", "blue");
boxColorSelect.setValue("red");
boxLineWidth.setMin(1);
boxLineWidth.setMax(6);
boxLineWidth.setValue(2);
boxLineWidth.setWidth("120px");
controls.add(confidenceSlider, showLabelsCheckbox, boxColorSelect, boxLineWidth);
        add(controls);
    }

    private void setupUploadArea() {
        VerticalLayout uploadContainer = new VerticalLayout();
        uploadContainer.setAlignItems(Alignment.CENTER);
        uploadContainer.setSpacing(true);
        uploadContainer.getStyle().set("padding", "2rem");
        uploadContainer.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        uploadContainer.getStyle().set("border-radius", "0.5rem");

        H3 uploadTitle = new H3("Select image(s) to detect faces");
        uploadTitle.getStyle().set("text-align", "center");
        uploadContainer.add(uploadTitle);

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp");
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.getStyle().set("margin", "1rem");

        Button detectButton = new Button("Detect Faces");
        detectButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        detectButton.getStyle().set("color", "white");
        detectButton.setEnabled(false);

        batchDetectButton = new Button("Batch Detect");
        batchDetectButton.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
        batchDetectButton.setEnabled(false);

        batchProgressBar = new ProgressBar(0, 1, 0);
        batchProgressBar.setWidth("320px");
        batchProgressBar.setVisible(false);
        batchProgressLabel = new Paragraph("0/0");
        batchProgressLabel.getStyle().set("margin", "0");
        batchProgressLabel.setVisible(false);

        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream(event.getFileName())) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    lastUploadedImageBytes = bytes;
                    batchFiles.add(new UploadedItem(event.getFileName(), bytes));
                    // Show preview of last uploaded
                    showPreview(lastUploadedImageBytes, event.getFileName());
                    detectButton.setEnabled(true);
                    batchDetectButton.setEnabled(true);
                    Notification.show("File uploaded: " + event.getFileName());
                }
            } catch (Exception ex) {
                Notification.show("Failed to read uploaded file: " + ex.getMessage());
            }
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("File rejected: " + event.getErrorMessage());
        });

        detectButton.addClickListener(e -> {
            if (lastUploadedImageBytes != null && lastUploadedImageBytes.length > 0) {
                performFaceDetection(lastUploadedImageBytes, "uploaded-image.jpg");
            } else {
                Notification.show("Please upload an image first");
            }
        });

        batchDetectButton.addClickListener(e -> performBatchDetection());

        HorizontalLayout buttons = new HorizontalLayout(detectButton, batchDetectButton);
        HorizontalLayout progress = new HorizontalLayout(batchProgressBar, batchProgressLabel);
        uploadContainer.add(upload, buttons, progress);
        add(uploadContainer);
    }

    private void setupPreviewArea() {
        previewWrapper.getStyle().set("position", "relative");
        previewWrapper.getStyle().set("display", "inline-block");
        previewWrapper.getStyle().set("max-width", "800px");
        previewWrapper.getStyle().set("margin-top", "1rem");
        previewWrapper.getStyle().set("border", "1px solid var(--lumo-contrast-30pct)");
        previewWrapper.getStyle().set("border-radius", "4px");
        previewWrapper.setVisible(false);

        previewImage.getStyle().set("max-width", "100%");
        previewImage.getStyle().set("height", "auto");
        previewWrapper.add(previewImage);
        add(previewWrapper);
    }

    private void showPreview(byte[] imageBytes, String fileName) {
        // Clear previous overlays
        clearOverlays();
        StreamResource resource = new StreamResource(fileName == null ? "image.jpg" : fileName,
            () -> new ByteArrayInputStream(imageBytes));
        previewImage.setSrc(resource);
        previewWrapper.setVisible(true);
    }

    private void setupResultsArea() {
        resultsContainer.getStyle().set("margin-top", "1rem");
        resultsContainer.getStyle().set("padding", "1rem");
        resultsContainer.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        resultsContainer.getStyle().set("border-radius", "0.5rem");
        resultsContainer.setVisible(false);

        // Initialize export links (hidden until results available)
        jsonDownloadLink = new Anchor();
        jsonDownloadLink.setText("Download JSON");
        jsonDownloadLink.getElement().setAttribute("download", true);
        jsonDownloadLink.getStyle().set("margin-right", "0.5rem");
        jsonDownloadLink.setVisible(false);

        csvDownloadLink = new Anchor();
        csvDownloadLink.setText("Download CSV");
        csvDownloadLink.getElement().setAttribute("download", true);
        csvDownloadLink.setVisible(false);

        HorizontalLayout exports = new HorizontalLayout(jsonDownloadLink, csvDownloadLink);
        exports.setVisible(false);
        exports.setId("exportButtons");
        resultsContainer.add(exports);
        add(resultsContainer);
    }

    private void setupStatusArea() {
        statusContainer.getStyle().set("margin-top", "1rem");
        statusContainer.getStyle().set("padding", "0.5rem");
        statusContainer.getStyle().set("background-color", "var(--lumo-success-color-10pct)");
        statusContainer.getStyle().set("border-radius", "0.25rem");
        add(statusContainer);
    }

    private void checkHealthStatus() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(HEALTH_ENDPOINT))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                updateStatus("✅ Application Status: Vaadin Application running successfully on port 8080", "success");
            } else {
                updateStatus("⚠️ Application Status: Health check failed - Status: " + response.statusCode(), "warning");
            }
        } catch (Exception e) {
            updateStatus("❌ Application Status: Unable to connect to backend - " + e.getMessage(), "error");
        }
    }

    private void performFaceDetection(byte[] imageData, String fileName) {
        try {
            System.out.println("Performing face detection for file: " + fileName);
            System.out.println("Image data size: " + imageData.length + " bytes");
            System.out.println("Request URL: " + FACE_DETECTION_ENDPOINT);

            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().substring(0, 8);
            byte[] multipartBody = createMultipartBody(imageData, fileName, boundary);
            double thr = Math.max(0.0, Math.min(1.0, (confidenceSlider.getValue() == null ? 0 : confidenceSlider.getValue()) / 100.0));
            String url = FACE_DETECTION_ENDPOINT + "?minConfidence=" + URLEncoder.encode(String.valueOf(thr), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                displayResults(response.body());
            } else {
                displayError("Detection failed (Status " + response.statusCode() + "): " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Exception during face detection: " + e.getMessage());
            e.printStackTrace();
            displayError("Error during face detection: " + e.getMessage());
        }
    }

    private void performBatchDetection() {
        if (batchFiles.isEmpty()) {
            Notification.show("Please upload one or more images first");
            return;
        }
        resultsContainer.removeAll();
        resultsContainer.setVisible(true);
        H3 title = new H3("Batch Detection Results");
        title.getStyle().set("text-align", "center");
        resultsContainer.add(title);

        batchProgressBar.setVisible(true);
        batchProgressLabel.setVisible(true);

        final List<String> perFileSummaries = new ArrayList<>();
        final int totalFiles = batchFiles.size();
        final var ui = getUI().orElse(null);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        CompletableFuture.runAsync(() -> {
            int totalDetections = 0;
            for (int i = 0; i < totalFiles; i++) {
                UploadedItem item = batchFiles.get(i);
                try {
                    String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().substring(0, 8);
                    byte[] multipartBody = createMultipartBody(item.bytes(), item.fileName(), boundary);
                    double thr = Math.max(0.0, Math.min(1.0, (confidenceSlider.getValue() == null ? 0 : confidenceSlider.getValue()) / 100.0));
                    String url = FACE_DETECTION_ENDPOINT + "?minConfidence=" + URLEncoder.encode(String.valueOf(thr), StandardCharsets.UTF_8);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                        .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    int count = 0;
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = Json.parse(response.body());
                            if (json.hasKey("detections")) {
                                JsonArray arr = json.getArray("detections");
                                count = arr != null ? arr.length() : 0;
                            } else if (json.hasKey("detectionCount")) {
                                count = (int) json.getNumber("detectionCount");
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    totalDetections += count;
                    perFileSummaries.add(item.fileName() + ": " + count + " face(s)");
                } catch (Exception ex) {
                    perFileSummaries.add(item.fileName() + ": 0 face(s)");
                }

                double progress = (i + 1) / (double) totalFiles;
                if (ui != null) {
                    final int currentIndex = i + 1;
                    final int total = totalFiles;
                    ui.access(() -> {
                        batchProgressBar.setValue(progress);
                        batchProgressLabel.setText(currentIndex + "/" + total);
                    });
                }
            }

            // finalize UI
            if (ui != null) {
                int finalTotal = totalDetections;
                ui.access(() -> {
                    Paragraph summary = new Paragraph("Processed " + totalFiles + " file(s), total faces: " + finalTotal);
                    summary.getStyle().set("text-align", "center");
                    resultsContainer.add(summary);
                    for (String line : perFileSummaries) {
                        resultsContainer.add(new Paragraph(line));
                    }
                    StringBuilder json = new StringBuilder();
                    json.append("{\"batch\":true,\"totalFiles\":").append(totalFiles)
                        .append(",\"totalDetections\":").append(finalTotal)
                        .append(",\"items\":[");
                    for (int i = 0; i < batchFiles.size(); i++) {
                        UploadedItem it = batchFiles.get(i);
                        String line = perFileSummaries.get(i);
                        int faces = 0;
                        try { faces = Integer.parseInt(line.replaceAll("[^0-9]", "").trim()); } catch (Exception ignore) {}
                        json.append("{\"file\":\"").append(it.fileName().replace("\"", ""))
                            .append("\",\"count\":").append(faces).append("}");
                        if (i < batchFiles.size() - 1) json.append(',');
                    }
                    json.append("]}");
                    lastResultsJson = json.toString();
                    lastResultsCsv = generateBatchCsv(perFileSummaries);

                    HorizontalLayout exports = new HorizontalLayout();
                    exports.setId("exportButtons");
                    jsonDownloadLink = new Anchor(new StreamResource("batch-results.json", () -> new ByteArrayInputStream(lastResultsJson.getBytes(StandardCharsets.UTF_8))), "Download JSON");
                    jsonDownloadLink.getElement().setAttribute("download", true);
                    csvDownloadLink = new Anchor(new StreamResource("batch-results.csv", () -> new ByteArrayInputStream(lastResultsCsv.getBytes(StandardCharsets.UTF_8))), "Download CSV");
                    csvDownloadLink.getElement().setAttribute("download", true);
                    exports.add(jsonDownloadLink, csvDownloadLink);
                    resultsContainer.add(exports);
                });
            }
        }, executor);
    }

    private byte[] createMultipartBody(byte[] imageData, String fileName, String boundary) {
        try {
            String contentType = "image/jpeg"; // default
            if (fileName != null) {
                String lower = fileName.toLowerCase();
                if (lower.endsWith(".png")) contentType = "image/png";
                else if (lower.endsWith(".gif")) contentType = "image/gif";
                else if (lower.endsWith(".bmp")) contentType = "image/bmp";
                else if (lower.endsWith(".webp")) contentType = "image/webp";
            }

            StringBuilder header = new StringBuilder();
            header.append("--").append(boundary).append("\r\n");
            header.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName == null ? "image.jpg" : fileName).append("\"\r\n");
            header.append("Content-Type: ").append(contentType).append("\r\n\r\n");

            String footer = "\r\n--" + boundary + "--\r\n";

            byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

            byte[] multipartBody = new byte[headerBytes.length + imageData.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
            System.arraycopy(imageData, 0, multipartBody, headerBytes.length, imageData.length);
            System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + imageData.length, footerBytes.length);
            return multipartBody;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create multipart body: " + e.getMessage(), e);
        }
    }

    private void displayResults(String jsonResponse) {
        try {
            JsonObject json = Json.parse(jsonResponse);
            resultsContainer.removeAll();
            resultsContainer.setVisible(true);

            H3 resultsTitle = new H3("Detection Results");
            resultsTitle.getStyle().set("text-align", "center");
            resultsContainer.add(resultsTitle);

            if (json.hasKey("error")) {
                displayError(json.getString("error"));
                return;
            }

            int detectionCount = (int) json.getNumber("detectionCount");
            double averageConfidence = json.getNumber("averageConfidence");
            long processingTime = (long) json.getNumber("processingTimeMs");

            Paragraph summary = new Paragraph(String.format(
                "Found %d face(s) with %.1f%% average confidence in %d ms",
                detectionCount, averageConfidence * 100, processingTime
            ));
            summary.getStyle().set("text-align", "center");
            summary.getStyle().set("font-weight", "500");
            resultsContainer.add(summary);

            clearOverlays();
            if (json.hasKey("detections")) {
                renderOverlays(json.getArray("detections"));
            }

            // Update export resources
            this.lastResultsJson = jsonResponse;
            this.lastResultsCsv = generateCsv(json);

            HorizontalLayout exports = new HorizontalLayout();
            exports.setId("exportButtons");
            jsonDownloadLink = new Anchor(new StreamResource("results.json", () -> new ByteArrayInputStream(lastResultsJson.getBytes(StandardCharsets.UTF_8))), "Download JSON");
            jsonDownloadLink.getElement().setAttribute("download", true);
            csvDownloadLink = new Anchor(new StreamResource("results.csv", () -> new ByteArrayInputStream(lastResultsCsv.getBytes(StandardCharsets.UTF_8))), "Download CSV");
            csvDownloadLink.getElement().setAttribute("download", true);
            exports.add(jsonDownloadLink, csvDownloadLink);
            resultsContainer.add(exports);

            if (detectionCount == 0) {
                Paragraph noFaces = new Paragraph("No faces detected in the uploaded image.");
                noFaces.getStyle().set("text-align", "center");
                noFaces.getStyle().set("color", "var(--lumo-secondary-text-color)");
                resultsContainer.add(noFaces);
            }

        } catch (Exception e) {
            displayError("Error parsing results: " + e.getMessage());
        }
    }

    private void renderOverlays(JsonArray detections) {
        if (detections == null) return;
        previewWrapper.getStyle().set("position", "relative");
        for (int i = 0; i < detections.length(); i++) {
            JsonObject det = detections.getObject(i);
            if (det == null || !det.hasKey("boundingBox")) continue;
            JsonObject bb = det.getObject("boundingBox");
            double x = bb.getNumber("x");
            double y = bb.getNumber("y");
            double w = bb.getNumber("width");
            double h = bb.getNumber("height");
            Div box = new Div();
            box.getStyle().set("position", "absolute");
            box.getStyle().set("left", String.format("%.3f%%", x * 100));
            box.getStyle().set("top", String.format("%.3f%%", y * 100));
            box.getStyle().set("width", String.format("%.3f%%", w * 100));
            box.getStyle().set("height", String.format("%.3f%%", h * 100));
            String colorHex = switch (String.valueOf(boxColorSelect.getValue())) {
                case "green" -> "#28a745";
                case "blue" -> "#1e88e5";
                default -> "#e53935"; // red
            };
            int line = Math.max(1, (boxLineWidth.getValue() == null ? 2 : boxLineWidth.getValue()));
            box.getStyle().set("border", line + "px solid " + colorHex);
            box.getStyle().set("box-sizing", "border-box");
            previewWrapper.add(box);

            if (Boolean.TRUE.equals(showLabelsCheckbox.getValue())) {
                Div label = new Div();
                label.setText("Face " + (i + 1));
                label.getStyle().set("position", "absolute");
                label.getStyle().set("left", String.format("%.3f%%", x * 100));
                label.getStyle().set("top", "calc(" + String.format("%.3f%%", y * 100) + " - 18px)");
                label.getStyle().set("padding", "1px 4px");
                label.getStyle().set("background-color", colorHex);
                label.getStyle().set("color", "#ffffff");
                label.getStyle().set("font-size", "12px");
                label.getStyle().set("line-height", "16px");
                previewWrapper.add(label);
            }
        }
    }

    private void displayError(String errorMessage) {
        resultsContainer.removeAll();
        resultsContainer.setVisible(true);
        H3 errorTitle = new H3("Error");
        errorTitle.getStyle().set("text-align", "center");
        errorTitle.getStyle().set("color", "var(--lumo-error-color)");
        resultsContainer.add(errorTitle);
        Paragraph errorText = new Paragraph(errorMessage);
        errorText.getStyle().set("text-align", "center");
        errorText.getStyle().set("color", "var(--lumo-error-color)");
        resultsContainer.add(errorText);
        Notification.show("Error: " + errorMessage);
        clearOverlays();
    }

    private void updateStatus(String message, String type) {
        statusContainer.removeAll();
        Paragraph statusText = new Paragraph(message);
        statusText.getStyle().set("text-align", "center");
        switch (type) {
            case "success":
                statusText.getStyle().set("color", "var(--lumo-success-color)");
                break;
            case "warning":
                statusText.getStyle().set("color", "var(--lumo-warning-color)");
                break;
            case "error":
                statusText.getStyle().set("color", "var(--lumo-error-color)");
                break;
        }
        statusContainer.add(statusText);
    }

    private void clearOverlays() {
        // Remove all overlay boxes while keeping the image
        if (previewWrapper.getElement().getChildCount() > 1) {
            // index 0 is the Image component
            while (previewWrapper.getElement().getChildCount() > 1) {
                previewWrapper.getElement().removeChild(previewWrapper.getElement().getChild(1));
            }
        }
    }

    private String generateCsv(JsonObject root) {
        StringBuilder sb = new StringBuilder();
        sb.append("face_index,confidence,x,y,width,height\n");
        if (root != null && root.hasKey("detections")) {
            JsonArray arr = root.getArray("detections");
            for (int i = 0; i < arr.length(); i++) {
                JsonObject det = arr.getObject(i);
                double conf = det.hasKey("confidence") ? det.getNumber("confidence") : 0.0;
                JsonObject bb = det.hasKey("boundingBox") ? det.getObject("boundingBox") : null;
                double x = bb != null && bb.hasKey("x") ? bb.getNumber("x") : 0.0;
                double y = bb != null && bb.hasKey("y") ? bb.getNumber("y") : 0.0;
                double w = bb != null && bb.hasKey("width") ? bb.getNumber("width") : 0.0;
                double h = bb != null && bb.hasKey("height") ? bb.getNumber("height") : 0.0;
                sb.append(i + 1).append(',')
                  .append(String.format(java.util.Locale.US, "%.6f", conf)).append(',')
                  .append(String.format(java.util.Locale.US, "%.6f", x)).append(',')
                  .append(String.format(java.util.Locale.US, "%.6f", y)).append(',')
                  .append(String.format(java.util.Locale.US, "%.6f", w)).append(',')
                  .append(String.format(java.util.Locale.US, "%.6f", h)).append('\n');
            }
        }
        return sb.toString();
    }

    private String generateBatchCsv(List<String> perFileSummaries) {
        StringBuilder sb = new StringBuilder();
        sb.append("file_name,count\n");
        for (String line : perFileSummaries) {
            int sep = line.lastIndexOf(": ");
            if (sep > 0) {
                String name = line.substring(0, sep);
                String cnt = line.substring(sep + 2).replace(" face(s)", "").trim();
                sb.append(name.replace(",", " ")).append(',').append(cnt).append('\n');
            }
        }
        return sb.toString();
    }

    private record UploadedItem(String fileName, byte[] bytes) {}
}
