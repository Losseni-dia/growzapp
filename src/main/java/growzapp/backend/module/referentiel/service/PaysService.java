package growzapp.backend.module.referentiel.service;

import growzapp.backend.module.referentiel.model.Pays;
import growzapp.backend.module.referentiel.repository.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaysService {

    private final PaysRepository paysRepository;

    public List<Pays> getAll() {
        return paysRepository.findAll();
    }

    public Pays getById(Long id) {
        return paysRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pays non trouvé (id=" + id + ")"));
    }

    @Transactional
    public Pays save(Pays pays) {
        return paysRepository.save(pays);
    }

    @Transactional
    public void deleteById(Long id) {
        paysRepository.deleteById(id);
    }
}
