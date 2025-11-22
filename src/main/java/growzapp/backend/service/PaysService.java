package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.paysDTO.PaysDTO;
import growzapp.backend.model.entite.Pays;
import growzapp.backend.repository.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaysService {

    private final PaysRepository paysRepository;
    private final DtoConverter converter;

    public List<PaysDTO> getAll() {
        return paysRepository.findAll().stream()
                .map(converter::toPaysDto)
                .toList();
    }

    public PaysDTO getById(Long id) {
        Pays pays = paysRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pays non trouvé"));
        return converter.toPaysDto(pays);
    }

    @Transactional
    public PaysDTO save(PaysDTO dto) {
        Pays pays = converter.toPaysEntity(dto);
        pays = paysRepository.save(pays);
        return converter.toPaysDto(pays);
    }

    @Transactional
    public void deleteById(Long id) {
        paysRepository.deleteById(id);
    }
}