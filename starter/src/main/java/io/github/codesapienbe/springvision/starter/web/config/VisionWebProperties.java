package io.github.codesapienbe.springvision.starter.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Opt-in switches for the {@code /api/vision/*} REST endpoints.
 *
 * @param enabled         registers the endpoints; off by default, so adding the starter to an
 *                        application exposes nothing until it asks for it
 * @param remoteImageUrls lets requests pass an {@code imageUrl} the server fetches over http(s);
 *                        off by default because a server-side fetch of a caller-chosen URL can
 *                        reach internal hosts. Redirects are never followed.
 */
@ConfigurationProperties(prefix = "spring.vision.web")
public record VisionWebProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean remoteImageUrls) {
}
