package growzapp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByProjetId(Long projetId);
    
}
