package com.springvision.opencvinstaller;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Utility to auto-install and verify OpenCV native libraries for the build.
 * Logs result to application.log in JSON format for monitoring.
 */
public class OpenCVInstaller {
    public static void main(String[] args) {
        String correlationId = UUID.randomUUID().toString();
        String logFile = "application.log";
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");
        String libPath = System.getProperty("java.library.path");
        String timestamp = Instant.now().toString();
        String diagnostics = String.format("{\"os\":\"%s\",\"arch\":\"%s\",\"java_version\":\"%s\",\"library_path\":\"%s\"}",
                sanitize(os), sanitize(arch), sanitize(javaVersion), sanitize(libPath));
        try (FileWriter writer = new FileWriter(logFile, true)) {
            Loader.load(Mat.class);
            String log = String.format("{\"timestamp\":\"%s\",\"level\":\"INFO\",\"component\":\"OpenCVInstaller\",\"message\":\"OpenCV native library loaded successfully\",\"correlation_id\":\"%s\",\"diagnostics\":%s,\"monitoring\":{\"status\":\"OK\",\"timestamp\":\"%s\"}}\n",
                    timestamp, correlationId, diagnostics, timestamp);
            writer.write(log);
            System.out.println("OpenCV native library loaded successfully.");
        } catch (Throwable t) {
            String stackTrace = sanitize(getStackTrace(t));
            try (FileWriter writer = new FileWriter(logFile, true)) {
                String log = String.format("{\"timestamp\":\"%s\",\"level\":\"ERROR\",\"component\":\"OpenCVInstaller\",\"message\":\"Failed to load OpenCV native library: %s\",\"correlation_id\":\"%s\",\"diagnostics\":%s,\"monitoring\":{\"status\":\"FAIL\",\"timestamp\":\"%s\"},\"stacktrace\":\"%s\"}\n",
                        timestamp, sanitize(t.getMessage()), correlationId, diagnostics, timestamp, stackTrace);
                writer.write(log);
            } catch (IOException ignored) {}
            System.err.println("Failed to load OpenCV native library: " + t.getMessage());
            System.exit(1);
        }
    }

    private static String sanitize(String msg) {
        if (msg == null) return "";
        return msg.replaceAll("[\n\r\t\"\\\\]", " ");
    }

    private static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append(e.toString()).append("; ");
        }
        return sb.toString();
    }
}
