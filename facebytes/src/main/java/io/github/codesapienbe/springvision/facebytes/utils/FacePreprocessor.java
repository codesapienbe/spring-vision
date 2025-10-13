package io.github.codesapienbe.springvision.facebytes.utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;

import io.github.codesapienbe.springvision.facebytes.enums.ModelType;

/**
 * Preprocesses face images for recognition, including alignment and resizing.
 */
public final class FacePreprocessor {

    private static final String EYE_CASCADE_CLASSPATH = "/haarcascade_eye.xml";
    private static final String EYE_CASCADE_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml";

    private final CascadeClassifier eyeClassifier;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter toFrame = new Java2DFrameConverter();

    /**
     * Default constructor for FacePreprocessor.
     */
    public FacePreprocessor() {
        Loader.load(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier.class);
        this.eyeClassifier = new CascadeClassifier(ensureEyeCascade());
    }

    /**
     * Aligns and resizes to a target size. This uses eye-based rotation estimation
     * when landmarks are not available and then resizes to the requested dimensions.
     * @param face The face image to process.
     * @param width The target width.
     * @param height The target height.
     * @return The aligned and resized image.
     */
    public BufferedImage alignAndResize(BufferedImage face, int width, int height) {
        BufferedImage src = face;
        try {
            double angle = estimateRotationAngle(face);
            if (Math.abs(angle) > 1.0) {
                src = rotate(face, angle);
            }
        } catch (Exception ignored) {
        }
        return resize(src, width, height);
    }

    /**
     * Aligns and resizes using external landmarks if provided (landmarks are 5 points: x1,y1,...).
     * @param face The face image to process.
     * @param landmarks5 The 5-point facial landmarks.
     * @param width The target width.
     * @param height The target height.
     * @return The aligned and resized image.
     */
    public BufferedImage alignAndResize(BufferedImage face, float[] landmarks5, int width, int height) {
        if (landmarks5 != null && landmarks5.length >= 10) {
            return alignWithLandmarks(face, landmarks5, width, height);
        }
        return alignAndResize(face, width, height);
    }

    /**
     * Aligns a face using 5-point landmarks (x1,y1,...,x5,y5). Uses three points (eyes + nose) to build affine.
     * Landmarks are in the input image coordinate space.
     * @param face The face image to process.
     * @param landmarks5 The 5-point facial landmarks.
     * @param targetW The target width.
     * @param targetH The target height.
     * @return The aligned image.
     */
    public BufferedImage alignWithLandmarks(BufferedImage face, float[] landmarks5, int targetW, int targetH) {
        // Default to ArcFace template for backward compatibility
        return alignWithLandmarks(face, landmarks5, targetW, targetH, ModelType.ARCFACE);
    }

    /**
     * Aligns using landmarks and a model-specific target template. Supports different
     * templates per model (e.g., ARCFACE, SFACE). Landmarks are expected as 5 points
     * (x1,y1,x2,y2,x3,...).
     * @param face The face image to process.
     * @param landmarks5 The 5-point facial landmarks.
     * @param targetW The target width.
     * @param targetH The target height.
     * @param model The model type to use for the template.
     * @return The aligned image.
     */
    public BufferedImage alignWithLandmarks(BufferedImage face, float[] landmarks5, int targetW, int targetH, ModelType model) {
        if (landmarks5 == null || landmarks5.length < 10) {
            return resize(face, targetW, targetH);
        }

        double scale = targetW / 112.0; // base template scale (112 is common anchor)
        Point2D.Double[] dst = getTemplatePointsForModel(model, scale);
        Point2D.Double dstLeftEye = dst[0];
        Point2D.Double dstRightEye = dst[1];
        Point2D.Double dstNose = dst[2];

        Point2D.Double srcLeftEye = new Point2D.Double(landmarks5[0], landmarks5[1]);
        Point2D.Double srcRightEye = new Point2D.Double(landmarks5[2], landmarks5[3]);
        Point2D.Double srcNose = new Point2D.Double(landmarks5[4], landmarks5[5]);

        AffineTransform at = computeAffineFrom3Points(srcLeftEye, srcRightEye, srcNose,
            dstLeftEye, dstRightEye, dstNose);
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(face, at, null);
        g.dispose();
        return out;
    }

    /**
     * Returns model-specific destination points {leftEye,rightEye,nose} scaled by provided factor.
     */
    private Point2D.Double[] getTemplatePointsForModel(ModelType model, double scale) {
        // Try to obtain overrides from DeepFaceConfig if present
        try {
            io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig cfg = io.github.codesapienbe.springvision.facebytes.config.DeepFaceConfig.current();
            double[] pts = cfg.templatePoints(io.github.codesapienbe.springvision.facebytes.enums.ModelType.valueOf(model.name()));
            if (pts != null && pts.length >= 6) {
                return new Point2D.Double[]{
                    new Point2D.Double(pts[0] * scale / 112.0 * 112.0, pts[1] * scale / 112.0 * 112.0),
                    new Point2D.Double(pts[2] * scale / 112.0 * 112.0, pts[3] * scale / 112.0 * 112.0),
                    new Point2D.Double(pts[4] * scale / 112.0 * 112.0, pts[5] * scale / 112.0 * 112.0)
                };
            }
        } catch (Throwable ignored) {
        }

        // Fallback to built-in defaults (ArcFace-centred values)
        return new Point2D.Double[]{
            new Point2D.Double(38.2946 * scale, 51.6963 * scale),
            new Point2D.Double(73.5318 * scale, 51.5014 * scale),
            new Point2D.Double(56.0252 * scale, 71.7366 * scale)
        };
    }

