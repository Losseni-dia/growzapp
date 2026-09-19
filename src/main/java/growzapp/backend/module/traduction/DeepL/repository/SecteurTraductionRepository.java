package growzapp.backend.module.traduction.DeepL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.traduction.DeepL.model.SecteurTraduction;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecteurTraductionRepository extends JpaRepository<SecteurTraduction, Long> {

    Optional<SecteurTraduction> findBySecteurIdAndLangue(Long secteurId, String langue);

    List<SecteurTraduction> findBySecteurId(Long secteurId);
}
