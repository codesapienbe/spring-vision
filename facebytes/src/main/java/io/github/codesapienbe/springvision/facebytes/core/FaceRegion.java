package io.github.codesapienbe.springvision.facebytes.core;

/**
 * Represents the detected region of a face within an image.
 * This record holds the coordinates and dimensions of the bounding box around the face,
 * the confidence score of the detection, and an optional array of facial landmarks.
 *
 * @param x          The x-coordinate of the top-left corner of the bounding box.
 * @param y          The y-coordinate of the top-left corner of the bounding box.
 * @param width      The width of the bounding box.
 * @param height     The height of the bounding box.
 * @param confidence The confidence score of the face detection, typically between 0.0 and 1.0.
 * @param landmarks  An optional array of facial landmark coordinates (e.g., eyes, nose, mouth corners).
 *                   The format can vary by detector but is often a flat array of [x1, y1, x2, y2, ...].
 */
public record FaceRegion(int x, int y, int width, int height, double confidence, float[] landmarks) {
}
