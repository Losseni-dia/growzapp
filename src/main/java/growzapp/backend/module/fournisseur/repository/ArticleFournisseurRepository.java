package growzapp.backend.module.fournisseur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.fournisseur.model.ArticleFournisseur;

public interface ArticleFournisseurRepository extends JpaRepository<ArticleFournisseur, Long> {

    List<ArticleFournisseur> findByFournisseurIdOrderByCreatedAtDesc(Long fournisseurId);

    List<ArticleFournisseur> findByFournisseurIdAndDisponibleTrueOrderByCreatedAtDesc(Long fournisseurId);
}
