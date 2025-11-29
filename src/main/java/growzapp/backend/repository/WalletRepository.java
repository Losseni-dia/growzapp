// src/main/java/growzapp/backend/repository/WalletRepository.java
// VERSION FINALE – TOUT EST LÀ

package growzapp.backend.repository;

import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithPessimisticLock(@Param("userId") Long userId);

    // MÉTHODES POUR LES WALLETS PROJET

    /** Trouve un wallet projet par ID du projet */
    Optional<Wallet> findByProjetId(Long projetId);

    /** Trouve un wallet projet + verrou pessimiste */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.projetId = :projetId")
    Optional<Wallet> findByProjetIdWithLock(@Param("projetId") Long projetId);

    /** Trouve un wallet projet + type (ultra-sécurisé) */
    Optional<Wallet> findByProjetIdAndWalletType(Long projetId, WalletType walletType);

    /** Trouve un wallet projet + type + verrou pessimiste */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.projetId = :projetId AND w.walletType = :type")
    Optional<Wallet> findByProjetIdAndWalletTypeWithLock(
            @Param("projetId") Long projetId,
            @Param("type") WalletType type);

    /** Liste tous les wallets d'un type donné */
    List<Wallet> findByWalletType(WalletType walletType);

    /** Liste tous les wallets projet avec verrou (utile pour admin) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletType = :type")
    List<Wallet> findByWalletTypeWithLock(@Param("type") WalletType type);

    // Bonus ultra-utile pour le dashboard admin
    @Query("SELECT COALESCE(SUM(w.soldeDisponible), 0) FROM Wallet w WHERE w.walletType = :type")
    BigDecimal sumSoldeDisponibleByWalletType(@Param("type") WalletType type);
}