package growzapp.backend.repository;

import growzapp.backend.model.entite.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByEmailIgnoreCase(String email);

    List<Employe> findByNomContainingIgnoreCase(String nom);
}