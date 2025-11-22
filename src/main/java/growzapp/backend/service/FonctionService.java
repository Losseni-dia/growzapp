package growzapp.backend.service;

import growzapp.backend.model.entite.Fonction;
import growzapp.backend.repository.FonctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FonctionService {

    private final FonctionRepository fonctionRepository;

    public List<Fonction> getAll() {
        return fonctionRepository.findAll();
    }

    public Fonction getById(Long id) {
        return fonctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fonction non trouvée"));
    }

    @Transactional
    public Fonction save(Fonction fonction) {
        return fonctionRepository.save(fonction);
    }

    @Transactional
    public void deleteById(Long id) {
        fonctionRepository.deleteById(id);
    }
}