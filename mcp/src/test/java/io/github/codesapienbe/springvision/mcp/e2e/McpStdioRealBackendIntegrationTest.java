package io.github.codesapienbe.springvision.mcp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.codesapienbe.springvision.mcp.SpringVisionMcpServerApplication;

/**
 * The real MCP server with the real vision backend, over stdio: models load, and an OCR call on
 * a rendered image returns the text. Needs the bundled models and tessdata
 * ({@code -Pdownload-models}) and a few GB of memory, so it runs with {@code make memory-tests}.
 */
@Tag("memory-intensive")
class McpStdioRealBackendIntegrationTest {

    @Test
    @DisplayName("extract_text over stdio reads the text of a rendered image")
    void ocrOverStdio(@TempDir Path workDir) throws Exception {
        try (StdioMcpProcess server = StdioMcpProcess.start(SpringVisionMcpServerApplication.class, workDir,
                List.of("-Xmx3g"), List.of("--spring.vision.djl.model-cache-dir=" + workDir.resolve("djl")))) {
            server.initialize();

            ToolResult result = server.call("extract_text_b", Map.of("imageBase64", renderedText("SPRING VISION 2026")));

            assertThat(result.error()).as(result.text()).isFalse();
            assertThat(result.text()).contains("SPRING VISION 2026");
            assertThat(server.nonJsonStdout()).isEmpty();
        }
    }

    private static String renderedText(String text) throws IOException {
        System.setProperty("java.awt.headless", "true");
        BufferedImage image = new BufferedImage(900, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 72));
        g.drawString(text, 40, 125);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return Base64.getEncoder().encodeToString(png.toByteArray());
    }
}
