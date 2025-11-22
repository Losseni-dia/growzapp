package growzapp.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>  {
    
}
