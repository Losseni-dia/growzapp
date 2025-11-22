package growzapp.backend.model.entite;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "localites")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Localite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codePostal;
    private String nom;

    @ManyToOne
    @JoinColumn(name = "pays_id")
    private Pays pays;

    // Relation avec SiteProjet (Localisation)
    @OneToMany(mappedBy = "localite")
    private List<Localisation> localisations = new ArrayList<>();

    // === NOUVEAU : 1 Localite → N Users (habitants) ===
    @OneToMany(mappedBy = "localite", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();
}
