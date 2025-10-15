package io.github.codesapienbe.springvision.facebytes.core;

import io.github.codesapienbe.springvision.facebytes.enums.DetectorBackend;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;

/**
 * Represents the result of a face verification operation, which compares two faces to determine if they are the same person.
 *
 * @param verified         True if the faces are determined to be the same person (i.e., distance &lt;= threshold), false otherwise.
 * @param distance         The calculated distance between the two face embeddings. Lower values indicate higher similarity.
 * @param threshold        The distance threshold used for the verification. A distance below this value is considered a match.
 * @param model            The facial recognition model used to generate the embeddings.
 * @param detector         The face detector backend used to locate the faces in the images.
 * @param processingTimeMs The total time in milliseconds taken to perform the verification.
 */
public record VerificationResult(
    boolean verified,
    double distance,
    double threshold,
    ModelType model,
    DetectorBackend detector,
    long processingTimeMs
) {
}
