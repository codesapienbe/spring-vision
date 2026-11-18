package io.github.codesapienbe.springvision.core.opencv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import io.github.codesapienbe.springvision.core.exception.VisionBackendException;

/**
 * Face detection with the Haar cascades that ship inside OpenCV (Intel License Agreement for
 * the Open Source Computer Vision Library, which permits commercial use). No model file is
 * downloaded: the cascades come with the bytedeco OpenCV natives.
 *
 * <p>Frontal faces use {@code haarcascade_frontalface_default.xml}. Side views use
 * {@code haarcascade_profileface.xml}, which only finds faces turned one way, so it also runs
 * on the mirrored image. Overlapping hits are merged, frontal first.</p>
 *
 * <p>A cascade accepts or rejects a region; it does not produce a probability.</p>
 */
public final class HaarFaceDetector implements AutoCloseable {

    /** Which cascade found a face. */
    public enum View {
        FRONTAL, PROFILE_LEFT, PROFILE_RIGHT
    }

    /** A detected face. */
    public record Face(PixelRegion region, View view) {
    }

    private static final double SCALE_FACTOR = 1.1;
    private static final int MIN_NEIGHBORS = 4;
    private static final int MIN_FACE_PIXELS = 24;
    private static final double DUPLICATE_IOU = 0.3;

    private final CascadeClassifier frontal;
    private final CascadeClassifier profile;

    public HaarFaceDetector() {
        this.frontal = loadCascade("haarcascade_frontalface_default.xml");
        this.profile = loadCascade("haarcascade_profileface.xml");
    }

    private static CascadeClassifier loadCascade(String fileName) {
        String resource = "/org/bytedeco/opencv/" + Loader.getPlatform() + "/share/opencv4/haarcascades/" + fileName;
        try {
            File file = Loader.cacheResource(opencv_objdetect.class, resource);
            if (file == null) {
                throw new VisionBackendException(
                    "OpenCV cascade " + fileName + " not found in the OpenCV natives for " + Loader.getPlatform(),
                    "cascade_missing", null);
            }
            CascadeClassifier cascade = new CascadeClassifier(file.getAbsolutePath());
            if (cascade.empty()) {
                cascade.close();
                throw new VisionBackendException("OpenCV cascade " + fileName + " failed to load",
                    "cascade_load_failed", null);
            }
            return cascade;
        } catch (IOException e) {
            throw new VisionBackendException("OpenCV cascade " + fileName + " could not be extracted",
                "cascade_load_failed", null, e);
        }
    }

    /**
     * Faces found in a BGR image, frontal hits first. CascadeClassifier is not documented as
     * thread-safe, so calls are serialised.
     */
    public synchronized List<Face> detect(Mat bgr) {
        try (Mat gray = new Mat(); Mat equalized = new Mat(); Mat mirrored = new Mat()) {
            opencv_imgproc.cvtColor(bgr, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, equalized);
            opencv_core.flip(equalized, mirrored, 1);
            int imageWidth = equalized.cols();

            List<Face> candidates = new ArrayList<>();
            candidates.addAll(facesOf(run(frontal, equalized), View.FRONTAL, imageWidth, false));
            candidates.addAll(facesOf(run(profile, equalized), View.PROFILE_LEFT, imageWidth, false));
            candidates.addAll(facesOf(run(profile, mirrored), View.PROFILE_RIGHT, imageWidth, true));
            return withoutDuplicates(candidates);
        }
    }

    private static List<Rect> run(CascadeClassifier cascade, Mat gray) {
        try (RectVector hits = new RectVector();
            Size minSize = new Size(MIN_FACE_PIXELS, MIN_FACE_PIXELS);
            Size maxSize = new Size()) {
            cascade.detectMultiScale(gray, hits, SCALE_FACTOR, MIN_NEIGHBORS, 0, minSize, maxSize);
            List<Rect> rects = new ArrayList<>((int) hits.size());
            for (long i = 0; i < hits.size(); i++) {
                Rect hit = hits.get(i);
                rects.add(new Rect(hit.x(), hit.y(), hit.width(), hit.height()));
            }
            return rects;
        }
    }

    private static List<Face> facesOf(List<Rect> rects, View view, int imageWidth, boolean mirrored) {
        List<Face> faces = new ArrayList<>(rects.size());
        for (Rect rect : rects) {
            try (rect) {
                int x = mirrored ? imageWidth - rect.x() - rect.width() : rect.x();
                faces.add(new Face(new PixelRegion(x, rect.y(), rect.width(), rect.height()), view));
            }
        }
        return faces;
    }

    private static List<Face> withoutDuplicates(List<Face> candidates) {
        List<Face> kept = new ArrayList<>();
        for (Face candidate : candidates) {
            boolean duplicate = kept.stream()
                .anyMatch(face -> face.region().iou(candidate.region()) > DUPLICATE_IOU);
            if (!duplicate) {
                kept.add(candidate);
            }
        }
        return List.copyOf(kept);
    }

    @Override
    public void close() {
        frontal.close();
        profile.close();
    }
}
