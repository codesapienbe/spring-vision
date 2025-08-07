package com.springvision.examples.vaadinapplication.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.Json;
import elemental.json.JsonObject;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Main view for the Vaadin face detection application.
 *
 * <p>This view provides a user interface for uploading images and performing face detection
 * using the Spring Vision framework. It includes file upload, result display, and error handling.</p>
 *
 * <p>The interface follows Vaadin design patterns and provides a modern, responsive user experience.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
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

    /**
     * Constructs the main view with all UI components.
     */
    public MainView() {
        setupLayout();
        setupHeader();
        setupUploadArea();
        setupResultsArea();
        setupStatusArea();
        checkHealthStatus();
    }

    /**
     * Sets up the main layout with proper spacing and styling.
     */
    private void setupLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        addClassName(LumoUtility.Background.CONTRAST_5);
    }

    /**
     * Sets up the application header.
     */
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

    /**
     * Sets up the file upload area with drag-and-drop support.
     */
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
            detectButton.setEnabled(true);
            Notification.show("File uploaded successfully: " + event.getFileName());
        });

        upload.addFileRejectedListener(event -> {
            Notification.show("File rejected: " + event.getErrorMessage());
        });

        detectButton.addClickListener(e -> {
            if (buffer.getInputStream() != null) {
                performFaceDetection(buffer.getInputStream(), buffer.getFileName());
            }
        });

        uploadContainer.add(upload, detectButton);
        add(uploadContainer);
    }

    /**
     * Sets up the results display area.
     */
    private void setupResultsArea() {
        resultsContainer.getStyle().set("margin-top", "2rem");
        resultsContainer.getStyle().set("padding", "1rem");
        resultsContainer.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        resultsContainer.getStyle().set("border-radius", "0.5rem");
        resultsContainer.setVisible(false);
        add(resultsContainer);
    }

    /**
     * Sets up the status display area.
     */
    private void setupStatusArea() {
        statusContainer.getStyle().set("margin-top", "1rem");
        statusContainer.getStyle().set("padding", "0.5rem");
        statusContainer.getStyle().set("background-color", "var(--lumo-success-color-10pct)");
        statusContainer.getStyle().set("border-radius", "0.25rem");
        add(statusContainer);
    }

    /**
     * Checks the health status of the vision backend.
     */
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

    /**
     * Performs face detection on the uploaded image.
     *
     * @param inputStream the image input stream
     * @param fileName the name of the uploaded file
     */
    private void performFaceDetection(InputStream inputStream, String fileName) {
        try {
            // Read the image data
            byte[] imageData = inputStream.readAllBytes();

            // Log request details for debugging
            System.out.println("Performing face detection for file: " + fileName);
            System.out.println("Image data size: " + imageData.length + " bytes");
            System.out.println("Request URL: " + FACE_DETECTION_ENDPOINT);

            // Create multipart request with proper binary data encoding
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().substring(0, 8);
            byte[] multipartBody = createMultipartBody(imageData, fileName, boundary);

            System.out.println("Multipart body size: " + multipartBody.length + " bytes");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(FACE_DETECTION_ENDPOINT))
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

    /**
     * Creates a multipart form body for file upload with proper binary data encoding.
     *
     * @param imageData the image data
     * @param fileName the file name
     * @param boundary the multipart boundary
     * @return the multipart body as byte array
     */
    private byte[] createMultipartBody(byte[] imageData, String fileName, String boundary) {
        try {
            // Determine content type based on file extension
            String contentType = "image/jpeg"; // default
            if (fileName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            } else if (fileName.toLowerCase().endsWith(".bmp")) {
                contentType = "image/bmp";
            } else if (fileName.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            }

            // Build multipart body
            StringBuilder header = new StringBuilder();
            header.append("--").append(boundary).append("\r\n");
            header.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            header.append("Content-Type: ").append(contentType).append("\r\n\r\n");

            String footer = "\r\n--" + boundary + "--\r\n";

            // Convert header and footer to bytes
            byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);

            // Combine all parts
            byte[] multipartBody = new byte[headerBytes.length + imageData.length + footerBytes.length];

            System.arraycopy(headerBytes, 0, multipartBody, 0, headerBytes.length);
            System.arraycopy(imageData, 0, multipartBody, headerBytes.length, imageData.length);
            System.arraycopy(footerBytes, 0, multipartBody, headerBytes.length + imageData.length, footerBytes.length);

            return multipartBody;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create multipart body: " + e.getMessage(), e);
        }
    }

    /**
     * Displays the face detection results.
     *
     * @param jsonResponse the JSON response from the API
     */
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

            if (detectionCount > 0) {
                displayDetections(json);
            } else {
                Paragraph noFaces = new Paragraph("No faces detected in the uploaded image.");
                noFaces.getStyle().set("text-align", "center");
                noFaces.getStyle().set("color", "var(--lumo-secondary-text-color)");
                resultsContainer.add(noFaces);
            }

        } catch (Exception e) {
            displayError("Error parsing results: " + e.getMessage());
        }
    }

    /**
     * Displays individual detection details.
     *
     * @param json the JSON response containing detections
     */
    private void displayDetections(JsonObject json) {
        if (json.hasKey("detections")) {
            // In a real implementation, you would iterate through the detections array
            // and display each detection with its bounding box and confidence
            Paragraph detectionsInfo = new Paragraph("Detection details would be displayed here.");
            detectionsInfo.getStyle().set("text-align", "center");
            detectionsInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
            resultsContainer.add(detectionsInfo);
        }
    }

    /**
     * Displays an error message.
     *
     * @param errorMessage the error message to display
     */
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
    }

    /**
     * Updates the status display.
     *
     * @param message the status message
     * @param type the status type (success, warning, error)
     */
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
}
