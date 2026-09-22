package growzapp.backend.module.fournisseur.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commande_lignes")
@Getter
@Setter
@NoArgsConstructor
public class CommandeLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    // Référence informative uniquement : le prix/libellé sont figés au moment
    // de la commande (colonnes ci-dessous), pour qu'une modification ou
    // suppression ultérieure de l'article n'altère jamais l'historique d'une
    // commande déjà passée.
    @ManyToOne
    @JoinColumn(name = "article_id")
    private ArticleFournisseur article;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "prix_unitaire", nullable = false, precision = 18, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private int quantite;

    @Column(name = "sous_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal sousTotal;
}
