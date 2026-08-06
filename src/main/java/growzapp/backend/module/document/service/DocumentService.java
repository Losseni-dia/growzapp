package growzapp.backend.module.document.service;

import growzapp.backend.module.document.model.Document;
import growzapp.backend.module.document.repository.DocumentRepository;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;

    public Document save(Document document) {
        return documentRepository.save(document);
    }

    public Document findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document non trouvé : " + id));
    }

    public List<Document> findByProjetId(Long projetId) {
        return documentRepository.findByProjetId(projetId);
    }

    public boolean hasAccessToProject(User user, Long projetId) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRole().replace("ROLE_", "").equals("ADMIN"));
        if (isAdmin)
            return true;
        Projet projet = projetRepository.findById(projetId).orElse(null);
        if (projet != null && projet.getPorteur().getId().equals(user.getId()))
            return true;
        return investissementRepository.existsByInvestisseurIdAndProjetId(user.getId(), projetId);
    }
    
    public List<Document> findAllForAdmin(Long projetId, growzapp.backend.module.document.enums.StatutDocument statut) {
        if (projetId != null && statut != null) {
            return documentRepository.findByProjetIdAndStatut(projetId, statut);
        }
        if (projetId != null) {
            return documentRepository.findByProjetId(projetId);
        }
        if (statut != null) {
            return documentRepository.findByStatut(statut);
        }
        return documentRepository.findAll();
    }

    public List<Document> findEnAttenteByProjetId(Long projetId) {
        return documentRepository.findByProjetIdAndStatut(
                projetId, growzapp.backend.module.document.enums.StatutDocument.EN_ATTENTE);
    }

    public long countEnAttenteByProjetId(Long projetId) {
        return documentRepository.countByProjetIdAndStatut(
                projetId, growzapp.backend.module.document.enums.StatutDocument.EN_ATTENTE);
    }

    public Document approuver(Long documentId) {
        Document doc = findById(documentId);
        doc.setStatut(growzapp.backend.module.document.enums.StatutDocument.APPROUVE);
        doc.setDateValidation(java.time.LocalDateTime.now());
        return documentRepository.save(doc);
    }

    public Document rejeter(Long documentId) {
        Document doc = findById(documentId);
        doc.setStatut(growzapp.backend.module.document.enums.StatutDocument.REJETE);
        doc.setDateValidation(java.time.LocalDateTime.now());
        return documentRepository.save(doc);
    }

}
