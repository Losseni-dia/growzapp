package growzapp.backend.module.projet.mapper;


import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import growzapp.backend.module.projet.dto.ProjetBrouillonDTO;
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
    @Mapping(target = "adresse", source = "siteProjet.adresse")
    @Mapping(target = "dureeMois", source = "dureeMois")
    ProjetDTO toDto(Projet projet);

    // googleMapsUrl n'a jamais été mappé (ni @Mapping ni méthode homonyme sur
    // Projet) : le champ restait toujours null malgré le lien "Voir sur
    // Google Maps" affiché çà et là côté frontend. Calculé ici une fois les
    // autres champs posés, à partir des coordonnées déjà mappées ci-dessus.
    @AfterMapping
    default void mapGoogleMapsUrl(@MappingTarget ProjetDTO dto) {
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            dto.setGoogleMapsUrl(
                    "https://www.google.com/maps/dir/?api=1&destination=" + dto.getLatitude() + "," + dto.getLongitude());
        }
    }

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

    // --- BROUILLON DTO -> ENTITY (Pour l'enregistrement d'un brouillon) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "statutProjet", constant = "BROUILLON")
    @Mapping(target = "partsDisponible", expression = "java(dto.partsDisponible() != null ? dto.partsDisponible() : 0)")
    @Mapping(target = "roiProjete", expression = "java(dto.roiProjete() != null ? dto.roiProjete() : 0.0)")
    @Mapping(target = "montantCollecte", ignore = true)
    @Mapping(target = "partsPrises", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "porteur", ignore = true)
    @Mapping(target = "siteProjet", ignore = true)
    @Mapping(target = "secteur", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "investissements", ignore = true)
    Projet toEntity(ProjetBrouillonDTO dto);
}