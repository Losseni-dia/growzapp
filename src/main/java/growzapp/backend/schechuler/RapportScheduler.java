package growzapp.backend.schechuler;


import growzapp.backend.service.EmailService;
import growzapp.backend.service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class RapportScheduler {

    private final RapportService rapportService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 1 * ?") // Tous les 1er du mois à 9h00
    public void envoyerRapportMensuel() {
        YearMonth moisPrecedent = YearMonth.now().minusMonths(1);
        try {
            byte[] pdf = rapportService.genererRapportMensuel(moisPrecedent);

            // Envoie à l’équipe (ou admin)
            emailService.envoyerRapportMensuel("admin@growzapp.com", moisPrecedent, pdf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}