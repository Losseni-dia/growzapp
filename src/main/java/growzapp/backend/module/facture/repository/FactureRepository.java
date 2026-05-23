package growzapp.backend.module.facture.repository;

import growzapp.backend.module.facture.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    boolean existsByDividendeId(Long dividendeId);

    @Query("SELECT COALESCE(MAX(f.id), 0) FROM Facture f")
    Long findMaxId();
}
