package growzapp.backend.module.fournisseur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.fournisseur.enums.StatutCommande;
import growzapp.backend.module.fournisseur.model.Commande;

public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByStatutOrderByDateCommandeDesc(StatutCommande statut);

    List<Commande> findByProjetPorteurIdOrderByDateCommandeDesc(Long porteurId);

    List<Commande> findByFournisseurIdOrderByDateCommandeDesc(Long fournisseurId);

    List<Commande> findByProjetIdOrderByDateCommandeDesc(Long projetId);

    List<Commande> findAllByOrderByDateCommandeDesc();
}
