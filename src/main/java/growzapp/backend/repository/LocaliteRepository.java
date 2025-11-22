package growzapp.backend.repository;

import growzapp.backend.model.entite.Localite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocaliteRepository extends JpaRepository<Localite, Long> {

    List<Localite> findByPaysId(Long paysId);

    List<Localite> findByNomContainingIgnoreCase(String nom);

    Optional<Localite> findByNomIgnoreCaseAndPaysNomIgnoreCase(String nom, String paysNom);

    Optional<Localite> findByNomIgnoreCaseAndPaysId(String nom, Long paysId);
    
    Optional<Localite> findByNomIgnoreCase(String nom);
}