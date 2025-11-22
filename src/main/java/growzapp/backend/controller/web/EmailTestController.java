// src/main/java/growzapp/backend/controller/web/EmailTestController.java
package growzapp.backend.controller.web;

import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.ContratService;
import growzapp.backend.service.EmailSenderService;
import growzapp.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;
    private final ContratService contratService;
    private final InvestissementRepository investissementRepository;
    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;

    @GetMapping("/test-email")
    public String testEmailSimple(RedirectAttributes redirectAttributes) {
        try {
            emailService.envoyerEmailTestDirect("losdiakite@gmail.com");
            redirectAttributes.addFlashAttribute("successMessage", "Email simple envoyé !");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Échec : " + e.getMessage());
        }
        return "redirect:/dividendes";
    }

    @GetMapping("/test-contrat-email")
    public String testEmailContrat(RedirectAttributes redirectAttributes) {
        try {
            // --- Création des entités ---
            User investisseur = new User();
            investisseur.setPrenom("Moussa");
            investisseur.setNom("Diakité");
            investisseur.setEmail("losdiakite@gmail.com");
            investisseur.setLogin("test_invest_" + System.currentTimeMillis());
            investisseur = userRepository.save(investisseur);

            User porteur = new User();
            porteur.setEmail("porteur@growzapp.com");
            porteur.setLogin("test_porteur_" + System.currentTimeMillis());
            porteur = userRepository.save(porteur);

            Projet projet = new Projet();
            projet.setLibelle("Ferme Bio du Sud");
            projet.setPrixUnePart(500.0);
            projet.setPorteur(porteur);

            Investissement inv = new Investissement();
            inv.setNombrePartsPris(3);
            inv.setDate(LocalDateTime.now());
            inv.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
            inv.setInvestisseur(investisseur);
            inv.setProjet(projet);
            inv = investissementRepository.save(inv);

            // --- Générer contrat ---
            Contrat contrat = contratService.genererEtSauvegarderContrat(inv);
            inv.setContrat(contrat);
            inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);

            // --- Générer PDF ---
            byte[] pdf = contratService.genererPdfDepuisContrat(contrat);

            // --- ENVOI EMAIL ASYNCHRONE DÉTACHÉ ---
            emailSenderService.sendContratEmail(inv, pdf);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Contrat généré – Email envoyé en arrière-plan !");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Échec : " + e.getMessage());
        }
        return "redirect:/dividendes";
    }

    @GetMapping("/test-smtp")
    @ResponseBody
    public String testSMTP() {
        emailService.envoyerEmailTestDirect("losdiakite@gmail.com");
        return "Test SMTP envoyé – Vérifie ta boîte mail + console !";
    }
}