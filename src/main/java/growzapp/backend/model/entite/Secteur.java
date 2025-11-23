// src/main/java/growzapp/backend/model/entite/Secteur.java
package growzapp.backend.model.entite;

import jakarta.persistence.*;
import lombok.*;

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

    // AJOUTE CE CONSTRUCTEUR (5 secondes)
    public Secteur(String nom) {
        this.nom = nom;
    }
}