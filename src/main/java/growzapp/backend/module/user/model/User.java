// growzapp/backend/model/entite/User.java
package growzapp.backend.module.user.model;

import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.referentiel.model.Langue;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.user.enums.Sexe;
import growzapp.backend.module.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "image", length = 255)
    private String image;

    @Column(nullable = false,unique = true,length = 50)
    private String login;

    @Column(nullable = true)
    private String password;

    private String prenom;
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexe", nullable = true, length = 1)
    private Sexe sexe;

    @Column(unique = true, length = 191)
    private String email;

    private String contact;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "cgu_accepted_at")
    private LocalDateTime cguAcceptedAt;

    @Column(name = "rgpd_accepted_at")
    private LocalDateTime rgpdAcceptedAt;

    @Column(name = "cgu_version_accepted")
    private String cguVersionAccepted; // Ex: "v1.0"

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
    private Set<Projet> projets = new HashSet<>();

    @OneToMany(mappedBy = "investisseur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Investissement> investissements = new HashSet<>();

   @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonBackReference  // Empêche la boucle infinie
   private Wallet wallet;
    
   @Column(name = "stripe_account_id", length = 100)
   private String stripeAccountId;

   @Column(name = "interface_language", length = 5)
   private String interfaceLanguage = "fr";


   // === NOUVEAUX CHAMPS KYC AJOUTÉS ===

   @Column(name = "kyc_numero_piece", length = 50)
   private String kycNumeroPiece;

   @Column(name = "kyc_date_delivrance")
   private LocalDate kycDateDelivrance;

   @Column(name = "kyc_date_expiration")
   private LocalDate kycDateExpiration;

   // On remplace kycDocumentUrl par des champs spécifiques ou on les ajoute
   @Column(name = "kyc_recto_url")
   private String kycRectoUrl;

   @Column(name = "kyc_verso_url")
   private String kycVersoUrl;

   @Column(name = "kyc_selfie_url")
   private String kycSelfieUrl;

   // === VOS CHAMPS EXISTANTS ===
   @Enumerated(EnumType.STRING)
   @Column(name = "kyc_status", nullable = false, length = 20)
   private KycStatus kycStatus = KycStatus.NON_SOUMIS;

   @Column(name = "date_naissance")
   private LocalDate dateNaissance;

   @Column(name = "adresse_residencielle")
   private String adresseResidencielle;

   @Column(name = "kyc_date_validation")
   private LocalDateTime kycDateValidation;

   @Column(name = "kyc_commentaire_rejet")
   private String kycCommentaireRejet;
   
}