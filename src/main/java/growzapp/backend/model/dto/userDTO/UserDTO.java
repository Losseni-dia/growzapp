// growzapp/backend/model/dto/userDTO/UserDTO.java
package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.enumeration.Sexe;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
public class UserDTO {

    private Long id;
    private String image;
    private String login;

    // Jamais renvoyé au front
    private String password;

    private String prenom;
    private String nom;
    private Sexe sexe;
    private String email;
    private String contact;

    private LocaliteDTO localite;

    private String interfaceLanguage;

    private List<String> roles = new ArrayList<>();
    private List<String> langues = new ArrayList<>();

    private List<InvestissementDTO> investissements = new ArrayList<>();
    private List<ProjetDTO> projets = new ArrayList<>();

    // CHAMP OBLIGATOIRE POUR LE FRONT (admin + sécurité)
    private boolean enabled = true; // ← Maintenant bien présent et renvoyé
}