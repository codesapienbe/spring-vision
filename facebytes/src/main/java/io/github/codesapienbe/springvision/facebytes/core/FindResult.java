package io.github.codesapienbe.springvision.facebytes.core;

/**
 * Represents the result of a find operation, which searches for the best match for a query face in a gallery.
 * This record contains the identifier of the best matching image, the distance score, the threshold used for matching,
 * and a boolean indicating whether the distance was within the threshold.
 *
 * @param imagePath The identifier (e.g., file path or custom ID) of the best matching image in the gallery.
 * @param distance  The calculated distance between the query face and the best matching face. Lower values typically indicate higher similarity.
 * @param threshold The distance threshold used to determine if a match is considered valid. If {@code distance <= threshold}, the match is positive.
 * @param matched   A boolean flag that is true if the distance is less than or equal to the threshold, and false otherwise.
 */
public record FindResult(
    String imagePath,
    double distance,
    double threshold,
    boolean matched
) {
}
