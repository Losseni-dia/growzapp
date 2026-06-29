package growzapp.backend.module.investissement.model;

import growzapp.backend.module.contrat.model.Contrat;
import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
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
    private BigDecimal montantInvesti;

    @Column(name = "pourcent_equity", nullable = false)
    private double valeurPartsPrisEnPourcent;

    private double frais = 0.0;

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_investissement", nullable = false)
    private StatutPartInvestissement statutPartInvestissement = StatutPartInvestissement.EN_ATTENTE;

    @Column(name = "risk_warning_accepted_at")
    private LocalDateTime riskWarningAcceptedAt;

    @Column(name = "insurance_terms_accepted_at")
    private LocalDateTime insuranceTermsAcceptedAt;

    // ── Idempotence Stripe — évite de créer deux fois le même investissement ──
    @Column(name = "reference_externe_stripe", unique = true)
    private String referenceExterneStripe;

    // ── RELATIONS ─────────────────────────────────────────────────────────────

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

    // ── MÉTHODES UTILITAIRES ──────────────────────────────────────────────────

    public void calculerPourcentageEquity() {
        if (projet == null || projet.getValuation() == null
                || projet.getValuation().compareTo(BigDecimal.ZERO) <= 0) {
            this.valeurPartsPrisEnPourcent = 0.0;
            return;
        }
        this.valeurPartsPrisEnPourcent = this.montantInvesti
                .divide(projet.getValuation(), java.math.MathContext.DECIMAL128)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    public void calculerMontantInvesti() {
        if (projet != null && projet.getPrixUnePart() != null
                && projet.getPrixUnePart().compareTo(BigDecimal.ZERO) > 0) {
            this.montantInvesti = projet.getPrixUnePart()
                    .multiply(new BigDecimal(this.nombrePartsPris));
        }
    }

    public void calculerTout() {
        calculerMontantInvesti();
        calculerPourcentageEquity();
    }
}