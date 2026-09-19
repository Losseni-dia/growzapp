package growzapp.backend.module.traduction.DeepL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.referentiel.model.Secteur;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Traduction d'un secteur d'activité — un secteur est un champ libre (le
 * porteur peut taper n'importe quel nom, un nouveau Secteur est créé à la
 * volée si besoin), donc non couvert par le dictionnaire i18n statique
 * utilisé pour les valeurs de référence connues. Traduit UNE FOIS par
 * secteur (pas par projet) et partagé par tous les projets de ce secteur.
 */
@Entity
@Table(name = "secteur_traductions", uniqueConstraints = @UniqueConstraint(columnNames = { "secteur_id", "langue" }))
@Data
@NoArgsConstructor
public class SecteurTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secteur_id", nullable = false)
    @JsonIgnore
    private Secteur secteur;

    @Column(nullable = false, length = 5)
    private String langue; // "fr", "en", "es"

    @Column(length = 150)
    private String nom;
}
