// src/main/java/growzapp/backend/model/entite/Document.java

package growzapp.backend.model.entite;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // ← Nom affiché (ex: "Bilan 2024")

    // NOUVEAU CHAMP : le vrai nom du fichier sur le disque
    @Column(nullable = false)
    private String filename; // ← ex: "a1b2c3d4_budget_abidjan.xlsx"

    // On garde "type" pour l'icône et le Content-Type
    @Column(length = 20, nullable = false)
    private String type; // PDF, EXCEL, CSV, IMAGE

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "projet_id")
    private Projet projet;

    // Méthode pratique pour avoir l'URL complète (facultatif mais utile)
    public String getUrl() {
        return "/files/documents/" + this.filename;
    }
}