package growzapp.backend.controller.api;

// src/main/java/growzapp/backend/controller/TestController.java

// src/main/java/growzapp/backend/controller/TestController.java
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TestController {

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()") // plus simple et marche à tous les coups
    public ResponseEntity<String> profile() {
        return ResponseEntity.ok("YES ! Le backend marche et tu es authentifié !");
    }

    @GetMapping("/public")
    public ResponseEntity<String> publicTest() {
        return ResponseEntity.ok("Endpoint public OK – pas besoin d'être connecté");
    }
}