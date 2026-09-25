package growzapp.backend.module.traduction.DeepL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.news.model.News;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_traductions", uniqueConstraints = @UniqueConstraint(columnNames = { "news_id", "langue" }))
@Data
@NoArgsConstructor
public class NewsTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    @JsonIgnore
    private News news;

    @Column(nullable = false, length = 5)
    private String langue; // "fr", "en", "es"

    @Column(length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;
}
