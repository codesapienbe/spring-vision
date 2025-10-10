package io.github.codesapienbe.springvision.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable query object for expressing rich detection intent.
 *
 * <p>Use this to specify a detection type and optional refinements such as
 * categories (e.g., FACE, EYE), region-of-interest (ROI), minimum confidence,
 * maximum detections, class labels, and backend-specific options.</p>
 *
 * <p>In 1.x, this is an additive convenience. Backends may ignore unsupported
 * fields gracefully. Core will delegate to {@link VisionBackend#detect(ImageData, DetectionType)}
 * when only the type is present.</p>
 */
public final class DetectionQuery {

    private final DetectionType type;
    private final Set<DetectionCategory> categories;
    private final Set<String> classLabels;
    private final double minConfidence;
    private final int maxDetections;
    private final BoundingBox roi;
    private final Map<String, Object> options;

    private DetectionQuery(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "Detection type must not be null");
        this.categories = Collections.unmodifiableSet(builder.categories == null ? EnumSet.noneOf(DetectionCategory.class) : EnumSet.copyOf(builder.categories));
        this.classLabels = Collections.unmodifiableSet(builder.classLabels == null ? Set.of() : Set.copyOf(builder.classLabels));
        this.minConfidence = builder.minConfidence;
        this.maxDetections = builder.maxDetections;
        this.roi = builder.roi; // BoundingBox already validates ranges
        this.options = Collections.unmodifiableMap(builder.options == null ? Map.of() : Map.copyOf(builder.options));

        if (minConfidence < 0.0 || minConfidence > 1.0) {
            throw new IllegalArgumentException("minConfidence must be between 0.0 and 1.0");
        }
        if (maxDetections < 0) {
            throw new IllegalArgumentException("maxDetections must be >= 0");
        }
    }

    /**
     * Gets the primary detection type.
     *
     * @return the detection type
     */
    public DetectionType getType() {
        return type;
    }

    /**
     * Gets the set of detection categories to focus on.
     *
     * @return the categories
     */
    public Set<DetectionCategory> getCategories() {
        return categories;
    }

    /**
     * Gets the set of class labels to detect.
     *
     * @return the class labels
     */
    public Set<String> getClassLabels() {
        return classLabels;
    }

    /**
     * Gets the minimum confidence threshold for detections.
     *
     * @return the minimum confidence
     */
    public double getMinConfidence() {
        return minConfidence;
    }

    /**
     * Gets the maximum number of detections to return.
     *
     * @return the maximum number of detections
     */
    public int getMaxDetections() {
        return maxDetections;
    }

    /**
     * Gets the region of interest for detection.
     *
     * @return the region of interest, or null if not set
     */
    public BoundingBox getRoi() {
        return roi;
    }

    /**
     * Gets the backend-specific options.
     *
     * @return the options map
     */
    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * Gets the NMS (Non-Maximum Suppression) threshold from options.
     * Defaults to 0.45 if not specified.
     *
     * @return the NMS threshold
     */
    public double getNmsThreshold() {
        Object nmsThreshold = options.get("nmsThreshold");
        if (nmsThreshold instanceof Number) {
            return ((Number) nmsThreshold).doubleValue();
        }
        return 0.45; // Default NMS threshold
    }

    /**
     * Builder for DetectionQuery with sane defaults.
     */
    public static final class Builder {
        private DetectionType type;
        private Set<DetectionCategory> categories = EnumSet.noneOf(DetectionCategory.class);
        private Set<String> classLabels = Set.of();
        private double minConfidence = 0.0;
        private int maxDetections = 0; // 0 = unlimited
        private BoundingBox roi;
        private Map<String, Object> options = Map.of();

        /**
         * Sets the detection type.
         *
         * @param type the detection type
         * @return the builder instance
         */
        public Builder type(DetectionType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the detection categories.
         *
         * @param categories the categories
         * @return the builder instance
         */
        public Builder categories(Set<DetectionCategory> categories) {
            this.categories = categories;
            return this;
        }

        /**
         * Sets the class labels.
         *
         * @param classLabels the class labels
         * @return the builder instance
         */
        public Builder classLabels(Set<String> classLabels) {
            this.classLabels = classLabels;
            return this;
        }

        /**
         * Sets the minimum confidence.
         *
         * @param minConfidence the minimum confidence
         * @return the builder instance
         */
        public Builder minConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
            return this;
        }

        /**
         * Sets the maximum number of detections.
         *
         * @param maxDetections the maximum detections
         * @return the builder instance
         */
        public Builder maxDetections(int maxDetections) {
            this.maxDetections = maxDetections;
            return this;
        }

        /**
         * Sets the region of interest.
         *
         * @param roi the region of interest
         * @return the builder instance
         */
        public Builder roi(BoundingBox roi) {
            this.roi = roi;
            return this;
        }

        /**
         * Sets the backend-specific options.
         *
         * @param options the options
         * @return the builder instance
         */
        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        /**
         * Builds the {@link DetectionQuery} instance.
         *
         * @return a new {@link DetectionQuery}
         */
        public DetectionQuery build() {
            return new DetectionQuery(this);
        }
    }
}
