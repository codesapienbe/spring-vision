package com.deepface.core;

/**
 * Represents a single match candidate in a gallery search.
 */
public record FindMatch(
    String id,
    double distance
) {}
