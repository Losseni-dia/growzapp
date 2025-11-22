package growzapp.backend.controller.api.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.service.DividendeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dividendes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDividendeController {

    private final DividendeService dividendeService;

    @GetMapping
    public ApiResponseDTO<Page<DividendeDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDTO.success(dividendeService.getAllAdmin(pageable));
    }

    @PostMapping
    public ApiResponseDTO<DividendeDTO> create(@Valid @RequestBody DividendeDTO dto) {
        return ApiResponseDTO.success(dividendeService.save(dto))
                .message("Dividende créé avec succès");
    }

    @PutMapping("/{id}")
    public ApiResponseDTO<DividendeDTO> update(@PathVariable Long id, @Valid @RequestBody DividendeDTO dto) {
        dto = new DividendeDTO(id, dto.montantParPart(), dto.statutDividende(), dto.moyenPaiement(),
                dto.datePaiement(), dto.investissementId(), dto.investissementInfo(),
                dto.montantTotal(), dto.fileName());
        return ApiResponseDTO.success(dividendeService.save(dto))
                .message("Dividende mis à jour");
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<String> delete(@PathVariable Long id) {
        dividendeService.deleteById(id);
        return ApiResponseDTO.success("Dividende supprimé");
    }
}