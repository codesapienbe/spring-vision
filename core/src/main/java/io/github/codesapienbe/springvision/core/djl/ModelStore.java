package io.github.codesapienbe.springvision.core.djl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.codesapienbe.springvision.core.exception.ModelNotReadyException;

/**
 * Finds model files, and fetches them when the build did not bundle them.
 *
 * <p>A model is looked up on the classpath first ({@link ClasspathModels}, the library and Docker
 * builds), then in the store directory. When it is in neither and downloads are on, it is queued
 * and {@link #resolveUrl} throws {@link ModelNotReadyException}, so a caller never runs on a model
 * that is not there. Downloads run on one background thread, urgent requests first and otherwise
 * smallest first, resume an interrupted {@code .part} file, and are moved into place only after
 * the SHA-256 matches. A failed download is retried after a delay.</p>
 */
public final class ModelStore implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ModelStore.class);

    private static final List<Duration> RETRY_DELAYS =
        List.of(Duration.ofSeconds(30), Duration.ofMinutes(2), Duration.ofMinutes(10));
    private static final Duration POLL = Duration.ofMillis(200);

    /** Where a model stands. */
    public enum State {
        /** On the classpath; never downloaded. */
        BUNDLED,
        /** In the store directory and complete. */
        READY,
        /** Waiting for the download thread. */
        QUEUED,
        /** Being downloaded. */
        DOWNLOADING,
        /** The last download failed; it is retried after a delay. */
        FAILED,
        /** Not present, and downloads are off. */
        MISSING
    }

    /**
     * A model's state at one moment.
     *
     * @param path       the model's path under {@code models/}
     * @param state      where it stands
     * @param bytesDone  bytes downloaded so far
     * @param totalBytes size of the whole download
     * @param error      why the last download failed, or {@code null}
     */
    public record ModelStatus(String path, State state, long bytesDone, long totalBytes, String error) {

        /** Whether the model can be loaded now. */
        public boolean ready() {
            return state == State.BUNDLED || state == State.READY;
        }

        /** Whether the model is on its way: queued, downloading, or waiting to retry a failure. */
        public boolean pending() {
            return state == State.QUEUED || state == State.DOWNLOADING || state == State.FAILED;
        }

        /** Download progress in whole percent. */
        public int percent() {
            return totalBytes <= 0 ? 0 : (int) Math.min(100, bytesDone * 100 / totalBytes);
        }

        /** One sentence for an error message or a status report. */
        public String describe() {
            String name = path.substring(path.lastIndexOf('/') + 1);
            return switch (state) {
                case BUNDLED, READY -> name + " is ready";
                case MISSING -> name + " is not available (not bundled, and downloads are off)";
                case QUEUED -> name + " is queued for download (" + megabytes(totalBytes) + " MB)";
                case DOWNLOADING -> name + " is downloading: " + percent() + " % (" + megabytes(bytesDone)
                    + " of " + megabytes(totalBytes) + " MB)";
                case FAILED -> name + " failed to download (" + error + "); it is retried automatically";
            };
        }

        private static long megabytes(long bytes) {
            return Math.round(bytes / 1_048_576.0);
        }
    }

    /** A download request: urgent ones first, then the smallest. */
    private record Request(ModelSpec spec, boolean urgent, long sequence) implements Comparable<Request> {
        @Override
        public int compareTo(Request other) {
            if (urgent != other.urgent) {
                return urgent ? -1 : 1;
            }
            int bySize = Long.compare(spec.downloadBytes(), other.spec.downloadBytes());
            return bySize != 0 ? bySize : Long.compare(sequence, other.sequence);
        }
    }

    /** The download state of one model. Written only by this store. */
    private static final class Progress {
        private volatile State state;
        private final AtomicLong bytesDone = new AtomicLong();
        private volatile String error;
        private volatile int failures;
        private volatile boolean retryScheduled;

        private Progress(State state) {
            this.state = state;
        }
    }

    private final Path directory;
    private final boolean downloadEnabled;
    private final Map<String, ModelSpec> catalog;
    private final Map<String, Progress> progress = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Request> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequence = new AtomicLong();
    private final HttpClient http;
    private final Thread worker;
    private final ScheduledExecutorService retries;

    /**
     * @param directory       where downloaded models are kept; may be {@code null} when
     *                        {@code downloadEnabled} is false and only the classpath is used
     * @param catalog         the models this store knows how to fetch
     * @param downloadEnabled whether missing models are downloaded
     */
    public ModelStore(Path directory, List<ModelSpec> catalog, boolean downloadEnabled) {
        if (downloadEnabled && directory == null) {
            throw new IllegalArgumentException("A model directory is required when downloads are enabled");
        }
        this.directory = directory;
        this.downloadEnabled = downloadEnabled;
        Map<String, ModelSpec> byPath = new LinkedHashMap<>();
        catalog.forEach(spec -> byPath.put(spec.path(), spec));
        // keeps the catalog's order, so status() lists the models the same way every time
        this.catalog = Collections.unmodifiableMap(byPath);
        if (downloadEnabled) {
            this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
            this.worker = Thread.ofPlatform().daemon().name("spring-vision-model-download").unstarted(this::work);
            this.worker.start();
            this.retries = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("spring-vision-model-retry").factory());
        } else {
            this.http = null;
            this.worker = null;
            this.retries = null;
        }
    }

    /** A store that only looks on the classpath: the behaviour before runtime downloads existed. */
    public static ModelStore classpathOnly() {
        return new ModelStore(null, ModelCatalog.all(), false);
    }

    /** Whether missing models are downloaded. */
    public boolean downloadEnabled() {
        return downloadEnabled;
    }

    /**
     * A {@code file:} URL DJL can load for the model at {@code path}.
     *
     * @return the URL, or {@code null} when the model is neither bundled nor stored and downloads are off
     * @throws ModelNotReadyException when the model is being downloaded or its download failed
     */
    public String resolveUrl(String path) {
        String bundled = ClasspathModels.getModelUrl(path);
        if (bundled != null) {
            return bundled;
        }
        ModelSpec spec = catalog.get(path);
        if (spec == null || directory == null) {
            return null;
        }
        Path stored = directory.resolve(path);
        if (isComplete(spec, stored)) {
            return stored.toUri().toString();
        }
        if (!downloadEnabled) {
            return null;
        }
        request(spec, true);
        throw new ModelNotReadyException("spring-vision is still setting up: " + status(path).describe()
            + ". Try again shortly; the setup_status tool shows the progress.");
    }

    /** Queues every model that is neither bundled nor stored. Does nothing when downloads are off. */
    public void startBackgroundDownloads() {
        if (!downloadEnabled) {
            return;
        }
        // smallest first: the download thread takes the first request the moment it is queued
        catalog.values().stream()
            .filter(spec -> !status(spec.path()).ready())
            .sorted(Comparator.comparingLong(ModelSpec::downloadBytes))
            .forEach(spec -> request(spec, false));
    }

    /** The state of every model in the catalog. */
    public List<ModelStatus> status() {
        return catalog.keySet().stream().map(this::status).toList();
    }

    /** The state of one model. A path outside the catalog is reported from the classpath alone. */
    public ModelStatus status(String path) {
        ModelSpec spec = catalog.get(path);
        if (ClasspathModels.isModelAvailable(path)) {
            return new ModelStatus(path, State.BUNDLED, 0, 0, null);
        }
        if (spec == null) {
            return new ModelStatus(path, State.MISSING, 0, 0, null);
        }
        if (directory != null && isComplete(spec, directory.resolve(path))) {
            return new ModelStatus(path, State.READY, spec.downloadBytes(), spec.downloadBytes(), null);
        }
        Progress p = progress.get(path);
        if (p == null) {
            return new ModelStatus(path, downloadEnabled ? State.QUEUED : State.MISSING, 0, spec.downloadBytes(), null);
        }
        return new ModelStatus(path, p.state, p.bytesDone.get(), spec.downloadBytes(), p.error);
    }

    /**
     * Moves the given models to the front of the queue and waits up to {@code timeout} for them.
     *
     * @return the models that are still on their way when the wait ends; empty when all are ready
     *     or cannot be fetched at all (those fail later with their own message)
     */
    public List<ModelStatus> awaitReady(Collection<String> paths, Duration timeout) throws InterruptedException {
        paths.stream().map(catalog::get)
            .filter(spec -> spec != null && downloadEnabled && !status(spec.path()).ready())
            .forEach(spec -> request(spec, true));
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            List<ModelStatus> pending = paths.stream().map(this::status).filter(ModelStatus::pending).toList();
            if (pending.isEmpty() || System.nanoTime() >= deadline) {
                return pending;
            }
            Thread.sleep(POLL.toMillis());
        }
    }

    @Override
    public void close() {
        if (worker != null) {
            worker.interrupt();
        }
        if (retries != null) {
            retries.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- downloads

    private void request(ModelSpec spec, boolean urgent) {
        Progress p = progress.computeIfAbsent(spec.path(), key -> new Progress(State.QUEUED));
        if (p.state == State.DOWNLOADING || p.retryScheduled) {
            // already on its way, or waiting out the delay after a failure
            return;
        }
        if (p.state != State.FAILED) {
            p.state = State.QUEUED;
        }
        queue.offer(new Request(spec, urgent, sequence.incrementAndGet()));
    }

    private void work() {
        while (!Thread.currentThread().isInterrupted()) {
            Request next;
            try {
                next = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            ModelSpec spec = next.spec();
            Progress p = progress.get(spec.path());
            // the queue may hold the same model twice; skip what is done or waiting to retry
            if (p.retryScheduled || isComplete(spec, directory.resolve(spec.path()))) {
                continue;
            }
            p.state = State.DOWNLOADING;
            p.error = null;
            try {
                download(spec, p);
                p.state = State.READY;
                p.failures = 0;
                logger.info("Model downloaded: {}", spec.path());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException | RuntimeException e) {
                failed(spec, p, e);
            }
        }
    }

    private void failed(ModelSpec spec, Progress p, Exception e) {
        p.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        p.state = State.FAILED;
        Duration delay = RETRY_DELAYS.get(Math.min(p.failures, RETRY_DELAYS.size() - 1));
        p.failures = p.failures + 1;
        p.retryScheduled = true;
        logger.warn("Model download failed: {} ({}); retrying in {} s", spec.path(), p.error, delay.toSeconds());
        retries.schedule(() -> {
            p.retryScheduled = false;
            request(spec, false);
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void download(ModelSpec spec, Progress p) throws IOException, InterruptedException {
        Path target = directory.resolve(spec.path());
        Files.createDirectories(target.getParent());
        String downloadName = spec.archiveEntry() == null
            ? target.getFileName().toString()
            : URI.create(spec.url()).getPath().replaceAll(".*/", "");
        Path part = target.resolveSibling(downloadName + ".part");
        long resumeFrom = Files.exists(part) ? Files.size(part) : 0;

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(spec.url())).GET();
        if (resumeFrom > 0) {
            request.header("Range", "bytes=" + resumeFrom + "-");
        }
        HttpResponse<InputStream> response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        int code = response.statusCode();
        try (InputStream body = response.body()) {
            if (code == 416 && resumeFrom == spec.downloadBytes()) {
                p.bytesDone.set(resumeFrom);
            } else if (code == 416) {
                Files.deleteIfExists(part);
                throw new IOException("HTTP 416 resuming " + part.getFileName() + "; starting over");
            } else if (code == 206 || code == 200) {
                long start = code == 206 ? resumeFrom : 0;
                p.bytesDone.set(start);
                writeBody(body, part, start > 0, p);
            } else {
                throw new IOException("HTTP " + code + " from " + spec.url());
            }
        }

        String actual = sha256(part);
        if (!actual.equals(spec.sha256())) {
            Files.deleteIfExists(part);
            throw new IOException("checksum mismatch: expected " + spec.sha256() + ", got " + actual);
        }
        if (spec.archiveEntry() == null) {
            moveIntoPlace(part, target);
        } else {
            Path extracted = target.resolveSibling(target.getFileName() + ".extracting");
            extractEntry(part, spec.archiveEntry(), extracted);
            moveIntoPlace(extracted, target);
            Files.deleteIfExists(part);
        }
    }

    private static void writeBody(InputStream body, Path part, boolean append, Progress p) throws IOException {
        StandardOpenOption mode = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
        try (OutputStream out = Files.newOutputStream(part, StandardOpenOption.CREATE, StandardOpenOption.WRITE, mode)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = body.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                p.bytesDone.addAndGet(read);
            }
        }
    }

    private static void extractEntry(Path archive, String entryName, Path destination) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("archive has no entry " + entryName);
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isComplete(ModelSpec spec, Path file) {
        try {
            return Files.isRegularFile(file) && Files.size(file) == spec.fileBytes();
        } catch (IOException e) {
            return false;
        }
    }

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
