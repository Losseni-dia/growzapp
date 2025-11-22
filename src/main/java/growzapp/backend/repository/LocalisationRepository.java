package growzapp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Localisation;

@Repository
public interface LocalisationRepository extends JpaRepository<Localisation, Long> {
    List<Localisation> findByLocaliteId(Long localiteId);

    Optional<Localisation> findByNomIgnoreCase(String trim);
}
    

