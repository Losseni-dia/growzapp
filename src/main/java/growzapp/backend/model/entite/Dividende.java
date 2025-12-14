package growzapp.backend.model.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import growzapp.backend.model.enumeration.MoyenPaiement;
import growzapp.backend.model.enumeration.StatutDividende;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "dividendes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dividende {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant_par_part", precision = 15, scale = 2)
    private BigDecimal montantParPart;

    @Enumerated(EnumType.STRING)
    private StatutDividende statutDividende = StatutDividende.PLANIFIE;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyenPaiement;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "motif")
    private String motif;
    // ==================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investissement_id", nullable = false)
    @JsonIgnoreProperties({ "dividendes", "investisseur" }) // Évite de recharger trop de données parents
    @ToString.Exclude
    private Investissement investissement;

    // === RELATION OneToOne Inverse ===
    @OneToOne(mappedBy = "dividende")

    // STOP BOUCLE JSON : On affiche la facture, mais PAS le dividende à l'intérieur
    // de la facture
    @JsonIgnoreProperties("dividende")
    @ToString.Exclude
    private Facture facture;

    // Méthodes calculées
    public double getMontantTotal() {
        if (investissement == null || this.montantParPart == null)
            return 0.0;
        return this.montantParPart.multiply(BigDecimal.valueOf(investissement.getNombrePartsPris())).doubleValue();
    }
}