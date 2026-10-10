package growzapp.backend.module.projet.repository;

import growzapp.backend.module.projet.model.ProjetMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjetMessageRepository extends JpaRepository<ProjetMessage, Long> {
    List<ProjetMessage> findByProjetIdOrderByDateEnvoiAsc(Long projetId);
}
