// src/main/java/growzapp/backend/controller/api/DocumentController.java

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.entite.Document;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.*;
import growzapp.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final InvestissementRepository investissementRepository;

    // UPLOAD
    @PostMapping("/projet/{projetId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<DocumentDTO>> uploadDocument(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("nom") String nom,
            @RequestParam(value = "type", defaultValue = "PDF") String type) throws IOException {

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // Retourne juste le vrai filename (ex: uuid_budget.xlsx)
        String filename = fileStorageService.saveProjectDocument(file);

        Document doc = new Document();
        doc.setNom(nom);
        doc.setFilename(filename); // ← NOUVEAU CHAMP
        doc.setType(type.toUpperCase());
        doc.setProjet(projet);
        documentRepository.save(doc);

        return ResponseEntity.ok(ApiResponseDTO.success(new DocumentDTO(
                doc.getId(),
                doc.getNom(),
                doc.getUrl(), // ← getUrl() reconstruit /files/documents/...
                doc.getType(),
                doc.getUploadedAt())).message("Document uploadé avec succès"));
    }

    // LISTE DES DOCUMENTS D'UN PROJET
    // LISTE DES DOCUMENTS D'UN PROJET
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<List<DocumentDTO>>> getDocumentsByProjet(
                    @PathVariable Long projetId,
                    Authentication auth) {

            // ON UTILISE LA MÉTHODE QUI CHARGE LES ROLES (même si on n’en a pas besoin ici,
            // ça évite toute LazyException si jamais tu ajoutes un log ou autre chose plus
            // tard)
            User user = userRepository.findByLoginForAuth(auth.getName())
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Projet projet = projetRepository.findById(projetId)
                            .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

            boolean hasAccess = user.getRoles().stream()
                            .anyMatch(r -> "ADMIN".equals(r.getRole())) // ← plus propre : tu stockes "ADMIN", pas
                                                                        // "ROLE_ADMIN" dans la BDD
                            || projet.getPorteur().getId().equals(user.getId())
                            || investissementRepository.existsByInvestisseurIdAndProjetId(user.getId(), projetId);

            if (!hasAccess) {
                    return ResponseEntity.status(403)
                                    .body(ApiResponseDTO.error("Accès refusé aux documents de ce projet"));
            }

            List<DocumentDTO> docs = documentRepository.findByProjetId(projetId).stream()
                            .map(d -> new DocumentDTO(d.getId(), d.getNom(), d.getUrl(), d.getType(),
                                            d.getUploadedAt()))
                            .toList();

            return ResponseEntity.ok(ApiResponseDTO.success(docs));
    }

    // TÉLÉCHARGEMENT — MAINTENANT 100 % FIABLE
    @GetMapping("/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {

            User user = userRepository.findByLoginForAuth(authentication.getName())
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));

        if (!hasAccessToProject(user, doc.getProjet().getId())) {
            return ResponseEntity.status(403).build();
        }

        byte[] data = fileStorageService.loadDocumentAsBytes(doc.getFilename()); // ← vrai nom !
        ByteArrayResource resource = new ByteArrayResource(data);

        String contentType = switch (doc.getType().toUpperCase()) {
            case "PDF" -> "application/pdf";
            case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV" -> "text/csv";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getNom() + getExtension(doc.getType()) + "\"")
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String getExtension(String type) {
        return switch (type.toUpperCase()) {
            case "PDF" -> ".pdf";
            case "EXCEL" -> ".xlsx";
            case "CSV" -> ".csv";
            default -> "";
        };
    }

    private boolean hasAccessToProject(User user, Long projetId) {
        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRole())))
            return true;
        return projetRepository.findById(projetId)
                .map(p -> p.getPorteur().getId().equals(user.getId()))
                .orElse(false) ||
                investissementRepository.existsByInvestisseurIdAndProjetId(user.getId(), projetId);
    }
}