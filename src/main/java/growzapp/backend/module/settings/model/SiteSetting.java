package growzapp.backend.module.settings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ligne de configuration unique (id=1) — langue affichée aux visiteurs non
 * connectés. Ne remplace pas la préférence individuelle de langue de chaque
 * utilisateur connecté (User.interfaceLanguage), qui reste prioritaire.
 */
@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteSetting {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "default_language", nullable = false, length = 5)
    private String defaultLanguage = "fr";
}
