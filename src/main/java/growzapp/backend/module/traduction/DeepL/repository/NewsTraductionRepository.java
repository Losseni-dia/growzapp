package growzapp.backend.module.traduction.DeepL.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.traduction.DeepL.model.NewsTraduction;
import growzapp.backend.module.traduction.DeepL.model.NewsTraductionProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsTraductionRepository extends JpaRepository<NewsTraduction, Long> {

    Optional<NewsTraduction> findByNewsIdAndLangue(Long newsId, String langue);

    List<NewsTraduction> findByNewsId(Long newsId);

    void deleteByNewsId(Long newsId);

    @Query("SELECT t.title as title, t.content as content FROM NewsTraduction t WHERE t.news.id = :newsId AND t.langue = :langue")
    Optional<NewsTraductionProjection> findProjectionByNewsIdAndLangue(
            @Param("newsId") Long newsId,
            @Param("langue") String langue);
}
