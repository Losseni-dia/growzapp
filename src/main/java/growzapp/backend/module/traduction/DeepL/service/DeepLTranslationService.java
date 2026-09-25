package growzapp.backend.module.traduction.DeepL.service;


import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.news.model.News;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
import growzapp.backend.module.traduction.DeepL.model.ArticleMarketTraduction;
import growzapp.backend.module.traduction.DeepL.model.FicheBioTraduction;
import growzapp.backend.module.traduction.DeepL.model.NewsTraduction;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraduction;
import growzapp.backend.module.traduction.DeepL.model.SecteurTraduction;
import growzapp.backend.module.traduction.DeepL.repository.ArticleMarketTraductionRepository;
import growzapp.backend.module.traduction.DeepL.repository.FicheBioTraductionRepository;
import growzapp.backend.module.traduction.DeepL.repository.NewsTraductionRepository;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.traduction.DeepL.repository.SecteurTraductionRepository;
import growzapp.backend.module.user.model.User;
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
    private final SecteurTraductionRepository secteurTraductionRepository;
    private final SecteurRepository secteurRepository;
    private final ArticleMarketTraductionRepository articleMarketTraductionRepository;
    private final FicheBioTraductionRepository ficheBioTraductionRepository;
    private final NewsTraductionRepository newsTraductionRepository;

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

            // Le secteur est un champ libre (créé à la volée par le porteur,
            // pas une liste fermée), donc jamais couvert par le dictionnaire
            // i18n statique — traduit une fois pour tout le secteur (partagé
            // par tous les projets), pas dupliqué par projet.
            if (projet.getSecteur() != null) {
                traduireSecteur(translator, projet.getSecteur());
            }

        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour projet {} : {}",
                    projet.getId(), e.getMessage());
        }
    }

    /**
     * Traduit automatiquement le nom et la description d'un article
     * GrowzMarket en anglais et en espagnol via l'API DeepL.
     * Appelé à la création et à la modification de l'article.
     */
    @Transactional
    public void traduireArticle(ArticleMarket article) {
        try {
            com.deepl.api.TranslatorOptions options = new com.deepl.api.TranslatorOptions()
                    .setServerUrl(deeplBaseUrl);
            Translator translator = new Translator(deeplApiKey, options);

            // Sauvegarder d'abord la version française (langue source)
            saveArticleTraduction(article, "fr", article.getNom(), article.getDescription());

            for (String targetLang : TARGET_LANGUAGES) {
                try {
                    String nomTradu = translate(translator, article.getNom(), targetLang);
                    String descriptionTradu = translate(translator, article.getDescription(), targetLang);

                    String langCode = DEEPL_LANG_MAP.get(targetLang);
                    saveArticleTraduction(article, langCode, nomTradu, descriptionTradu);

                    log.info("Article {} traduit en {}", article.getId(), langCode);

                } catch (Exception e) {
                    log.error("Erreur traduction article {} en {} : {}",
                            article.getId(), targetLang, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour article {} : {}",
                    article.getId(), e.getMessage());
        }
    }

    /**
     * Traduit automatiquement la bio de la fiche de présentation d'un
     * porteur en anglais et en espagnol via l'API DeepL. Appelé à chaque
     * enregistrement de la fiche par l'admin (la bio est un texte libre,
     * pas versionné comme un projet, donc retraduit à chaque modification).
     */
    @Transactional
    public void traduireFicheBio(User porteur) {
        try {
            com.deepl.api.TranslatorOptions options = new com.deepl.api.TranslatorOptions()
                    .setServerUrl(deeplBaseUrl);
            Translator translator = new Translator(deeplApiKey, options);

            saveFicheBioTraduction(porteur, "fr", porteur.getFicheBio());

            for (String targetLang : TARGET_LANGUAGES) {
                try {
                    String bioTradu = translate(translator, porteur.getFicheBio(), targetLang);
                    String langCode = DEEPL_LANG_MAP.get(targetLang);
                    saveFicheBioTraduction(porteur, langCode, bioTradu);
                    log.info("Bio fiche porteur {} traduite en {}", porteur.getId(), langCode);
                } catch (Exception e) {
                    log.error("Erreur traduction bio fiche porteur {} en {} : {}",
                            porteur.getId(), targetLang, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour bio fiche porteur {} : {}",
                    porteur.getId(), e.getMessage());
        }
    }

    /**
     * Traduit automatiquement le titre et le contenu d'un article
     * d'actualité en anglais et en espagnol via l'API DeepL.
     * Appelé à la création et à la modification de l'article.
     */
    @Transactional
    public void traduireNews(News news) {
        try {
            com.deepl.api.TranslatorOptions options = new com.deepl.api.TranslatorOptions()
                    .setServerUrl(deeplBaseUrl);
            Translator translator = new Translator(deeplApiKey, options);

            saveNewsTraduction(news, "fr", news.getTitle(), news.getContent());

            for (String targetLang : TARGET_LANGUAGES) {
                try {
                    String titleTradu = translate(translator, news.getTitle(), targetLang);
                    String contentTradu = translate(translator, news.getContent(), targetLang);

                    String langCode = DEEPL_LANG_MAP.get(targetLang);
                    saveNewsTraduction(news, langCode, titleTradu, contentTradu);

                    log.info("Article d'actualité {} traduit en {}", news.getId(), langCode);
                } catch (Exception e) {
                    log.error("Erreur traduction article d'actualité {} en {} : {}",
                            news.getId(), targetLang, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour article d'actualité {} : {}",
                    news.getId(), e.getMessage());
        }
    }

    private void saveNewsTraduction(News news, String langue, String title, String content) {
        NewsTraduction traduction = newsTraductionRepository
                .findByNewsIdAndLangue(news.getId(), langue)
                .orElse(new NewsTraduction());

        traduction.setNews(news);
        traduction.setLangue(langue);
        traduction.setTitle(title);
        traduction.setContent(content);

        newsTraductionRepository.save(traduction);
    }

    /**
     * Retraduit tous les articles d'actualité déjà publiés — à utiliser en
     * backfill une fois après l'ajout de cette fonctionnalité.
     */
    @Transactional
    public int traduireToutesLesNews(List<News> newsList) {
        int count = 0;
        for (News news : newsList) {
            try {
                traduireNews(news);
                count++;
            } catch (Exception e) {
                log.warn("Erreur traduction article d'actualité {} : {}", news.getId(), e.getMessage());
            }
        }
        return count;
    }

    private void saveFicheBioTraduction(User porteur, String langue, String bio) {
        FicheBioTraduction traduction = ficheBioTraductionRepository
                .findByUserIdAndLangue(porteur.getId(), langue)
                .orElse(new FicheBioTraduction());
        traduction.setUser(porteur);
        traduction.setLangue(langue);
        traduction.setBio(bio);
        ficheBioTraductionRepository.save(traduction);
    }

    /**
     * Traduit le nom d'un secteur (une fois, partagé par tous les projets de
     * ce secteur) et sauvegarde le résultat. Idempotent — écrase la
     * traduction existante si le nom du secteur a changé.
     */
    @Transactional
    public void traduireSecteur(Secteur secteur) {
        try {
            com.deepl.api.TranslatorOptions options = new com.deepl.api.TranslatorOptions()
                    .setServerUrl(deeplBaseUrl);
            Translator translator = new Translator(deeplApiKey, options);
            traduireSecteur(translator, secteur);
        } catch (Exception e) {
            log.error("Erreur initialisation DeepL pour secteur {} : {}", secteur.getId(), e.getMessage());
        }
    }

    private void traduireSecteur(Translator translator, Secteur secteur) {
        saveSecteurTraduction(secteur, "fr", secteur.getNom());
        for (String targetLang : TARGET_LANGUAGES) {
            try {
                String nomTradu = translate(translator, secteur.getNom(), targetLang);
                String langCode = DEEPL_LANG_MAP.get(targetLang);
                saveSecteurTraduction(secteur, langCode, nomTradu.isBlank() ? secteur.getNom() : nomTradu);
                log.info("Secteur {} ({}) traduit en {}", secteur.getId(), secteur.getNom(), langCode);
            } catch (Exception e) {
                log.error("Erreur traduction secteur {} en {} : {}",
                        secteur.getId(), targetLang, e.getMessage());
            }
        }
    }

    private void saveSecteurTraduction(Secteur secteur, String langue, String nom) {
        SecteurTraduction traduction = secteurTraductionRepository
                .findBySecteurIdAndLangue(secteur.getId(), langue)
                .orElse(new SecteurTraduction());
        traduction.setSecteur(secteur);
        traduction.setLangue(langue);
        traduction.setNom(nom);
        secteurTraductionRepository.save(traduction);
    }

    /**
     * Retraduit la bio de toutes les fiches porteur déjà soumises — à
     * utiliser en backfill une fois après l'ajout de cette fonctionnalité,
     * pour les fiches créées avant que la traduction ne soit automatique.
     */
    @Transactional
    public int traduireToutesLesFichesBio(List<User> porteursAvecFiche) {
        int count = 0;
        for (User porteur : porteursAvecFiche) {
            if (porteur.getFicheBio() == null || porteur.getFicheBio().isBlank())
                continue;
            try {
                traduireFicheBio(porteur);
                count++;
            } catch (Exception e) {
                log.warn("Erreur traduction bio fiche porteur {} : {}", porteur.getId(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * Retraduit tous les secteurs existants en base — à utiliser en backfill
     * après ajout de cette fonctionnalité, ou en maintenance ponctuelle.
     */
    @Transactional
    public int traduireTousLesSecteurs() {
        List<Secteur> secteurs = secteurRepository.findAll();
        int count = 0;
        for (Secteur secteur : secteurs) {
            try {
                traduireSecteur(secteur);
                count++;
            } catch (Exception e) {
                log.warn("Erreur traduction secteur {} : {}", secteur.getId(), e.getMessage());
            }
        }
        return count;
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

    /**
     * Sauvegarde ou met à jour la traduction d'un article GrowzMarket.
     */
    private void saveArticleTraduction(ArticleMarket article, String langue, String nom, String description) {
        ArticleMarketTraduction traduction = articleMarketTraductionRepository
                .findByArticleIdAndLangue(article.getId(), langue)
                .orElse(new ArticleMarketTraduction());

        traduction.setArticle(article);
        traduction.setLangue(langue);
        traduction.setNom(nom);
        traduction.setDescription(description);

        articleMarketTraductionRepository.save(traduction);
    }
}