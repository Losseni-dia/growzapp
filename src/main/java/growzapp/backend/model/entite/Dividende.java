package growzapp.backend.model.entite;

import java.time.LocalDateTime;

import growzapp.backend.model.enumeration.MoyenPaiement;
import growzapp.backend.model.enumeration.StatutDividende;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dividendes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dividende {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant_par_part")
    private double montantParPart;

    @Enumerated(EnumType.STRING)
    private StatutDividende statutDividende = StatutDividende.PLANIFIE;

    @Enumerated(EnumType.STRING)
    private MoyenPaiement moyenPaiement;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investissement_id", nullable = false)
    private Investissement investissement;

    @OneToOne(mappedBy = "dividende", cascade = CascadeType.ALL, orphanRemoval = true)
    private Facture facture;

    public double getMontantTotal() {
        Investissement investissement = this.getInvestissement();
        double montant = this.montantParPart * investissement.getNombrePartsPris();

        return montant;
    }
}
