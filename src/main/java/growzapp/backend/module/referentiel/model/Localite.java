package growzapp.backend.module.referentiel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    @JsonIgnoreProperties("localites")
    @JoinColumn(name = "pays_id")
    private Pays pays;

    @OneToMany(mappedBy = "localite")
    private List<Localisation> localisations = new ArrayList<>();

    @OneToMany(mappedBy = "localite", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();
}
