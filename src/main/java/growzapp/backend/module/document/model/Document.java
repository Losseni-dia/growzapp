package growzapp.backend.module.document.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import growzapp.backend.module.document.enums.StatutDocument;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@ToString(exclude = { "projet", "uploadePar" })
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(nullable = false)
    private String filename;

    @Column(length = 20, nullable = false)
    private String type;

    // Ce que le document représente — renseigné à l'upload, notamment
    // utile pour les documents ajoutés par le porteur
    @Column(columnDefinition = "TEXT")
    private String description;

    // EN_ATTENTE si uploadé par un porteur, APPROUVE d'office si uploadé
    // par un admin — voir DocumentService pour la logique de décision
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDocument statut = StatutDocument.APPROUVE;

    private LocalDateTime uploadedAt = LocalDateTime.now();
    private LocalDateTime dateValidation;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploade_par_id")
    private User uploadePar;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    public String getUrl() {
        return "/files/documents/" + this.filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Document))
            return false;
        Document that = (Document) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}