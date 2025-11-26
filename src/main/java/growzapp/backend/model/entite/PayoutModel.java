package growzapp.backend.model.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// src/main/java/growzapp/backend/model/entite/Payout.java
@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String userLogin;
    private String userPhone; // +225... ou +221...

    private BigDecimal montant;

    @Column(name = "external_payout_id")
    private String externalPayoutId;
    
    @Enumerated(EnumType.STRING)
    private TypeTransaction type; // PAYOUT_OM, PAYOUT_MTN, PAYOUT_WAVE

    @Enumerated(EnumType.STRING)
    private StatutTransaction statut = StatutTransaction.EN_ATTENTE_PAIEMENT;

    private String paydunyaToken;
    private String paydunyaInvoiceUrl;
    private String paydunyaStatus;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    private String motifEchec; // si échec
}