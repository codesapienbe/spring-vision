package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.github.codesapienbe.springvision.core.exception.ModelNotReadyException;

@DisplayName("ModelStore")
class ModelStoreTest {

    private static final byte[] CONTENT = "pretend this is an ONNX model".repeat(2000).getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path directory;

    private HttpServer server;
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final List<String> rangeHeaders = new CopyOnWriteArrayList<>();
    private final AtomicInteger requests = new AtomicInteger();
    private final List<String> requestOrder = new CopyOnWriteArrayList<>();
    private ModelStore store;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::serve);
        server.start();
    }

    @AfterEach
    void stop() {
        if (store != null) {
            store.close();
        }
        server.stop(0);
    }

    private void serve(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        requestOrder.add(exchange.getRequestURI().getPath());
        byte[] body = files.get(exchange.getRequestURI().getPath());
        if (body == null) {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }
        String range = exchange.getRequestHeaders().getFirst("Range");
        int status = 200;
        if (range != null) {
            rangeHeaders.add(range);
            int from = Integer.parseInt(range.replace("bytes=", "").replace("-", ""));
            body = java.util.Arrays.copyOfRange(body, from, body.length);
            status = 206;
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private ModelSpec served(String modelPath, byte[] content) throws Exception {
        files.put("/" + modelPath, content);
        return ModelSpec.file(modelPath, url("/" + modelPath), sha256(content), content.length);
    }

    @Test
    @DisplayName("a missing model is reported as not ready, then downloaded, verified and resolved")
    void downloadsVerifiesAndResolves() throws Exception {
        ModelSpec spec = served("emotion/model.onnx", CONTENT);
        store = new ModelStore(directory, List.of(spec), true);

        assertThatThrownBy(() -> store.resolveUrl(spec.path()))
            .isInstanceOf(ModelNotReadyException.class)
            .hasMessageContaining("model.onnx")
            .hasMessageContaining("setup_status");

        assertThat(store.awaitReady(List.of(spec.path()), Duration.ofSeconds(10))).isEmpty();
        assertThat(store.status(spec.path()).state()).isEqualTo(ModelStore.State.READY);
        assertThat(Files.readAllBytes(directory.resolve(spec.path()))).isEqualTo(CONTENT);
        assertThat(store.resolveUrl(spec.path())).isEqualTo(directory.resolve(spec.path()).toUri().toString());
        assertThat(Files.exists(directory.resolve(spec.path() + ".part"))).isFalse();
    }

    @Test
    @DisplayName("a download whose checksum does not match is refused and never put in place")
    void refusesChecksumMismatch() throws Exception {
        files.put("/bad/model.onnx", CONTENT);
        ModelSpec spec = ModelSpec.file("bad/model.onnx", url("/bad/model.onnx"), "0".repeat(64), CONTENT.length);
        store = new ModelStore(directory, List.of(spec), true);

        List<ModelStore.ModelStatus> pending = store.awaitReady(List.of(spec.path()), Duration.ofSeconds(3));

        assertThat(pending).singleElement().satisfies(status -> {
            assertThat(status.state()).isEqualTo(ModelStore.State.FAILED);
            assertThat(status.error()).contains("checksum mismatch");
        });
        assertThat(Files.exists(directory.resolve(spec.path()))).isFalse();
        assertThat(Files.exists(directory.resolve(spec.path() + ".part"))).isFalse();
        assertThatThrownBy(() -> store.resolveUrl(spec.path()))
            .isInstanceOf(ModelNotReadyException.class)
            .hasMessageContaining("failed to download");
    }

    @Test
    @DisplayName("an interrupted download resumes from its .part file")
    void resumesPartialDownload() throws Exception {
        ModelSpec spec = served("palm/model.onnx", CONTENT);
        Path part = directory.resolve("palm/model.onnx.part");
        Files.createDirectories(part.getParent());
        Files.write(part, java.util.Arrays.copyOf(CONTENT, 1000));
        store = new ModelStore(directory, List.of(spec), true);

        assertThat(store.awaitReady(List.of(spec.path()), Duration.ofSeconds(10))).isEmpty();

        assertThat(rangeHeaders).containsExactly("bytes=1000-");
        assertThat(Files.readAllBytes(directory.resolve(spec.path()))).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("a model shipped inside a zip is extracted after the archive is verified")
    void extractsArchiveEntry() throws Exception {
        ByteArrayOutputStream zipped = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipped)) {
            zip.putNextEntry(new ZipEntry("face_feature.pt"));
            zip.write(CONTENT);
            zip.closeEntry();
        }
        byte[] archive = zipped.toByteArray();
        files.put("/face_feature.zip", archive);
        ModelSpec spec = ModelSpec.zipEntry("archived/face.pt", url("/face_feature.zip"), sha256(archive),
            archive.length, CONTENT.length, "face_feature.pt");
        store = new ModelStore(directory, List.of(spec), true);

        assertThat(store.awaitReady(List.of(spec.path()), Duration.ofSeconds(10))).isEmpty();

        assertThat(Files.readAllBytes(directory.resolve(spec.path()))).isEqualTo(CONTENT);
        assertThat(Files.exists(directory.resolve("archived/face_feature.zip.part"))).isFalse();
    }

    @Test
    @DisplayName("with downloads off, a missing model resolves to null and nothing is fetched")
    void downloadsOff() throws Exception {
        ModelSpec spec = served("nsfw/model.onnx", CONTENT);
        store = new ModelStore(directory, List.of(spec), false);

        store.startBackgroundDownloads();

        assertThat(store.resolveUrl(spec.path())).isNull();
        assertThat(store.status(spec.path()).state()).isEqualTo(ModelStore.State.MISSING);
        assertThat(store.awaitReady(List.of(spec.path()), Duration.ofMillis(300))).isEmpty();
        assertThat(requests).hasValue(0);
    }

    @Test
    @DisplayName("a complete file already in the directory is used without a download")
    void usesStoredFile() throws Exception {
        ModelSpec spec = served("age/model.onnx", CONTENT);
        Files.createDirectories(directory.resolve("age"));
        Files.write(directory.resolve(spec.path()), CONTENT);
        store = new ModelStore(directory, List.of(spec), true);

        store.startBackgroundDownloads();

        assertThat(store.resolveUrl(spec.path())).isEqualTo(directory.resolve(spec.path()).toUri().toString());
        assertThat(store.status(spec.path()).ready()).isTrue();
        assertThat(requests).hasValue(0);
    }

    @Test
    @DisplayName("background downloads fetch every missing model, and status reports each one")
    void backgroundDownloadsEverything() throws Exception {
        ModelSpec small = served("a/small.onnx", "small".getBytes(StandardCharsets.UTF_8));
        ModelSpec large = served("b/large.onnx", CONTENT);
        store = new ModelStore(directory, List.of(large, small), true);

        store.startBackgroundDownloads();

        assertThat(store.awaitReady(List.of(small.path(), large.path()), Duration.ofSeconds(10))).isEmpty();
        assertThat(store.status()).extracting(ModelStore.ModelStatus::state)
            .containsOnly(ModelStore.State.READY);
    }

    @Test
    @DisplayName("background downloads start with the smallest model and status keeps the catalog order")
    void smallestFirstAndStableOrder() throws Exception {
        ModelSpec large = served("c/large.onnx", CONTENT);
        ModelSpec small = served("d/small.onnx", "small".getBytes(StandardCharsets.UTF_8));
        store = new ModelStore(directory, List.of(large, small), true);

        store.startBackgroundDownloads();
        assertThat(store.awaitReady(List.of(small.path(), large.path()), Duration.ofSeconds(10))).isEmpty();

        assertThat(requestOrder).containsExactly("/d/small.onnx", "/c/large.onnx");
        assertThat(store.status()).extracting(ModelStore.ModelStatus::path).containsExactly(large.path(), small.path());
    }

    @Test
    @DisplayName("a store with downloads on needs a directory")
    void downloadsNeedDirectory() {
        assertThatThrownBy(() -> new ModelStore(null, List.of(), true))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
