package growzapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Langue;


@Repository
public interface LangueRepository extends JpaRepository<Langue, Long> {
    
}
