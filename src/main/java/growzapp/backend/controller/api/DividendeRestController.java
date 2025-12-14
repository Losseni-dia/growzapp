package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.DividendeService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dividendes")
@RequiredArgsConstructor
public class DividendeRestController {

    private final DividendeService dividendeService;
    private final UserRepository userRepository;

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
        // Conservation des champs calculés et de l'ID
        DividendeDTO updated = new DividendeDTO(
                id,
                dto.montantParPart(),
                dto.statutDividende(),
                dto.moyenPaiement(),
                dto.datePaiement(),
                dto.investissementId(),
                dto.investissementInfo(),
                dto.montantTotal(),
                dto.fileName(),
                dto.factureUrl(), // ← LE 10ÈME ARGUMENT MANQUAIT !
                dto.facture(),
                dto.motif()
        );

        return ApiResponseDTO.success(dividendeService.save(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        dividendeService.deleteById(id);
        return ApiResponseDTO.success(null);
    }


    @GetMapping("/mes-dividendes")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<DividendeDTO>> getMesDividendes(Authentication authentication) {
        String login = authentication.getName(); // le login (email ou username)

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<DividendeDTO> mesDividendes = dividendeService.getByInvestisseurId(user.getId());
        return ApiResponseDTO.success(mesDividendes);
    }

    
}