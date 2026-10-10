package growzapp.backend.module.document.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Archivage d'un document par un utilisateur donné — strictement personnel,
// comme dans Gmail : chaque utilisateur (admin, porteur, investisseur) peut
// archiver un document pour lui-même sans que ça change quoi que ce soit
// pour les autres utilisateurs qui ont accès au même document.
@Entity
@Table(name = "document_archives", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
public class DocumentArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt = LocalDateTime.now();
}
