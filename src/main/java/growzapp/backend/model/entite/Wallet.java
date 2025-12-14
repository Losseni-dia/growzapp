// src/main/java/growzapp/backend/model/entite/Wallet.java
// VERSION FINALE – 27 NOVEMBRE 2025 – PROPRE, SÛR, INTELLIGENT

package growzapp.backend.model.entite;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import growzapp.backend.model.enumeration.WalletType;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = false)
    @JsonIgnoreProperties({ "wallet", "roles", "langues", "projets", "investissements", "hibernateLazyInitializer" })
    private User user;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal soldeDisponible = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal soldeBloque = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal soldeRetirable = BigDecimal.ZERO;

    @Column(name = "wallet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletType walletType = WalletType.USER;

    @Column(name = "projet_id")
    private Long projetId;

    // ===================================================================
    // MÉTHODES GÉNÉRALES (utilisables par USER et PROJET)
    // ===================================================================

    public BigDecimal getSoldeTotal() {
        return soldeDisponible.add(soldeBloque).add(soldeRetirable);
    }

    private void checkPositive(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
    }

    // ===================================================================
    // MÉTHODES POUR WALLET USER (investisseur)
    // ===================================================================

    /** Bloque des fonds disponibles → soldeBloque (investissement en attente) */
    public void bloquerFonds(BigDecimal montant) {
        checkPositive(montant);
        if (montant.compareTo(soldeDisponible) > 0) {
            throw new IllegalStateException("Solde disponible insuffisant");
        }
        soldeDisponible = soldeDisponible.subtract(montant);
        soldeBloque = soldeBloque.add(montant);
    }

    /** Débloque des fonds bloqués → soldeDisponible (validation ou refus) */
    public void debloquerFonds(BigDecimal montant) {
        checkPositive(montant);
        if (montant.compareTo(soldeBloque) > 0) {
            throw new IllegalStateException("Fonds bloqués insuffisants");
        }
        soldeBloque = soldeBloque.subtract(montant);
        soldeDisponible = soldeDisponible.add(montant);
    }

    /**
     * Validation d’un investissement : les fonds bloqués deviennent "définitifs"
     * (disparaissent du wallet user)
     */
    public void validerInvestissement(BigDecimal montant) {
        checkPositive(montant);
        if (montant.compareTo(soldeBloque) > 0) {
            throw new IllegalStateException("Fonds bloqués insuffisants pour validation");
        }
        soldeBloque = soldeBloque.subtract(montant); // l'argent sort définitivement du wallet user
    }

    // ===================================================================
    // MÉTHODES POUR WALLET PROJET (collecte séquestrée)
    // ===================================================================

    /** Crédite le wallet projet quand un investissement est validé */
    public void crediterCollecte(BigDecimal montant) {
        checkPositive(montant);
        if (this.walletType == WalletType.PROJET)
            throw new IllegalStateException("crediterCollecte() uniquement sur wallet PROJET");
        soldeDisponible = soldeDisponible.add(montant);
    }

    /** Débite le wallet projet lors du versement au porteur */
    public void debiterVersementPorteur(BigDecimal montant) {
        checkPositive(montant);
        if (this.walletType != WalletType.PROJET) {
            throw new IllegalStateException("debitVersementPorteur() uniquement sur wallet PROJET");
        }
        if (montant.compareTo(soldeDisponible) > 0) {
            throw new IllegalStateException("Solde disponible insuffisant dans le wallet projet");
        }
        soldeDisponible = soldeDisponible.subtract(montant);
    }

    // ===================================================================
    // MÉTHODES POUR RETRAITS & GAINS (porteur ou investisseur)
    // ===================================================================

    /** Admin valide un retrait → passe de soldeBloque à soldeRetirable */
    public void validerRetrait(BigDecimal montant) {
        checkPositive(montant);
        if (montant.compareTo(soldeBloque) > 0) {
            throw new IllegalStateException("Fonds bloqués insuffisants");
        }
        soldeBloque = soldeBloque.subtract(montant);
        soldeRetirable = soldeRetirable.add(montant);
    }

    /** L'utilisateur retire ses gains */
    public void retirerGains(BigDecimal montant) {
        checkPositive(montant);
        if (montant.compareTo(soldeRetirable) > 0) {
            throw new IllegalStateException("Solde retirable insuffisant");
        }
        soldeRetirable = soldeRetirable.subtract(montant);
        soldeDisponible = soldeDisponible.add(montant);
    }

    // ===================================================================
    // MÉTHODE UNIVERSELLE (à utiliser dans le service)
    // ===================================================================

    /** À utiliser uniquement dans InvestissementService.validerInvestissement() */
    public void transfererVersProjetLorsValidation(BigDecimal montant, Wallet walletProjet) {
        this.validerInvestissement(montant); // retire du wallet user
        walletProjet.crediterCollecte(montant); // ajoute au wallet projet
    }

    public void crediterDisponible(BigDecimal montant) {
        checkPositive(montant);
        soldeDisponible = soldeDisponible.add(montant);
    }
}