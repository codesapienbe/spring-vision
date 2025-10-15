package io.github.codesapienbe.springvision.facebytes.core;

/**
 * Represents a single match candidate in a gallery search.
 *
 * @param id       the identifier of the matched gallery item (path or id)
 * @param distance the computed distance between query and gallery embedding
 */
public record FindMatch(
    String id,
    double distance
) {
}
