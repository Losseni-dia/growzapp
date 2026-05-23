package growzapp.backend.module.projet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Projets", description = "Gestion complète des projets de financement par l'administrateur : liste, modification, changement de statut et suppression")
public class AdminProjetController {

    private final ProjetService projetService;
    private final ProjetMapper projetMapper;
    private final FileUploadService fileUploadService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(
        summary = "Lister tous les projets",
        description = "Retourne la liste complète des projets, tous statuts confondus. Supporte un filtre de recherche par libellé ou description.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des projets retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<ProjetDTO>> getAll(
            @Parameter(description = "Terme de recherche optionnel (libellé, description)", example = "solaire")
            @RequestParam(required = false) String search) {
        List<Projet> entities = projetService.getAllAdmin(search);
        List<ProjetDTO> dtos = projetMapper.toDtoList(entities);
        return ApiResponseDTO.success(dtos)
                .message(dtos.isEmpty() ? "Aucun projet trouvé" : "Projets récupérés avec succès");
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'un projet",
        description = "Retourne le détail complet d'un projet par son identifiant.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projet trouvé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<ProjetDTO> getOne(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long id) {
        Projet entity = projetService.getById(id);
        return ApiResponseDTO.success(projetMapper.toDto(entity));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Modifier un projet",
        description = "Met à jour un projet existant. Le champ 'projet' contient les données JSON (ProjetCreateDTO), 'poster' est l'image optionnelle.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projet mis à jour avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<ProjetDTO> update(
            @Parameter(description = "Identifiant du projet à modifier", example = "7", required = true)
            @PathVariable Long id,

            @Parameter(description = "Données du projet au format JSON (ProjetCreateDTO sérialisé)",
                schema = @Schema(implementation = ProjetCreateDTO.class))
            @RequestPart("projet") ProjetCreateDTO dto,

            @Parameter(description = "Nouvelle affiche du projet (optionnel)",
                schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "poster", required = false) MultipartFile poster) {
        Projet saved = projetService.updateFull(id, dto, poster);
        return ApiResponseDTO.success(projetMapper.toDto(saved));
    }

    @PatchMapping("/{id}/statut")
    @Operation(
        summary = "Changer le statut d'un projet",
        description = "Modifie le statut d'un projet. Valeurs possibles : EN_PREPARATION, SOUMIS, VALIDE, REJETE, EN_COURS, TERMINE, EN_ATTENTE.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut mis à jour",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<ProjetDTO> changerStatut(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Nouveau statut du projet",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = StatutProjet.class,
                        example = "\"VALIDE\"")))
            @RequestBody StatutProjet nouveauStatut) {
        Projet updated = projetService.changerStatut(id, nouveauStatut);
        return ApiResponseDTO.success(projetMapper.toDto(updated));
    }

    @PatchMapping("/{id}/valider")
    @Operation(
        summary = "Valider un projet",
        description = "Raccourci pour passer le statut d'un projet à VALIDE. Équivalent à PATCH /{id}/statut avec la valeur VALIDE.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projet validé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<ProjetDTO> valider(
            @Parameter(description = "Identifiant du projet à valider", example = "7", required = true)
            @PathVariable Long id) {
        return changerStatut(id, StatutProjet.VALIDE);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un projet",
        description = "Supprime définitivement un projet et toutes ses données associées. Action irréversible.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projet supprimé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant du projet à supprimer", example = "7", required = true)
            @PathVariable Long id) {
        projetService.deleteById(id);
        return new ApiResponseDTO<>(true, "Projet supprimé avec succès", null);
    }

    private void updateEntityFromNode(Projet p, JsonNode node) {
        if (node.has("libelle"))
            p.setLibelle(node.get("libelle").asText());
        if (node.has("description"))
            p.setDescription(node.get("description").asText());
        if (node.has("objectifFinancement"))
            p.setObjectifFinancement(node.get("objectifFinancement").decimalValue());
        if (node.has("prixUnePart"))
            p.setPrixUnePart(node.get("prixUnePart").decimalValue());
        if (node.has("partsDisponible"))
            p.setPartsDisponible(node.get("partsDisponible").asInt());
        if (node.has("roiProjete"))
            p.setRoiProjete(node.get("roiProjete").asDouble());
    }
}
