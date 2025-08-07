package com.springvision.examples.gwtapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GWT-Based GUI Example Application.
 *
 * <p>This is a Spring Boot application that demonstrates face detection capabilities
 * using the Spring Vision framework with a GWT-based interface. It provides
 * a web interface for uploading images and viewing face detection results via REST API.</p>
 *
 * <p>The application includes:</p>
 * <ul>
 *   <li>GWT-based web interface for image upload</li>
 *   <li>REST API for face detection processing</li>
 *   <li>Result visualization with JavaScript</li>
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
                "com.springvision.examples.gwtapplication" // local GWT code
        }
)
public class GwtApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GwtApplication.class, args);
    }
}
