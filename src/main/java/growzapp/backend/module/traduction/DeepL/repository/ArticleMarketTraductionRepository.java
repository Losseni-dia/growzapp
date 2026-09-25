package growzapp.backend.module.traduction.DeepL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.traduction.DeepL.model.ArticleMarketTraduction;
import growzapp.backend.module.traduction.DeepL.model.ArticleMarketTraductionProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleMarketTraductionRepository extends JpaRepository<ArticleMarketTraduction, Long> {

    Optional<ArticleMarketTraduction> findByArticleIdAndLangue(Long articleId, String langue);

    List<ArticleMarketTraduction> findByArticleId(Long articleId);

    void deleteByArticleId(Long articleId);

    @Query("SELECT t.nom as nom, t.description as description FROM ArticleMarketTraduction t WHERE t.article.id = :articleId AND t.langue = :langue")
    Optional<ArticleMarketTraductionProjection> findProjectionByArticleIdAndLangue(
            @Param("articleId") Long articleId,
            @Param("langue") String langue);
}
