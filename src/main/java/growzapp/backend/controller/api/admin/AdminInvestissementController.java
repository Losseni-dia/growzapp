package growzapp.backend.controller.api.admin;


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


import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.service.ContratService;
import growzapp.backend.service.EmailService;
import growzapp.backend.service.InvestissementService;
import growzapp.backend.service.PdfReactService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/investissements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvestissementController {

    private final InvestissementRepository investissementRepository;
    private final PdfReactService pdfReactService;


    private final InvestissementService investissementService;
    private final EmailService emailService;
    private final ContratService contratService;

    @GetMapping
    public ApiResponseDTO<List<InvestissementDTO>> getAll(
            @RequestParam(required = false) String search) {

        List<InvestissementDTO> investissements = investissementService.getAllAdmin(search);
        return ApiResponseDTO.success(investissements);
    }


    @GetMapping("/{id}")
    public ApiResponseDTO<InvestissementDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }



    @PostMapping("/{id}/valider-et-envoyer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> validerEtEnvoyer(@PathVariable Long id) {
        try {
            Investissement inv = investissementService.validerInvestissement(id);

            // 1. Génère le contrat avec le vrai numéro
            Contrat contrat = contratService.genererEtSauvegarderContrat(inv);
            inv.setContrat(contrat);
            investissementRepository.saveAndFlush(inv); // flush obligatoire

            // 2. Génère le PDF avec le VRAI numéro (on passe contrat + inv)
            byte[] pdfFinal = pdfReactService.genererPdfAvecVraiContrat(contrat, inv);

            // 3. Sauvegarde le PDF final
            contratService.sauvegarderPdfFinal(contrat, pdfFinal);

            // 4. Envoie par email
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
    public ApiResponseDTO<InvestissementDTO> annuler(@PathVariable Long id) {
        investissementService.annulerInvestissement(id);
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement annulé");
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<String> delete(@PathVariable Long id) {
        investissementRepository.deleteById(id); // ou ton repo direct
        return ApiResponseDTO.success("Investissement supprimé");
    }
}