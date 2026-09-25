package growzapp.backend.module.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vue admin d'une fiche de présentation de porteur, avec toutes les infos pour décision")
public record FichePorteurAdminDTO(
        Long userId,
        String nom,
        String prenom,
        String login,
        String email,
        String photoUrl,
        String bio,
        String statutJuridique,
        String raisonSociale,
        Integer anneesExperience,
        String projetsPrecedents,
        List<Long> projetsMisEnAvantIds,
        String contactTelephone,
        String contactEmail,
        String siteWeb,
        String linkedin,
        String reseauxAutres,
        String ficheStatut,
        LocalDateTime submittedAt,
        String kycStatus
) {
}
