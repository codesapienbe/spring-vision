package com.springvision.jpa.template;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import com.springvision.core.exception.VisionProcessingException;
import com.springvision.jpa.dto.FaceLookupOptions;
import com.springvision.jpa.dto.FaceMatchResult;
import com.springvision.jpa.dto.FaceRegistrationOptions;
import com.springvision.jpa.util.VectorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter that exposes convenient register/lookup methods while delegating
 * core work to the provided `VisionTemplate` (which may itself have a VectorService).
 */
@Service
public class VectorEnabledVisionTemplate {

    private final VisionTemplate delegate;

    public VectorEnabledVisionTemplate(VisionTemplate delegate) {
        this.delegate = delegate;
    }

    public List<FaceMatchResult> lookupFaces(byte[] imageData, FaceLookupOptions options) {
        try {
            ImageData img = ImageData.fromBytes(imageData);
            VisionResult vr = delegate.detectFaces(img);
            List<Detection> detections = vr.detections();
            List<float[]> embeddings = delegate.extractEmbeddings(img);

            List<FaceMatchResult> results = new ArrayList<>();
            int n = Math.min(detections.size(), embeddings == null ? 0 : embeddings.size());
            for (int i = 0; i < n; i++) {
                Detection det = detections.get(i);
                float[] emb = embeddings.get(i);
                if (emb == null || emb.length == 0) continue;

                // delegate.lookupFaces expects low-level vector input and returns maps
                java.util.List<java.util.Map<String, Object>> matches = delegate.lookupFaces(
                    emb,
                    options.getModelName(),
                    options.getMetric().name(),
                    options.getThreshold(),
                    options.getLimit(),
                    options.getIncludePersonIds(),
                    options.getExcludePersonIds()
                );

                // Convert map results to SimilaritySearchResult-like DTOs via FaceMatchResult wrapper
                List<com.springvision.jpa.dto.SimilaritySearchResult> converted = matches.stream().map(m -> {
                    String embeddingId = m.getOrDefault("embeddingId", null) == null ? null : m.get("embeddingId").toString();
                    String personId = m.getOrDefault("personId", null) == null ? null : m.get("personId").toString();
                    Double similarity = m.getOrDefault("similarity", null) == null ? 0.0 : ((Number)m.get("similarity")).doubleValue();
                    Double distance = m.getOrDefault("distance", null) == null ? null : ((Number)m.get("distance")).doubleValue();
                    String model = m.getOrDefault("modelName", options.getModelName()) == null ? null : m.get("modelName").toString();
                    java.time.LocalDateTime createdAt = null;
                    if (m.get("createdAt") != null) {
                        try {
                            createdAt = java.time.LocalDateTime.parse(m.get("createdAt").toString());
                        } catch (Exception ignored) {}
                    }
                    Map<String,Object> meta = (Map<String,Object>) m.getOrDefault("metadata", Map.of());
                    return new com.springvision.jpa.dto.SimilaritySearchResult(embeddingId, personId, similarity, distance, model, createdAt, meta);
                }).collect(Collectors.toList());

                results.add(new FaceMatchResult(det, converted, options.getMetric()));
            }

            return results;
        } catch (VisionProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException("Lookup failed", "lookup_failed", "lookup", e);
        }
    }

    public String registerFace(String personId, byte[] imageData, FaceRegistrationOptions options) {
        try {
            ImageData img = ImageData.fromBytes(imageData);
            VisionResult vr = delegate.detectFaces(img);
            List<Detection> detections = vr.detections();

            Detection best = detections.stream()
                .max(Comparator.comparing(Detection::confidence))
                .orElseThrow(() -> new VisionProcessingException("No face detected", "no_face", "register"));

            List<float[]> embeddings = delegate.extractEmbeddings(img);
            float[] bestEmb = null;
            if (embeddings != null && !embeddings.isEmpty()) {
                int idx = detections.indexOf(best);
                if (idx >= 0 && idx < embeddings.size()) bestEmb = embeddings.get(idx);
                else bestEmb = embeddings.get(0);
            }

            if (bestEmb == null) throw new VisionProcessingException("No embedding available", "no_embedding", "register");

            return delegate.storeFaceEmbedding(personId, bestEmb, options.getModelName(), VectorUtils.calculateImageHash(imageData), best.confidence(), options.getMetadata());
        } catch (VisionProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException("Register failed", "register_failed", "register", e);
        }
    }
} 