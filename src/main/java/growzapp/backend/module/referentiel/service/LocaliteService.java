package growzapp.backend.module.referentiel.service;

import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.model.Pays;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.repository.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocaliteService {

    private final LocaliteRepository localiteRepository;
    private final PaysRepository paysRepository;

    public List<Localite> getAll() {
        return localiteRepository.findAll();
    }

    public Localite getById(Long id) {
        return localiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localité non trouvée (id=" + id + ")"));
    }

    /**
     * Sauvegarde une localité en résolvant son pays par nom (crée le pays si inexistant).
     *
     * @param localite entité à sauvegarder (id = null pour création, id = x pour update)
     * @param paysNom  nom du pays à résoudre ; ignoré si null ou vide
     */
    @Transactional
    public Localite save(Localite localite, String paysNom) {
        if (paysNom != null && !paysNom.trim().isEmpty()) {
            String nom = paysNom.trim();
            Pays pays = paysRepository.findByNomIgnoreCase(nom)
                    .orElseGet(() -> {
                        Pays p = new Pays();
                        p.setNom(nom);
                        return paysRepository.save(p);
                    });
            localite.setPays(pays);
        }
        return localiteRepository.save(localite);
    }

    @Transactional
    public void deleteById(Long id) {
        localiteRepository.deleteById(id);
    }
}
