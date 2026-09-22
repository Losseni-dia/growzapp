package growzapp.backend.module.fournisseur.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.fournisseur.enums.StatutFournisseur;
import growzapp.backend.module.fournisseur.enums.StatutJuridiqueFournisseur;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fournisseurs")
@Getter
@Setter
@NoArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_juridique", nullable = false, length = 20)
    private StatutJuridiqueFournisseur statutJuridique;

    @Column(name = "raison_sociale", length = 150)
    private String raisonSociale;

    @ManyToOne
    @JoinColumn(name = "secteur_id")
    private Secteur secteur;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false, length = 100)
    private String pays;

    @Column(length = 30)
    private String telephone;

    @Column(length = 191)
    private String email;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutFournisseur statut = StatutFournisseur.EN_ATTENTE;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @JsonIgnore
    @OneToMany(mappedBy = "fournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleFournisseur> articles = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.dateSoumission = LocalDateTime.now();
    }
}
