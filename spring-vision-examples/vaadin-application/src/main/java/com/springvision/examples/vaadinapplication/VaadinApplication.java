package com.springvision.examples.vaadinapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vaadin-Based GUI Example Application.
 *
 * <p>This is a Spring Boot application that demonstrates face detection capabilities
 * using the Spring Vision framework with a Vaadin-based interface. It provides
 * a web interface for uploading images and viewing face detection results via REST API.</p>
 *
 * <p>The application includes:</p>
 * <ul>
 *   <li>Vaadin-based web interface for image upload</li>
 *   <li>REST API for face detection processing</li>
 *   <li>Result visualization with Vaadin components</li>
 *   <li>Health monitoring</li>
 *   <li>Performance metrics</li>
 * </ul>
 *
 * <p>To run this application:</p>
 * <pre>{@code
 * mvn spring-boot:run
 * }</pre>
 *
 * <p>Then access the web interface at:</p>
 * <ul>
 *   <li>Main application: http://localhost:8080</li>
 *   <li>Health check: http://localhost:8080/actuator/health</li>
 *   <li>API endpoint: http://localhost:8080/api/vision/health</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.springvision.autoconfigure",  // auto-config beans & props
        "com.springvision.core",          // core domain + backend
        "com.springvision.starter",        // REST API (VisionController)
        "com.springvision.examples.vaadinapplication" // local Vaadin code
    }
)
public class VaadinApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VaadinApplication.class, args);
    }
}
