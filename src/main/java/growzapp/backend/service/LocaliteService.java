package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.entite.Localite;
import growzapp.backend.model.entite.Pays;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.repository.PaysRepository;
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
    private final DtoConverter converter;

    // === LISTE ===
    public List<LocaliteDTO> getAll() {
        return localiteRepository.findAll().stream()
                .map(converter::toLocaliteDto)
                .toList();
    }

    // === DÉTAIL ===
    public LocaliteDTO getById(Long id) {
        Localite localite = localiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Localité non trouvée"));
        return converter.toLocaliteDto(localite);
    }

    // === SAUVEGARDE (CREATE / UPDATE) ===
    @Transactional
    public LocaliteDTO save(LocaliteDTO dto) {
        Localite localite = converter.toLocaliteEntity(dto);

        // === GÉRER LE PAYS PAR NOM ===
        if (dto.paysNom() != null && !dto.paysNom().trim().isEmpty()) {
            String nomPays = dto.paysNom().trim();
            Pays pays = paysRepository.findByNomIgnoreCase(nomPays)
                    .orElseGet(() -> {
                        Pays newPays = new Pays();
                        newPays.setNom(nomPays);
                        return paysRepository.save(newPays);
                    });
            localite.setPays(pays);
        }

        localite = localiteRepository.save(localite);
        return converter.toLocaliteDto(localite);
    }

    // === SUPPRESSION ===
    @Transactional
    public void deleteById(Long id) {
        localiteRepository.deleteById(id);
    }
}