package growzapp.backend.module.wallet.repository;

import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        // =========================================================================
        // MÉTHODES DE RECHERCHE PAR RÉFÉRENCE (POUR WEBHOOKS ET LIENS INTERNES)
        // =========================================================================

        Optional<Transaction> findByReferenceExterne(String referenceExterne);

        Optional<Transaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

        // =========================================================================
        // MÉTHODES D'HISTORIQUE ET FILTRAGE
        // =========================================================================

        @EntityGraph(attributePaths = { "destinataireWallet.user" })
        @Query("SELECT t FROM Transaction t WHERE t.walletType = 'USER' AND t.walletId = :walletId ORDER BY t.createdAt DESC")
        List<Transaction> findByWalletTypeAndWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId);

        @Query("SELECT t FROM Transaction t WHERE t.walletType = :type AND t.walletId = :id ORDER BY t.createdAt DESC")
        List<Transaction> findByWalletTypeAndWalletId(
                        @Param("type") WalletType type,
                        @Param("id") Long id);

        // HISTORIQUE USER (méthode simple)
        default List<Transaction> findByUserWalletId(Long walletId) {
                return findByWalletTypeAndWalletId(WalletType.USER, walletId);
        }

        // HISTORIQUE PROJET (méthode simple)
        default List<Transaction> findByProjetWalletId(Long walletId) {
                return findByWalletTypeAndWalletId(WalletType.PROJET, walletId);
        }

        // Toutes les transactions (admin)
        List<Transaction> findAllByOrderByCreatedAtDesc();

        @EntityGraph(attributePaths = { "destinataireWallet" })
        @Query("""
                    SELECT t FROM Transaction t
                    WHERE (:type IS NULL OR t.type = :type)
                      AND (:walletType IS NULL OR t.walletType = :walletType)
                      AND (:statut IS NULL OR t.statut = :statut)
                      AND (CAST(:search AS string) IS NULL OR :search = '' OR
                           LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                           LOWER(t.referenceExterne) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                    ORDER BY t.createdAt DESC
                """)
        Page<Transaction> findAdmin(
                        @Param("type") TypeTransaction type,
                        @Param("walletType") WalletType walletType,
                        @Param("statut") StatutTransaction statut,
                        @Param("search") String search,
                        Pageable pageable);
}