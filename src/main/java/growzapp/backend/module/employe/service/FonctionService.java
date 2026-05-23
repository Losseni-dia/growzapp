package growzapp.backend.module.employe.service;

import growzapp.backend.module.employe.model.Fonction;
import growzapp.backend.module.employe.repository.FonctionRepository;
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
