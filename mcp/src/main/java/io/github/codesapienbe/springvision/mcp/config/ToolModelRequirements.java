package io.github.codesapienbe.springvision.mcp.config;

import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.ACTION;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.AGE;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.DEEPFAKE;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.EMOTION;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.FACE_FEATURE;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.GENDER;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.NSFW;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.PALM;
import static io.github.codesapienbe.springvision.core.djl.ModelCatalog.TESSDATA;

import java.util.List;
import java.util.Map;

/**
 * Which downloadable models each tool needs before it can run. {@link GuardedToolCallback} waits
 * briefly for them and otherwise answers with a "still setting up" error, so a tool never runs
 * half-installed. A tool not listed here needs no downloaded model (face detection uses the Haar
 * cascades shipped with OpenCV; barcodes, EXIF and annotation use no model at all).
 */
final class ToolModelRequirements {

    private ToolModelRequirements() {
    }

    private static final List<String> FACE_EMBEDDING = List.of(FACE_FEATURE);

    static final Map<String, List<String>> BY_TOOL = Map.ofEntries(
        Map.entry("detect_emotions_b", List.of(EMOTION)),
        Map.entry("detect_emotions_u", List.of(EMOTION)),
        Map.entry("analyze_stress_b", List.of(EMOTION)),
        Map.entry("analyze_stress_u", List.of(EMOTION)),
        Map.entry("detect_demographics_b", List.of(AGE, GENDER)),
        Map.entry("detect_demographics_u", List.of(AGE, GENDER)),
        Map.entry("detect_deepfake_b", List.of(DEEPFAKE)),
        Map.entry("detect_deepfake_u", List.of(DEEPFAKE)),
        Map.entry("detect_nsfw_b", List.of(NSFW)),
        Map.entry("detect_nsfw_u", List.of(NSFW)),
        Map.entry("detect_hands_b", List.of(PALM)),
        Map.entry("detect_hands_u", List.of(PALM)),
        Map.entry("recognize_actions_b", List.of(ACTION)),
        Map.entry("recognize_actions_u", List.of(ACTION)),
        Map.entry("detect_threats_b", List.of(ACTION)),
        Map.entry("extract_face_embeddings_b", FACE_EMBEDDING),
        Map.entry("extract_face_embeddings_u", FACE_EMBEDDING),
        Map.entry("extract_face_embeddings_b_validated", FACE_EMBEDDING),
        Map.entry("verify_faces_between_urls", FACE_EMBEDDING),
        Map.entry("verify_faces_between_bytes", FACE_EMBEDDING),
        Map.entry("lookup_faces_in_dataset_u", FACE_EMBEDDING),
        Map.entry("lookup_faces_in_dataset_b", FACE_EMBEDDING),
        Map.entry("authenticate_access_b", FACE_EMBEDDING),
        Map.entry("store_identity_u", FACE_EMBEDDING),
        Map.entry("extract_text_b", TESSDATA),
        Map.entry("extract_text_u", TESSDATA),
        Map.entry("recognize_identity_card_b", TESSDATA),
        Map.entry("recognize_identity_card_u", TESSDATA),
        Map.entry("recognize_driver_license_b", TESSDATA),
        Map.entry("recognize_driver_license_u", TESSDATA),
        Map.entry("recognize_license_plate_b", TESSDATA),
        Map.entry("recognize_license_plate_u", TESSDATA));

    /** The models {@code tool} needs; empty when it needs none. */
    static List<String> modelsFor(String tool) {
        return BY_TOOL.getOrDefault(tool, List.of());
    }
}
