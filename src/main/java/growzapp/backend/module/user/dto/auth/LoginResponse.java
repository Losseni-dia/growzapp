package growzapp.backend.module.user.dto.auth;

import growzapp.backend.module.user.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse de connexion contenant le token JWT et le profil utilisateur")
public class LoginResponse {

    @Schema(
        description = "Token JWT à inclure dans le header Authorization des requêtes sécurisées",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZSJ9.SflKxw"
    )
    private String token;

    @Schema(description = "Profil complet de l'utilisateur connecté")
    private UserDTO user;
}
