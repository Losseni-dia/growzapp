package growzapp.backend.module.contact.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.contact.enums.StatutContact;
import growzapp.backend.module.user.model.User;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String sujet;

    @Column(nullable = false, length = 3000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutContact statut = StatutContact.NOUVEAU;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    // Masquage indépendant par côté : "supprimer" un fil ne l'efface jamais
    // réellement, il disparaît seulement de la liste de celui qui l'a masqué
    // — l'autre partie continue de le voir normalement.
    @Column(name = "hidden_for_user", nullable = false)
    private boolean hiddenForUser = false;

    @Column(name = "hidden_for_admin", nullable = false)
    private boolean hiddenForAdmin = false;

    @JsonIgnore
    @OneToMany(mappedBy = "contactMessage", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateEnvoi ASC")
    private List<ContactReply> reponses = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }
}
