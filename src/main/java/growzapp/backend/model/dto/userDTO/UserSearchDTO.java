package growzapp.backend.model.dto.userDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDTO {
    private Long id;
    private String nomComplet;
    private String login;
    private String image;
}