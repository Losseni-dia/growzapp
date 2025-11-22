package growzapp.backend.controller.api;

import com.lowagie.text.DocumentException;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.service.FactureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureRestController {

    private final FactureService factureService;

    /**
     * Récupère une facture par ID
     */
    @GetMapping("/{id}")
    public ApiResponseDTO<FactureDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(factureService.getById(id));
    }

    /**
     * Génère automatiquement une facture PDF pour un dividende donné
     * Utilise ton FactureService actuel (PDF + stockage + base)
     */
    @PostMapping("/dividende/{dividendeId}/generer")
    public ResponseEntity<ApiResponseDTO<FactureDTO>> genererFacturePourDividende(
            @PathVariable Long dividendeId) {

        try {
            Facture facture = factureService.genererEtSauvegarderFacture(dividendeId);
            FactureDTO dto = factureService.toFactureDto(facture); // ou via DtoConverter

            return ResponseEntity.ok(
                    ApiResponseDTO.success(dto)
                            .message("Facture générée avec succès !") // ← message() après success()
            );

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponseDTO.error("Erreur d'entrée/sortie lors de la génération du PDF : " + e.getMessage()));
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponseDTO.error("Erreur lors de la création du PDF : " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponseDTO.error("Erreur inattendue : " + e.getMessage()));
        }
    }
}