package growzapp.backend.module.projet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer; // Ajouté pour gérer les accents
import java.util.Locale; // Ajouté pour la mise en minuscule

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.document.model.Document;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.user.model.User;
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
import jakarta.persistence.Transient;
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

    @Column(name = "description", length = 5000)
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


    // Durée du projet en mois — null = durée indéterminée (choix explicite du
    // porteur, pas une valeur manquante à défaulter).
    @Column(name = "duree_mois")
    @Min(1)
    private Integer dureeMois;


    @Min(0)
    @Column(name = "objectif_financement")
    private BigDecimal objectifFinancement;

    @Column(name = "montant_collecte")
    private BigDecimal montantCollecte = BigDecimal.ZERO;

    @Column(name = "financement_debut")
    private LocalDate dateDebut;

    @Column(name = "financement_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    private StatutProjet statutProjet = StatutProjet.SOUMIS;

    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User porteur;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Localisation siteProjet;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "secteur_id")
    private Secteur secteur;

    // === STATUT PREMIUM (mise en avant catalogue, remplace l'ancienne
    // "certification" qui n'était jamais réellement câblée) ===
    @Column(name = "premium_debut")
    private LocalDateTime premiumDebut;

    @Column(name = "premium_fin")
    private LocalDateTime premiumFin;

    @Transient
    public boolean isPremiumActif() {
        return premiumFin != null && premiumFin.isAfter(LocalDateTime.now());
    }

    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjetPhoto> photos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "projet", cascade = CascadeType.ALL)
    private List<Investissement> investissements = new ArrayList<>();

    // === SUPPRESSION LOGIQUE (SOFT DELETE) ===
    @Column(name = "supprime_le")
    private LocalDateTime supprimeLe;

    @Column(name = "supprime_par")
    private String supprimePar;

    @Column(name = "motif_suppression", length = 500)
    private String motifSuppression;

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
