package growzapp.backend.module.referentiel.service;

import growzapp.backend.module.referentiel.model.Langue;
import growzapp.backend.module.referentiel.repository.LangueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LangueService {

    private final LangueRepository langueRepository;

    public List<Langue> getAll() {
        return langueRepository.findAll();
    }

    public Langue getById(Long id) {
        return langueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Langue non trouvée (id=" + id + ")"));
    }

    @Transactional
    public Langue save(Langue langue) {
        return langueRepository.save(langue);
    }

    @Transactional
    public void deleteById(Long id) {
        langueRepository.deleteById(id);
    }
}
