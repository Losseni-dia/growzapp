package growzapp.backend.module.facture.controller;

import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.facture.service.FactureService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@Tag(name = "Factures", description = "Consultation et téléchargement des factures de dividendes au format PDF")
public class FactureRestController {

    private final FactureService factureService;

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'une facture",
        description = "Retourne les métadonnées d'une facture par son identifiant (montant HT, TTC, statut, dates, URL du PDF).",
        tags = {"Factures"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Facture trouvée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Facture introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<FactureDTO> getById(
            @Parameter(description = "Identifiant de la facture", example = "22", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(factureService.getById(id));
    }

    @GetMapping("/{factureId}/download")
    @Operation(
        summary = "Télécharger une facture PDF",
        description = "Télécharge la facture au format PDF. Si le fichier existe sur le disque et que la langue est 'fr', il est servi directement. Sinon, le PDF est régénéré dynamiquement dans la langue demandée.",
        tags = {"Factures"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF de la facture retourné",
            content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "404", description = "Facture introuvable ou PDF non générable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<Resource> downloadFacture(
            @Parameter(description = "Identifiant de la facture", example = "22", required = true)
            @PathVariable Long factureId,
            @Parameter(description = "Langue du document PDF généré", example = "fr",
                schema = @Schema(allowableValues = {"fr", "en", "es"}))
            @RequestParam(name = "lang", defaultValue = "fr") String lang) {
        try {
            byte[] pdfBytes = factureService.genererPdf(factureId, lang);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"facture-" + factureId + "_" + lang + ".pdf\"")
                    .body(new ByteArrayResource(pdfBytes));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}
