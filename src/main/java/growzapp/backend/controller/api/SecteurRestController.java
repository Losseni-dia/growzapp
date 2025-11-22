package growzapp.backend.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.secteurDTO.SecteurDTO;
import growzapp.backend.service.SecteurService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/secteurs") // ← spécifique
@RequiredArgsConstructor
public class SecteurRestController {

    private final SecteurService secteurService;

    @GetMapping
    public ApiResponseDTO<List<SecteurDTO>> getAll() {
        return ApiResponseDTO.success(secteurService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponseDTO<SecteurDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(secteurService.getById(id));
    }

    @PostMapping
    public ApiResponseDTO<SecteurDTO> create(@RequestBody SecteurDTO dto) {
        return ApiResponseDTO.success(secteurService.save(dto));
    }

    @PutMapping("/{id}")
    public ApiResponseDTO<SecteurDTO> update(@PathVariable Long id, @RequestBody SecteurDTO dto) {
        dto = new SecteurDTO(id, dto.nom(), dto.projets());
        return ApiResponseDTO.success(secteurService.save(dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        secteurService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}

