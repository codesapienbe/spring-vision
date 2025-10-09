package com.springvision.mcp;

import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.Detection;
import com.springvision.core.BoundingBox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisionController.class)
public class VisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisionTemplate visionTemplate;

    @Test
    public void testHealth() throws Exception {
        mockMvc.perform(get("/api/vision/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Spring Vision MCP Server"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    public void testDetectObjects_Face() throws Exception {
        // Mock the VisionTemplate
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 100);
        when(visionTemplate.detectFaces(any(ImageData.class))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile)
                        .param("detectionType", "FACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjects_Object() throws Exception {
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.OBJECT,
                List.of(new Detection("car", 0.8, new BoundingBox(0.2, 0.2, 0.6, 0.6), Map.of())), 0.8, 150);
        when(visionTemplate.detectObjects(any(ImageData.class))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile)
                        .param("detectionType", "OBJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testExtractText() throws Exception {
        Detection textDetection = new Detection("text", 0.95, new BoundingBox(0.0, 0.0, 1.0, 0.1), Map.of("text", "Hello World"));
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.TEXT, List.of(textDetection), 0.95, 200);
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.TEXT))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/ocr")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello World"));
    }

    @Test
    public void testRecognizeFaces() throws Exception {
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 120);
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.FACE))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/faces")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjectsBase64() throws Exception {
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 130);
        when(visionTemplate.detectFaces(any(ImageData.class))).thenReturn(mockResult);

        String base64Image = java.util.Base64.getEncoder().encodeToString("test image content".getBytes());
        String requestBody = "{\"image\":\"" + base64Image + "\"}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjectsBase64_InvalidBase64() throws Exception {
        String requestBody = "{\"image\":\"invalid base64\"}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid base64 image data"));
    }

    @Test
    public void testDetectObjectsBase64_MissingImage() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing 'image' field in request body"));
    }

    @Test
    public void testDetectObjects_DefaultDetectionType() throws Exception {
        // Test default detection type (FACE) when no param provided
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 100);
        when(visionTemplate.detectFaces(any(ImageData.class))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjects_InvalidDetectionType() throws Exception {
        // Test invalid detection type defaults to FACE
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 100);
        when(visionTemplate.detectFaces(any(ImageData.class))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile)
                        .param("detectionType", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjectsBase64_Object() throws Exception {
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.OBJECT,
                List.of(new Detection("car", 0.8, new BoundingBox(0.2, 0.2, 0.6, 0.6), Map.of())), 0.8, 150);
        when(visionTemplate.detectObjects(any(ImageData.class))).thenReturn(mockResult);

        String base64Image = java.util.Base64.getEncoder().encodeToString("test image content".getBytes());
        String requestBody = "{\"image\":\"" + base64Image + "\"}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .param("detectionType", "OBJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testDetectObjectsBase64_DefaultDetectionType() throws Exception {
        // Test default detection type for base64 endpoint
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE,
                List.of(new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of())), 0.9, 130);
        when(visionTemplate.detectFaces(any(ImageData.class))).thenReturn(mockResult);

        String base64Image = java.util.Base64.getEncoder().encodeToString("test image content".getBytes());
        String requestBody = "{\"image\":\"" + base64Image + "\"}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    public void testExtractText_MultipleDetections() throws Exception {
        Detection textDetection1 = new Detection("text", 0.95, new BoundingBox(0.0, 0.0, 1.0, 0.1), Map.of("text", "Hello"));
        Detection textDetection2 = new Detection("text", 0.90, new BoundingBox(0.0, 0.1, 1.0, 0.2), Map.of("text", "World"));
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.TEXT, List.of(textDetection1, textDetection2), 0.925, 200);
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.TEXT))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/ocr")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello World"));
    }

    @Test
    public void testRecognizeFaces_MultipleFaces() throws Exception {
        Detection face1 = new Detection("face", 0.9, new BoundingBox(0.1, 0.1, 0.5, 0.5), Map.of());
        Detection face2 = new Detection("face", 0.85, new BoundingBox(0.6, 0.1, 1.0, 0.5), Map.of());
        VisionResult mockResult = VisionResult.of(com.springvision.core.DetectionType.FACE, List.of(face1, face2), 0.875, 120);
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.FACE))).thenReturn(mockResult);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/faces")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    public void testDetectObjects_IOException() throws Exception {
        // Mock MultipartFile to throw IOException
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile)
                        .param("detectionType", "FACE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to process image: Simulated IO error"));
    }

    @Test
    public void testExtractText_IOException() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        mockMvc.perform(multipart("/api/vision/ocr")
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to process image: Simulated IO error"));
    }

    @Test
    public void testRecognizeFaces_IOException() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes()) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        mockMvc.perform(multipart("/api/vision/faces")
                        .file(imageFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to process image: Simulated IO error"));
    }

    @Test
    public void testDetectObjects_GeneralException() throws Exception {
        when(visionTemplate.detectFaces(any(ImageData.class))).thenThrow(new RuntimeException("Vision processing failed"));

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/detect")
                        .file(imageFile)
                        .param("detectionType", "FACE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Detection failed: Vision processing failed"));
    }

    @Test
    public void testExtractText_GeneralException() throws Exception {
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.TEXT))).thenThrow(new RuntimeException("OCR processing failed"));

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/ocr")
                        .file(imageFile))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("OCR failed: OCR processing failed"));
    }

    @Test
    public void testRecognizeFaces_GeneralException() throws Exception {
        when(visionTemplate.detect(any(ImageData.class), eq(com.springvision.core.DetectionType.FACE))).thenThrow(new RuntimeException("Face recognition failed"));

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/vision/faces")
                        .file(imageFile))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Face recognition failed: Face recognition failed"));
    }

    @Test
    public void testDetectObjectsBase64_GeneralException() throws Exception {
        when(visionTemplate.detectFaces(any(ImageData.class))).thenThrow(new RuntimeException("Vision processing failed"));

        String base64Image = java.util.Base64.getEncoder().encodeToString("test image content".getBytes());
        String requestBody = "{\"image\":\"" + base64Image + "\"}";

        mockMvc.perform(post("/api/vision/detect/base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Detection failed: Vision processing failed"));
    }
}
