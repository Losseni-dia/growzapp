package growzapp.backend.module.growzmarket.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Échange entre acheteur, vendeur et admin sur un litige GrowzMarket, avant
// clôture (arbitrage). Sans ce fil, aucune des parties ne pouvait exposer
// son point de vue autrement qu'à travers le motif initial figé.
@Entity
@Table(name = "commande_market_litige_messages")
@Getter
@Setter
@NoArgsConstructor
public class CommandeMarketLitigeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeMarket commande;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    // Figé à l'écriture (ACHETEUR / VENDEUR / ADMIN) plutôt que recalculé à
    // l'affichage, pour rester cohérent avec l'historique même si le rôle
    // de l'auteur change ensuite (ex. un porteur qui perd son projet).
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 1000)
    private String contenu;

    // Uniquement rempli pour un message admin (role=ADMIN) : à qui ce
    // message est destiné (ACHETEUR ou VENDEUR) — l'autre partie ne le
    // voit jamais. Les messages acheteur/vendeur restent visibles de
    // toutes les parties, pas besoin de ce champ pour eux.
    @Column(length = 20)
    private String destinataire;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    public void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }
}
