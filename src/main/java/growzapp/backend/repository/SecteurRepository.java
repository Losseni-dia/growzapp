package growzapp.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Secteur;

@Repository
public interface SecteurRepository extends JpaRepository<Secteur, Long> {

    Optional<Secteur> findByNomIgnoreCase(String trim);

    
}
