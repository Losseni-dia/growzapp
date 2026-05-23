package growzapp.backend.module.news.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Data
@Schema(description = "Article d'actualité publié sur la plateforme")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identifiant unique de l'article", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Titre de l'article", example = "Lancement du projet Ferme Solaire Nord")
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Schema(description = "Contenu complet de l'article en HTML ou texte brut", example = "Nous sommes ravis d'annoncer...")
    private String content;

    @Schema(description = "URL de l'image de couverture de l'article", example = "/uploads/posters/news-1.jpg")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Catégorie de l'article",
            example = "INVESTMENT_OPPORTUNITY",
            allowableValues = {"PLATFORM_UPDATE", "INVESTMENT_OPPORTUNITY", "PERFORMANCE_REPORT", "EDUCATION", "SECURITY"})
    private NewsCategory category;

    @Schema(description = "Date et heure de publication de l'article", example = "2025-06-01T09:00:00")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
