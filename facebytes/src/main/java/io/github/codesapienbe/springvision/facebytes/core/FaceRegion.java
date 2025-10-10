package io.github.codesapienbe.springvision.facebytes.core;

public record FaceRegion(int x, int y, int width, int height, double confidence, float[] landmarks) {
}
