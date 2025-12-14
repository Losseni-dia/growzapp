package growzapp.backend.controller.web;

import com.lowagie.text.DocumentException;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.service.FactureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class FactureWebController {

    private final FactureService factureService;

    @GetMapping("/factures/dividende/{dividendeId}/generer")
    public String genererFacture(
            @PathVariable Long dividendeId,
            RedirectAttributes redirectAttributes) throws Exception {

        try {
            factureService.genererFactureEtEnvoyerEmailAsync(dividendeId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Facture générée avec succès !" +
                            " Vous recevrez un email sous peu."  );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erreur lors de la génération de la facture : " + e.getMessage());
        }

        return "redirect:/dividendes/" + dividendeId;
    }
}