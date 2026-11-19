package io.github.codesapienbe.springvision.core.djl;

import java.util.Objects;

/**
 * One model file the {@link ModelStore} can fetch: where it lives under {@code models/}, where it
 * is downloaded from, and the SHA-256 the download must match.
 *
 * @param path          the model's path under {@code models/}, for example {@code tessdata/eng.traineddata};
 *                      the same path the classpath lookup uses
 * @param url           where the file (or the archive holding it) is downloaded from
 * @param sha256        lower-case hex SHA-256 of the downloaded bytes
 * @param downloadBytes size of the download, for progress reporting
 * @param fileBytes     size of the model file once in place, used to recognise a complete file
 * @param archiveEntry  name of the entry to extract when {@code url} points at a zip archive, or
 *                      {@code null} when the download is the model file itself
 */
public record ModelSpec(String path, String url, String sha256, long downloadBytes, long fileBytes,
                        String archiveEntry) {

    public ModelSpec {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(sha256, "sha256");
    }

    /** A model downloaded as the file itself. */
    public static ModelSpec file(String path, String url, String sha256, long bytes) {
        return new ModelSpec(path, url, sha256, bytes, bytes, null);
    }

    /** A model shipped as one entry of a zip archive. */
    public static ModelSpec zipEntry(String path, String url, String sha256, long downloadBytes, long fileBytes,
                                     String entry) {
        return new ModelSpec(path, url, sha256, downloadBytes, fileBytes, entry);
    }

    /** The file name without its directories, for messages. */
    public String fileName() {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
