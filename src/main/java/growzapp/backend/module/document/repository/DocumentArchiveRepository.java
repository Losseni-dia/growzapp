package growzapp.backend.module.document.repository;

import growzapp.backend.module.document.model.DocumentArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentArchiveRepository extends JpaRepository<DocumentArchive, Long> {
    List<DocumentArchive> findAllByUserId(Long userId);

    Optional<DocumentArchive> findByDocumentIdAndUserId(Long documentId, Long userId);

    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);

    void deleteByDocumentIdAndUserId(Long documentId, Long userId);
}
