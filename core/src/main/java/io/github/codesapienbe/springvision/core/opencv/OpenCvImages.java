package io.github.codesapienbe.springvision.core.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_8U;

import java.util.List;
import java.util.Locale;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Decoding, encoding and pixelation with OpenCV. Every method returns a new {@link Mat} or
 * byte array; the caller's {@code Mat} is never modified.
 */
public final class OpenCvImages {

    private OpenCvImages() {
    }

    /** Decodes encoded image bytes (JPEG, PNG, …) into a BGR {@link Mat}; the caller closes it. */
    public static Mat decode(byte[] encoded) {
        try (Mat buffer = new Mat(1, encoded.length, CV_8U, new BytePointer(encoded))) {
            Mat image = opencv_imgcodecs.imdecode(buffer, opencv_imgcodecs.IMREAD_COLOR);
            if (image == null || image.empty()) {
                throw new VisionProcessingException("Failed to decode image data", "image_decode_failed", null);
            }
            return image;
        }
    }

    /** Encodes {@code image} in {@code format} ("jpeg", "png", …). */
    public static byte[] encode(Mat image, String format) {
        String extension = "." + (format == null || format.isBlank() ? "png" : format.toLowerCase(Locale.ROOT));
        try (BytePointer out = new BytePointer()) {
            if (!opencv_imgcodecs.imencode(extension, image, out)) {
                throw new VisionProcessingException(
                    "Failed to encode image as " + extension, "image_encode_failed", null);
            }
            byte[] bytes = new byte[(int) out.limit()];
            out.get(bytes);
            return bytes;
        }
    }

    /**
     * A copy of {@code image} with every region pixelated into blocks of roughly
     * {@code blockSize} pixels. Pixelation discards the detail inside a region, unlike a
     * blur, which can be partly reversed.
     */
    public static Mat pixelated(Mat image, List<PixelRegion> regions, int blockSize) {
        Mat result = image.clone();
        for (PixelRegion region : regions) {
            try (Rect rect = new Rect(region.x(), region.y(), region.width(), region.height());
                Mat roi = result.apply(rect);
                Mat small = new Mat()) {
                int columns = Math.max(1, region.width() / blockSize);
                int rows = Math.max(1, region.height() / blockSize);
                opencv_imgproc.resize(roi, small, new Size(columns, rows), 0, 0, opencv_imgproc.INTER_AREA);
                // roi is a view into result, which this method owns
                opencv_imgproc.resize(small, roi, new Size(region.width(), region.height()), 0, 0,
                    opencv_imgproc.INTER_NEAREST);
            }
        }
        return result;
    }
}
