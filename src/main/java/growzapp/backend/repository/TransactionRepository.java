// src/main/java/growzapp/backend/repository/TransactionRepository.java
// VERSION FINALE ULTIME – 100 % COMPATIBLE wallet_id + wallet_type

package growzapp.backend.repository;

import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.enumeration.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        // HISTORIQUE PERSONNEL (USER) – wallet_id + wallet_type
        @EntityGraph(attributePaths = { "destinataireWallet.user" })
        @Query("SELECT t FROM Transaction t WHERE t.walletType = 'USER' AND t.walletId = :walletId ORDER BY t.createdAt DESC")
        List<Transaction> findByWalletTypeAndWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId);

        // RETRAITS EN ATTENTE (ADMIN) – USER seulement
        @Query("SELECT t FROM Transaction t " +
                        "WHERE t.type = :type " +
                        "AND t.statut = :statut " +
                        "AND t.walletType = 'USER' " +
                        "ORDER BY t.createdAt DESC")
        List<Transaction> findByTypeAndStatutAndWalletType(
                        @Param("type") TypeTransaction type,
                        @Param("statut") StatutTransaction statut);

        // HISTORIQUE GÉNÉRIQUE (USER + PROJET + DIVIDENDE)
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
}