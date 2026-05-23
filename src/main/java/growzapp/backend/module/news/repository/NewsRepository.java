package growzapp.backend.module.news.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.news.model.News;
import growzapp.backend.module.news.model.NewsCategory;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    // Pour récupérer les articles du plus récent au plus ancien
    List<News> findAllByOrderByCreatedAtDesc();

    // Pour filtrer par catégorie (ex: uniquement les opportunités)
    List<News> findByCategoryOrderByCreatedAtDesc(NewsCategory category);
}