package growzapp.backend.module.document.controller;

import growzapp.backend.module.document.model.Document;
import growzapp.backend.module.document.repository.DocumentRepository;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
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
import java.util.Optional;

/**
 * Sert les documents liés aux projets (/files/documents/**, actuellement
 * en denyAll() complet) — accessible uniquement aux investisseurs du
 * projet concerné, ou à l'admin. HIGH-05 de l'audit.
 */
@RestController
@RequiredArgsConstructor
public class ProjetDocumentFileController {

    private final DocumentRepository documentRepository;
    private final InvestissementRepository investissementRepository;
    private final UserService userService;

    @GetMapping("/files/documents/{filename:.+}")
    public ResponseEntity<?> getDocument(@PathVariable String filename) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Document> documentOpt = documentRepository.findAll().stream()
                .filter(d -> filename.equals(d.getFilename()))
                .findFirst();
        if (documentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Document document = documentOpt.get();

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRole()));
        boolean hasInvested = document.getProjet() != null
                && investissementRepository.existsByInvestisseurIdAndProjetId(
                        currentUser.getId(), document.getProjet().getId());

        if (!isAdmin && !hasInvested) {
            return ResponseEntity.status(403).build();
        }

        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path root = Paths.get(System.getProperty("user.dir"))
                    .resolve("uploads").resolve("documents").normalize();
            Path filePath = root.resolve(filename).normalize();

            if (!filePath.startsWith(root) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] content = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(content));

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}