package growzapp.backend.model.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // --- NOUVEAUX CHAMPS GÉO ---

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude; // Ex: -5.34843400

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude; // Ex: 4.02073900

    @Column(name = "what3words")
    private String what3words; // Ex: ///maïs.fonds.récolte

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "localite_id")
    private Localite localite;

    @OneToMany(mappedBy = "siteProjet")
    private List<Projet> projets = new ArrayList<>();

    // --- HELPER : Générer le lien Google Maps pour le Frontend ---
    public String getGoogleMapsUrl() {
        if (latitude != null && longitude != null) {
            return "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
        }
        return null;
    }
}