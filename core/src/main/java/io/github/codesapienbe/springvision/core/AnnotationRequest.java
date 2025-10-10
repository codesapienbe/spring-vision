package io.github.codesapienbe.springvision.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Generic request for image annotation operations such as obscuring,
 * marking (drawing rectangles), and tagging (drawing labels).
 */
public final class AnnotationRequest {

    /**
     * Defines the type of annotation action to be performed.
     */
    public enum Action {
        /**
         * Obscure (e.g., blur or pixelate) detected objects.
         */
        OBSCURE,
        /**
         * Mark (e.g., draw a bounding box) around detected objects.
         */
        MARK,
        /**
         * Tag (e.g., draw a label) on detected objects.
         */
        TAG
    }

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

    /**
     * Gets the annotation action to be performed.
     *
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Gets the set of detection categories to which the annotation applies.
     *
     * @return the categories
     */
    public Set<DetectionCategory> getCategories() {
        return categories;
    }

    /**
     * Gets the label to be used for the TAG action. This is optional and null for other actions.
     *
     * @return the label, or null if not applicable
     */
    public String getLabel() {
        return label;
    }

    /**
     * Builder for {@link AnnotationRequest}.
     */
    public static final class Builder {
        private Action action;
        private Set<DetectionCategory> categories = EnumSet.noneOf(DetectionCategory.class);
        private String label;

        /**
         * Sets the annotation action.
         *
         * @param action the action to set
         * @return the builder instance
         */
        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        /**
         * Sets the detection categories for the annotation.
         *
         * @param categories the categories to set
         * @return the builder instance
         */
        public Builder categories(Set<DetectionCategory> categories) {
            this.categories = categories;
            return this;
        }

        /**
         * Sets the label for the TAG action.
         *
         * @param label the label to set
         * @return the builder instance
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * Builds the {@link AnnotationRequest} instance.
         *
         * @return a new {@link AnnotationRequest}
         */
        public AnnotationRequest build() {
            return new AnnotationRequest(this);
        }
    }
}
