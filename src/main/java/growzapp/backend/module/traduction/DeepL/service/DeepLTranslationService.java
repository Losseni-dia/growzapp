package growzapp.backend.module.traduction.DeepL.service;


import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraduction;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeepLTranslationService {

    private final ProjetTraductionRepository traductionRepository;

    @Value("${deepl.api-key}")
    private String deeplApiKey;

    @Value("${deepl.base-url}")
    private String deeplBaseUrl;
    // Langues cibles — FR est la langue source
    private static final List<String> TARGET_LANGUAGES = List.of("EN-US", "ES");

    // Mapping code i18n → code DeepL
    private static final java.util.Map<String, String> DEEPL_LANG_MAP = java.util.Map.of(
            "EN-US", "en",
            "ES", "es");

    /**
     * Traduit automatiquement le libelle et la description d'un projet
     * en anglais et en espagnol via l'API DeepL.
     * Appelé à la création du projet.
     */
    @Transactional
    public void traduireProjet(Projet projet) {
        try {
            com.deepl.api.TranslatorOptions options = new com.deepl.api.TranslatorOptions()
                    .setServerUrl(deeplBaseUrl);
            Translator translator = new Translator(deeplApiKey, options);

            // Sauvegarder d'abord la version française (langue source)
            saveTraduction(projet, "fr", projet.getLibelle(), projet.getDescription());

            // Traduire vers chaque langue cible
            for (String targetLang : TARGET_LANGUAGES) {
                try {
                    String libelleTradu = translate(translator, projet.getLibelle(), targetLang);
                    String descriptionTradu = translate(translator, projet.getDescription(), targetLang);

                    String langCode = DEEPL_LANG_MAP.get(targetLang);
                    saveTraduction(projet, langCode, libelleTradu, descriptionTradu);

                    log.info("Projet {} traduit en {}", projet.getId(), langCode);

                } catch (Exception e) {
                    log.error("Erreur traduction projet {} en {} : {}",
                            projet.getId(), targetLang, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour projet {} : {}",
                    projet.getId(), e.getMessage());
        }
    }

    /**
     * Traduit un texte vers une langue cible via DeepL.
     * La langue source est toujours le français.
     */
    private String translate(Translator translator, String text, String targetLang)
            throws DeepLException, InterruptedException {
        if (text == null || text.isBlank())
            return "";
        TextResult result = translator.translateText(text, "FR", targetLang);
        return result.getText();
    }

    /**
     * Sauvegarde ou met à jour une traduction en base.
     */
    private void saveTraduction(Projet projet, String langue, String libelle, String description) {
        ProjetTraduction traduction = traductionRepository
                .findByProjetIdAndLangue(projet.getId(), langue)
                .orElse(new ProjetTraduction());

        traduction.setProjet(projet);
        traduction.setLangue(langue);
        traduction.setLibelle(libelle);
        traduction.setDescription(description);

        traductionRepository.save(traduction);
    }
}