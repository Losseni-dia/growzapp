// src/main/java/growzapp/backend/controller/web/VerificationController.java
package growzapp.backend.controller.web;

import growzapp.backend.model.entite.Contrat;
import growzapp.backend.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class VerificationController {

    private final ContratRepository contratRepository;

    @GetMapping("/verifier-contrat")
    public String verifierContrat(@RequestParam("code") String numeroContrat, Model model) {

        Optional<Contrat> optionalContrat = contratRepository.findByNumeroContrat(numeroContrat);

        if (optionalContrat.isEmpty()) {
            model.addAttribute("valide", false);
            model.addAttribute("error", "Contrat non trouvé ou invalide.");
            return "verification/resultat";
        }

        Contrat contrat = optionalContrat.get();
        model.addAttribute("valide", true);
        model.addAttribute("contrat", contrat);
        model.addAttribute("investissement", contrat.getInvestissement());
        model.addAttribute("projet", contrat.getInvestissement().getProjet());
        model.addAttribute("user", contrat.getInvestissement().getInvestisseur());

        return "verification/resultat";
    }
}