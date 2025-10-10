package io.github.codesapienbe.springvision.facebytes.core;

public record FindResult(
    String imagePath,
    double distance,
    double threshold,
    boolean matched
) {
}
