package growzapp.backend.module.contact.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_replies")
@Getter
@Setter
@NoArgsConstructor
public class ContactReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "contact_message_id", nullable = false)
    private ContactMessage contactMessage;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    @Column(nullable = false, length = 3000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    public void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }
}
