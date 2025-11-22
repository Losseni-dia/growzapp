package growzapp.backend.repository;

import growzapp.backend.model.entite.Fonction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FonctionRepository extends JpaRepository<Fonction, Long> {
    Optional<Fonction> findByNomIgnoreCase(String nom);

    List<Fonction> findByNomContainingIgnoreCase(String nom);
}
