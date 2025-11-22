// src/main/java/growzapp/backend/model/dto/AuthDTO/LoginResponse.java
package growzapp.backend.model.dto.AuthDTO;

import growzapp.backend.model.dto.userDTO.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor // ← CETTE ANNOTATION CRÉE LE CONSTRUCTEUR QU'IL TE MANQUE
public class LoginResponse {
    private String token;
    private UserDTO user; // ← CHANGÉ DE User → UserDTO
}