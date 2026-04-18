package growzapp.backend.model.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer; // Ajouté pour gérer les accents
import java.util.Locale; // Ajouté pour la mise en minuscule

import growzapp.backend.model.enumeration.StatutProjet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Projet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String slug;


    private String poster; // URL
    private Integer reference;
    private String libelle;
    private String description;
    private BigDecimal valuation;
    private double roiProjete;

    @Min(0)
    @Column(name = "parts_disponible")
    private int partsDisponible;

    private double valeurTotalePartsEnPourcent;

    @Column(name = "parts_prises")
    private int partsPrises = 0;


    @Min(0)
    @Column(name = "prix_une_part")
    private BigDecimal prixUnePart;


    // NOUVEAU CHAMP : Durée du projet en mois
    @Column(name = "duree_mois", nullable = false)
    @Min(1)
    private Integer dureeMois = 36; // valeur par défaut : 36 mois
    

    @Min(0)
    @Column(name = "objectif_financement")
    private BigDecimal objectifFinancement;

    @Column(name = "montant_collecte")
    private BigDecimal montantCollecte = BigDecimal.ZERO;

    @Column(name = "financement_debut")
    private LocalDateTime dateDebut;

    @Column(name = "financement_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    private StatutProjet statutProjet = StatutProjet.SOUMIS;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "porteur_id")
    private User porteur;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Localisation siteProjet;

    @ManyToOne
    @JoinColumn(name = "secteur_id")
    private Secteur secteur;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<Investissement> investissements = new ArrayList<>();


    // --- LOGIQUE DE GÉNÉRATION DU SLUG ---
    // Cette méthode s'exécute automatiquement avant l'enregistrement en base
    @PrePersist
    @PreUpdate
    public void onCreateOrUpdate() {
        if (this.libelle != null) {
            this.slug = makeSlug(this.libelle);
        }
    }

    private String makeSlug(String input) {
        if (input == null) return null;
        
        // 1. Enlever les accents (ex: 'é' devient 'e')
        String nonInterpreted = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        // 2. Tout mettre en minuscule et remplacer les caractères non-alphanumériques par des tirets
        return nonInterpreted.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9]+", "-") // Remplace tout ce qui n'est pas a-z ou 0-9 par "-"
                .replaceAll("^-|-$", "");      // Enlève les tirets au début ou à la fin
    }

    


    /*Note:Les%
    equity sont calculés logiquement:--Ex:Projet 1:50 parts×50 €=2500 €→2500/50000=5%
    --Projet 3:100 parts×200 €=20000 €→20000/100000=20% */

    

}
