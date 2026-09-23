package growzapp.backend.module.growzmarket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.growzmarket.enums.CategorieMarket;
import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "articles_market")
@Getter
@Setter
@NoArgsConstructor
public class ArticleMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le revenu de la vente crédite la trésorerie de CE projet — le porteur
    // choisit explicitement pour quel projet il vend, il peut en avoir
    // plusieurs.
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal prix;

    @Column(nullable = false, length = 50)
    private String unite;

    @Column(nullable = false)
    private boolean disponible = true;

    // null = stock illimité, sinon décrémenté à chaque commande payée.
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategorieMarket categorie = CategorieMarket.AUTRE;

    // Ex. "Prêt sous 2 jours" — informe l'acheteur avant qu'il ne commande.
    @Column(name = "delai_preparation", length = 100)
    private String delaiPreparation;

    // EAGER : listes toujours petites (quelques photos par article), évite
    // les soucis de lazy-loading Hibernate/Jackson hors session (observé en
    // test : la collection revenait `null` en JSON malgré des lignes
    // présentes en base, faute d'initialisation déclenchée avant sérialisation).
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "articles_market_photos", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "url", nullable = false)
    @OrderColumn(name = "position")
    private List<String> photos = new ArrayList<>();

    // Texte libre déclaré par le porteur — lieu + éventuelles indications,
    // affiché sur la fiche produit avant tout achat (V1 : pas de réseau de
    // points relais géré, voir docs/GROWZMARKET_VISION.md).
    @Column(name = "point_retrait", nullable = false, length = 255)
    private String pointRetrait;

    @Column(name = "telephone_contact", length = 50)
    private String telephoneContact;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
