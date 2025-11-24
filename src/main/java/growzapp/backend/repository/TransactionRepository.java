package growzapp.backend.repository;

import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByWallet_UserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    // === NOUVELLE MÉTHODE POUR L'ADMIN : retraits en attente ===
    List<Transaction> findByTypeAndStatutOrderByCreatedAtDesc(
            TypeTransaction type,
            StatutTransaction statut
    );

    // OU (si tu veux une requête explicite, encore plus sûr)
    /*
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.type = :type " +
           "AND t.statut = :statut " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findPendingWithdrawals(
            @Param("type") TypeTransaction type,
            @Param("statut") StatutTransaction statut
    );
    */
}
