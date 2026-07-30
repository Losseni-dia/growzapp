package growzapp.backend.module.contrat.controller;

import growzapp.backend.module.contrat.model.Contrat;
import growzapp.backend.module.contrat.repository.ContratRepository;
import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sert les fichiers PDF de contrats — remplace l'ancien accès statique
 * public par une vérification stricte de propriétaire (admin ou
 * investisseur concerné uniquement), HIGH-05 de l'audit.
 */
@RestController
@RequiredArgsConstructor
public class ContratFileController {

    private final ContratRepository contratRepository;
    private final UserService userService;

    @GetMapping("/uploads/contrats/{filename:.+}")
    public ResponseEntity<?> getContrat(@PathVariable String filename) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Retrouve le contrat correspondant à ce nom de fichier — le
        // fichierUrl stocké en base contient le chemin complet
        Contrat contrat = contratRepository.findByFichierUrlContaining(filename)
                .orElse(null);
        if (contrat == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRole()));
        boolean isOwner = contrat.getInvestissement() != null
                && contrat.getInvestissement().getInvestisseur() != null
                && contrat.getInvestissement().getInvestisseur().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Sécurité anti path-traversal — le nom de fichier ne doit
            // contenir ni ".." ni séparateur de dossier
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path root = Paths.get(System.getProperty("user.dir"))
                    .resolve("uploads").resolve("contrats").normalize();
            Path filePath = root.resolve(filename).normalize();

            if (!filePath.startsWith(root) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(content));

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}