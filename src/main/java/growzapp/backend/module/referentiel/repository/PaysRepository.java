package growzapp.backend.module.referentiel.repository;

import growzapp.backend.module.referentiel.model.Pays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaysRepository extends JpaRepository<Pays, Long> {

    Optional<Pays> findByNomIgnoreCase(String nom);
}
