package growzapp.backend.module.traduction.DeepL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projet_traductions", uniqueConstraints = @UniqueConstraint(columnNames = { "projet_id", "langue" }))
@Data
@NoArgsConstructor
public class ProjetTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    @JsonIgnore
    private Projet projet;

    @Column(nullable = false, length = 5)
    private String langue; // "fr", "en", "es"

    @Column(length = 200)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;


    public ProjetTraduction(Long id, String langue, String libelle, String description) {
        this.id = id;
        this.langue = langue;
        this.libelle = libelle;
        this.description = description;
    }
}