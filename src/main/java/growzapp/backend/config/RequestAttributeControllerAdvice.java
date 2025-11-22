// src/main/java/growzapp/backend/Config/RequestAttributeControllerAdvice.java
package growzapp.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class RequestAttributeControllerAdvice {

    @ModelAttribute
    public void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        model.addAttribute("currentUri", request.getRequestURI()); // Plus propre dans les templates
    }
}