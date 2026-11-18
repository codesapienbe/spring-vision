package io.github.codesapienbe.springvision.core.opencv;

import static org.bytedeco.opencv.global.opencv_core.bitwise_not;
import static org.bytedeco.opencv.global.opencv_core.bitwise_or;
import static org.bytedeco.opencv.global.opencv_core.countNonZero;
import static org.bytedeco.opencv.global.opencv_core.inRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;

/**
 * Finds licence-plate candidates with classical OpenCV operations, no trained model.
 *
 * <ul>
 *   <li>{@link Cue#RED_ON_WHITE}: Belgian plates print red characters on a white background, so
 *       red blobs that line up into a plate-shaped band inside a white area are strong candidates.</li>
 *   <li>{@link Cue#EDGES}: for plates with dark characters, a black-hat filter plus horizontal
 *       gradients finds dense character rows with a plate's aspect ratio.</li>
 * </ul>
 *
 * <p>Every method returns new values; the caller's image is not modified.</p>
 */
public final class PlateLocator {

    /** How a candidate was found. */
    public enum Cue {
        RED_ON_WHITE, EDGES
    }

    /** A candidate plate region: {@code text} is the character band, {@code plate} adds margin. */
    public record Candidate(PixelRegion text, PixelRegion plate, Cue cue) {
    }

    private static final double MIN_ASPECT = 2.0;
    private static final double MAX_ASPECT = 8.0;
    private static final int MIN_TEXT_HEIGHT = 10;
    private static final double MIN_WHITE_FRACTION = 0.3;
    private static final int MAX_CANDIDATES = 8;

    private PlateLocator() {
    }

    /** Plate candidates in {@code bgr}, red-on-white first, then edges, larger first. */
    public static List<Candidate> locate(Mat bgr) {
        int width = bgr.cols();
        int height = bgr.rows();
        List<Candidate> red = redOnWhite(bgr, width, height);
        List<Candidate> edges = edgeCandidates(bgr, width, height).stream()
            .filter(e -> red.stream().noneMatch(r -> e.plate().fractionInside(r.plate()) > 0.5))
            .toList();
        List<Candidate> all = new ArrayList<>(red);
        all.addAll(edges);
        return all.stream().limit(MAX_CANDIDATES).toList();
    }

