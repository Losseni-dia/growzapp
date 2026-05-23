package growzapp.backend.module.referentiel.repository;

import growzapp.backend.module.referentiel.model.Secteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecteurRepository extends JpaRepository<Secteur, Long> {

    Optional<Secteur> findByNomIgnoreCase(String nom);
}
