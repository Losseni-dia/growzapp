package growzapp.backend.module.settings.repository;

import growzapp.backend.module.settings.model.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSetting, Long> {
}
