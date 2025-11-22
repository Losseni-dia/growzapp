package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.model.entite.Localite;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.service.LocalisationService;
import lombok.RequiredArgsConstructor;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/localisations")
@RequiredArgsConstructor
public class LocalisationRestController {

    private final LocalisationService localisationService;
    private final LocaliteRepository localiteRepository;

    @GetMapping
    public ApiResponseDTO<List<LocalisationDTO>> getAll() {
        return ApiResponseDTO.success(localisationService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponseDTO<LocalisationDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(localisationService.getById(id));
    }

    @PostMapping
    public ApiResponseDTO<LocalisationDTO> create(@RequestBody LocalisationDTO dto) {
        return ApiResponseDTO.success(localisationService.save(dto));
    }

    @GetMapping("/localites/{localiteId}/create-localisation")
    public String createFormWithLocalite(@PathVariable Long localiteId, Model model) {
        Localite localite = localiteRepository.findById(localiteId)
                .orElseThrow(() -> new RuntimeException("Localité non trouvée"));

        LocalisationDTO dto = new LocalisationDTO(localiteId, null, null, null, null, null, null, localiteId, null,
                null);

        model.addAttribute("localisation", dto);
        model.addAttribute("title", "Créer une localisation à " + localite.getNom());
        return "localisation/form";
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        localisationService.deleteById(id);
        return ApiResponseDTO.success(null);
    }


}
