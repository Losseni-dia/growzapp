package growzapp.backend.module.fournisseur.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.fournisseur.enums.StatutFournisseur;
import growzapp.backend.module.fournisseur.model.Fournisseur;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByUserId(Long userId);

    List<Fournisseur> findByStatutOrderByDateSoumissionDesc(StatutFournisseur statut);

    List<Fournisseur> findByStatut(StatutFournisseur statut);
}
