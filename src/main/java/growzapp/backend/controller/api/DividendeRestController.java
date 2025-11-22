package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.service.DividendeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dividendes")
@RequiredArgsConstructor
public class DividendeRestController {

    private final DividendeService dividendeService;

    @GetMapping
    public ApiResponseDTO<List<DividendeDTO>> getAll() {
        return ApiResponseDTO.success(dividendeService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponseDTO<DividendeDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(dividendeService.getById(id));
    }

    @PostMapping
    public ApiResponseDTO<DividendeDTO> create(@RequestBody DividendeDTO dto) {
        return ApiResponseDTO.success(dividendeService.save(dto));
    }

    @PutMapping("/{id}")
    public ApiResponseDTO<DividendeDTO> update(@PathVariable Long id, @RequestBody DividendeDTO dto) {
        // On conserve l'ID + champs calculés (montantTotal, fileName,
        // investissementInfo)
        DividendeDTO updated = new DividendeDTO(
                id,
                dto.montantParPart(),
                dto.statutDividende(),
                dto.moyenPaiement(),
                dto.datePaiement(),
                dto.investissementId(),
                dto.investissementInfo(), // ← important : on garde l'info affichée
                dto.montantTotal(),
                dto.fileName());
        return ApiResponseDTO.success(dividendeService.save(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        dividendeService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}