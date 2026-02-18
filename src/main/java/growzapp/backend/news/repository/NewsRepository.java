package growzapp.backend.news.repository;
import growzapp.backend.news.model.News;
import growzapp.backend.news.model.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    // Pour récupérer les articles du plus récent au plus ancien
    List<News> findAllByOrderByCreatedAtDesc();

    // Pour filtrer par catégorie (ex: uniquement les opportunités)
    List<News> findByCategoryOrderByCreatedAtDesc(NewsCategory category);
}