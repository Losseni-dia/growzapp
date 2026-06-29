package growzapp.backend.module.investissement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Tag(name = "Admin - Investissements", description = "Gestion avancée des investissements par l'administrateur")
public class AdminInvestissementController {

    private final InvestissementRepository investissementRepository;
    private final PdfReactService pdfReactService;
    private final InvestissementService investissementService;
    private final EmailService emailService;
    private final ContratService contratService;

    @GetMapping
    @Operation(summary = "Lister tous les investissements", tags = { "Admin - Investissements" })
    public ApiResponseDTO<List<InvestissementDTO>> getAll(
            @Parameter(description = "Terme de recherche optionnel") @RequestParam(required = false) String search) {
        List<InvestissementDTO> investissements = investissementService.getAllAdmin(search);
        return ApiResponseDTO.success(investissements);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un investissement", tags = { "Admin - Investissements" })
    public ApiResponseDTO<InvestissementDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }

    @PostMapping("/{id}/valider-et-envoyer")
    @Operation(summary = "Valider l'investissement et envoyer le contrat", tags = { "Admin - Investissements" })
    public ResponseEntity<?> validerEtEnvoyer(@PathVariable Long id) {
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

    // ── ANNULER AVEC MOTIF ────────────────────────────────────────────────────
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Refuser un investissement avec motif", description = "Refuse l'investissement, restitue les fonds et notifie l'investisseur avec le motif.", tags = {
            "Admin - Investissements" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(example = "{\"motif\": \"Documents insuffisants\"}"))))
    public ApiResponseDTO<InvestissementDTO> annuler(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String motif = (body != null && body.containsKey("motif") && !body.get("motif").isBlank())
                ? body.get("motif")
                : "Refusé par l'administration";

        investissementService.annulerInvestissement(id, motif);
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement refusé — fonds restitués et investisseur notifié");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un investissement", tags = { "Admin - Investissements" })
    public ApiResponseDTO<String> delete(@PathVariable Long id) {
        investissementRepository.deleteById(id);
        return ApiResponseDTO.success("Investissement supprimé");
    }
}