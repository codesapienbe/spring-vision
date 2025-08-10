package com.deepface.detectors;

import com.deepface.core.FaceRegion;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.equalizeHist;

public final class OpenCVDetector implements FaceDetector {

    private static final String CASCADE_CLASSPATH = "/haarcascade_frontalface_default.xml";
    private static final String CASCADE_URL =
        "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";

    private final CascadeClassifier classifier;
    private final OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter toFrame = new Java2DFrameConverter();

    public OpenCVDetector() {
        Loader.load(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier.class);
        String cascadePath = ensureCascadeAvailable();
        this.classifier = new CascadeClassifier(cascadePath);
        if (this.classifier.empty()) {
            throw new IllegalStateException("Failed to load Haar cascade from: " + cascadePath);
        }
    }

    @Override
    public List<FaceRegion> detectFaces(BufferedImage image) {
        if (image == null) {
            return List.of();
        }
        Mat matColor = toMat.convert(toFrame.convert(image));
        Mat gray = new Mat();
        cvtColor(matColor, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        // Reasonable defaults for photos; tune later
        classifier.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

        List<FaceRegion> list = new ArrayList<>();
        for (long i = 0; i < faces.size(); i++) {
            Rect r = faces.get(i);
            list.add(new FaceRegion(r.x(), r.y(), r.width(), r.height(), 0.9));
        }
        return list;
    }

    private String ensureCascadeAvailable() {
        // 1) Try classpath
        try (InputStream is = getClass().getResourceAsStream(CASCADE_CLASSPATH)) {
            if (is != null) {
                Path tmp = Files.createTempFile("haarcascade", ".xml");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                tmp.toFile().deleteOnExit();
                return tmp.toAbsolutePath().toString();
            }
        } catch (IOException ignored) {}

        // 2) Cache in user home
        try {
            Path cacheDir = Path.of(System.getProperty("user.home", "."), ".spring-vision", "facebytes", "models");
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve("haarcascade_frontalface_default.xml");
            if (Files.exists(target) && Files.size(target) > 0) {
                return target.toAbsolutePath().toString();
            }
            // Download securely with timeouts
            java.net.URL url = new java.net.URL(CASCADE_URL);
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
                if (Files.size(target) == 0) {
                    throw new IOException("Downloaded cascade file is empty");
                }
                return target.toAbsolutePath().toString();
            }
            throw new IOException("HTTP " + code + " for cascade download");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Haar cascade: " + e.getMessage(), e);
        }
    }
}
