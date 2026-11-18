package io.github.codesapienbe.springvision.starter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.starter.web.config.VisionWebProperties;

@DisplayName("VisionController — imageUrl policy")
class VisionControllerImageUrlPolicyTest {

    private final VisionController defaults = new VisionController(mock(VisionTemplate.class));

    @Test
    @DisplayName("never reads server-side files, by path or file: URI")
    void rejectsServerFiles() {
        assertThatThrownBy(() -> defaults.downloadImageFromUrl("/etc/hosts"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> defaults.downloadImageFromUrl("file:///etc/hosts"))
            .isInstanceOf(IllegalArgumentException.class);
        VisionController remoteEnabled = new VisionController(mock(VisionTemplate.class),
            new VisionWebProperties(true, true));
        assertThatThrownBy(() -> remoteEnabled.downloadImageFromUrl("file:///etc/hosts"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuses http(s) URLs unless remote-image-urls is enabled")
    void refusesRemoteUrlsByDefault() {
        assertThatThrownBy(() -> defaults.downloadImageFromUrl("http://169.254.169.254/latest/meta-data"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spring.vision.web.remote-image-urls");
    }

    @Test
    @DisplayName("accepts data: URIs")
    void acceptsDataUris() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        String uri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        assertThat(defaults.downloadImageFromUrl(uri)).containsExactly(bytes);
    }
}
