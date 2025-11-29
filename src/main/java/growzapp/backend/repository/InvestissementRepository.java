// src/main/java/growzapp/backend/repository/InvestissementRepository.java
// AJOUT DE LA MÉTHODE MANQUANTE → tout compile maintenant

package growzapp.backend.repository;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvestissementRepository extends JpaRepository<Investissement, Long> {

  @Query("SELECT i FROM Investissement i JOIN FETCH i.projet p JOIN FETCH i.investisseur u")
  List<Investissement> findAllWithDetails();

  @Query("SELECT i FROM Investissement i WHERE i.investisseur.id = :investisseurId")
  List<Investissement> findByInvestisseurId(@Param("investisseurId") Long investisseurId);

  long countByInvestisseurId(Long investisseurId);

  @Query("""
      SELECT DISTINCT i FROM Investissement i
      LEFT JOIN FETCH i.dividendes
      WHERE i.investisseur.id = :investisseurId
      ORDER BY i.date DESC
      """)
  List<Investissement> findByInvestisseurIdWithDividendes(@Param("investisseurId") Long investisseurId);

  @Query("SELECT i FROM Investissement i WHERE i.projet.id = :projetId")
  List<Investissement> findByProjetId(@Param("projetId") Long projetId);

  @Query("SELECT i FROM Investissement i " +
      "LEFT JOIN i.investisseur u " +
      "LEFT JOIN i.projet p " +
      "WHERE LOWER(u.login) LIKE LOWER(:term) " +
      "   OR LOWER(u.prenom) LIKE LOWER(:term) " +
      "   OR LOWER(u.nom) LIKE LOWER(:term) " +
      "   OR LOWER(p.libelle) LIKE LOWER(:term)")
  List<Investissement> findBySearchTerm(@Param("term") String term);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Investissement i WHERE i.id = :id")
  Optional<Investissement> findByIdWithLock(@Param("id") Long id);

  // MÉTHODE MANQUANTE → AJOUTÉE ICI
  @Query("""
      SELECT i FROM Investissement i
      WHERE i.date >= :debut
        AND i.date <= :fin
        AND i.statutPartInvestissement = :statut
      """)
  List<Investissement> findValidInvestmentsByDateRange(
      @Param("debut") LocalDateTime debut,
      @Param("fin") LocalDateTime fin,
      @Param("statut") StatutPartInvestissement statut);
}