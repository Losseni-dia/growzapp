package growzapp.backend.module.user.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Corps de la requête de connexion")
public class LoginRequest {

    @Schema(description = "Identifiant de connexion (login ou email)", example = "john.doe")
    private String login;

    @Schema(description = "Mot de passe de l'utilisateur", example = "motDePasse123!", format = "password")
    private String password;
}
