package growzapp.backend.model.entite;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "langues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Langue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom; // ex: "Français"

    // Relation ManyToMany avec User
    @ManyToMany(mappedBy = "langues")
    private List<User> users = new ArrayList<>();
}
