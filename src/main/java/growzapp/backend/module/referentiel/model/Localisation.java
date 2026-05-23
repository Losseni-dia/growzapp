package growzapp.backend.module.referentiel.model;

import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "localisations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Localisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String adresse;
    private String contact;
    private String responsable;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "what3words")
    private String what3words;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "localite_id")
    private Localite localite;

    @OneToMany(mappedBy = "siteProjet")
    private List<Projet> projets = new ArrayList<>();

    public String getGoogleMapsUrl() {
        if (latitude != null && longitude != null) {
            return "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
        }
        return null;
    }
}
