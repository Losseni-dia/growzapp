package growzapp.backend.module.projet.service;

import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Notifie une fois par jour (in-app + email) l'admin et le porteur de chaque
// projet dont l'échéance de financement approche (J-30) ou est dépassée —
// avant ce job, ces deux alertes n'étaient que des indicateurs visuels
// consultés à la demande sur les dashboards, jamais poussés activement.
// derniereAlerteEcheanceEnvoyeeLe évite de renotifier plusieurs fois le même
// jour si le job tourne ou est relancé plusieurs fois.
@Slf4j
@Component
@RequiredArgsConstructor
public class EcheanceFinancementAlerteScheduler {

    private final ProjetService projetService;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // Une fois par jour à 4h du matin — pas besoin de plus fréquent pour des
    // alertes qui se comptent en jours.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void alerterEcheancesDeFinancement() {
        try {
            List<Projet> echeanceProche = projetService.listeEcheanceProche();
            List<Projet> echeanceDepassee = projetService.listeEcheanceDepassee();

            int alertesEnvoyees = 0;
            for (Projet projet : echeanceProche) {
                if (alerterProjet(projet, false)) {
                    alertesEnvoyees++;
                }
            }
            for (Projet projet : echeanceDepassee) {
                if (alerterProjet(projet, true)) {
                    alertesEnvoyees++;
                }
            }
            log.info("alerterEcheancesDeFinancement : {} alerte(s) envoyée(s) ({} proche, {} dépassée)",
                    alertesEnvoyees, echeanceProche.size(), echeanceDepassee.size());
        } catch (Exception e) {
            log.error("Échec de la vérification des échéances de financement", e);
        }
    }

    // Retourne false sans rien faire si l'alerte a déjà été envoyée
    // aujourd'hui pour ce projet (idempotence si le job est relancé).
    private boolean alerterProjet(Projet projet, boolean depassee) {
        LocalDate aujourdHui = LocalDate.now();
        if (aujourdHui.equals(projet.getDerniereAlerteEcheanceEnvoyeeLe())) {
            return false;
        }

        List<User> admins = userRepository.findByRoles_Role("ADMIN");
        String titre = depassee
                ? "⏰ Échéance dépassée — " + projet.getLibelle()
                : "⏳ Échéance proche (30 jours) — " + projet.getLibelle();
        String contenuAdmin = depassee
                ? "Le projet n'a pas atteint son objectif de financement avant sa date limite."
                : "Le projet approche de sa date limite de financement sans avoir atteint son objectif.";

        notificationService.notifyAdmins(titre, contenuAdmin, "/admin/projets");
        for (User admin : admins) {
            emailService.envoyerAlerteEcheanceAdmin(admin.getEmail(), projet.getLibelle(), depassee);
        }

        if (projet.getPorteur() != null) {
            String contenuPorteur = depassee
                    ? "Votre projet « " + projet.getLibelle() + " » a dépassé sa date limite de financement "
                            + "sans avoir atteint son objectif."
                    : "Votre projet « " + projet.getLibelle() + " » approche de sa date limite de financement "
                            + "(moins de 30 jours) sans avoir encore atteint son objectif.";
            notificationService.notifyProjectOwner(projet.getPorteur(), titre, contenuPorteur, projet.getId());
            emailService.envoyerAlerteEcheanceProjet(
                    projet.getPorteur().getEmail(),
                    projet.getPorteur().getPrenom() + " " + projet.getPorteur().getNom(),
                    projet.getLibelle(),
                    depassee);
        }

        projet.setDerniereAlerteEcheanceEnvoyeeLe(aujourdHui);
        projetRepository.save(projet);
        return true;
    }
}
