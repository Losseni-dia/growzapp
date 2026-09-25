package growzapp.backend.module.traduction.DeepL.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fiche_bio_traductions", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "langue" }))
@Data
@NoArgsConstructor
public class FicheBioTraduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 5)
    private String langue; // "fr", "en", "es"

    @Column(columnDefinition = "TEXT")
    private String bio;
}
