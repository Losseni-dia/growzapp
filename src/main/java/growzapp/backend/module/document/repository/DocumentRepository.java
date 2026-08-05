package growzapp.backend.module.document.repository;

import growzapp.backend.module.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByProjetId(Long projetId);

    List<Document> findByProjetIdAndStatut(Long projetId, growzapp.backend.module.document.enums.StatutDocument statut);

    long countByProjetIdAndStatut(Long projetId, growzapp.backend.module.document.enums.StatutDocument statut);
}
