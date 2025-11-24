package growzapp.backend.repository;

import growzapp.backend.model.entite.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** Trouve le wallet par ID utilisateur (sans verrou) */
    Optional<Wallet> findByUserId(Long userId);

    /**
     * Charge le wallet avec verrou pessimiste (PESSIMISTIC_WRITE)
     * Essentiel pour éviter les conditions de course sur les soldes
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}