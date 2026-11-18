package io.github.codesapienbe.springvision.mcp.e2e;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The MCP server running as a child process over stdio, the way a local MCP client launches it:
 * JSON-RPC lines on stdin and stdout, logs on stderr. Every stdout line that is not JSON is
 * kept, because on stdio any such line corrupts the protocol.
 */
public final class StdioMcpProcess implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(180);

    private final Process process;
    private final BufferedWriter stdin;
    private final Path stderrLog;
    private final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
    private final List<String> nonJsonStdout = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private StdioMcpProcess(Process process, Path stderrLog) {
        this.process = process;
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.stderrLog = stderrLog;
        Thread reader = new Thread(this::readStdout, "stdio-mcp-stdout");
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Launches the server on the test classpath with the {@code stdio} profile.
     * @param mainClass the application main class.
     * @param workDir a scratch directory for logs and caches.
     * @param jvmArgs extra JVM options.
     * @param appArgs extra Spring Boot arguments.
     * @return the running process.
     * @throws IOException if it cannot be started.
     */
    public static StdioMcpProcess start(Class<?> mainClass, Path workDir, List<String> jvmArgs, List<String> appArgs)
            throws IOException {
        Path stderrLog = workDir.resolve("stderr.log");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(jvmArgs);
        command.add("-DSPRING_VISION_LOG_HOME=" + workDir.resolve("logs"));
        command.add("-cp");
        command.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        command.add(mainClass.getName());
        command.add("--spring.profiles.active=stdio");
        command.addAll(appArgs);
        Process process = new ProcessBuilder(command)
                .redirectError(stderrLog.toFile())
                .start();
        return new StdioMcpProcess(process, stderrLog);
    }

    /**
     * Runs {@code initialize} and {@code notifications/initialized}.
     * @return the {@code initialize} result.
     */
    public JsonNode initialize() {
        JsonNode result = request("initialize", Map.of("protocolVersion", "2025-06-18", "capabilities", Map.of(),
                "clientInfo", Map.of("name", "e2e", "version", "0")));
        send(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"));
        return result;
    }

    /**
     * Sends a request and waits for its {@code result}.
     * @param method the JSON-RPC method.
     * @param params its params.
     * @return the result node.
     */
    public JsonNode request(String method, Map<String, Object> params) {
        int id = nextId.getAndIncrement();
        send(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params));
        long deadline = System.nanoTime() + ANSWER_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode message = poll(deadline);
            if (message != null && message.path("id").asInt(-1) == id) {
                return message.path("result");
            }
        }
        throw new AssertionError("No answer to " + method + " within " + ANSWER_TIMEOUT + "; stderr: " + stderrTail());
    }

    /** @return the names of the tools the server lists. */
    public List<String> toolNames() {
        return request("tools/list", Map.of()).path("tools").findValuesAsText("name");
    }

    /**
     * Calls a tool.
     * @param tool the tool name.
     * @param arguments its arguments.
     * @return the outcome.
     */
    public ToolResult call(String tool, Map<String, Object> arguments) {
        return ToolResult.of(request("tools/call", Map.of("name", tool, "arguments", arguments)));
    }

    /** @return every stdout line that was not a JSON-RPC message, so far. */
    public List<String> nonJsonStdout() {
        return List.copyOf(nonJsonStdout);
    }

    /** @return the server's stderr so far. */
    public String stderr() {
        try {
            return Files.readString(stderrLog);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Closes stdin, as a client does when it disconnects, and waits for the process to end.
     * @param timeout how long to wait.
     * @return whether the process ended on its own within the timeout.
     * @throws InterruptedException if interrupted while waiting.
     */
    public boolean closeStdinAndAwaitExit(Duration timeout) throws InterruptedException {
        try {
            stdin.close();
        } catch (IOException e) {
            // The process already closed its end; waiting below tells whether it exited.
        }
        return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws InterruptedException {
        if (process.isAlive()) {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    private void send(Map<String, Object> message) {
        try {
            stdin.write(JSON.writeValueAsString(message));
            stdin.write('\n');
            stdin.flush();
        } catch (IOException e) {
            throw new AssertionError("Could not write to the server; stderr: " + stderrTail(), e);
        }
    }

    private JsonNode poll(long deadlineNanos) {
        try {
            return messages.poll(Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted", e);
        }
    }

    private void readStdout() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode message = parseJson(line);
                if (message == null) {
                    nonJsonStdout.add(line);
                } else {
                    messages.add(message);
                }
            }
        } catch (IOException e) {
            // The process ended; request() reports the missing answer with stderr attached.
        }
    }

    private static JsonNode parseJson(String line) {
        try {
            JsonNode node = JSON.readTree(line);
            return node != null && node.isObject() && node.has("jsonrpc") ? node : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String stderrTail() {
        String all = stderr();
        return all.length() > 4000 ? all.substring(all.length() - 4000) : all;
    }
}
