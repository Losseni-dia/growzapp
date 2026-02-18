package growzapp.backend.notification.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import growzapp.backend.model.entite.User;
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
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private LocalDateTime date = LocalDateTime.now();
    private boolean isRead = false;

   @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
   @JsonIgnoreProperties({"projets", "investissements", "roles", "wallet", "localite", "langues", "password"}) 
    private User recipient;
}
