package growzapp.backend.module.growzmarket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.growzmarket.model.ArticleMarket;

public interface ArticleMarketRepository extends JpaRepository<ArticleMarket, Long> {

    List<ArticleMarket> findByProjetIdOrderByCreatedAtDesc(Long projetId);

    List<ArticleMarket> findByProjetPorteurIdOrderByCreatedAtDesc(Long porteurId);

    List<ArticleMarket> findByDisponibleTrueOrderByCreatedAtDesc();
}
