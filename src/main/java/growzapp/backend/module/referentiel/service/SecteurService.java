package growzapp.backend.module.referentiel.service;

import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecteurService {

    private final SecteurRepository secteurRepository;

    public List<Secteur> getAll() {
        return secteurRepository.findAll();
    }

    public Secteur getById(Long id) {
        return secteurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé (id=" + id + ")"));
    }

    @Transactional
    public Secteur save(Secteur secteur) {
        return secteurRepository.save(secteur);
    }

    @Transactional
    public void deleteById(Long id) {
        secteurRepository.deleteById(id);
    }
}
