package growzapp.backend.module.investissement.model;

import growzapp.backend.module.investissement.enums.ChoixEcheance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Journal d'audit append-only de chaque decision individuelle prise sur un
// investissement a l'echeance de financement d'un projet (choisie par
// l'investisseur ou declenchee par la cloture admin) — jamais modifie ni
// supprime, c'est la trace legale de ce qui a ete decide et avec quel texte
// de consentement exact, utile en cas de litige.
@Entity
@Table(name = "decision_echeance_investissement")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEcheanceInvestissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investissement_id", nullable = false)
    private Long investissementId;

    @Column(name = "projet_id", nullable = false)
    private Long projetId;

    @Column(name = "investisseur_id", nullable = false)
    private Long investisseurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChoixEcheance choix;

    @Column(name = "date_decision", nullable = false)
    private LocalDateTime dateDecision;

    @Column(name = "date_fin_projet_au_moment")
    private LocalDate dateFinProjetAuMoment;

    @Column(name = "consentement_texte", nullable = false, length = 4000)
    private String consentementTexte;

    @Column(name = "declenche_par_admin", nullable = false)
    private boolean declencheParAdmin;
}