    private static AffineTransform computeAffineFrom3Points(Point2D p1, Point2D p2, Point2D p3,
                                                            Point2D q1, Point2D q2, Point2D q3) {
        // Solve for affine parameters a,b,c,d,e,f mapping x' = a x + b y + c; y' = d x + e y + f
        double[][] A = new double[][]{
            {p1.getX(), p1.getY(), 1, 0, 0, 0},
            {0, 0, 0, p1.getX(), p1.getY(), 1},
            {p2.getX(), p2.getY(), 1, 0, 0, 0},
            {0, 0, 0, p2.getX(), p2.getY(), 1},
            {p3.getX(), p3.getY(), 1, 0, 0, 0},
            {0, 0, 0, p3.getX(), p3.getY(), 1},
        };
        double[] b = new double[]{q1.getX(), q1.getY(), q2.getX(), q2.getY(), q3.getX(), q3.getY()};
        double[] x = solve6(A, b);
        return new AffineTransform(x[0], x[3], x[1], x[4], x[2], x[5]);
    }

    private static double[] solve6(double[][] A, double[] b) {
        // Simple Gaussian elimination for 6x6
        int n = 6;
        double[][] M = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            M[i][n] = b[i];
        }
        for (int i = 0; i < n; i++) {
            int pivot = i;
            for (int r = i + 1; r < n; r++) if (Math.abs(M[r][i]) > Math.abs(M[pivot][i])) pivot = r;
            double[] tmp = M[i];
            M[i] = M[pivot];
            M[pivot] = tmp;
            double div = M[i][i];
            if (Math.abs(div) < 1e-9) continue;
            for (int j = i; j <= n; j++) M[i][j] /= div;
            for (int r = 0; r < n; r++)
                if (r != i) {
                    double factor = M[r][i];
                    for (int j = i; j <= n; j++) M[r][j] -= factor * M[i][j];
                }
        }
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = M[i][n];
        return x;
    }

    private double estimateRotationAngle(BufferedImage img) {
        Mat matColor = toMat.convert(toFrame.convert(img));
        Mat gray = new Mat();
        cvtColor(matColor, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);
        RectVector eyes = new RectVector();
        eyeClassifier.detectMultiScale(gray, eyes, 1.1, 2, 0, new Size(15, 15), new Size());
        if (eyes.size() < 2) return 0.0;
        // Pick two largest by width
        int idx1 = -1, idx2 = -1;
        int w1 = -1, w2 = -1;
        for (long i = 0; i < eyes.size(); i++) {
            int w = eyes.get(i).width();
            if (w > w1) {
                w2 = w1;
                idx2 = idx1;
                w1 = w;
                idx1 = (int) i;
            } else if (w > w2) {
                w2 = w;
                idx2 = (int) i;
            }
        }
        if (idx1 < 0 || idx2 < 0) return 0.0;
        var e1 = eyes.get(idx1);
        var e2 = eyes.get(idx2);
        double cx1 = e1.x() + e1.width() / 2.0;
        double cy1 = e1.y() + e1.height() / 2.0;
        double cx2 = e2.x() + e2.width() / 2.0;
        double cy2 = e2.y() + e2.height() / 2.0;
        double dy = cy2 - cy1;
        double dx = cx2 - cx1;
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private static BufferedImage rotate(BufferedImage img, double angleDegrees) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rot = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rot.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(angleDegrees), w / 2.0, h / 2.0);
        g.drawRenderedImage(img, at);
        g.dispose();
        return rot;
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private String ensureEyeCascade() {
        try (InputStream is = getClass().getResourceAsStream(EYE_CASCADE_CLASSPATH)) {
            if (is != null) {
                Path tmp = Files.createTempFile("haarcascade_eye", ".xml");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toAbsolutePath().toString();
            }
        } catch (IOException ignored) {
        }
        try {
            Path cacheDir = Path.of(System.getProperty("user.home", "."), ".spring-vision", "facebytes", "models");
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve("haarcascade_eye.xml");
            if (Files.exists(target) && Files.size(target) > 0) return target.toAbsolutePath().toString();
            java.net.URL url = new java.net.URL(EYE_CASCADE_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "spring-vision-facebytes/1.0");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                if (Files.size(target) == 0) throw new IOException("Downloaded eye cascade is empty");
                return target.toAbsolutePath().toString();
            }
            throw new IOException("HTTP " + code + " for eye cascade download");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to obtain eye cascade: " + e.getMessage(), e);
        }
    }
}
