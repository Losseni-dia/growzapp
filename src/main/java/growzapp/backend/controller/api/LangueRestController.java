// src/main/java/growzapp/backend/controller/api/LangueRestController.java

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.langueDTO.LangueDTO;
import growzapp.backend.repository.LangueRepository;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/langues")
@RequiredArgsConstructor
public class LangueRestController {

    private final LangueRepository langueRepository;

    @GetMapping
    @PermitAll // PUBLIC = plus de 401 !
    public ApiResponseDTO<List<LangueDTO>> getAll() {
        List<LangueDTO> dtos = langueRepository.findAll().stream()
                .map(l -> new LangueDTO(l.getId(), l.getNom()))
                .toList();
        return ApiResponseDTO.success(dtos);
    }
}