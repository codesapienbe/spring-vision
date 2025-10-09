package com.springvision.mcp;

import com.springvision.core.VisionTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    public VisionTemplate visionTemplate() {
        return mock(VisionTemplate.class);
    }
}
