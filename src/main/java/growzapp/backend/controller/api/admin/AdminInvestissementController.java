package growzapp.backend.controller.api.admin;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.service.InvestissementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/investissements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvestissementController {

    private final InvestissementRepository investissementRepository;


    private final InvestissementService investissementService;
    private final DtoConverter converter;

    @GetMapping
    public ApiResponseDTO<List<InvestissementDTO>> getAll(
            @RequestParam(required = false) String search) {

        List<InvestissementDTO> investissements = investissementService.getAllAdmin(search);
        return ApiResponseDTO.success(investissements);
    }


    @GetMapping("/{id}")
    public ApiResponseDTO<InvestissementDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }

    @PostMapping("/{id}/valider")
    public ApiResponseDTO<InvestissementDTO> valider(@PathVariable Long id) throws Exception {
        Investissement inv = investissementService.validerInvestissement(id);
        return ApiResponseDTO.success(converter.toInvestissementDto(inv))
                .message("Investissement validé et contrat envoyé");
    }

    @PostMapping("/{id}/annuler")
    public ApiResponseDTO<InvestissementDTO> annuler(@PathVariable Long id) {
        investissementService.annulerInvestissement(id);
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement annulé");
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<String> delete(@PathVariable Long id) {
        investissementRepository.deleteById(id); // ou ton repo direct
        return ApiResponseDTO.success("Investissement supprimé");
    }
}