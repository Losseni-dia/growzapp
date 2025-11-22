// UserCreateDTO.java
package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.langueDTO.LangueDTO;
import growzapp.backend.model.enumeration.Sexe;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserCreateDTO {

    @NotBlank(message = "Le login est obligatoire")
    @Size(min = 3, max = 60)
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6)
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;

    @NotBlank
    private String prenom;
    @NotBlank
    private String nom;
    @NotNull
    private Sexe sexe;
    @Email
    @NotBlank
    private String email;
    private String contact;

    // AJOUTÉ : Localité
    private LocaliteDTO localite;

    // AJOUTÉ : Langues (liste d'objets avec id)
    private List<LangueDTO> langues = new ArrayList<>();

    // Pour la photo
    private String image;
}