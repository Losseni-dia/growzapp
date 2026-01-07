// growzapp/backend/model/dto/userDTO/UserDTO.java
package growzapp.backend.model.dto.userDTO;

import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.enumeration.KycStatus;
import growzapp.backend.model.enumeration.Sexe;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// growzapp/backend/model/dto/userDTO/UserDTO.java

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
public class UserDTO {

    private Long id;
    private String image;
    private String login;
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
    private boolean enabled = true;

    // === CHAMPS KYC AJOUTÉS ===
    private KycStatus kycStatus;
    private String kycNumeroPiece;
    private java.time.LocalDate kycDateDelivrance;
    private java.time.LocalDate kycDateExpiration;
    private String kycRectoUrl;
    private String kycVersoUrl;
    private String kycSelfieUrl;
    private String kycCommentaireRejet;
}