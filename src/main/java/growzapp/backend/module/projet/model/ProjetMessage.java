package growzapp.backend.module.projet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Canal de messagerie par projet entre admin/communicant et investisseurs —
// le porteur est strictement exclu de ce canal. Un message écrit par un
// investisseur a toujours destinataireIds vide (visible par lui-même et
// tous les admins/communicants, jamais par les autres investisseurs). Un
// message écrit par un admin peut être diffusé à tous les investisseurs
// (destinataireIds vide) ou ciblé sur un sous-ensemble.
@Entity
@Table(name = "projet_messages")
@Getter
@Setter
@NoArgsConstructor
public class ProjetMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id", nullable = false)
    private Projet projet;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    // Figé à l'écriture (ADMIN / INVESTISSEUR) plutôt que recalculé à
    // l'affichage, pour rester cohérent avec l'historique même si le rôle de
    // l'auteur change ensuite.
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @ElementCollection
    @CollectionTable(name = "projet_message_destinataires", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "investisseur_id")
    private Set<Long> destinataireIds = new HashSet<>();

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    public void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }
}
