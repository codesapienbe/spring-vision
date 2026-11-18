package io.github.codesapienbe.springvision.core.opencv;

/**
 * A rectangle in pixel coordinates of a decoded image.
 *
 * @param x      left edge, in pixels
 * @param y      top edge, in pixels
 * @param width  width, in pixels
 * @param height height, in pixels
 */
public record PixelRegion(int x, int y, int width, int height) {

    public PixelRegion {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
    }

    /** Intersection over union with another region; 0 when they do not overlap. */
    public double iou(PixelRegion other) {
        int left = Math.max(x, other.x);
        int top = Math.max(y, other.y);
        int right = Math.min(x + width, other.x + other.width);
        int bottom = Math.min(y + height, other.y + other.height);
        if (right <= left || bottom <= top) {
            return 0.0;
        }
        double intersection = (double) (right - left) * (bottom - top);
        double union = (double) width * height + (double) other.width * other.height - intersection;
        return intersection / union;
    }

    /** The share of this region's area that lies inside {@code other}, from 0 to 1. */
    public double fractionInside(PixelRegion other) {
        int left = Math.max(x, other.x);
        int top = Math.max(y, other.y);
        int right = Math.min(x + width, other.x + other.width);
        int bottom = Math.min(y + height, other.y + other.height);
        if (right <= left || bottom <= top) {
            return 0.0;
        }
        return (double) (right - left) * (bottom - top) / ((double) width * height);
    }

    /**
     * This region grown by {@code fraction} of its size on every side, clipped to an image of
     * {@code imageWidth} x {@code imageHeight}.
     */
    public PixelRegion expandedWithin(double fraction, int imageWidth, int imageHeight) {
        int dx = (int) Math.round(width * fraction);
        int dy = (int) Math.round(height * fraction);
        int left = Math.max(0, x - dx);
        int top = Math.max(0, y - dy);
        int right = Math.min(imageWidth, x + width + dx);
        int bottom = Math.min(imageHeight, y + height + dy);
        return new PixelRegion(left, top, right - left, bottom - top);
    }
}
