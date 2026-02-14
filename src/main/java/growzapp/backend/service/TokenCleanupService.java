package growzapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import growzapp.backend.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;

@Component
@RequiredArgsConstructor
public class TokenCleanupService {
    private final PasswordResetTokenRepository tokenRepo;

    @Scheduled(cron = "0 0 * * * *") // toutes les heures
    @Transactional
    public void removeExpiredTokens() {
        tokenRepo.findAll().stream()
                .filter(token -> token.getExpiryDate().isBefore(java.time.LocalDateTime.now()))
                .forEach(token -> tokenRepo.delete(token));
    }
}
