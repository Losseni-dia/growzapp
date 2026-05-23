package growzapp.backend.module.referentiel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import growzapp.backend.module.user.model.User;

@Entity
@Table(name = "langues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Langue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @ManyToMany(mappedBy = "langues")
    private List<User> users = new ArrayList<>();
}
