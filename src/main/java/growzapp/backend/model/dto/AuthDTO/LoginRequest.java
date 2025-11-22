package growzapp.backend.model.dto.AuthDTO;

import lombok.Data;

@Data
public class LoginRequest {
    private String login;
    private String password;
}