package growzapp.backend.module.projet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.LockModeType;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    List<Projet> findBySecteurId(Long secteurId);

    List<Projet> findBySiteProjetId(Long siteId);

    // Pour éviter les doublons lors de la création
    boolean existsBySlug(String slug);

    long countByPorteurId(Long porteurId);

    @Override
    @Query("SELECT p FROM Projet p LEFT JOIN FETCH p.porteur LEFT JOIN FETCH p.secteur WHERE p.supprimeLe IS NULL")
    List<Projet> findAll();

    @Query("SELECT p FROM Projet p LEFT JOIN FETCH p.porteur LEFT JOIN FETCH p.secteur WHERE p.supprimeLe IS NOT NULL")
    List<Projet> findArchived();

    @Query("SELECT p FROM Projet p " +
            "LEFT JOIN FETCH p.porteur " +
            "LEFT JOIN FETCH p.secteur " +
            "LEFT JOIN FETCH p.siteProjet site " +
            "LEFT JOIN FETCH site.localite " +
            "WHERE p.statutProjet = :statut AND p.supprimeLe IS NULL")
    List<Projet> findByStatutProjet(@Param("statut") StatutProjet statut);

    @Query("SELECT p FROM Projet p " +
            "LEFT JOIN FETCH p.porteur " +
            "LEFT JOIN FETCH p.secteur " +
            "LEFT JOIN FETCH p.siteProjet site " +
            "LEFT JOIN FETCH site.localite " +
            "WHERE p.slug = :slug AND p.supprimeLe IS NULL")
    Optional<Projet> findBySlug(@Param("slug") String slug);

    @Query("SELECT p FROM Projet p " +
            "LEFT JOIN FETCH p.porteur " +
            "LEFT JOIN FETCH p.secteur " +
            "LEFT JOIN FETCH p.siteProjet site " +
            "LEFT JOIN FETCH site.localite " +
            "WHERE p.porteur.id = :id AND p.supprimeLe IS NULL")
    List<Projet> findByPorteurId(@Param("id") Long id);

    @Query("SELECT p FROM Projet p " +
            "LEFT JOIN FETCH p.porteur porteur " +
            "LEFT JOIN FETCH p.secteur " +
            "LEFT JOIN FETCH p.siteProjet site " +
            "LEFT JOIN FETCH site.localite loc " +
            "LEFT JOIN FETCH loc.pays " +
            "WHERE (LOWER(p.libelle) LIKE LOWER(:search) " +
            "   OR LOWER(p.description) LIKE LOWER(:search) " +
            "   OR LOWER(porteur.nom) LIKE LOWER(:search) " +
            "   OR LOWER(porteur.prenom) LIKE LOWER(:search) " +
            "   OR LOWER(loc.nom) LIKE LOWER(:search)) " +
            "   AND p.supprimeLe IS NULL")
    List<Projet> findBySearchTerm(@Param("search") String search);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Projet p WHERE p.id = :id")
    Optional<Projet> findByIdWithLock(@Param("id") Long id);

    


}