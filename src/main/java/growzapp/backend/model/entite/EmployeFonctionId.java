package growzapp.backend.model.entite;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeFonctionId implements java.io.Serializable {
    private Long employeId;
    private Long fonctionId;
}