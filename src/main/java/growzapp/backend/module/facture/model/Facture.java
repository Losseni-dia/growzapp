package growzapp.backend.module.facture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.facture.enums.TypeFacture;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "factures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_facture", nullable = false, unique = true, length = 100)
    private String numeroFacture;

    @Column(name = "montant_ht")
    private double montantHT;

    @Column(name = "tva")
    private double tva = 0.0;

    @Column(name = "montant_ttc")
    private double montantTTC;

    @Column(name = "date_emission")
    private LocalDateTime dateEmission = LocalDateTime.now();

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Column(name = "fichier_url")
    private String fichierUrl;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private StatutFacture statut = StatutFacture.EMISE;

    // Nullable : une facture Premium n'a pas de dividende associé.
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "dividende_id", unique = true)
    @JsonIgnoreProperties("facture")
    @ToString.Exclude
    private Dividende dividende;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investisseur_id", nullable = false)
    @JsonIgnoreProperties({ "wallet", "investissements", "roles", "password" })
    @ToString.Exclude
    private User investisseur;

    // === FACTURES NON LIÉES À UN DIVIDENDE (ex: achat Premium) ===
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeFacture type = TypeFacture.DIVIDENDE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    @JsonIgnoreProperties({ "porteur", "investissements", "documents" })
    @ToString.Exclude
    private Projet projet;

    @Column(name = "libelle", length = 255)
    private String libelle;

    // === ARCHIVAGE ===
    // Une facture est un document légal, jamais supprimable — seulement
    // sortie des listes actives via un archivage.
    @Column(name = "archive_le")
    private LocalDateTime archiveLe;

    @Column(name = "archive_par")
    private String archivePar;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Facture))
            return false;
        Facture that = (Facture) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
