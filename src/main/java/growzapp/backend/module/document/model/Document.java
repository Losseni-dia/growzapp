package growzapp.backend.module.document.model;

import growzapp.backend.module.projet.model.Projet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
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

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    public String getUrl() {
        return "/files/documents/" + this.filename;
    }
}
