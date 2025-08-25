package com.springvision.examples.vaadinapplication.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.Slider;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
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
import java.util.UUID;

/**
 * Main view for the Vaadin face detection application.
 *
 * <p>This view provides a user interface for uploading images and performing face detection
 * using the Spring Vision framework. It includes file upload, image preview with overlay,
 * result display, and error handling.</p>
 */
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

    private final Slider confidenceSlider = new Slider(0, 100, 0);

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
        Paragraph label = new Paragraph("Min confidence (%):");
        label.getStyle().set("margin", "0 0.5rem 0 0");
        confidenceSlider.setWidth("240px");
        confidenceSlider.setValue(0.0);
        confidenceSlider.setStep(1.0);
        controls.add(label, confidenceSlider);
        add(controls);
    }

    private void setupUploadArea() {
        VerticalLayout uploadContainer = new VerticalLayout();
        uploadContainer.setAlignItems(Alignment.CENTER);
        uploadContainer.setSpacing(true);
        uploadContainer.getStyle().set("padding", "2rem");
        uploadContainer.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        uploadContainer.getStyle().set("border-radius", "0.5rem");

        H3 uploadTitle = new H3("Select an image to detect faces");
        uploadTitle.getStyle().set("text-align", "center");
        uploadContainer.add(uploadTitle);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp");
        upload.setMaxFileSize(50 * 1024 * 1024); // 50MB
        upload.getStyle().set("margin", "1rem");

        Button detectButton = new Button("Detect Faces");
        detectButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        detectButton.getStyle().set("color", "white");
        detectButton.setEnabled(false);

        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                if (in != null) {
                    lastUploadedImageBytes = in.readAllBytes();
                    // Show preview
                    showPreview(lastUploadedImageBytes, event.getFileName());
                    detectButton.setEnabled(true);
                    Notification.show("File uploaded successfully: " + event.getFileName());
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

        uploadContainer.add(upload, detectButton);
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
            double thr = Math.max(0.0, Math.min(1.0, confidenceSlider.getValue() / 100.0));
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
            box.getStyle().set("border", "2px solid #e53935");
            box.getStyle().set("box-sizing", "border-box");
            previewWrapper.add(box);
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
}
