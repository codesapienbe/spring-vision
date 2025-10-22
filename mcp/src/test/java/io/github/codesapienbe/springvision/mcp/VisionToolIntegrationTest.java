package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration test that requires ML models - disabled to avoid OOM in unit test runs")
class VisionToolIntegrationTest {

    @Autowired
    private VisionTool visionTool;

    @Test
    void testCountFacesUrl() {
        int result = visionTool.countFaces("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg").size();
        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testExtractFaceEmbeddingsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.extractEmbeddings("https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"));
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void testDetectObjectsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectObjects("https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testClassifyImageUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.classifyImage("https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg", 5));
        assertThat(result).isNotNull();
        assertThat(result.size()).isLessThanOrEqualTo(5);
    }

    @Test
    void testExtractTextUrl() {
        String result = String.valueOf(visionTool.extractText("https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testScanBarcodeUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.scanBarcode("https://images.pexels.com/photos/159304/network-cable-ethernet-computer-159304.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectEmotionsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectEmotions("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectDemographicsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectDemographics("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testAnalyzeStressUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.analyzeStress("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectPosesUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectPoses("https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testRecognizeActionsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.recognizeActions("https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectHandsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectHands("https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectFallUrl() {
        Map<String, Object> result = visionTool.detectFall("https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg");
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectNsfwUrl() {
        Map<String, Object> result = visionTool.detectNSFW("https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg");
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectDeepfakeUrl() {
        Map<String, Object> result = visionTool.detectDeepfake("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg");
        assertThat(result).isNotNull();
    }

    @Test
    void testDetectThreatsUrl() {
        List<Map<String, Object>> result = Collections.singletonList(visionTool.detectThreats("https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg"));
        assertThat(result).isNotNull();
    }

    @Test
    void testAuthenticateAccessUrl() {
        Map<String, Object> result = visionTool.authenticateAccess("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg");
        assertThat(result).isNotNull();
    }

    @Test
    void testEstimateHeartRateUrl() {
        Map<String, Object> result = visionTool.estimateHeartRate(List.of(
            "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
            "https://images.pexels.com/photos/1222271/pexels-photo-1222271.jpeg",
            "https://images.pexels.com/photos/614810/pexels-photo-614810.jpeg"
        ));
        assertThat(result).isNotNull();
    }

    @Test
    void testExtractImageMetadataUrl() {
        Map<String, Object> result = visionTool.extractImageMetadata("https://images.pexels.com/photos/2253275/pexels-photo-2253275.jpeg");
        assertThat(result).isNotNull();
    }

}
