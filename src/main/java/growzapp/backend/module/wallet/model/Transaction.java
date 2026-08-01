// growzapp.backend.model.entite.Transaction.java
package growzapp.backend.module.wallet.model;

import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Colonne ID du Wallet (pour la simplicité)
    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    // Si vous voulez la relation complète pour tx.getWallet().getUser()
    // Si vous voulez utiliser tx.getWallet(), VOUS DEVEZ AVOIR CE LIEN:
    /*
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "wallet_id", insertable = false, updatable = false)
     * private Wallet wallet;
     */
    // *NOTE: Si vous ne pouvez pas modifier la DB, vous devez le chercher
    // manuellement dans le Webhook.
    // L'erreur getWallet() vient du code du Webhook qui s'attend à ce champ!

    @Column(name = "wallet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeTransaction type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutTransaction statut = StatutTransaction.EN_ATTENTE_PAIEMENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_wallet_id")
    private Wallet destinataireWallet;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    // NOUVEAU CHAMP REQUIS PAR LE WEBHOOK PAYDUNYA
    @Column(name = "reference_externe", length = 255)
    private String referenceExterne;

    // Protection contre le rejeu (double-clic, retry réseau) des mouvements de
    // trésorerie projet initiés côté client (déblocage admin, virement interne
    // porteur). Nullable : les transactions existantes et les autres flux
    // (dépôt, investissement...) n'en ont pas besoin.
    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    // Identifiant de l'utilisateur (généralement un admin) à l'origine d'une
    // action effectuée pour le compte d'un tiers (déblocage de trésorerie
    // projet, versement au porteur, retrait admin). Nullable : les mouvements
    // que l'utilisateur effectue lui-même (dépôt, retrait perso...) n'ont pas
    // besoin de ce champ, l'acteur = le propriétaire du wallet.
    @Column(name = "auteur_id")
    private Long auteurId;

    // Méthodes utilitaires
    public void markAsSuccess() {
        this.statut = StatutTransaction.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.statut = StatutTransaction.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsPending() {
        this.statut = StatutTransaction.EN_ATTENTE_VALIDATION;
    }
}