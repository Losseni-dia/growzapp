package growzapp.backend.module.growzmarket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.growzmarket.enums.StatutCommandeMarket;
import growzapp.backend.module.projet.model.Projet;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commandes_market")
@Getter
@Setter
@NoArgsConstructor
public class CommandeMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "acheteur_id", nullable = false)
    private User acheteur;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Column(name = "montant_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCommandeMarket statut = StatutCommandeMarket.PAYEE;

    // Case cochée obligatoirement par l'acheteur avant paiement — preuve
    // qu'il a vu et accepté le point de retrait déclaré sur l'article.
    @Column(name = "confirmation_lieu_retrait", nullable = false)
    private boolean confirmationLieuRetrait;

    @Column(name = "date_commande", nullable = false)
    private LocalDateTime dateCommande;

    @Column(name = "date_prete")
    private LocalDateTime datePrete;

    @Column(name = "date_retrait_confirme")
    private LocalDateTime dateRetraitConfirme;

    @Column(name = "motif_litige", length = 1000)
    private String motifLitige;

    @Column(name = "facture_url")
    private String factureUrl;

    @JsonIgnore
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommandeMarketLigne> lignes = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.dateCommande = LocalDateTime.now();
    }
}
