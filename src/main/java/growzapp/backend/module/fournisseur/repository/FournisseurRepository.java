package growzapp.backend.module.fournisseur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import growzapp.backend.module.fournisseur.enums.StatutFournisseur;
import growzapp.backend.module.fournisseur.model.Fournisseur;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByUserId(Long userId);

    List<Fournisseur> findByStatutOrderByDateSoumissionDesc(StatutFournisseur statut);

    List<Fournisseur> findByStatut(StatutFournisseur statut);

    @Query("SELECT f FROM Fournisseur f WHERE f.statut = growzapp.backend.module.fournisseur.enums.StatutFournisseur.VALIDE "
            + "AND (:ville IS NULL OR LOWER(f.ville) = LOWER(:ville)) "
            + "AND (:pays IS NULL OR LOWER(f.pays) = LOWER(:pays)) "
            + "AND (:secteurId IS NULL OR f.secteur.id = :secteurId)")
    List<Fournisseur> rechercher(@Param("ville") String ville, @Param("pays") String pays,
            @Param("secteurId") Long secteurId);
}
