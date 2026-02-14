package growzapp.backend.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.model.entite.PasswordResetToken;


public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByToken(String token);
}
