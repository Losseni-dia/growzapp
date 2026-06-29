package growzapp.backend.module.notification.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.module.user.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "notifications")
@Entity
@Schema(description = "Notification envoyée à un utilisateur de la plateforme")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identifiant unique de la notification", example = "42")
    private Long id;

    @Schema(description = "Titre court de la notification", example = "Dividende reçu")
    private String title;

    @Schema(description = "Contenu détaillé de la notification")
    private String content;

    @Schema(description = "Date et heure d'émission", example = "2025-06-15T14:30:00")
    private LocalDateTime date = LocalDateTime.now();

    @Schema(description = "Indique si la notification a été lue", example = "false")
    private boolean isRead = false;

    @Schema(description = "Identifiant numérique du projet lié", example = "7")
    private Long projetId;

    @Schema(description = "Slug du projet lié — utilisé pour la redirection frontend", example = "ferme-solaire-nord")
    private String projetSlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({ "projets", "investissements", "roles", "wallet", "localite", "langues", "password" })
    @Schema(description = "Utilisateur destinataire de la notification")
    private User recipient;
}