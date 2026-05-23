package growzapp.backend.module.facture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "dividende_id", nullable = false, unique = true)
    @JsonIgnoreProperties("facture")
    @ToString.Exclude
    private Dividende dividende;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investisseur_id", nullable = false)
    @JsonIgnoreProperties({ "wallet", "investissements", "roles", "password" })
    @ToString.Exclude
    private User investisseur;
}
