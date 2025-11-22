// src/main/java/growzapp/backend/service/LocalisationService.java
package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.model.entite.Localisation;
import growzapp.backend.model.entite.Localite;
import growzapp.backend.model.entite.Pays;
import growzapp.backend.repository.LocalisationRepository;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.repository.PaysRepository;
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
    private final DtoConverter converter;
    private final PaysRepository paysRepository;

    public List<LocalisationDTO> getAll() {
        return localisationRepository.findAll().stream()
                .map(converter::toLocalisationDto)
                .toList();
    }

    public LocalisationDTO getById(Long id) {
        Localisation localisation = localisationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localisation non trouvée"));
        return converter.toLocalisationDto(localisation);
    }

    @Transactional
    public LocalisationDTO save(LocalisationDTO dto) {
        Localisation localisation = converter.toLocalisationEntity(dto);

        // Gérer la localité par nom (création si inexistante)
        if (dto.localiteNom() != null && !dto.localiteNom().trim().isEmpty()) {
            String nomLocalite = dto.localiteNom().trim();

            Localite localite = localiteRepository.findByNomIgnoreCase(nomLocalite)
                    .orElseGet(() -> {
                        Localite newLocalite = new Localite();
                        newLocalite.setNom(nomLocalite);

                        // Extraire pays si format "Ville, Pays"
                        String[] parts = nomLocalite.split(",\\s*");
                        if (parts.length >= 2) {
                            newLocalite.setNom(parts[0].trim());
                            String paysNom = parts[1].trim();
                            Pays pays = paysRepository.findByNomIgnoreCase(paysNom)
                                    .orElseGet(() -> {
                                        Pays newPays = new Pays();
                                        newPays.setNom(paysNom);
                                        return paysRepository.save(newPays);
                                    });
                            newLocalite.setPays(pays);
                        }
                        return localiteRepository.save(newLocalite);
                    });

            localisation.setLocalite(localite);
        }

        localisation = localisationRepository.save(localisation);
        return converter.toLocalisationDto(localisation);
    }

    @Transactional
    public void deleteById(Long id) {
        localisationRepository.deleteById(id);
    }
}