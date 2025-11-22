// src/main/java/growzapp/backend/model/dto/userDTO/UserUpdateDTO.java

package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.langueDTO.LangueDTO;
import growzapp.backend.model.enumeration.Sexe;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserUpdateDTO {

    private Long id;

    private String image; // base64 ou null

    @Size(min = 3, max = 60)
    private String login;

    private String password; // optionnel

    private String prenom;
    private String nom;
    private Sexe sexe;

    @Email
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String contact;

    // Localité
    private LocaliteDTO localite;

    // Langues (plusieurs)
    private List<LangueDTO> langues = new ArrayList<>();
}