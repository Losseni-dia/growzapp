package growzapp.backend.module.settings.service;

import growzapp.backend.module.settings.dto.SiteSettingDTO;
import growzapp.backend.module.settings.model.SiteSetting;
import growzapp.backend.module.settings.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteSettingService {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("fr", "en", "es");

    private final SiteSettingRepository siteSettingRepository;

    public SiteSettingDTO get() {
        return new SiteSettingDTO(getOrCreate().getDefaultLanguage());
    }

    public SiteSettingDTO updateDefaultLanguage(String language) {
        if (language == null || !SUPPORTED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Langue non supportée : " + language);
        }
        SiteSetting setting = getOrCreate();
        setting.setDefaultLanguage(language);
        siteSettingRepository.save(setting);
        return new SiteSettingDTO(setting.getDefaultLanguage());
    }

    private SiteSetting getOrCreate() {
        return siteSettingRepository.findById(SiteSetting.SINGLETON_ID)
                .orElseGet(() -> siteSettingRepository.save(
                        new SiteSetting(SiteSetting.SINGLETON_ID, "fr")));
    }
}
