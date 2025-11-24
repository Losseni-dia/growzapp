// Transaction.java → VERSION FINALE 100% COMPATIBLE HIBERNATE 6 + MYSQL
package growzapp.backend.model.entite;

import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // CHANGÉ : double → BigDecimal
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeTransaction type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutTransaction statut = StatutTransaction.EN_COURS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_wallet_id")
    private Wallet destinataireWallet;

    @Column(length = 500)
    private String description;

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