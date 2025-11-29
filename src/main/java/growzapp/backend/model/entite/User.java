// growzapp/backend/model/entite/User.java

package growzapp.backend.model.entite;

import growzapp.backend.model.enumeration.Sexe;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    @Column(nullable = false,unique = true,length = 50)
    private String login;

    @Column(nullable = false)
    private String password;

    private String prenom;
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexe", nullable = false, length = 1)
    private Sexe sexe;

    @Column(unique = true, length = 191)
    private String email;

    private String contact;

    @Column(nullable = false)
    private boolean enabled;

    // === RELATIONS ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localite_id")
    private Localite localite;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("users")
    @JoinTable(name = "user_langues", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "langue_id"))
    private List<Langue> langues = new ArrayList<>();

   @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
   )
    @JsonIgnoreProperties("users")
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "porteur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Projet> projets = new ArrayList<>();

    @OneToMany(mappedBy = "investisseur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Investissement> investissements = new ArrayList<>();

   @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonBackReference  // Empêche la boucle infinie
   private Wallet wallet;
    
   @Column(name = "stripe_account_id", length = 100)
   private String stripeAccountId;
   
}