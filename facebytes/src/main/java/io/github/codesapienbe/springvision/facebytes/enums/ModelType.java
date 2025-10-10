package io.github.codesapienbe.springvision.facebytes.enums;

/**
 * Enumerates the supported facial recognition models for generating embeddings and performing analysis.
 * Each model has different characteristics in terms of accuracy, speed, and embedding dimension.
 */
public enum ModelType {
    /** VGG-Face model developed by the Visual Geometry Group at Oxford. */
    VGG_FACE,
    /** FaceNet model developed by Google, known for its high accuracy. */
    FACENET,
    /** A variant of FaceNet that produces a 512-dimensional embedding. */
    FACENET512,
    /** OpenFace model, a lightweight and open-source alternative. */
    OPEN_FACE,
    /** DeepFace model, one of the early high-performing models in face recognition. */
    DEEP_FACE,
    /** ArcFace (Additive Angular Margin Loss) model, a state-of-the-art model known for its discriminative power. */
    ARCFACE,
    /** SFace model, a lightweight and efficient model suitable for mobile or edge devices. */
    SFACE,
    /** DeepID (Deep Hidden IDentity) model series. */
    DEEPID,
    /** Dlib's face recognition model, often used in combination with its face detector. */
    DLIB
}
