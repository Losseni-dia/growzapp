package growzapp.backend.module.news.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import growzapp.backend.module.news.model.News;
import growzapp.backend.module.news.model.NewsCategory;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    // Pour récupérer les articles du plus récent au plus ancien
    List<News> findAllByOrderByCreatedAtDesc();

    // Pour filtrer par catégorie (ex: uniquement les opportunités)
    List<News> findByCategoryOrderByCreatedAtDesc(NewsCategory category);

    @Query("SELECT n FROM News n WHERE " +
            "(CAST(:search AS string) IS NULL OR :search = '' OR " +
            " LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "ORDER BY n.createdAt DESC")
    Page<News> findForAdmin(@Param("search") String search, Pageable pageable);
}