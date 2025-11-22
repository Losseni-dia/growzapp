package growzapp.backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeWebController {

    @GetMapping("/")
    public String home() {
        return "redirect:/projets"; // ← OK si déjà connecté
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // → templates/login.html
    }
}