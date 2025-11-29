package growzapp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.enumeration.StatutProjet;
import jakarta.persistence.LockModeType;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    List<Projet> findBySecteurId(Long secteurId);

    List<Projet> findBySiteProjetId(Long siteId);

    List<Projet> findByPorteurId(Long id);

    long countByPorteurId(Long porteurId);

    List<Projet> findByStatutProjet(StatutProjet statut);

    // RECHERCHE ADMIN (celle qui marche à tous les coups)
    @Query("SELECT p FROM Projet p " +
            "LEFT JOIN p.porteur porteur " +
            "WHERE LOWER(p.libelle) LIKE LOWER(:search) " +
            "   OR LOWER(p.description) LIKE LOWER(:search) " +
            "   OR LOWER(porteur.nom) LIKE LOWER(:search) " +
            "   OR LOWER(porteur.prenom) LIKE LOWER(:search)")
    List<Projet> findBySearchTerm(@Param("search") String search); // ← "search" partout !



    // Dans ProjetRepository.java

    @Query("SELECT p FROM Projet p WHERE p.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Projet> findByIdWithLock(@Param("id") Long id);


    


}