package io.github.codesapienbe.springvision.examples.basicfacedetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Basic Face Detection Example Application.
 *
 * <p>This is a simple Spring Boot application that demonstrates the basic
 * face detection capabilities of the Spring Vision framework. It provides
 * a web interface for uploading images and viewing face detection results.</p>
 *
 * <p>The application includes:</p>
 * <ul>
 *   <li>Web interface for image upload</li>
 *   <li>Face detection processing</li>
 *   <li>Result visualization</li>
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
 * @see io.github.codesapienbe.springvision.examples.basicfacedetection.controller.FaceDetectionController
 * @since 1.0.0
 */
@SpringBootApplication
public class BasicFaceDetectionApplication {

    /**
     * Creates a new {@link BasicFaceDetectionApplication}.
     */
    public BasicFaceDetectionApplication() {
        // Default constructor
    }

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BasicFaceDetectionApplication.class, args);
    }
}
