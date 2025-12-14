// src/main/java/growzapp/backend/model/entite/Investissement.java

package growzapp.backend.model.entite;

import growzapp.backend.model.enumeration.StatutPartInvestissement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investissements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Investissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_parts_pris", nullable = false)
    private int nombrePartsPris;

    @Column(name = "montant_investi", nullable = false)
    private BigDecimal montantInvesti; // = nombrePartsPris × prixUnePart (persisté pour historique)

    @Column(name = "pourcent_equity", nullable = false)
    private double valeurPartsPrisEnPourcent; // % d'equity acquis

    private double frais = 0.0; // frais plateforme (ex: 5%)

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_investissement", nullable = false)
    private StatutPartInvestissement statutPartInvestissement = StatutPartInvestissement.EN_ATTENTE;

    // ==================== RELATIONS ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User investisseur;

    @OneToOne(mappedBy = "investissement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Contrat contrat;

    @OneToMany(mappedBy = "investissement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dividende> dividendes = new ArrayList<>();

    // ==================== METHODES UTILITAIRES ====================

    /** Calcul automatique du % d'equity basé sur la valorisation du projet */
    public void calculerPourcentageEquity() {
        // On compare avec BigDecimal.ZERO en utilisant compareTo
        // compareTo renvoie : -1 (inférieur), 0 (égal), 1 (supérieur)
        if (projet == null || projet.getValuation() == null || projet.getValuation().compareTo(BigDecimal.ZERO) <= 0) {
            this.valeurPartsPrisEnPourcent = 0.0;
            return;
        }

        // Formule : (montantInvesti / valuation) * 100
        // On utilise MathContext pour définir la précision de la division
        this.valeurPartsPrisEnPourcent = this.montantInvesti
                .divide(projet.getValuation(), java.math.MathContext.DECIMAL128)
                .multiply(new BigDecimal("100"))
                .doubleValue(); // On peut garder double ici car c'est un pourcentage
    }

    /** Calcul automatique du montant investi */
    public void calculerMontantInvesti() {
        if (projet != null && projet.getPrixUnePart() != null
                && projet.getPrixUnePart().compareTo(BigDecimal.ZERO) > 0) {
            // Formule : nombrePartsPris * prixUnePart
            this.montantInvesti = projet.getPrixUnePart().multiply(new BigDecimal(this.nombrePartsPris));
        }
    }

    /** Méthode complète : calcule tout d'un coup */
    public void calculerTout() {
        calculerMontantInvesti();
        calculerPourcentageEquity();
    }
}