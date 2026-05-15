package growzapp.backend.module.projet.mapper;


import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.model.Projet;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjetMapper {

    // --- ENTITY -> DTO (Pour l'affichage) ---
    @Mapping(target = "secteurId", source = "secteur.id")
    @Mapping(target = "secteurNom", source = "secteur.nom")
    @Mapping(target = "siteId", source = "siteProjet.id")
    @Mapping(target = "siteNom", source = "siteProjet.nom")
    @Mapping(target = "localiteId", source = "siteProjet.localite.id")
    @Mapping(target = "localiteNom", source = "siteProjet.localite.nom")
    @Mapping(target = "paysNom", source = "siteProjet.localite.pays.nom")
    @Mapping(target = "paysId", source = "siteProjet.localite.pays.id")
    @Mapping(target = "porteurId", source = "porteur.id")
    @Mapping(target = "porteurNom", expression = "java(projet.getPorteur() != null ? projet.getPorteur().getPrenom() + \" \" + projet.getPorteur().getNom() : null)")
    @Mapping(target = "latitude", source = "siteProjet.latitude")
    @Mapping(target = "longitude", source = "siteProjet.longitude")
    @Mapping(target = "what3words", source = "siteProjet.what3words")
    @Mapping(target = "dureeMois", source = "dureeMois")
    // On laisse googleMapsUrl pour le traitement manuel si besoin ou via une
    // expression
    ProjetDTO toDto(Projet projet);

    // --- CREATE DTO -> ENTITY (Pour la création) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true) // Géré par @PrePersist dans l'entité
    @Mapping(target = "statutProjet", constant = "SOUMIS")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "montantCollecte", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "partsPrises", constant = "0")
    // Les relations complexes sont ignorées car créées manuellement dans le Service
    @Mapping(target = "porteur", ignore = true)
    @Mapping(target = "siteProjet", ignore = true)
    @Mapping(target = "secteur", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "investissements", ignore = true)
    Projet toEntity(ProjetCreateDTO dto);


    List<ProjetDTO> toDtoList(List<Projet> projets);
}