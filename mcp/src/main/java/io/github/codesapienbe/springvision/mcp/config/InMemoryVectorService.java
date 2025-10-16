package io.github.codesapienbe.springvision.mcp.config;

import io.github.codesapienbe.springvision.core.VectorService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory VectorService implementation intended for development and tests.
 */
public class InMemoryVectorService implements VectorService {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private static final class Entry {
        final String id;
        final String personId;
        final float[] embedding;
        final String modelName;
        final String imageHash;
        final Double confidence;
        final Map<String, Object> metadata;
        final Instant createdAt;

        Entry(String id, String personId, float[] embedding, String modelName, String imageHash, Double confidence, Map<String, Object> metadata) {
            this.id = id;
            this.personId = personId;
            this.embedding = embedding;
            this.modelName = modelName;
            this.imageHash = imageHash;
            this.confidence = confidence;
            this.metadata = metadata == null ? Collections.emptyMap() : new HashMap<>(metadata);
            this.createdAt = Instant.now();
        }
    }

    @Override
    public String storeFaceEmbedding(String personId, float[] embedding, String modelName, String imageHash, Double confidence, Map<String, Object> metadata) {
        String id = UUID.randomUUID().toString();
        Entry e = new Entry(id, personId, embedding.clone(), modelName, imageHash, confidence, metadata);
        store.put(id, e);
        return id;
    }

    @Override
    public List<Map<String, Object>> findSimilarFaces(float[] queryEmbedding, String modelName, String metric, Double threshold, Integer limit, Set<String> includePersonIds, Set<String> excludePersonIds) {
        if (queryEmbedding == null) return Collections.emptyList();
        List<Map<String, Object>> out = new ArrayList<>();

        for (Entry e : store.values()) {
            if (modelName != null && e.modelName != null && !modelName.equals(e.modelName)) continue;
            if (includePersonIds != null && !includePersonIds.isEmpty() && (e.personId == null || !includePersonIds.contains(e.personId)))
                continue;
            if (excludePersonIds != null && e.personId != null && excludePersonIds.contains(e.personId)) continue;

            double dist = computeDistance(queryEmbedding, e.embedding, metric);
            if (threshold != null && dist > threshold) continue;

            Map<String, Object> m = new HashMap<>();
            m.put("embeddingId", e.id);
            m.put("personId", e.personId);
            m.put("distance", dist);
            m.put("modelName", e.modelName);
            m.put("createdAt", e.createdAt.toString());
            out.add(m);
        }

        // sort by increasing distance (more similar first)
        out.sort((a, b) -> Double.compare((Double) a.get("distance"), (Double) b.get("distance")));
        if (limit != null && out.size() > limit) return out.subList(0, limit);
        return out;
    }

    @Override
    public List<Map<String, Object>> findEntriesByImageHash(String imageHash) {
        if (imageHash == null) return Collections.emptyList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Entry e : store.values()) {
            if (e.imageHash != null && e.imageHash.equals(imageHash)) {
                Map<String, Object> m = new HashMap<>();
                m.put("embeddingId", e.id);
                m.put("personId", e.personId);
                m.put("modelName", e.modelName);
                m.put("createdAt", e.createdAt.toString());
                out.add(m);
            }
        }
        return out;
    }

    @Override
    public void deleteEmbeddingById(String embeddingId) {
        store.remove(embeddingId);
    }

    @Override
    public List<Map<String, Object>> findEntriesByPersonId(String personId) {
        if (personId == null) return Collections.emptyList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Entry e : store.values()) {
            if (personId.equals(e.personId)) {
                Map<String, Object> m = new HashMap<>();
                m.put("embeddingId", e.id);
                m.put("personId", e.personId);
                m.put("modelName", e.modelName);
                m.put("createdAt", e.createdAt.toString());
                out.add(m);
            }
        }
        return out;
    }

    private static double computeDistance(float[] a, float[] b, String metric) {
        if (a == null || b == null || a.length != b.length) return Double.POSITIVE_INFINITY;
        if (metric == null || "cosine".equalsIgnoreCase(metric)) {
            // cosine distance = 1 - cosine similarity
            double dot = 0.0, na = 0.0, nb = 0.0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                na += a[i] * a[i];
                nb += b[i] * b[i];
            }
            if (na <= 0 || nb <= 0) return Double.POSITIVE_INFINITY;
            double sim = dot / (Math.sqrt(na) * Math.sqrt(nb));
            return 1.0 - sim;
        } else {
            // euclidean
            double s = 0.0;
            for (int i = 0; i < a.length; i++) {
                double d = a[i] - b[i];
                s += d * d;
            }
            return Math.sqrt(s);
        }
    }
}
