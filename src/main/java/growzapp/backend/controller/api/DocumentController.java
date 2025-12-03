// src/main/java/growzapp/backend/controller/api/DocumentController.java
package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.entite.Document;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.DocumentRepository;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @PostMapping("/projet/{projetId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponseDTO<DocumentDTO> uploadDocument(
            @PathVariable Long projetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("nom") String nom,
            @RequestParam(value = "type", defaultValue = "PDF") String type) throws IOException {

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // CETTE MÉTHODE EXISTE MAINTENANT
        String url = fileStorageService.saveProjectDocument(file);

        Document doc = new Document();
        doc.setNom(nom);
        doc.setUrl(url); // ex: /files/documents/uuid_123_bilan.pdf
        doc.setType(type.toUpperCase());
        doc.setProjet(projet);

        documentRepository.save(doc);

        return ApiResponseDTO.success(new DocumentDTO(
                doc.getId(),
                doc.getNom(),
                url,
                doc.getType(),
                doc.getUploadedAt())).message("Document uploadé avec succès");
    }


    // === TÉLÉCHARGEMENT SÉCURISÉ ===
    @GetMapping("/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {

        User user = userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));

        // Vérification des droits (admin / porteur / investisseur)
        if (!hasAccessToProject(user, doc.getProjet().getId())) {
            throw new SecurityException("Accès refusé à ce document");
        }

        // Extrait le nom du fichier depuis l'URL stockée
        String url = doc.getUrl(); // ex: /files/documents/uuid_monbilan.pdf
        String filename = url.substring(url.lastIndexOf("/") + 1);

        byte[] data = fileStorageService.loadDocumentAsBytes(filename);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNom() + "\"")
                .contentLength(data.length)
                .contentType(MediaType.parseMediaType(determineContentType(doc.getType())))
                .body(resource);
    }

    private String determineContentType(String type) {
        return switch (type.toUpperCase()) {
            case "PDF" -> "application/pdf";
            case "XLSX", "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV" -> "text/csv";
            case "JPG", "JPEG", "IMAGE" -> "image/jpeg";
            case "PNG" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    private boolean hasAccessToProject(User user, Long projetId) {
        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getRole()))) {
            return true;
        }
        if (projetRepository.findById(projetId)
                .map(Projet::getPorteur)
                .map(p -> p.getId().equals(user.getId()))
                .orElse(false)) {
            return true;
        }
        return investissementRepository.existsByInvestisseurIdAndProjetId(user.getId(), projetId);
    }


    // CET ENDPOINT MANQUAIT → C’EST LUI QUI BLOQUE TOUT
    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<List<DocumentDTO>>> getDocumentsByProjet(
            @PathVariable Long projetId,
            Authentication auth) {

        User user = userRepository.findByLogin(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        // Vérification des droits : Admin OU Porteur OU Investisseur
        boolean hasAccess = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getRole())) ||
            projet.getPorteur().getId().equals(user.getId()) ||
            investissementRepository.existsByInvestisseurIdAndProjetId(user.getId(), projetId);

        if (!hasAccess) {
            return ResponseEntity.status(403)
                    .body(ApiResponseDTO.error("Accès refusé aux documents de ce projet"));
        }

        List<DocumentDTO> docs = documentRepository.findByProjetId(projetId).stream()
                .map(d -> new DocumentDTO(d.getId(), d.getNom(), d.getUrl(), d.getType(), d.getUploadedAt()))
                .toList();

        return ResponseEntity.ok(ApiResponseDTO.success(docs));
    }
}