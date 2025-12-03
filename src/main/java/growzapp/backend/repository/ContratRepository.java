package growzapp.backend.repository;

import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.enumeration.StatutPartInvestissement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <-- AJOUTE CETTE INTERFACE
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContratRepository
        extends JpaRepository<Contrat, Long>,
        JpaSpecificationExecutor<Contrat> { // <-- C'EST ÇA QUI MANQUAIT !

    List<Contrat> findByInvestissement(Investissement investissement);

    long count();

    Optional<Contrat> findByNumeroContrat(String numeroContrat);

   // src/main/java/growzapp/backend/repository/ContratRepository.java

   @Query("""
               SELECT c FROM Contrat c
            JOIN c.investissement i
            JOIN i.projet p
            JOIN i.investisseur u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(c.numeroContrat) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(p.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:dateDebut IS NULL OR c.dateGeneration >= :dateDebut)
              AND (:dateFin IS NULL OR c.dateGeneration <= :dateFin)
              AND (:statut IS NULL OR i.statutPartInvestissement = :statut)
              AND (:montantMin IS NULL OR i.montantInvesti >= :montantMin)
              AND (:montantMax IS NULL OR i.montantInvesti <= :montantMax)
           """)
   Page<Contrat> rechercherAvecFiltres(
           @Param("search") String search,
           @Param("dateDebut") LocalDateTime dateDebut,
           @Param("dateFin") LocalDateTime dateFin,
           @Param("statut") StatutPartInvestissement statut,
           @Param("montantMin") Integer montantMin,
           @Param("montantMax") Integer montantMax,
           Pageable pageable);
}