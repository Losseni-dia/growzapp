package growzapp.backend.module.employe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.employe.model.Fonction;

import java.util.List;
import java.util.Optional;

@Repository
public interface FonctionRepository extends JpaRepository<Fonction, Long> {
    Optional<Fonction> findByNomIgnoreCase(String nom);

    List<Fonction> findByNomContainingIgnoreCase(String nom);
}
