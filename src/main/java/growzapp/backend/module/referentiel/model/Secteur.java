package growzapp.backend.module.referentiel.model;

import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "secteurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Secteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @OneToMany(mappedBy = "secteur")
    private List<Projet> projets = new ArrayList<>();

    public Secteur(String nom) {
        this.nom = nom;
    }
}
