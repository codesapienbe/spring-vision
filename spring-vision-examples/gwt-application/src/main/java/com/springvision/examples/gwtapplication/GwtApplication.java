package com.springvision.examples.gwtapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GWT-Based GUI Example Application.
 *
 * <p>Skeleton for a Google Web Toolkit (GWT) based GUI integrating Spring Vision.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.springvision.autoconfigure",  // auto-config beans & props
                "com.springvision.core",          // core domain + backend
                "com.springvision.starter",        // REST API (VisionController)
                "com.springvision.examples.gwtapplication" // local GUI code
        }
)
public class GwtApplication {
    public static void main(String[] args) {
        SpringApplication.run(GwtApplication.class, args);
    }
}
