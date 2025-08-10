package com.deepface.utils;

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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;

public final class FacePreprocessor {

    private static final String EYE_CASCADE_CLASSPATH = "/haarcascade_eye.xml";
    private static final String EYE_CASCADE_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_eye.xml";

    private final CascadeClassifier eyeClassifier;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter toFrame = new Java2DFrameConverter();

    public FacePreprocessor() {
        Loader.load(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier.class);
        this.eyeClassifier = new CascadeClassifier(ensureEyeCascade());
    }

    public BufferedImage alignAndResize(BufferedImage face, int width, int height) {
        BufferedImage src = face;
        try {
            double angle = estimateRotationAngle(face);
            if (Math.abs(angle) > 1.0) {
                src = rotate(face, angle);
            }
        } catch (Exception ignored) {}
        return resize(src, width, height);
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
        int idx1 = -1, idx2 = -1; int w1 = -1, w2 = -1;
        for (long i = 0; i < eyes.size(); i++) {
            int w = eyes.get(i).width();
            if (w > w1) { w2 = w1; idx2 = idx1; w1 = w; idx1 = (int) i; }
            else if (w > w2) { w2 = w; idx2 = (int) i; }
        }
        if (idx1 < 0 || idx2 < 0) return 0.0;
        var e1 = eyes.get(idx1); var e2 = eyes.get(idx2);
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
        } catch (IOException ignored) {}
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
                if (Files.size(target) == 0) throw new IOException("Downloaded cascade file is empty");
                return target.toAbsolutePath().toString();
            }
            throw new IOException("HTTP " + code + " for eye cascade download");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to obtain eye cascade: " + e.getMessage(), e);
        }
    }
}