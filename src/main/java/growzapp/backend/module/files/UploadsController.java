package growzapp.backend.module.files;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Uploads", description = "Serveur de fichiers statiques — affiches des projets et médias publics")
public class UploadsController {

    private final Path rootLocation = Paths.get("uploads/posters")
            .toAbsolutePath()
            .normalize();

    @GetMapping("/uploads/posters/{filename:.+}")
    @Operation(
        summary = "Récupérer l'affiche d'un projet",
        description = "Sert l'image d'affiche d'un projet depuis le stockage local. Protégé contre les attaques de path traversal (../../). Endpoint public, aucune authentification requise.",
        tags = {"Uploads"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image retournée",
            content = @Content(mediaType = "image/jpeg")),
        @ApiResponse(responseCode = "400", description = "Nom de fichier invalide ou tentative de path traversal"),
        @ApiResponse(responseCode = "404", description = "Fichier introuvable")
    })
    public ResponseEntity<Resource> getPoster(
            @Parameter(description = "Nom du fichier image (ex: uuid_poster.jpg)", example = "a1b2c3-ferme-solaire.jpg", required = true)
            @PathVariable String filename) {
        try {
            Path filePath = rootLocation.resolve(filename).normalize();

            if (!filePath.startsWith(rootLocation)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            }

            return ResponseEntity.notFound().build();

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
