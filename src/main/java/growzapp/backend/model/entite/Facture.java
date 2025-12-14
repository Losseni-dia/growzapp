package growzapp.backend.model.entite;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import growzapp.backend.model.enumeration.StatutFacture;

@Entity
@Table(name = "factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_facture", nullable = false, unique = true, length = 100)
    private String numeroFacture;

    @Column(name = "montant_ht")
    private double montantHT;

    @Column(name = "tva")
    private double tva = 0.0;

    @Column(name = "montant_ttc")
    private double montantTTC;

    @Column(name = "date_emission")
    private LocalDateTime dateEmission = LocalDateTime.now();

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "fichier_url")
    private String fichierUrl;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private StatutFacture statut = StatutFacture.EMISE;

    // === RELATION OneToOne avec Dividende ===
    @OneToOne(cascade = CascadeType.ALL) // OK de laisser ALL ici
    @JoinColumn(name = "dividende_id", nullable = false, unique = true)

    // STOP BOUCLE JSON : On affiche le dividende, mais PAS la facture à l'intérieur
    // du dividende
    @JsonIgnoreProperties("facture")
    @ToString.Exclude // Stop boucle console
    private Dividende dividende;

    // === RELATION avec Investisseur ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investisseur_id", nullable = false)

    // Optimisation : On ne charge pas tout l'arbre utilisateur (trop lourd)
    @JsonIgnoreProperties({ "wallet", "investissements", "roles", "password" })
    @ToString.Exclude
    private User investisseur;
}