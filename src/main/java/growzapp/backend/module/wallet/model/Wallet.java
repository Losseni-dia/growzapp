// src/main/java/growzapp/backend/model/entite/Wallet.java
// VERSION FINALE – 27 NOVEMBRE 2025 – PROPRE, SÛR, INTELLIGENT

package growzapp.backend.module.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.enums.WalletType;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "wallets")
@Getter
@Setter
@ToString(exclude = { "user" })
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

    @Column(name = "wallet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WalletType walletType = WalletType.USER;

    @Column(name = "projet_id")
    private Long projetId;

    // ===================================================================
    // MÉTHODES GÉNÉRALES (utilisables par USER et PROJET)
    // ===================================================================

    public BigDecimal getSoldeTotal() {
        return soldeDisponible.add(soldeBloque);
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
    // MÉTHODES POUR WALLET PROJET (trésorerie séquestrée)
    // ===================================================================

    /** Crédite le wallet projet en soldeBloque quand un investissement est validé */
    public void crediterBloqueProjet(BigDecimal montant) {
        checkPositive(montant);
        if (this.walletType != WalletType.PROJET) {
            throw new IllegalStateException("crediterBloqueProjet() uniquement sur wallet PROJET");
        }
        soldeBloque = soldeBloque.add(montant);
    }

    /** Débloque une partie de la trésorerie séquestrée du projet → soldeDisponible (action admin) */
    public void debloquerVersDisponible(BigDecimal montant) {
        checkPositive(montant);
        if (this.walletType != WalletType.PROJET) {
            throw new IllegalStateException("debloquerVersDisponible() uniquement sur wallet PROJET");
        }
        if (montant.compareTo(soldeBloque) > 0) {
            throw new IllegalStateException("Fonds bloqués insuffisants dans le wallet projet");
        }
        soldeBloque = soldeBloque.subtract(montant);
        soldeDisponible = soldeDisponible.add(montant);
    }

    public void crediterDisponible(BigDecimal montant) {
        checkPositive(montant);
        soldeDisponible = soldeDisponible.add(montant);
    }
}