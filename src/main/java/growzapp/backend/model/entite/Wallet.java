package growzapp.backend.model.entite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Wallet.java
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
    private User user;

    @Column(nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private double soldeDisponible = 0.0;

    @Column(nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private double soldeBloque = 0.0; // argent réservé tant que l'investissement est EN_ATTENTE

    public double getSoldeTotal() {
        return soldeDisponible + soldeBloque;
    }

    // Méthodes utilitaires qu’on va utiliser dans le service
    public void debiterDisponible(double montant) {
        if (montant > soldeDisponible)
            throw new IllegalArgumentException("Solde insuffisant");
        this.soldeDisponible -= montant;
    }

    public void crediterDisponible(double montant) {
        this.soldeDisponible += montant;
    }

    public void bloquer(double montant) {
        debiterDisponible(montant);
        this.soldeBloque += montant;
    }

    public void debloquer(double montant) {
        if (montant > soldeBloque)
            throw new IllegalArgumentException("Fonds bloqués insuffisants");
        this.soldeBloque -= montant;
        crediterDisponible(montant);
    }

    public void validerBloque(double montant) {
        if (montant > soldeBloque)
            throw new IllegalArgumentException("Fonds bloqués insuffisants");
        this.soldeBloque -= montant;
        // L'argent reste dans le système mais n'est plus dans le wallet de
        // l'utilisateur
        // (il est transféré au projet/porteur)
    }

    public void bloquerFonds(double montant) {
        if (montant > soldeDisponible)
            throw new RuntimeException("Solde insuffisant");
        this.soldeDisponible -= montant;
        this.soldeBloque += montant;
    }

    public void debloquerFonds(double montant) {
        if (montant > soldeBloque)
            throw new IllegalStateException("Fonds bloqués insuffisants");
        this.soldeBloque -= montant;
        this.soldeDisponible += montant;
    }

    public void validerInvestissement(double montant) {
        if (montant > soldeBloque)
            throw new IllegalStateException("Fonds bloqués insuffisants");
        this.soldeBloque -= montant;
        // L'argent est transféré au projet → il sort du wallet
    }
}