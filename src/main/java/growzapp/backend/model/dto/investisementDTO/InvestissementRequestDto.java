package growzapp.backend.model.dto.investisementDTO;

// InvestissementRequestDto.java
public record InvestissementRequestDto(
        Long projetId,
        int nombrePartsPris, 
        Double frais,
        Long investisseurId
) {
}
