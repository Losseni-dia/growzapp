package growzapp.backend.module.traduction.DeepL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.traduction.DeepL.model.ProjetTraduction;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjetTraductionRepository extends JpaRepository<ProjetTraduction, Long> {

    Optional<ProjetTraduction> findByProjetIdAndLangue(Long projetId, String langue);

    List<ProjetTraduction> findByProjetId(Long projetId);

    void deleteByProjetId(Long projetId);

    @Query("SELECT t.libelle as libelle, t.description as description FROM ProjetTraduction t WHERE t.projet.id = :projetId AND t.langue = :langue")
Optional<ProjetTraductionProjection> findProjectionByProjetIdAndLangue(
    @Param("projetId") Long projetId,
    @Param("langue") String langue
);
}