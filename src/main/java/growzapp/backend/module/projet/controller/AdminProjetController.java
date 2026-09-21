package growzapp.backend.module.projet.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import growzapp.backend.module.document.service.DocumentService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.dto.RevalorisationRequestDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/projets", "/api/admin/projets"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Projets", description = "Gestion complète des projets de financement par l'administrateur : liste, modification, changement de statut et suppression")
public class AdminProjetController {

   private final ProjetService projetService;
    private final ProjetMapper projetMapper;
    private final DeepLTranslationService deepLTranslationService;
    private final ProjetTraductionRepository traductionRepository;
    private final DocumentService documentService;
    
  

    @PostMapping("/admin/traduire-tous")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Traduire tous les projets existants", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponseDTO<String> traduireTousLesProjets() {
        List<Projet> projets = projetService.getAllAdmin(null);
        int count = 0;
        for (Projet projet : projets) {
            try {
                deepLTranslationService.traduireProjet(projet);
                count++;
            } catch (Exception e) {
                log.warn("Erreur traduction projet {} : {}", projet.getId(), e.getMessage());
            }
        }
        return ApiResponseDTO.<String>success(null)
                .message(count + " projets traduits avec succès");
    }

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
            @Parameter(description = "Terme de recherche optionnel (libellé, description)", example = "solaire") @RequestParam(required = false) String search,
            @Parameter(description = "Langue de traduction souhaitée", example = "es") @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<Projet> entities = projetService.getAllAdmin(search);
        List<ProjetDTO> dtos = projetMapper.toDtoList(entities);

        // Badge admin : nombre de documents en attente de validation par projet
        dtos.forEach(dto -> dto.setDocumentsEnAttente(documentService.countEnAttenteByProjetId(dto.getId())));

        List<ProjetDTO> traduits = applyTraductionsAdmin(dtos, langue);
        return ApiResponseDTO.success(traduits)
                .message(traduits.isEmpty() ? "Aucun projet trouvé" : "Projets récupérés avec succès");
    }

    @GetMapping("/porteur/{porteurId}")
    @Operation(
        summary = "Lister les projets d'un porteur donné",
        description = "Retourne tous les projets (tous statuts confondus) d'un porteur — utilisé notamment pour choisir quels projets mettre en avant dans sa fiche de présentation.",
        tags = {"Admin - Projets"}
    )
    public ApiResponseDTO<List<ProjetDTO>> getByPorteur(
            @Parameter(description = "Identifiant du porteur", example = "42") @PathVariable Long porteurId) {
        List<ProjetDTO> dtos = projetMapper.toDtoList(projetService.getProjetsPubliesByPorteurId(porteurId));
        return ApiResponseDTO.success(dtos);
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
            @RequestPart(value = "poster", required = false) MultipartFile poster,

            @Parameter(description = "Photos additionnelles à ajouter à la galerie (optionnel, s'ajoutent aux existantes)")
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        Projet saved = projetService.updateFull(id, dto, poster, photos);
        return ApiResponseDTO.success(projetMapper.toDto(saved));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @Operation(summary = "Supprimer une photo de la galerie d'un projet", tags = {"Admin - Projets"})
    public ApiResponseDTO<String> supprimerPhoto(
            @PathVariable Long id,
            @PathVariable Long photoId) {
        projetService.supprimerPhoto(id, photoId);
        return ApiResponseDTO.<String>success(null).message("Photo supprimée");
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

    @PostMapping("/{id}/premium/revoquer")
    @Operation(summary = "Révoquer le statut Premium d'un projet", description = "Désactive immédiatement le Premium (utile en cas d'abus ou de projet clôturé). Aucun remboursement automatique.", tags = {"Admin - Projets"})
    public ApiResponseDTO<String> revoquerPremium(@PathVariable Long id) {
        projetService.revoquerPremium(id);
        return ApiResponseDTO.<String>success(null).message("Statut Premium révoqué");
    }

    @GetMapping("/premium/en-attente")
    @Operation(summary = "Lister les achats Premium bloqués en attente de paiement", description = "Cas où le webhook FedaPay/PayDunya n'a jamais atteint le backend (tunnel fermé, dashboard mal configuré...).", tags = {"Admin - Projets"})
    public ApiResponseDTO<List<growzapp.backend.module.projet.dto.PremiumEnAttenteDTO>> getPremiumEnAttente() {
        List<growzapp.backend.module.projet.dto.PremiumEnAttenteDTO> dtos = projetService.getPremiumEnAttente().stream()
                .map(tx -> {
                    String libelle = "Projet #" + tx.getReferenceId();
                    try {
                        libelle = projetService.getById(tx.getReferenceId()).getLibelle();
                    } catch (Exception ignored) {
                    }
                    return new growzapp.backend.module.projet.dto.PremiumEnAttenteDTO(
                            tx.getId(), tx.getReferenceId(), libelle, tx.getMontant(),
                            tx.getCreatedAt(), tx.getSourcePaiement() != null ? tx.getSourcePaiement().name() : null);
                })
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/premium/en-attente/{transactionId}/reconcilier")
    @Operation(summary = "Vérifier et régulariser un achat Premium bloqué", description = "Interroge directement FedaPay/PayDunya : active le Premium si le paiement a réellement été confirmé, sinon annule la trace en attente (aucune somme n'a été débitée du wallet interne dans ce flux).", tags = {"Admin - Projets"})
    public ApiResponseDTO<String> reconcilierPremiumEnAttente(@PathVariable Long transactionId) {
        String resultat = projetService.reconcilierPremiumEnAttente(transactionId);
        String message = "CONFIRME".equals(resultat)
                ? "Paiement confirmé — statut Premium activé"
                : "Paiement non confirmé — achat annulé";
        return ApiResponseDTO.success(resultat).message(message);
    }

    @GetMapping("/corbeille")
    @Operation(summary = "Lister les projets supprimés (corbeille)", tags = {"Admin - Projets"})
    public ApiResponseDTO<List<ProjetDTO>> getCorbeille() {
        List<Projet> entities = projetService.getArchived();
        return ApiResponseDTO.success(projetMapper.toDtoList(entities));
    }

    @PostMapping("/{id}/restaurer")
    @Operation(summary = "Restaurer un projet depuis la corbeille", tags = {"Admin - Projets"})
    public ApiResponseDTO<String> restaurer(@PathVariable Long id) {
        projetService.restaurer(id);
        return ApiResponseDTO.<String>success(null).message("Projet restauré");
    }

    @DeleteMapping("/{id}/purger")
    @Operation(
        summary = "Supprimer définitivement un projet",
        description = "Purge définitive et irréversible d'un projet déjà présent dans la corbeille.",
        tags = {"Admin - Projets"}
    )
    public ApiResponseDTO<String> purger(@PathVariable Long id) {
        projetService.purger(id);
        return ApiResponseDTO.<String>success(null).message("Projet supprimé définitivement");
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un projet (corbeille)",
        description = "Suppression logique (soft delete) : le projet est masqué des listes actives et du catalogue public, mais reste restaurable depuis la corbeille.",
        tags = {"Admin - Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projet déplacé dans la corbeille",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant du projet à supprimer", example = "7", required = true)
            @PathVariable Long id,
            @Parameter(description = "Motif de suppression (facultatif, conservé pour audit)")
            @RequestParam(required = false) String motif,
            org.springframework.security.core.Authentication authentication) {
        projetService.softDeleteById(id, authentication.getName(), motif);
        return new ApiResponseDTO<>(true, "Projet déplacé dans la corbeille", null);
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

       // ── Helper : appliquer traduction sur un ProjetDTO ───────────────────────
    private ProjetDTO applyTraductionAdmin(ProjetDTO dto, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return dto;

        Optional<ProjetTraductionProjection> traduction = traductionRepository
                .findProjectionByProjetIdAndLangue(dto.getId(), langue);

        traduction.ifPresent(t -> {
            if (t.getLibelle() != null && !t.getLibelle().isBlank())
                dto.setLibelleTradu(t.getLibelle());
            if (t.getDescription() != null && !t.getDescription().isBlank())
                dto.setDescriptionTradu(t.getDescription());
        });

        return dto;
    }

    @Operation(summary = "Revaloriser un projet", description = "Permet à un administrateur de mettre à jour manuellement la valorisation d'un projet et d'en garder un historique traçable.")
    @PatchMapping("/{id}/revaloriser")
    public ApiResponseDTO<ProjetDTO> revaloriser(
            @PathVariable Long id,
            @RequestBody RevalorisationRequestDTO request) {
        Projet projet = projetService.revaloriser(id, request.nouvelleValorisation(), request.motif());
        ProjetDTO dto = projetMapper.toDto(projet);
        return ApiResponseDTO.success(dto).message("Projet revalorisé avec succès");
    }

    @Operation(
        summary = "Recalculer le montant collecté d'un projet",
        description = "Recalcule montantCollecte et partsPrises à partir de la somme réelle des investissements VALIDE du projet, et corrige tout écart trouvé. Outil de diagnostic/réparation — ne touche jamais au wallet du projet.",
        tags = {"Admin - Projets"}
    )
    @PostMapping("/{id}/recalculer-collecte")
    public ApiResponseDTO<java.util.Map<String, Object>> recalculerMontantCollecte(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long id) {
        java.util.Map<String, Object> resultat = projetService.recalculerMontantCollecte(id);
        boolean ecart = Boolean.TRUE.equals(resultat.get("ecartDetecte"));
        return ApiResponseDTO.success(resultat)
                .message(ecart
                        ? "Écart détecté et corrigé"
                        : "Aucun écart — montantCollecte reflète déjà les investissements validés");
    }

    // ── Helper : appliquer traduction sur une liste ──────────────────────────
    private List<ProjetDTO> applyTraductionsAdmin(List<ProjetDTO> dtos, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return dtos;
        return dtos.stream()
                .map(dto -> applyTraductionAdmin(dto, langue))
                .collect(Collectors.toList());
    }
}
