package com.deepface.core;

public record FaceRegion(int x, int y, int width, int height, double confidence, float[] landmarks) {}
