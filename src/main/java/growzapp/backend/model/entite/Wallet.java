// Wallet.java → VERSION BANQUE PROFESSIONNELLE
package growzapp.backend.model.entite;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @JsonIgnoreProperties({ "wallet", "roles", "langues", "projets", "investissements", "hibernateLazyInitializer" })
    private User user;
    

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeDisponible = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeBloque = BigDecimal.ZERO;

    // 3. ARGENT VALIDÉ PAR ADMIN → PRÊT À ÊTRE RETIRÉ (nouveau champ)
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal soldeRetirable = BigDecimal.ZERO; // CELUI QUE TU VEUX !

    public BigDecimal getSoldeTotal() {
        return soldeDisponible.add(soldeBloque);
    }

    // Méthodes utilitaires — propres et safe
    public void debiterDisponible(BigDecimal montant) {
        if (montant.compareTo(soldeDisponible) > 0)
            throw new IllegalArgumentException("Solde insuffisant");
        this.soldeDisponible = this.soldeDisponible.subtract(montant);
    }

    public void crediterDisponible(BigDecimal montant) {
        this.soldeDisponible = this.soldeDisponible.add(montant);
    }

    public void bloquer(BigDecimal montant) {
        debiterDisponible(montant);
        this.soldeBloque = this.soldeBloque.add(montant);
    }

    public void debloquer(BigDecimal montant) {
        if (montant.compareTo(soldeBloque) > 0)
            throw new IllegalArgumentException("Fonds bloqués insuffisants");
        this.soldeBloque = this.soldeBloque.subtract(montant);
        crediterDisponible(montant);
    }

    public void bloquerFonds(BigDecimal montant) {
        if (montant.compareTo(soldeDisponible) > 0)
            throw new RuntimeException("Solde insuffisant");
        this.soldeDisponible = this.soldeDisponible.subtract(montant);
        this.soldeBloque = this.soldeBloque.add(montant);
    }

    public void debloquerFonds(BigDecimal montant) {
        if (montant.compareTo(soldeBloque) > 0)
            throw new IllegalStateException("Fonds bloqués insuffisants");
        this.soldeBloque = this.soldeBloque.subtract(montant);
        this.soldeDisponible = this.soldeDisponible.add(montant);
    }

    public void validerInvestissement(BigDecimal montant) {
        if (montant.compareTo(soldeBloque) > 0)
            throw new IllegalStateException("Fonds bloqués insuffisants");
        this.soldeBloque = this.soldeBloque.subtract(montant);
    }


    // NOUVELLE MÉTHODE : quand admin valide un retrait
    public void validerRetrait(BigDecimal montant) {
        if (montant.compareTo(soldeBloque) > 0)
            throw new IllegalStateException("Fonds bloqués insuffisants");
        this.soldeBloque = this.soldeBloque.subtract(montant);
        this.soldeRetirable = this.soldeRetirable.add(montant); // L'ARGENT VA ICI
    }

    
}