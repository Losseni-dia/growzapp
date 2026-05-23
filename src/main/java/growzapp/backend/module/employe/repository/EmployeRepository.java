package growzapp.backend.module.employe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.employe.model.Employe;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByEmailIgnoreCase(String email);

    List<Employe> findByNomContainingIgnoreCase(String nom);
}