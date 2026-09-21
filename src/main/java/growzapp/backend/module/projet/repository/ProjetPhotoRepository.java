package growzapp.backend.module.projet.repository;

import growzapp.backend.module.projet.model.ProjetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjetPhotoRepository extends JpaRepository<ProjetPhoto, Long> {

    List<ProjetPhoto> findByProjetIdOrderByCreatedAtAsc(Long projetId);

    void deleteByIdAndProjetId(Long id, Long projetId);
}
