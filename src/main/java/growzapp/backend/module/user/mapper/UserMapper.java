package growzapp.backend.module.user.mapper;

import growzapp.backend.module.investissement.mapper.InvestissementMapper;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper; // <-- L'import corrigé
import growzapp.backend.module.referentiel.model.Langue;
import growzapp.backend.module.user.dto.UserDTO;
import growzapp.backend.module.user.model.Role;
import growzapp.backend.module.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {
                ProjetMapper.class,
                InvestissementMapper.class,
                ReferentielMapper.class // <-- On utilise le ReferentielMapper qui contient la Localite
})
public interface UserMapper {

        // La clause 'uses' s'occupe de déléguer automatiquement le mapping des
        // sous-objets complexes
        UserDTO toDto(User user);

        @Mapping(target = "localite", ignore = true)
        @Mapping(target = "langues", ignore = true)
        @Mapping(target = "roles", ignore = true)
        @Mapping(target = "projets", ignore = true)
        @Mapping(target = "investissements", ignore = true)
        @Mapping(target = "wallet", ignore = true)
        User toEntity(UserDTO dto);

        // --- Traducteurs d'éléments de collection (MapStruct les applique
        // automatiquement) ---
        default String mapRole(Role role) {
                return role != null ? role.getRole() : null;
        }

        default String mapLangue(Langue langue) {
                return langue != null ? langue.getNom() : null;
        }
}