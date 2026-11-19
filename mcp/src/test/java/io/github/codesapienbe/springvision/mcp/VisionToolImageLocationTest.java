package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;

/** How the {@code _u} tools read an image location that names a local file. */
class VisionToolImageLocationTest {

    private static final byte[] IMAGE = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    private final VisionTool tool = new VisionTool(new VisionTemplate(mock(VisionBackend.class)));

    @Test
    @DisplayName("reads a path whose name has spaces, like every macOS screenshot")
    void pathWithSpaces(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("Screenshot 2026-11-20 at 00.25.11.png"), IMAGE);

        assertThat(tool.downloadImageFromUrl(file.toString())).isEqualTo(IMAGE);
    }

    @Test
    @DisplayName("reads a file: URI with raw spaces and with percent-encoding")
    void fileUris(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("my photo.png"), IMAGE);

        assertThat(tool.downloadImageFromUrl("file://" + file)).isEqualTo(IMAGE);
        assertThat(tool.downloadImageFromUrl(file.toUri().toString())).isEqualTo(IMAGE);
    }

    @Test
    @DisplayName("expands ~ to the user's home")
    void tilde() {
        assertThat(VisionTool.localPath("~/Downloads/people.jpg"))
            .isEqualTo(Paths.get(System.getProperty("user.home"), "Downloads", "people.jpg"));
    }

    @Test
    @DisplayName("recognises Windows drive and UNC paths as local files, not as URI schemes")
    void windowsPaths() {
        assertThat(VisionTool.localPath("C:\\Users\\me\\Pictures\\people.jpg")).isNotNull();
        assertThat(VisionTool.localPath("D:/photos/people.jpg")).isNotNull();
        assertThat(VisionTool.localPath("\\\\server\\share\\people.jpg")).isNotNull();
    }

    @Test
    @DisplayName("leaves URLs, data URIs and base64 to the other readers")
    void notLocal() {
        assertThat(VisionTool.localPath("https://example.com/people.jpg")).isNull();
        assertThat(VisionTool.localPath("data:image/png;base64,iVBORw0KGgo=")).isNull();
        assertThat(VisionTool.localPath("iVBORw0KGgo=")).isNull();
    }

    @Test
    @DisplayName("names the missing file instead of trying it as base64 or a URL")
    void missingFile(@TempDir Path dir) {
        String missing = dir.resolve("no such file.png").toString();

        assertThatThrownBy(() -> tool.downloadImageFromUrl(missing))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File not found")
            .hasMessageContaining("no such file.png");
    }
}
