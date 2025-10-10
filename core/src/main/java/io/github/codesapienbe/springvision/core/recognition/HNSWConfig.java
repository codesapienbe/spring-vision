package io.github.codesapienbe.springvision.core.recognition;

/**
 * Configuration for HNSW (Hierarchical Navigable Small World) algorithm parameters.
 *
 * <p>This class provides configuration options for tuning the HNSW algorithm
 * performance and accuracy trade-offs. Different configurations are optimized
 * for different use cases.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public record HNSWConfig(
    int maxElements,      // Maximum number of elements
    int M,               // Bi-directional links created for every new element during construction
    int efConstruction,   // Size of the dynamic candidate list
    int efSearch,        // Size of the dynamic candidate list used during search
    int expectedElements  // Expected number of elements for initial capacity
) {

    /**
     * Default configuration optimized for general face recognition.
     *
     * <p>Balanced between accuracy and performance:</p>
     * <ul>
     *   <li>M=16: Good balance between graph connectivity and memory usage</li>
     *   <li>efConstruction=200: High quality index construction</li>
     *   <li>efSearch=100: Good search accuracy</li>
     * </ul>
     */
    public static HNSWConfig defaultConfig() {
        return new HNSWConfig(
            2_000_000,  // 2M faces
            16,         // 16 connections per node
            200,        // 200 candidates during construction
            100,        // 100 candidates during search
            100_000     // Initial capacity for 100K faces
        );
    }

    /**
     * High accuracy configuration for critical applications.
     *
     * <p>Optimized for maximum accuracy with higher computational cost:</p>
     * <ul>
     *   <li>M=32: More connections for better graph structure</li>
     *   <li>efConstruction=400: Very high quality index construction</li>
     *   <li>efSearch=200: High search accuracy</li>
     * </ul>
     */
    public static HNSWConfig highAccuracyConfig() {
        return new HNSWConfig(
            2_000_000,  // 2M faces
            32,         // 32 connections per node
            400,        // 400 candidates during construction
            200,        // 200 candidates during search
            100_000     // Initial capacity
        );
    }

    /**
     * Fast search configuration for real-time applications.
     *
     * <p>Optimized for speed with acceptable accuracy trade-offs:</p>
     * <ul>
     *   <li>M=8: Fewer connections for faster traversal</li>
     *   <li>efConstruction=100: Faster index construction</li>
     *   <li>efSearch=50: Faster search with lower accuracy</li>
     * </ul>
     */
    public static HNSWConfig fastSearchConfig() {
        return new HNSWConfig(
            2_000_000,  // 2M faces
            8,          // 8 connections per node
            100,        // 100 candidates during construction
            50,         // 50 candidates during search
            100_000     // Initial capacity
        );
    }

    /**
     * Large database configuration for massive face collections.
     *
     * <p>Optimized for very large databases (5M+ faces):</p>
     * <ul>
     *   <li>M=16: Balanced connectivity for large graphs</li>
     *   <li>efConstruction=200: Good construction quality</li>
     *   <li>efSearch=100: Balanced search performance</li>
     * </ul>
     */
    public static HNSWConfig largeDatabaseConfig() {
        return new HNSWConfig(
            5_000_000,  // 5M faces
            16,         // 16 connections per node
            200,        // 200 candidates during construction
            100,        // 100 candidates during search
            1_000_000   // Larger initial capacity
        );
    }

    /**
     * Memory-efficient configuration for resource-constrained environments.
     *
     * <p>Optimized for minimal memory usage:</p>
     * <ul>
     *   <li>M=8: Fewer connections to reduce memory</li>
     *   <li>efConstruction=100: Lower construction memory</li>
     *   <li>efSearch=50: Lower search memory</li>
     * </ul>
     */
    public static HNSWConfig memoryEfficientConfig() {
        return new HNSWConfig(
            1_000_000,  // 1M faces
            8,          // 8 connections per node
            100,        // 100 candidates during construction
            50,         // 50 candidates during search
            50_000      // Smaller initial capacity
        );
    }

    /**
     * Custom configuration for specific requirements.
     *
     * @param maxElements      maximum number of elements the index can hold
     * @param M                number of bi-directional links per element
     * @param efConstruction   size of dynamic candidate list during construction
     * @param efSearch         size of dynamic candidate list during search
     * @param expectedElements expected number of elements for initial capacity
     * @return custom HNSW configuration
     */
    public static HNSWConfig customConfig(int maxElements, int M, int efConstruction,
                                          int efSearch, int expectedElements) {
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements must be positive");
        }
        if (M <= 0) {
            throw new IllegalArgumentException("M must be positive");
        }
        if (efConstruction <= 0) {
            throw new IllegalArgumentException("efConstruction must be positive");
        }
        if (efSearch <= 0) {
            throw new IllegalArgumentException("efSearch must be positive");
        }
        if (expectedElements <= 0) {
            throw new IllegalArgumentException("expectedElements must be positive");
        }
        if (expectedElements > maxElements) {
            throw new IllegalArgumentException("expectedElements cannot exceed maxElements");
        }

        return new HNSWConfig(maxElements, M, efConstruction, efSearch, expectedElements);
    }

    /**
     * Validate the configuration parameters.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void validate() {
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements must be positive");
        }
        if (M <= 0) {
            throw new IllegalArgumentException("M must be positive");
        }
        if (efConstruction <= 0) {
            throw new IllegalArgumentException("efConstruction must be positive");
        }
        if (efSearch <= 0) {
            throw new IllegalArgumentException("efSearch must be positive");
        }
        if (expectedElements <= 0) {
            throw new IllegalArgumentException("expectedElements must be positive");
        }
        if (expectedElements > maxElements) {
            throw new IllegalArgumentException("expectedElements cannot exceed maxElements");
        }
        if (efSearch > efConstruction) {
            throw new IllegalArgumentException("efSearch should not exceed efConstruction");
        }
    }

    @Override
    public String toString() {
        return String.format("HNSWConfig{maxElements=%d, M=%d, efConstruction=%d, efSearch=%d, expectedElements=%d}",
            maxElements, M, efConstruction, efSearch, expectedElements);
    }
}
