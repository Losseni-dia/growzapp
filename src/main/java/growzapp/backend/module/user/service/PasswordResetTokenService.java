package growzapp.backend.module.user.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import growzapp.backend.module.user.model.PasswordResetToken;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepo;

    @Transactional
    public PasswordResetToken createTokenForUser(User user) {
        tokenRepo.deleteByUser(user);
        tokenRepo.flush();

        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUser(user);
        prt.setExpiryDate(LocalDateTime.now().plusHours(1));
        return tokenRepo.save(prt);
    }

    public User validatePasswordResetToken(String token) {
        PasswordResetToken prt = tokenRepo.findByToken(token).orElse(null);
        if (prt == null || prt.isExpired()) {
            return null;
        }
        return prt.getUser();
    }

    @Transactional
    public void deleteToken(String token) {
        tokenRepo.findByToken(token).ifPresent(tokenRepo::delete);
    }
}
