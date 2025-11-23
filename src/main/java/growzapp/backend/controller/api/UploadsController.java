// src/main/java/growzapp/backend/controller/api/UploadsController.java
package growzapp.backend.controller.api;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class UploadsController {

    // Chemin ABSOLU + NORMALISÉ → marche à 100% Windows, Mac, Linux
    private final Path rootLocation = Paths.get("uploads/posters")
            .toAbsolutePath()
            .normalize();

    @GetMapping("/uploads/posters/{filename:.+}")
    public ResponseEntity<Resource> getPoster(@PathVariable String filename) {
        try {
            Path filePath = rootLocation.resolve(filename).normalize();

            // Sécurité anti-traversal (empêche ../../etc)
            if (!filePath.startsWith(rootLocation)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // ou IMAGE_PNG, etc.
                        .body(resource);
            }

            return ResponseEntity.notFound().build();

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}