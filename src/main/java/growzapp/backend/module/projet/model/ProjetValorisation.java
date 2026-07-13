package growzapp.backend.module.projet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import growzapp.backend.module.projet.enums.TypeEvenementValorisation;

@Entity
@Table(name = "projet_valorisations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetValorisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @Column(name = "montant_valorisation")
    private BigDecimal montantValorisation;

    @Column(name = "montant_collecte")
    private BigDecimal montantCollecte;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 30)
    private TypeEvenementValorisation typeEvenement;

    @Column(name = "montant_evenement")
    private BigDecimal montantEvenement; // ex: montant investi ou montant du dividende, selon le type

    @Column(name = "date_snapshot")
    private LocalDateTime dateSnapshot = LocalDateTime.now();
}