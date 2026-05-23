package growzapp.backend.module.facture.mapper;

import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.facture.model.Facture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FactureMapper {

    @Mapping(source = "dividende.id", target = "dividendeId")
    @Mapping(source = "investisseur.id", target = "investisseurId")
    @Mapping(target = "investisseurNom", expression = "java(facture.getInvestisseur() != null ? facture.getInvestisseur().getPrenom() + \" \" + facture.getInvestisseur().getNom() : \"Inconnu\")")
    FactureDTO toFactureDto(Facture facture);

    List<FactureDTO> toFactureDtoList(List<Facture> factures);
}
