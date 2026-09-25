package growzapp.backend.module.traduction.DeepL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.growzmarket.model.ArticleMarket;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "articles_market_traductions", uniqueConstraints = @UniqueConstraint(columnNames = { "article_id", "langue" }))
@Data
@NoArgsConstructor
public class ArticleMarketTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    @JsonIgnore
    private ArticleMarket article;

    @Column(nullable = false, length = 5)
    private String langue; // "fr", "en", "es"

    @Column(length = 200)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;
}
