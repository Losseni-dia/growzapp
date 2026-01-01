package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.investisementDTO.InvestissementCreateDTO; // ← Tu vas créer ce DTO
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.InvestissementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/investissements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvestissementController {

    private final InvestissementRepository investissementRepository;
    private final InvestissementService investissementService;
    private final UserRepository userRepository;
    private final DtoConverter converter;



    // ──────────────────────────────────────────────────────
    // 1. Créer un investissement (tout utilisateur connecté)
    // ──────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<InvestissementDTO> create(
            @RequestBody InvestissementCreateDTO dto,
            Authentication auth) {

        User investisseur = (User) auth.getPrincipal();
        dto.setInvestisseurId(investisseur.getId());

        InvestissementDTO created = investissementService.save(dto, null);

        return ApiResponseDTO.success(created)
                .message("Investissement enregistré avec succès. En attente de validation admin.");
    }

    // ──────────────────────────────────────────────────────
    // 2. Modifier un investissement (admin only ou investisseur avant validation)
    // ──────────────────────────────────────────────────────
 

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @investissementSecurityService.isOwnerAndPending(#id, authentication.principal.id)")
    public ApiResponseDTO<InvestissementDTO> update(
            @PathVariable Long id,
            @RequestBody InvestissementCreateDTO dto,
            Authentication auth) {

        User investisseur = (User) auth.getPrincipal();
        dto.setInvestisseurId(investisseur.getId());

        InvestissementDTO updated = investissementService.save(dto, id);

        return ApiResponseDTO.success(updated)
                .message("Investissement modifié avec succès");
    }


    // ──────────────────────────────────────────────────────
    // 3. Mes investissements (tout connecté)
    // ──────────────────────────────────────────────────────
    @GetMapping("/mes-investissements")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<InvestissementDTO>> getMyInvestments(
            Authentication authentication) { // ← plus simple et plus sûr que @AuthenticationPrincipal

        // On utilise la méthode qui charge les rôles → zéro risque de LazyException
        User user = userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<InvestissementDTO> mesInvestissements = investissementService.getByInvestisseurId(user.getId());

        return ApiResponseDTO.success(mesInvestissements);
    }

    // ──────────────────────────────────────────────────────
    // 2. Tous les investissements (admin only)
    // ──────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<List<InvestissementDTO>> getAll() {
        return ApiResponseDTO.success(investissementService.getAllInvestissements());
    }

    // ──────────────────────────────────────────────────────
    // 3. Détail d'un investissement (admin + l'investisseur concerné)
    // ──────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @investissementSecurityService.isOwner(#id, authentication.principal.id)")
    public ApiResponseDTO<InvestissementDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }

    // ──────────────────────────────────────────────────────
    // 4. Valider un investissement (admin only)
    // ──────────────────────────────────────────────────────
    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<InvestissementDTO> valider(@PathVariable Long id) throws Exception {
        Investissement investissement = investissementService.validerInvestissement(id);
        InvestissementDTO dto = converter.toInvestissementDto(investissement);
        return ApiResponseDTO.success(dto)
                .message("Investissement validé et contrat envoyé par email");
    }

    // ──────────────────────────────────────────────────────
    // 5. Annuler un investissement (admin only)
    // ──────────────────────────────────────────────────────
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<InvestissementDTO> annuler(@PathVariable Long id) {
        investissementService.annulerInvestissement(id);
        // Tu peux recharger l'investissement si tu veux le renvoyer
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement annulé");
    }
   
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDTO<String> delete(@PathVariable Long id) {
        investissementRepository.deleteById(id);
        return ApiResponseDTO.success("Investissement supprimé");
    }



   



    
}