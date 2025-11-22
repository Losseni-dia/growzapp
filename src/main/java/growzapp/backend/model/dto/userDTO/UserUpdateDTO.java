package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.enumeration.Sexe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {

    private Long id;

    private String image;

    @Size(min = 3, max = 60)
    private String login;

    // Mot de passe optionnel
    private String password;

    private String prenom;
    private String nom;
    private Sexe sexe;

    @Email
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String contact;

    private LocaliteDTO localite;

    // Pas de rôles ici → l'utilisateur ne peut pas les modifier lui-même
}