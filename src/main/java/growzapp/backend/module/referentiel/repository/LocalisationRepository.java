package growzapp.backend.module.referentiel.repository;

import growzapp.backend.module.referentiel.model.Localisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalisationRepository extends JpaRepository<Localisation, Long> {

    List<Localisation> findByLocaliteId(Long localiteId);

    Optional<Localisation> findByNomIgnoreCase(String nom);
}
