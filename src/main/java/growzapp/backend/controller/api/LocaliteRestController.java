package growzapp.backend.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.service.LocaliteService;

import java.util.List;

@RestController
@RequestMapping("/api/localites")
@RequiredArgsConstructor
public class LocaliteRestController {

    private final LocaliteService localiteService;

    @GetMapping
    public ApiResponseDTO<List<LocaliteDTO>> getAll() {
        return ApiResponseDTO.success(localiteService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponseDTO<LocaliteDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(localiteService.getById(id));
    }

    @PostMapping
    public ApiResponseDTO<LocaliteDTO> create(@RequestBody LocaliteDTO dto) {
        return ApiResponseDTO.success(localiteService.save(dto));
    }

    @PutMapping("/{id}")
    public ApiResponseDTO<LocaliteDTO> update(@PathVariable Long id, @RequestBody LocaliteDTO dto) {
        dto = new LocaliteDTO(
                id,
                dto.codePostal(),
                dto.nom(),
                dto.paysNom(),
                dto.users(),
                dto.localisations());
        return ApiResponseDTO.success(localiteService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        localiteService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}