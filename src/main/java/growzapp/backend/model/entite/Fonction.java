package growzapp.backend.model.entite;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fonctions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fonction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column
    private String description;

    // === RELATION AVEC DATE ===
    @OneToMany(mappedBy = "fonction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeFonction> employers = new ArrayList<>();
}
