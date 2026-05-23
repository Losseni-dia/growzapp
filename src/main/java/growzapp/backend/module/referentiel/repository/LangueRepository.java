package growzapp.backend.module.referentiel.repository;

import growzapp.backend.module.referentiel.model.Langue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LangueRepository extends JpaRepository<Langue, Long> {
}
