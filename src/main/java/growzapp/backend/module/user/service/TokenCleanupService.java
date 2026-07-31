package growzapp.backend.module.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import growzapp.backend.module.user.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupService {

    private final PasswordResetTokenRepository tokenRepo;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void removeExpiredTokens() {
        // JAVA-05 : logging ajouté — sans ça, un échec silencieux de cette
        // tâche n'était jamais visible, la table pouvait grossir
        // indéfiniment sans aucune alerte.
        try {
            tokenRepo.deleteAllExpiredBefore(java.time.LocalDateTime.now());
            log.info("Nettoyage des tokens expirés exécuté avec succès");
        } catch (Exception e) {
            log.error("Échec du nettoyage des tokens expirés", e);
        }
    }
}