package growzapp.backend.model.entite;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employe_fonctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeFonction {

    @EmbeddedId
    private EmployeFonctionId id;

    @ManyToOne
    @MapsId("employeId")
    @JoinColumn(name = "employe_id")
    private Employe employe;

    @ManyToOne
    @MapsId("fonctionId")
    @JoinColumn(name = "fonction_id")
    private Fonction fonction;

    @Column(name = "date_prise_fonction", nullable = false)
    private LocalDate datePriseFonction;
}