package growzapp.backend.module.fournisseur.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.fournisseur.enums.StatutCommande;
import growzapp.backend.module.projet.model.Projet;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commandes")
@Getter
@Setter
@NoArgsConstructor
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(name = "montant_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCommande statut = StatutCommande.EN_ATTENTE_VALIDATION;

    @Column(name = "date_commande", nullable = false)
    private LocalDateTime dateCommande;

    @Column(name = "date_validation_admin")
    private LocalDateTime dateValidationAdmin;

    @Column(name = "date_acceptation")
    private LocalDateTime dateAcceptation;

    @Column(name = "date_expedition")
    private LocalDateTime dateExpedition;

    @Column(name = "date_confirmation_reception")
    private LocalDateTime dateConfirmationReception;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @Column(name = "motif_litige", length = 1000)
    private String motifLitige;

    // Justificatif transmis par le fournisseur à l'expédition — condition de
    // transparence pour les investisseurs du projet, qui financent cet achat
    // sans jamais voir transiter l'argent par le porteur. Redistribuée
    // (email + Documents du projet) une fois le paiement exécuté.
    @Column(name = "facture_url")
    private String factureUrl;

    @JsonIgnore
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommandeLigne> lignes = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.dateCommande = LocalDateTime.now();
    }
}
