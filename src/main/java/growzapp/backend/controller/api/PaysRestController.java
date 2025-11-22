package growzapp.backend.controller.api;


import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.paysDTO.PaysDTO;
import growzapp.backend.service.PaysService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pays") // ← spécifique
@RequiredArgsConstructor
public class PaysRestController {

    private final PaysService paysService;

    @GetMapping
    public ApiResponseDTO<List<PaysDTO>> getAll() {
        return ApiResponseDTO.success(paysService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponseDTO<PaysDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(paysService.getById(id));
    }

    @PostMapping
    public ApiResponseDTO<PaysDTO> create(@RequestBody PaysDTO dto) {
        return ApiResponseDTO.success(paysService.save(dto));
    }

    @PutMapping("/{id}")
    public ApiResponseDTO<PaysDTO> update(@PathVariable Long id, @RequestBody PaysDTO dto) {
        dto = new PaysDTO(id, dto.nom(), dto.localites());
        return ApiResponseDTO.success(paysService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        paysService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}