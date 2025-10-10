package io.github.codesapienbe.springvision.examples.facerecognitionexample.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving the face recognition example web interface.
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Controller
public class WebController {

    /**
     * Serve the main face recognition interface.
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "Face Recognition Example");
        model.addAttribute("appVersion", "1.0.0");
        return "index";
    }

    /**
     * Serve the main interface on /demo path as well.
     */
    @GetMapping("/demo")
    public String demo(Model model) {
        return index(model);
    }
}
