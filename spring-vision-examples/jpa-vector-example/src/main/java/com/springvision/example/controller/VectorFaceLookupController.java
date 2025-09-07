package com.springvision.example.controller;

import com.springvision.jpa.dto.FaceLookupOptions;
import com.springvision.jpa.dto.FaceRegistrationOptions;
import com.springvision.jpa.dto.FaceMatchResult;
import com.springvision.jpa.template.VectorEnabledVisionTemplate;
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

    private final VectorEnabledVisionTemplate visionTemplate;

    public VectorFaceLookupController(VectorEnabledVisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    @PostMapping("/lookup")
    public ResponseEntity<List<FaceMatchResult>> lookupFaces(@RequestParam("file") MultipartFile file) {
        try {
            FaceLookupOptions options = new FaceLookupOptions();
            options.setModelName("arcface");
            options.setMetric(com.springvision.jpa.enums.SimilarityMetric.COSINE);
            options.setThreshold(0.75);
            options.setLimit(10);

            List<FaceMatchResult> results = visionTemplate.lookupFaces(file.getBytes(), options);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerPerson(@RequestParam String personId, @RequestParam("file") MultipartFile file) {
        try {
            FaceRegistrationOptions options = new FaceRegistrationOptions();
            options.setModelName("arcface");
            String embeddingId = visionTemplate.registerFace(personId, file.getBytes(), options);
            return ResponseEntity.ok(embeddingId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to register face: " + e.getMessage());
        }
    }
} 