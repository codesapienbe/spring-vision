package com.springvision.core;

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

	public DetectionType getType() { return type; }
	public Set<DetectionCategory> getCategories() { return categories; }
	public Set<String> getClassLabels() { return classLabels; }
	public double getMinConfidence() { return minConfidence; }
	public int getMaxDetections() { return maxDetections; }
	public BoundingBox getRoi() { return roi; }
	public Map<String, Object> getOptions() { return options; }
	
	/**
	 * Gets the NMS (Non-Maximum Suppression) threshold from options.
	 * Defaults to 0.45 if not specified.
	 */
	public double getNmsThreshold() {
		Object nmsThreshold = options.get("nmsThreshold");
		if (nmsThreshold instanceof Number) {
			return ((Number) nmsThreshold).doubleValue();
		}
		return 0.45; // Default NMS threshold
	}

	/** Builder for DetectionQuery with sane defaults. */
	public static final class Builder {
		private DetectionType type;
		private Set<DetectionCategory> categories = EnumSet.noneOf(DetectionCategory.class);
		private Set<String> classLabels = Set.of();
		private double minConfidence = 0.0;
		private int maxDetections = 0; // 0 = unlimited
		private BoundingBox roi;
		private Map<String, Object> options = Map.of();

		public Builder type(DetectionType type) { this.type = type; return this; }
		public Builder categories(Set<DetectionCategory> categories) { this.categories = categories; return this; }
		public Builder classLabels(Set<String> classLabels) { this.classLabels = classLabels; return this; }
		public Builder minConfidence(double minConfidence) { this.minConfidence = minConfidence; return this; }
		public Builder maxDetections(int maxDetections) { this.maxDetections = maxDetections; return this; }
		public Builder roi(BoundingBox roi) { this.roi = roi; return this; }
		public Builder options(Map<String, Object> options) { this.options = options; return this; }

		public DetectionQuery build() { return new DetectionQuery(this); }
	}
} 