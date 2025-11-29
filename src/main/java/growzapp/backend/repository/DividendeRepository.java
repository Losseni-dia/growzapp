package growzapp.backend.repository;

import growzapp.backend.model.entite.Dividende;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DividendeRepository extends JpaRepository<Dividende, Long> {



    // Pour récupérer les dividendes d’un investisseur

    // Optionnel : plus explicite
    @Query("SELECT d FROM Dividende d JOIN FETCH d.investissement i JOIN FETCH i.projet WHERE i.id = :investissementId")
    List<Dividende> findByInvestissementIdWithDetails(@Param("investissementId") Long investissementId);


    List<Dividende> findByInvestissement_Investisseur_Id(Long investisseurId);

    @Query("SELECT d FROM Dividende d WHERE d.investissement.id = :investissementId")
    List<Dividende> findByInvestissementId(@Param("investissementId") Long investissementId);


    @Query("SELECT d FROM Dividende d WHERE d.investissement.projet.id = :projetId ORDER BY d.datePaiement DESC")
    List<Dividende> findByInvestissement_Projet_Id(@Param("projetId") Long projetId);

}