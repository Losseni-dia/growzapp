package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.enumeration.Sexe;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotBlank(message = "Le login est obligatoire")
    @Size(min = 3, max = 60)
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères")
    private String password;

    @NotBlank
    private String prenom;

    @NotBlank
    private String nom;

    @NotNull
    private Sexe sexe;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String contact;
}