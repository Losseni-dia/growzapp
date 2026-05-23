package growzapp.backend.module.document.controller;

import growzapp.backend.module.document.dto.DocumentDTO;
import growzapp.backend.module.document.mapper.DocumentMapper;
import growzapp.backend.module.document.model.Document;
import growzapp.backend.module.document.service.DocumentService;
import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Documents", description = "Gestion des documents attachés aux projets (upload, consultation et téléchargement) — accès restreint aux investisseurs, porteurs et administrateurs")
public class DocumentController {

    private final FileStorageService fileStorageService;
    private final DocumentService documentService;
    private final DocumentMapper documentMapper;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    @PostMapping("/projet/{projetId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Uploader un document sur un projet",
        description = "Attache un fichier (PDF, Excel, CSV) à un projet. Le fichier est stocké avec un UUID unique pour éviter les collisions. Réservé aux administrateurs.",
        tags = {"Documents"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document uploadé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<DocumentDTO>> uploadDocument(
            @Parameter(description = "Identifiant du projet auquel attacher le document", example = "7", required = true)
            @PathVariable Long projetId,

            @Parameter(description = "Fichier à uploader (PDF, Excel, CSV)",
                schema = @Schema(type = "string", format = "binary"), required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Nom affiché du document", example = "Budget prévisionnel 2025", required = true)
            @RequestParam("nom") String nom,

            @Parameter(description = "Type du document", example = "EXCEL",
                schema = @Schema(allowableValues = {"PDF", "EXCEL", "CSV"}))
            @RequestParam(value = "type", defaultValue = "PDF") String type) throws IOException {

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));

        String filename = fileStorageService.saveProjectDocument(file);

        Document doc = new Document();
        doc.setNom(nom);
        doc.setFilename(filename);
        doc.setType(type.toUpperCase());
        doc.setProjet(projet);
        documentService.save(doc);

        return ResponseEntity.ok(ApiResponseDTO.success(documentMapper.toDocumentDto(doc))
                .message("Document uploadé avec succès"));
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Lister les documents d'un projet",
        description = "Retourne la liste des documents attachés à un projet. Accessible uniquement à l'administrateur, au porteur du projet et aux investisseurs ayant une participation sur ce projet.",
        tags = {"Documents"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — non investisseur, non porteur, non admin",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<List<DocumentDTO>>> getDocumentsByProjet(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            Authentication auth) {

        User user = userRepository.findByLoginForAuth(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!documentService.hasAccessToProject(user, projetId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponseDTO
                            .error("Seuls les investisseurs, le porteur ou l'admin peuvent consulter ces documents."));
        }

        List<DocumentDTO> docs = documentMapper.toDocumentDtoList(
                documentService.findByProjetId(projetId));

        return ResponseEntity.ok(ApiResponseDTO.success(docs));
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Télécharger un document",
        description = "Télécharge un document projet. Le type MIME est déterminé automatiquement selon le type du document (PDF, Excel, CSV). Accès restreint : admin, porteur ou investisseur du projet.",
        tags = {"Documents"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fichier téléchargé",
            content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Document introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @Parameter(description = "Identifiant du document", example = "9", required = true)
            @PathVariable Long documentId,
            Authentication authentication) throws IOException {

        User user = userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Document doc = documentService.findById(documentId);

        if (!documentService.hasAccessToProject(user, doc.getProjet().getId())) {
            return ResponseEntity.status(403).build();
        }

        byte[] data = fileStorageService.loadDocumentAsBytes(doc.getFilename());
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
}
