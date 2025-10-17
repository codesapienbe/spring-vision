package io.github.codesapienbe.springvision.mcp;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VisionToolDownloadTest {

    private MockWebServer server;

    @BeforeEach
    void setup() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }

    @Test
    void transient429_then_success_shouldRetryAndReturnBytes() throws Exception {
        // Arrange: enqueue a 429 then a 200 with a small PNG-like payload
        server.enqueue(new MockResponse().setResponseCode(429));
        byte[] payload = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n'}; // small marker bytes
        server.enqueue(new MockResponse().setResponseCode(200).setBody(new okio.Buffer().write(payload)));

        String url = server.url("/image.png").toString();

        // Use a mock VisionTemplate because the constructor requires it; we won't call into it here
        var mockVisionTemplate = mock(io.github.codesapienbe.springvision.core.VisionTemplate.class);

        HttpClient client = HttpClient.newBuilder().build();
        VisionTool tool = new VisionTool(mockVisionTemplate, client);

        // Act
        byte[] result = tool.downloadImageFromUrl(url);

        // Assert
        assertArrayEquals(payload, result);
        assertEquals(2, server.getRequestCount(), "Expected two requests (one 429 then one 200)");
    }

    @Test
    void repeated5xx_shouldThrowIOExceptionAfterRetries() throws Exception {
        // Arrange: enqueue three 500 responses to exceed retries
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(502));
        server.enqueue(new MockResponse().setResponseCode(503));

        String url = server.url("/broken.png").toString();

        var mockVisionTemplate = mock(io.github.codesapienbe.springvision.core.VisionTemplate.class);
        HttpClient client = HttpClient.newBuilder().build();
        VisionTool tool = new VisionTool(mockVisionTemplate, client);

        // Act & Assert
        IOException ex = assertThrows(IOException.class, () -> tool.downloadImageFromUrl(url));
        assertTrue(ex.getMessage().contains("HTTP error"));
        assertEquals(3, server.getRequestCount(), "Expected three attempts for transient server errors");
    }
}