    /**
     * A black-on-white, upscaled image of a candidate's characters, ready for single-line OCR.
     * For red-on-white plates only the red ink is kept, which drops the plate's background.
     */
    public static Mat ocrImage(Mat bgr, Candidate candidate) {
        PixelRegion r = candidate.text().expandedWithin(0.06, bgr.cols(), bgr.rows());
        try (Rect rect = new Rect(r.x(), r.y(), r.width(), r.height());
            Mat crop = bgr.apply(rect);
            Mat ink = new Mat();
            Mat blackOnWhite = new Mat();
            Mat scaled = new Mat()) {
            if (candidate.cue() == Cue.RED_ON_WHITE) {
                try (Mat redMask = redMask(crop); Mat characters = withoutFrame(redMask)) {
                    characters.copyTo(ink);
                }
            } else {
                try (Mat gray = new Mat()) {
                    opencv_imgproc.cvtColor(crop, gray, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.threshold(gray, ink, 0, 255,
                        opencv_imgproc.THRESH_BINARY_INV | opencv_imgproc.THRESH_OTSU);
                }
            }
            bitwise_not(ink, blackOnWhite);
            double factor = Math.max(1.0, 60.0 / r.height());
            opencv_imgproc.resize(blackOnWhite, scaled, new Size(), factor, factor, opencv_imgproc.INTER_CUBIC);
            Mat padded = new Mat();
            org.bytedeco.opencv.global.opencv_core.copyMakeBorder(scaled, padded, 16, 16, 16, 16,
                org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT, new Scalar(255.0));
            return padded;
        }
    }

    /**
     * A copy of a binary mask without the plate's printed border, which OCR would otherwise read as
     * I or 1. Long horizontal and vertical strokes are found with morphological openings and
     * removed, so a character that touches the border keeps its own pixels.
     */
    private static Mat withoutFrame(Mat mask) {
        try (Mat horizontal = new Mat(); Mat vertical = new Mat(); Mat frame = new Mat(); Mat notFrame = new Mat();
            Mat hKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT,
                new Size(Math.max(3, (int) (mask.cols() * 0.4)), 1));
            Mat vKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT,
                new Size(1, Math.max(3, (int) (mask.rows() * 0.8))))) {
            opencv_imgproc.morphologyEx(mask, horizontal, opencv_imgproc.MORPH_OPEN, hKernel);
            opencv_imgproc.morphologyEx(mask, vertical, opencv_imgproc.MORPH_OPEN, vKernel);
            bitwise_or(horizontal, vertical, frame);
            // widen the found strokes by a pixel so anti-aliased edges of the border go too
            try (Mat grown = new Mat(); Mat k = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(3, 3))) {
                opencv_imgproc.dilate(frame, grown, k);
                bitwise_not(grown, notFrame);
            }
            Mat characters = new Mat();
            org.bytedeco.opencv.global.opencv_core.bitwise_and(mask, notFrame, characters);
            return characters;
        }
    }

    private static List<Candidate> redOnWhite(Mat bgr, int width, int height) {
        try (Mat red = redMask(bgr); Mat white = whiteMask(bgr); Mat closed = new Mat();
            Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT,
                new Size(Math.max(5, width / 60), Math.max(3, height / 200)))) {
            opencv_imgproc.morphologyEx(red, closed, opencv_imgproc.MORPH_CLOSE, kernel);
            List<Candidate> candidates = new ArrayList<>();
            for (PixelRegion text : boxesOf(closed)) {
                if (!plateShaped(text)) {
                    continue;
                }
                PixelRegion plate = text.expandedWithin(0.1, width, height);
                if (fraction(white, text) >= MIN_WHITE_FRACTION) {
                    candidates.add(new Candidate(text, plate, Cue.RED_ON_WHITE));
                }
            }
            candidates.sort(Comparator.comparingInt((Candidate c) -> c.text().width() * c.text().height()).reversed());
            return candidates;
        }
    }

    private static List<Candidate> edgeCandidates(Mat bgr, int width, int height) {
        try (Mat gray = new Mat(); Mat blackhat = new Mat(); Mat gradX = new Mat(); Mat abs = new Mat();
            Mat binary = new Mat(); Mat closed = new Mat();
            Mat rectKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(13, 5));
            Mat closeKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT,
                new Size(Math.max(9, width / 50), 3))) {
            opencv_imgproc.cvtColor(bgr, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.morphologyEx(gray, blackhat, opencv_imgproc.MORPH_BLACKHAT, rectKernel);
            opencv_imgproc.Sobel(blackhat, gradX, org.bytedeco.opencv.global.opencv_core.CV_32F, 1, 0);
            org.bytedeco.opencv.global.opencv_core.convertScaleAbs(gradX, abs);
            opencv_imgproc.threshold(abs, binary, 0, 255, opencv_imgproc.THRESH_BINARY | opencv_imgproc.THRESH_OTSU);
            opencv_imgproc.morphologyEx(binary, closed, opencv_imgproc.MORPH_CLOSE, closeKernel);
            List<Candidate> candidates = new ArrayList<>();
            for (PixelRegion text : boxesOf(closed)) {
                if (plateShaped(text) && text.width() < width * 0.8) {
                    candidates.add(new Candidate(text, text.expandedWithin(0.15, width, height), Cue.EDGES));
                }
            }
            candidates.sort(Comparator.comparingInt((Candidate c) -> c.text().width() * c.text().height()).reversed());
            return candidates;
        }
    }

    private static boolean plateShaped(PixelRegion r) {
        double aspect = (double) r.width() / r.height();
        return r.height() >= MIN_TEXT_HEIGHT && aspect >= MIN_ASPECT && aspect <= MAX_ASPECT;
    }

    private static List<PixelRegion> boxesOf(Mat binary) {
        try (MatVector contours = new MatVector(); Mat hierarchy = new Mat(); Mat copy = binary.clone()) {
            opencv_imgproc.findContours(copy, contours, hierarchy, opencv_imgproc.RETR_EXTERNAL,
                opencv_imgproc.CHAIN_APPROX_SIMPLE);
            List<PixelRegion> boxes = new ArrayList<>();
            for (long i = 0; i < contours.size(); i++) {
                try (Rect rect = opencv_imgproc.boundingRect(contours.get(i))) {
                    if (rect.width() > 0 && rect.height() > 0) {
                        boxes.add(new PixelRegion(rect.x(), rect.y(), rect.width(), rect.height()));
                    }
                }
            }
            return boxes;
        }
    }

    private static Mat redMask(Mat bgr) {
        try (Mat hsv = new Mat(); Mat low = new Mat(); Mat high = new Mat();
            Mat lowFrom = hsvBound(0, 90, 60); Mat lowTo = hsvBound(10, 255, 255);
            Mat highFrom = hsvBound(170, 90, 60); Mat highTo = hsvBound(180, 255, 255)) {
            opencv_imgproc.cvtColor(bgr, hsv, opencv_imgproc.COLOR_BGR2HSV);
            inRange(hsv, lowFrom, lowTo, low);
            inRange(hsv, highFrom, highTo, high);
            Mat mask = new Mat();
            bitwise_or(low, high, mask);
            return mask;
        }
    }

    private static Mat whiteMask(Mat bgr) {
        try (Mat hsv = new Mat(); Mat from = hsvBound(0, 0, 150); Mat to = hsvBound(180, 60, 255)) {
            opencv_imgproc.cvtColor(bgr, hsv, opencv_imgproc.COLOR_BGR2HSV);
            Mat mask = new Mat();
            inRange(hsv, from, to, mask);
            return mask;
        }
    }

    /** A 1x1 bound for inRange. It must be a floating-point Mat: a CV_8UC3 one is not read as a scalar. */
    private static Mat hsvBound(double h, double s, double v) {
        return new Mat(1, 1, org.bytedeco.opencv.global.opencv_core.CV_64FC3, new Scalar(h, s, v, 0));
    }

    private static double fraction(Mat mask, PixelRegion region) {
        try (Rect rect = new Rect(region.x(), region.y(), region.width(), region.height());
            Mat roi = mask.apply(rect)) {
            return (double) countNonZero(roi) / ((long) region.width() * region.height());
        }
    }

}
