package growzapp.backend.module.referentiel.service;

import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.model.Pays;
import growzapp.backend.module.referentiel.repository.LocalisationRepository;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.repository.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalisationService {

    private final LocalisationRepository localisationRepository;
    private final LocaliteRepository localiteRepository;
    private final PaysRepository paysRepository;

    public List<Localisation> getAll() {
        return localisationRepository.findAll();
    }

    public Localisation getById(Long id) {
        return localisationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localisation non trouvée (id=" + id + ")"));
    }

    /**
     * Sauvegarde une localisation en résolvant sa localité par nom.
     * Si le nom contient "Ville, Pays" (format avec virgule), le pays est extrait et créé si besoin.
     *
     * @param localisation entité à sauvegarder
     * @param localiteNom  nom de la localité à résoudre ; ignoré si null ou vide
     */
    @Transactional
    public Localisation save(Localisation localisation, String localiteNom) {
        if (localiteNom != null && !localiteNom.trim().isEmpty()) {
            String nom = localiteNom.trim();
            Localite localite = localiteRepository.findByNomIgnoreCase(nom)
                    .orElseGet(() -> {
                        Localite l = new Localite();
                        String[] parts = nom.split(",\\s*");
                        l.setNom(parts.length >= 2 ? parts[0].trim() : nom);
                        if (parts.length >= 2) {
                            String paysNom = parts[1].trim();
                            Pays pays = paysRepository.findByNomIgnoreCase(paysNom)
                                    .orElseGet(() -> {
                                        Pays p = new Pays();
                                        p.setNom(paysNom);
                                        return paysRepository.save(p);
                                    });
                            l.setPays(pays);
                        }
                        return localiteRepository.save(l);
                    });
            localisation.setLocalite(localite);
        }
        return localisationRepository.save(localisation);
    }

    @Transactional
    public void deleteById(Long id) {
        localisationRepository.deleteById(id);
    }

    public double calculerDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public String genererLienItineraire(Localisation loc) {
        if (loc.getLatitude() == null || loc.getLongitude() == null) return null;
        return String.format("https://www.google.com/maps/dir/?api=1&destination=%s,%s",
                loc.getLatitude(), loc.getLongitude());
    }
}
