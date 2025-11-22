package growzapp.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.secteurDTO.SecteurDTO;
import growzapp.backend.model.entite.Secteur;
import growzapp.backend.repository.SecteurRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecteurService {

    private final SecteurRepository secteurRepository;
    private final DtoConverter converter;

    public List<SecteurDTO> getAll() {
        return secteurRepository.findAll().stream()
                .map(converter::toSecteurDto)
                .toList();
    }

    public SecteurDTO getById(Long id) {
        Secteur secteur = secteurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Secteur non trouvé"));
        return converter.toSecteurDto(secteur);
    }

    @Transactional
    public SecteurDTO save(SecteurDTO dto) {
        Secteur secteur = converter.toSecteurEntity(dto);
        secteur = secteurRepository.save(secteur);
        return converter.toSecteurDto(secteur);
    }

    @Transactional
    public void deleteById(Long id) {
        secteurRepository.deleteById(id);
    }
}