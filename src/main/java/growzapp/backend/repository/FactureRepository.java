package growzapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Facture;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    boolean existsByDividendeId(Long dividendeId);
    
    @Query("SELECT COALESCE(MAX(f.id), 0) FROM Facture f")
    Long findMaxId();
    
}
