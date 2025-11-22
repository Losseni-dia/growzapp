// growzapp/backend/model/entite/User.java

package growzapp.backend.model.entite;

import growzapp.backend.model.enumeration.Sexe;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String image;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    private String prenom;
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexe", nullable = false, length = 1)
    private Sexe sexe;

    @Column(unique = true)
    private String email;

    private String contact;

    @Column(nullable = false)
    private boolean enabled;

    // === RELATIONS ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localite_id")
    private Localite localite;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_langues", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "langue_id"))
    private List<Langue> langues = new ArrayList<>();

   @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "porteur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Projet> projets = new ArrayList<>();

    @OneToMany(mappedBy = "investisseur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Investissement> investissements = new ArrayList<>();
   
}