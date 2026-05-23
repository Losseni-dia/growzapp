package growzapp.backend.module.investissement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.contrat.model.Contrat;
import growzapp.backend.module.contrat.service.ContratService;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.files.PdfReactService;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
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

@RestController
@RequestMapping("/api/admin/investissements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Investissements", description = "Gestion avancée des investissements par l'administrateur : validation avec génération de contrat PDF, annulation et suppression")
public class AdminInvestissementController {

    private final InvestissementRepository investissementRepository;
    private final PdfReactService pdfReactService;
    private final InvestissementService investissementService;
    private final EmailService emailService;
    private final ContratService contratService;

    @GetMapping
    @Operation(
        summary = "Lister tous les investissements",
        description = "Retourne la liste de tous les investissements, avec filtre de recherche optionnel par nom d'investisseur ou libellé de projet.",
        tags = {"Admin - Investissements"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des investissements",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<InvestissementDTO>> getAll(
            @Parameter(description = "Terme de recherche optionnel (nom investisseur, libellé projet)", example = "ferme")
            @RequestParam(required = false) String search) {

        List<InvestissementDTO> investissements = investissementService.getAllAdmin(search);
        return ApiResponseDTO.success(investissements);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'un investissement",
        description = "Retourne le détail complet d'un investissement par son identifiant.",
        tags = {"Admin - Investissements"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Investissement trouvé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Investissement introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> getById(
            @Parameter(description = "Identifiant de l'investissement", example = "15", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }

    @PostMapping("/{id}/valider-et-envoyer")
    @Operation(
        summary = "Valider l'investissement et envoyer le contrat",
        description = "Valide l'investissement, génère le contrat PDF avec numéro officiel, le sauvegarde et l'envoie par email à l'investisseur. Opération atomique.",
        tags = {"Admin - Investissements"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contrat validé et envoyé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"Contrat validé et envoyé avec succès\", \"numeroContrat\": \"CONTRAT-2025-00015\", \"lienVerification\": \"https://...\"}"))),
        @ApiResponse(responseCode = "500", description = "Erreur lors de la génération ou de l'envoi du contrat",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> validerEtEnvoyer(
            @Parameter(description = "Identifiant de l'investissement à valider", example = "15", required = true)
            @PathVariable Long id) {
        try {
            Investissement inv = investissementService.validerInvestissement(id);

            Contrat contrat = contratService.genererEtSauvegarderContrat(inv);
            inv.setContrat(contrat);
            investissementRepository.saveAndFlush(inv);

            byte[] pdfFinal = pdfReactService.genererPdfAvecVraiContrat(contrat, inv);

            contratService.sauvegarderPdfFinal(contrat, pdfFinal);

            emailService.envoyerContratParEmail(inv, pdfFinal);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Contrat validé et envoyé avec succès",
                    "numeroContrat", contrat.getNumeroContrat(),
                    "lienVerification", contrat.getLienVerification()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur : " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/annuler")
    @Operation(
        summary = "Annuler un investissement",
        description = "Annule un investissement et restitue les fonds à l'investisseur.",
        tags = {"Admin - Investissements"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Investissement annulé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Investissement introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> annuler(
            @Parameter(description = "Identifiant de l'investissement à annuler", example = "15", required = true)
            @PathVariable Long id) {
        investissementService.annulerInvestissement(id);
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement annulé");
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un investissement",
        description = "Supprime définitivement un investissement. Action irréversible.",
        tags = {"Admin - Investissements"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Investissement supprimé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Investissement introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> delete(
            @Parameter(description = "Identifiant de l'investissement à supprimer", example = "15", required = true)
            @PathVariable Long id) {
        investissementRepository.deleteById(id);
        return ApiResponseDTO.success("Investissement supprimé");
    }
}
