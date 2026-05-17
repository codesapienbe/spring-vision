package io.github.codesapienbe.springvision.core.auth;

import java.util.List;
import java.util.Optional;

/**
 * SPI for persisting and querying enrolled biometric identities used by
 * {@code AccessAuthenticationCapability.authenticateAccess}.
 *
 * <p>Embeddings are stored as the raw float vector emitted by the face
 * embedding model (RetinaFace + face-feature). Matching is performed via
 * cosine similarity inside {@link #findBestMatch(float[], double)}.</p>
 */
public interface EnrolledUserStore {

    /**
     * Persist a single enrolled identity. Overwrites if {@code userId} already exists.
     * {@code remoteId} may be {@code null}; it is reserved for linking the local
     * identity to an external identity provider (e.g. Keycloak subject UUID).
     */
    void enroll(String userId, String userName, String remoteId, float[] embedding);

    /** Convenience overload that stores a null {@code remoteId}. */
    default void enroll(String userId, String userName, float[] embedding) {
        enroll(userId, userName, null, embedding);
    }

    /** Remove an enrolled identity. Returns {@code true} if a row was deleted. */
    boolean delete(String userId);

    /** All currently enrolled identities (without the embedding payload). */
    List<EnrolledUser> list();

    /**
     * Find the best-matching enrolled user for {@code probe} where cosine similarity
     * is at or above {@code threshold}. Returns empty when no row clears the bar.
     */
    Optional<Match> findBestMatch(float[] probe, double threshold);

    /** Lightweight summary of an enrolled user. {@code remoteId} is null until linked to an external IdP. */
    record EnrolledUser(String userId, String userName, String remoteId, long createdAt) { }

    /** Result of a similarity search. {@code remoteId} is null until linked to an external IdP. */
    record Match(String userId, String userName, String remoteId, double similarity) { }
}
