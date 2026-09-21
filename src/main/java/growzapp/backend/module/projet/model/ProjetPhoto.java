package growzapp.backend.module.projet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Photo additionnelle d'un projet — distincte de {@link Projet#getPoster()},
 * qui reste l'unique image "épinglée" (vignette catalogue, hero page
 * détail). Ces photos ne sont consultables que sur la page détail, en
 * galerie.
 */
@Entity
@Table(name = "projet_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    @JsonIgnore
    private Projet projet;

    @Column(nullable = false)
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
