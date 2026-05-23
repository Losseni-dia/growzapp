package growzapp.backend.module.referentiel.mapper;

import growzapp.backend.module.referentiel.dto.*;
import growzapp.backend.module.referentiel.model.*;
import growzapp.backend.module.user.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReferentielMapper {

    // ===== PAYS =====
    // Plus besoin d'expression ! MapStruct utilise la méthode mapLocaliteToString
    // ci-dessous
    PaysDTO toPaysDto(Pays pays);

    @Mapping(target = "localites", ignore = true)
    Pays toPaysEntity(PaysDTO dto);

    List<PaysDTO> toPaysDtoList(List<Pays> pays);

    // ===== LOCALITE =====
    @Mapping(source = "pays.nom", target = "paysNom")
    // MapStruct appelle automatiquement toUserInfo et toSiteInfo pour gérer les
    // listes
    LocaliteDTO toLocaliteDto(Localite localite);

    @Mapping(target = "pays", ignore = true)
    @Mapping(target = "localisations", ignore = true)
    @Mapping(target = "users", ignore = true)
    Localite toLocaliteEntity(LocaliteDTO dto);

    List<LocaliteDTO> toLocaliteDtoList(List<Localite> localites);

    // ===== LOCALISATION =====
    @Mapping(source = "localite.nom", target = "localiteNom")
    @Mapping(source = "localite.id", target = "localiteId")
    @Mapping(source = "localite.pays.nom", target = "paysNom")
    LocalisationDTO toLocalisationDto(Localisation localisation);

    @Mapping(target = "localite", ignore = true)
    @Mapping(target = "projets", ignore = true)
    Localisation toLocalisationEntity(LocalisationDTO dto);

    List<LocalisationDTO> toLocalisationDtoList(List<Localisation> localisations);

    // ===== LANGUE =====
    LangueDTO toLangueDto(Langue langue);

    Langue toLangueEntity(LangueDTO dto);

    List<LangueDTO> toLangueDtoList(List<Langue> langues);

    // ===== SECTEUR =====
    // Plus besoin d'expression ! Utilise mapProjetToString
    SecteurDTO toSecteurDto(Secteur secteur);

    @Mapping(target = "projets", ignore = true)
    Secteur toSecteurEntity(SecteurDTO dto);

    List<SecteurDTO> toSecteurDtoList(List<Secteur> secteurs);

    // ========================================================================
    // TRADUCTEURS ÉLÉMENTAIRES (La magie MapStruct)
    // ========================================================================

    // Convertit automatiquement un objet Localite en String (son nom) pour le
    // PaysDTO
    default String mapLocaliteToString(Localite localite) {
        return localite != null ? localite.getNom() : null;
    }

    // Convertit automatiquement un Projet en String (son libellé) pour le
    // Secteur/Localisation
    default String mapProjetToString(growzapp.backend.module.projet.model.Projet projet) {
        return projet != null ? projet.getLibelle() : null;
    }

    // Définit la règle pour mapper UN utilisateur vers UN UserInfo
    LocaliteDTO.UserInfo toUserInfo(User user);

    // Définit la règle pour mapper UNE localisation vers UNE SiteInfo
    @Mapping(source = "localisations.id", target = "id") // Optionnel si les noms matchent déjà
    LocaliteDTO.SiteInfo toSiteInfo(Localisation localisations);
}