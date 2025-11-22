package growzapp.backend.model.dto.localiteDTO;

import java.util.List; 

public record LocaliteDTO(
        Long id,
        String codePostal,
        String nom,
        String paysNom,
        List<UserInfo> users,
        List<SiteInfo> localisations // ← avec contact
) {
    public record UserInfo(String nom, String prenom, String email) {
    }

    public record SiteInfo(Long id, String nom, String contact) {
    } // ← contact
}