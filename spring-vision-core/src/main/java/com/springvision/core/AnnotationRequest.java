package com.springvision.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Generic request for image annotation operations such as obscuring,
 * marking (drawing rectangles), and tagging (drawing labels).
 */
public final class AnnotationRequest {

	public enum Action { OBSCURE, MARK, TAG }

	private final Action action;
	private final Set<DetectionCategory> categories;
	private final String label; // optional, used for TAG

	private AnnotationRequest(Builder builder) {
		this.action = Objects.requireNonNull(builder.action, "action must not be null");
		this.categories = Collections.unmodifiableSet(builder.categories == null
			? EnumSet.noneOf(DetectionCategory.class)
			: EnumSet.copyOf(builder.categories));
		this.label = builder.label;

		if (action == Action.TAG) {
			if (label == null || label.strip().isEmpty()) {
				throw new IllegalArgumentException("label must be provided for TAG action");
			}
			if (label.length() > 255) {
				throw new IllegalArgumentException("label must be <= 255 characters");
			}
		}
	}

	public Action getAction() { return action; }
	public Set<DetectionCategory> getCategories() { return categories; }
	public String getLabel() { return label; }

	public static final class Builder {
		private Action action;
		private Set<DetectionCategory> categories = EnumSet.noneOf(DetectionCategory.class);
		private String label;

		public Builder action(Action action) { this.action = action; return this; }
		public Builder categories(Set<DetectionCategory> categories) { this.categories = categories; return this; }
		public Builder label(String label) { this.label = label; return this; }
		public AnnotationRequest build() { return new AnnotationRequest(this); }
	}
} 