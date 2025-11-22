package growzapp.backend.model.entite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import growzapp.backend.model.enumeration.StatutPartInvestissement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "investissements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Investissement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_parts_pris")
    private int nombrePartsPris;

    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "pourcent_equity")
    private double valeurPartsPrisEnPourcent;

    private double frais;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_investissement")
    private StatutPartInvestissement statutPartInvestissement = StatutPartInvestissement.EN_ATTENTE;

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User investisseur;

    @OneToOne(mappedBy = "investissement", cascade = CascadeType.ALL, orphanRemoval = true)
    private Contrat contrat;

    @OneToMany(mappedBy = "investissement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dividende> dividendes = new ArrayList<>();

    public List<Dividende> getDividendes() {
        return dividendes;
    }
}


