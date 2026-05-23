package growzapp.backend.module.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import growzapp.backend.module.user.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;

@Component
@RequiredArgsConstructor
public class TokenCleanupService {

    private final PasswordResetTokenRepository tokenRepo;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void removeExpiredTokens() {
        tokenRepo.deleteAllExpiredBefore(java.time.LocalDateTime.now());
    }
}
