package growzapp.backend.module.traduction.DeepL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.traduction.DeepL.model.FicheBioTraduction;

import java.util.Optional;

@Repository
public interface FicheBioTraductionRepository extends JpaRepository<FicheBioTraduction, Long> {

    Optional<FicheBioTraduction> findByUserIdAndLangue(Long userId, String langue);

    void deleteByUserId(Long userId);
}
