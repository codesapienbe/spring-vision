package com.springvision.jpa.template;

import com.springvision.core.Detection;
import com.springvision.core.VisionTemplate;
import com.springvision.core.exception.VisionProcessingException;
import com.springvision.jpa.dto.FaceLookupOptions;
import com.springvision.jpa.dto.FaceMatchResult;
import com.springvision.jpa.dto.FaceRegistrationOptions;
import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.dto.StoreFaceEmbeddingRequest;
import com.springvision.jpa.service.VectorSimilarityService;
import com.springvision.jpa.util.VectorUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enhanced VisionTemplate that adds vector-enabled face lookup and registration
 * by delegating detection to the provided VisionTemplate and using the
 * VectorSimilarityService for persist/search operations.
 */
@Service
public class VectorEnabledVisionTemplate extends VisionTemplate {

    private final VisionTemplate delegate;
    private final VectorSimilarityService vectorService;

    public VectorEnabledVisionTemplate(VisionTemplate delegate, VectorSimilarityService vectorService) {
        super(delegate.getBackend());
        this.delegate = delegate;
        this.vectorService = vectorService;
    }

    @Override
    public java.util.List<Detection> detectFaces(byte[] imageData) throws VisionProcessingException {
        return delegate.detectFaces(imageData);
    }

    /**
     * Face lookup using vector similarity search.
     */
    public List<FaceMatchResult> lookupFaces(byte[] imageData, FaceLookupOptions options) {
        List<Detection> faces;
        try {
            faces = detectFaces(imageData);
        } catch (Exception e) {
            throw new VisionProcessingException("Failed to detect faces", "detect_fail", "detect", e);
        }

        List<FaceMatchResult> results = new ArrayList<>();
        for (Detection face : faces) {
            if (face.embedding() != null) {
                SimilaritySearchRequest request = new SimilaritySearchRequest(
                    face.embedding(),
                    options.getModelName(),
                    options.getMetric(),
                    options.getThreshold(),
                    options.getLimit(),
                    options.getIncludePersonIds(),
                    options.getExcludePersonIds()
                );

                List<SimilaritySearchResult> matches = vectorService.findSimilarFaces(request);
                results.add(new FaceMatchResult(face, matches, options.getMetric()));
            }
        }

        return results;
    }

    /**
     * Register a person's face for future lookups.
     */
    public String registerFace(String personId, byte[] imageData, FaceRegistrationOptions options) {
        List<Detection> faces = detectFaces(imageData);

        Detection bestFace = faces.stream()
            .max(Comparator.comparing(Detection::confidence))
            .orElseThrow(() -> new VisionProcessingException("No face detected", "no_face", "register"));

        if (bestFace.embedding() == null) {
            throw new VisionProcessingException("No embedding available for detected face", "no_embedding", "register");
        }

        StoreFaceEmbeddingRequest request = new StoreFaceEmbeddingRequest(
            personId,
            bestFace.embedding(),
            options.getModelName(),
            VectorUtils.calculateImageHash(imageData),
            bestFace.confidence(),
            options.getMetadata()
        );

        return vectorService.storeFaceEmbedding(request);
    }
} 