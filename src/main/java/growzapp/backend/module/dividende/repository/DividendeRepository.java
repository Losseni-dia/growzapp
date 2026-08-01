package growzapp.backend.module.dividende.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import growzapp.backend.module.dividende.enums.StatutDividende;
import growzapp.backend.module.dividende.model.Dividende;

import java.util.List;

public interface DividendeRepository extends JpaRepository<Dividende, Long> {

    @Query("SELECT d FROM Dividende d " +
            "LEFT JOIN d.investissement i " +
            "LEFT JOIN i.projet p " +
            "LEFT JOIN i.investisseur u " +
            "WHERE (:statut IS NULL OR d.statutDividende = :statut) " +
            "AND (CAST(:search AS string) IS NULL OR :search = '' OR " +
            "     LOWER(p.libelle) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "     LOWER(u.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "     LOWER(u.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "ORDER BY d.datePaiement DESC NULLS LAST, d.id DESC")
    Page<Dividende> rechercherAdmin(
            @Param("search") String search,
            @Param("statut") StatutDividende statut,
            Pageable pageable);



    // Pour récupérer les dividendes d’un investisseur

    // Optionnel : plus explicite
    @Query("SELECT d FROM Dividende d JOIN FETCH d.investissement i JOIN FETCH i.projet WHERE i.id = :investissementId")
    List<Dividende> findByInvestissementIdWithDetails(@Param("investissementId") Long investissementId);


    List<Dividende> findByInvestissement_Investisseur_Id(Long investisseurId);



    @Query("SELECT d FROM Dividende d WHERE d.investissement.id = :investissementId")
    List<Dividende> findByInvestissementId(@Param("investissementId") Long investissementId);

    @Query("SELECT d FROM Dividende d WHERE d.investissement.projet.id = :projetId ORDER BY d.datePaiement DESC")
    List<Dividende> findByInvestissement_Projet_Id(@Param("projetId") Long projetId);

    // === CORRECTION ICI : Ajout de "LEFT" ===
    // Cela permet de voir le dividende même si la facture n'est pas encore générée
    @Query("SELECT d FROM Dividende d LEFT JOIN FETCH d.facture WHERE d.investissement.projet.id = :projetId")
    List<Dividende> findByInvestissement_Projet_IdWithFacture(@Param("projetId") Long projetId);

}