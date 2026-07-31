package growzapp.backend.module.facture.repository;

import growzapp.backend.module.facture.dto.FactureAdminDTO;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.facture.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    boolean existsByDividendeId(Long dividendeId);

    @Query("SELECT COALESCE(MAX(f.id), 0) FROM Facture f")
    Long findMaxId();

    java.util.Optional<Facture> findByFichierUrlContaining(String filename);

    @Query("""
                SELECT NEW growzapp.backend.module.facture.dto.FactureAdminDTO(
                    f.id, f.numeroFacture, f.montantHT, f.montantTTC, f.statut,
                    f.dateEmission, f.datePaiement,
                    CONCAT(u.prenom, ' ', u.nom), u.email)
                FROM Facture f
                JOIN f.investisseur u
                WHERE (CAST(:search AS string) IS NULL OR :search = '' OR
                       LOWER(f.numeroFacture) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                       LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                       LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                  AND (:statut IS NULL OR f.statut = :statut)
                ORDER BY f.dateEmission DESC
            """)
    Page<FactureAdminDTO> rechercherAdminDTO(
            @Param("search") String search,
            @Param("statut") StatutFacture statut,
            Pageable pageable);
}
