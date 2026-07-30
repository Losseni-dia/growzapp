package growzapp.backend.module.facture.controller;

import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.facture.repository.FactureRepository;
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
 * Sert les fichiers PDF de factures — remplace l'accès statique par une
 * vérification stricte de propriétaire (admin ou investisseur concerné
 * uniquement), HIGH-05 de l'audit.
 */
@RestController
@RequiredArgsConstructor
public class FactureFileController {

    private final FactureRepository factureRepository;
    private final UserService userService;

    @GetMapping("/uploads/factures/{filename:.+}")
    public ResponseEntity<?> getFacture(@PathVariable String filename) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Facture facture = factureRepository.findByFichierUrlContaining(filename)
                .orElse(null);
        if (facture == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRole()));
        boolean isOwner = facture.getInvestisseur() != null
                && facture.getInvestisseur().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).build();
        }

        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path root = Paths.get(System.getProperty("user.dir"))
                    .resolve("uploads").resolve("factures").normalize();
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