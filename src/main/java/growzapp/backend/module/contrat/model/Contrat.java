package growzapp.backend.module.contrat.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import growzapp.backend.module.investissement.model.Investissement;

@Entity
@Table(name = "contrats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "investissement_id", unique = true)
    private Investissement investissement;

    @Column(unique = true, nullable = false)
    private String numeroContrat;

    private String fichierUrl;
    
    private String lienVerification;

    @Column(unique = true)
    private String tokenVerification;

    private String hashSha256;

    private LocalDateTime dateGeneration = LocalDateTime.now();

    // === ARCHIVAGE ===
    // Un contrat est un document légal, jamais supprimable — seulement
    // sorti des listes actives via un archivage.
    @Column(name = "archive_le")
    private LocalDateTime archiveLe;

    @Column(name = "archive_par")
    private String archivePar;
}
