package growzapp.backend.module.projet.repository;

import growzapp.backend.module.projet.model.ProjetValorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjetValorisationRepository extends JpaRepository<ProjetValorisation, Long> {

    @Query("SELECT v FROM ProjetValorisation v WHERE v.projet.id = :projetId ORDER BY v.dateSnapshot ASC")
    List<ProjetValorisation> findByProjetIdOrderByDateSnapshotAsc(@Param("projetId") Long projetId);
}