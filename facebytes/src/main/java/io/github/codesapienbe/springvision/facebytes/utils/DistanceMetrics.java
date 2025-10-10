package io.github.codesapienbe.springvision.facebytes.utils;

public final class DistanceMetrics {

    private DistanceMetrics() {
    }

    public static double cosineDistance(double[] a, double[] b) {
        validateSameLength(a, b);
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        if (denom == 0.0) return 1.0;
        double cos = dot / denom;
        return 1.0 - Math.max(-1.0, Math.min(1.0, cos));
    }

    public static double euclideanDistance(double[] a, double[] b) {
        validateSameLength(a, b);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    public static double euclideanL2Distance(double[] a, double[] b) {
        return euclideanDistance(a, b);
    }

    private static void validateSameLength(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("Vectors must be non-null and have same length");
        }
    }
}
