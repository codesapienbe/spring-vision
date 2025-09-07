package com.springvision.example.controller;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.VisionTemplate;
import com.springvision.jpa.dto.FaceLookupOptions;
import com.springvision.jpa.dto.FaceRegistrationOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/faces")
public class VectorFaceLookupController {

    private final VisionTemplate visionTemplate;

    public VectorFaceLookupController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    @PostMapping("/lookup")
    public ResponseEntity<List<java.util.Map<String,Object>>> lookupFaces(@RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            ImageData img = ImageData.fromBytes(data);

            // Extract embeddings and use the first one for lookup
            List<float[]> embeddings = visionTemplate.extractEmbeddings(img);
            if (embeddings == null || embeddings.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            float[] emb = embeddings.get(0);
            FaceLookupOptions options = new FaceLookupOptions();
            options.setModelName("arcface");
            options.setMetric(com.springvision.jpa.enums.SimilarityMetric.COSINE);
            options.setThreshold(0.75);
            options.setLimit(10);

            List<java.util.Map<String,Object>> matches = visionTemplate.lookupFaces(
                emb, 
                options.getModelName(), 
                options.getMetric().name(), 
                options.getThreshold(), 
                options.getLimit(), 
                options.getIncludePersonIds(), 
                options.getExcludePersonIds()
            );

            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerPerson(@RequestParam String personId, @RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            ImageData img = ImageData.fromBytes(data);

            var vr = visionTemplate.detectFaces(img);
            List<Detection> detections = vr.detections();
            if (detections.isEmpty()) return ResponseEntity.badRequest().body("No face detected");

            Detection best = detections.stream().max((a,b)->Double.compare(a.confidence(), b.confidence())).get();

            List<float[]> embeddings = visionTemplate.extractEmbeddings(img);
            if (embeddings == null || embeddings.isEmpty()) return ResponseEntity.badRequest().body("No embedding extracted");

            float[] emb = embeddings.get(0);

            FaceRegistrationOptions options = new FaceRegistrationOptions();
            options.setModelName("arcface");

            String id = visionTemplate.storeFaceEmbedding(personId, emb, options.getModelName(), com.springvision.jpa.util.VectorUtils.calculateImageHash(data), best.confidence(), java.util.Map.of("source","web_upload"));

            return ResponseEntity.ok(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to register face: " + e.getMessage());
        }
    }
} 