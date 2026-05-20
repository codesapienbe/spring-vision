package io.github.codesapienbe.springvision.core.document;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Image preparation pipeline tuned for OCR quality on identity documents.
 *
 * <p>The existing {@code normalize_image} MCP tool resizes images <em>down</em>
 * for model inference, which is the opposite of what Tesseract wants — small
 * scanned text degrades rapidly below ~300 DPI. This preprocessor upscales
 * small inputs, removes colour noise, sharpens edges, stretches contrast, and
 * re-encodes at high JPEG quality.</p>
 *
 * <p>Steps:</p>
 * <ol>
 *   <li>Decode into {@link BufferedImage}.</li>
 *   <li>Convert to single-channel grayscale (smaller input, less colour noise).</li>
 *   <li>If the shortest edge is below {@code upscaleMinEdgePx} (default 1000),
 *       bicubic upscale so the shortest edge reaches that target, capping the
 *       longest edge at 2400 px.</li>
 *   <li>Apply a light unsharp-mask 3×3 convolution to recover edge contrast
 *       lost during phone-camera capture.</li>
 *   <li>Contrast-stretch the histogram to the 2nd–98th percentile.</li>
 *   <li>Re-encode JPEG at quality 0.92.</li>
 * </ol>
 *
 * <p>Pure Java2D — no extra dependencies.</p>
 */
public final class DocumentImagePreprocessor {

    /** Default minimum shortest-edge size after upscale (in pixels). */
    public static final int DEFAULT_MIN_EDGE_PX = 1000;
    /** Hard cap on the longest edge to keep memory usage sane. */
    public static final int MAX_LONGEST_EDGE_PX = 2400;
    /** JPEG quality used for the preprocessed output. */
    public static final float OUTPUT_JPEG_QUALITY = 0.92f;

    private DocumentImagePreprocessor() {
    }

    public static byte[] enhanceForOcr(byte[] imageBytes) throws IOException {
        return enhanceForOcr(imageBytes, DEFAULT_MIN_EDGE_PX);
    }

    public static byte[] enhanceForOcr(byte[] imageBytes, int upscaleMinEdgePx) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Image bytes are required and cannot be empty");
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            throw new IOException("Could not decode image for OCR preprocessing");
        }

        BufferedImage gray = toGrayscale(src);
        BufferedImage scaled = upscaleIfSmall(gray, upscaleMinEdgePx);
        BufferedImage sharpened = unsharpMask(scaled);
        BufferedImage stretched = contrastStretch(sharpened, 2.0, 98.0);

        return encodeJpeg(stretched, OUTPUT_JPEG_QUALITY);
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(src, out);
        return out;
    }

    private static BufferedImage upscaleIfSmall(BufferedImage src, int minEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        int shortest = Math.min(w, h);
        if (shortest >= minEdge) {
            return src;
        }
        double scale = (double) minEdge / shortest;
        int longest = Math.max(w, h);
        if (longest * scale > MAX_LONGEST_EDGE_PX) {
            scale = (double) MAX_LONGEST_EDGE_PX / longest;
        }
        int newW = Math.max(1, (int) Math.round(w * scale));
        int newH = Math.max(1, (int) Math.round(h * scale));

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, newW, newH, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static BufferedImage unsharpMask(BufferedImage src) {
        float[] sharpen = {
            0f, -0.2f, 0f,
           -0.2f, 1.8f, -0.2f,
            0f, -0.2f, 0f
        };
        Kernel kernel = new Kernel(3, 3, sharpen);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        op.filter(src, out);
        return out;
    }

    private static BufferedImage contrastStretch(BufferedImage src, double lowPct, double highPct) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] histogram = new int[256];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = src.getRaster().getSample(x, y, 0) & 0xff;
                histogram[v]++;
            }
        }
        int total = w * h;
        int lowCount = (int) (total * (lowPct / 100.0));
        int highCount = (int) (total * (highPct / 100.0));

        int low = 0;
        int high = 255;
        int acc = 0;
        for (int i = 0; i < 256; i++) {
            acc += histogram[i];
            if (acc >= lowCount) {
                low = i;
                break;
            }
        }
        acc = 0;
        for (int i = 0; i < 256; i++) {
            acc += histogram[i];
            if (acc >= highCount) {
                high = i;
                break;
            }
        }
        if (high <= low) {
            return src;
        }
        double scale = 255.0 / (high - low);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = src.getRaster().getSample(x, y, 0) & 0xff;
                int stretched;
                if (v <= low) {
                    stretched = 0;
                } else if (v >= high) {
                    stretched = 255;
                } else {
                    stretched = (int) Math.round((v - low) * scale);
                }
                out.getRaster().setSample(x, y, 0, stretched);
            }
        }
        return out;
    }

    private static byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
        // Tesseract handles grayscale JPEGs fine. RGB jpegs are also fine if we ever need to retain colour.
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            // JPEG cannot encode a single-band gray BufferedImage directly via every JRE — re-tag if needed.
            BufferedImage encodable = ensureEncodable(img);
            writer.write(null, new IIOImage(encodable, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private static BufferedImage ensureEncodable(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_3BYTE_BGR
            || img.getType() == BufferedImage.TYPE_INT_RGB) {
            return img;
        }
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(img, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
