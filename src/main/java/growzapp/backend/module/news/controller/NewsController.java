package growzapp.backend.module.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.news.model.News;
import growzapp.backend.module.news.model.NewsCategory;
import growzapp.backend.module.news.repository.NewsRepository;
import growzapp.backend.module.news.service.NewsService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.NewsTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.NewsTraductionRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({ "/api/v1/news", "/api/news" })
@Tag(name = "News", description = "Articles d'actualité de la plateforme Growzapp")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsService newsService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private NewsTraductionRepository newsTraductionRepository;

    @Autowired
    private DeepLTranslationService deepLTranslationService;

    @GetMapping
    @Operation(summary = "Lister les articles", description = "Retourne tous les articles, triés du plus récent au plus ancien. Filtrable par catégorie.", tags = {
            "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des articles", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = News.class))))
    })
    public ResponseEntity<ApiResponseDTO<List<News>>> getAllNews(
            @Parameter(description = "Filtrer par catégorie", schema = @Schema(allowableValues = { "PLATFORM_UPDATE",
                    "INVESTMENT_OPPORTUNITY", "PERFORMANCE_REPORT", "EDUCATION",
                    "SECURITY" })) @RequestParam(required = false) NewsCategory category,
            @Parameter(description = "Langue de traduction souhaitée", example = "es") @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<News> result = category != null
                ? newsRepository.findByCategoryOrderByCreatedAtDesc(category)
                : newsRepository.findAllByOrderByCreatedAtDesc();
        applyTraductions(result, langue);
        return ResponseEntity.ok(ApiResponseDTO.success(result));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNICANT')")
    @Operation(summary = "[Admin] Lister les articles (paginé, recherche par titre)", tags = { "News" })
    public ResponseEntity<ApiResponseDTO<Page<News>>> getAllForAdmin(
            @Parameter(description = "Recherche par titre") @RequestParam(required = false) String search,
            @Parameter(description = "Numéro de page (commence à 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Langue de traduction souhaitée", example = "es") @RequestParam(required = false, defaultValue = "fr") String langue) {
        Pageable pageable = PageRequest.of(page, size);
        Page<News> result = newsRepository.findForAdmin(search, pageable);
        applyTraductions(result.getContent(), langue);
        return ResponseEntity.ok(ApiResponseDTO.success(result));
    }

    @PostMapping
    @Operation(summary = "Créer un article", tags = { "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article créé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class)))
    })
    public ResponseEntity<ApiResponseDTO<News>> createNews(@RequestBody News news) {
        News createdNews = newsService.createNews(news);
        return ResponseEntity.ok(ApiResponseDTO.success(createdNews));
    }

    // 1. Mets la route RSS AVANT la route avec ID
    @GetMapping(value = "/rss", produces = "application/xml")
    @Operation(summary = "Flux RSS des articles", description = "Retourne le flux RSS au format XML pour abonnement externe.", tags = {
            "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flux RSS XML", content = @Content(mediaType = "application/xml", schema = @Schema(type = "string", format = "xml")))
    })
    public ResponseEntity<String> getRssFeed() {
        return ResponseEntity.ok(newsService.generateRssFeed());
    }

    // 2. La route avec ID vient après
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un article", tags = { "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article trouvé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class))),
            @ApiResponse(responseCode = "404", description = "Article introuvable", content = @Content(schema = @Schema()))
    })
    public ResponseEntity<ApiResponseDTO<News>> getNewsById(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true) @PathVariable Long id,
            @Parameter(description = "Langue de traduction souhaitée", example = "es") @RequestParam(required = false, defaultValue = "fr") String langue) {
        News news = newsService.getNewsById(id);
        applyTraduction(news, langue);
        return ResponseEntity.ok(ApiResponseDTO.success(news));
    }

    // ── ADMIN : RETRADUIRE TOUS LES ARTICLES (BACKFILL) ─────────────────────
    @PostMapping("/admin/retraduire-tout")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNICANT')")
    @Operation(summary = "[Admin] Retraduit via DeepL le titre et le contenu de tous les articles déjà publiés", description = "À exécuter une fois après le déploiement de la traduction automatique, pour les articles créés avant cet ajout.", tags = {
            "News" })
    public ResponseEntity<ApiResponseDTO<String>> retraduireTout() {
        List<News> all = newsRepository.findAllByOrderByCreatedAtDesc();
        int count = deepLTranslationService.traduireToutesLesNews(all);
        return ResponseEntity.ok(ApiResponseDTO.<String>success(null).message(count + " article(s) retraduit(s)"));
    }

    // ── Helper : appliquer traduction sur un article ─────────────────────────
    private void applyTraduction(News news, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return;
        Optional<NewsTraductionProjection> traduction = newsTraductionRepository
                .findProjectionByNewsIdAndLangue(news.getId(), langue);
        traduction.ifPresent(t -> {
            if (t.getTitle() != null && !t.getTitle().isBlank())
                news.setTitle(t.getTitle());
            if (t.getContent() != null && !t.getContent().isBlank())
                news.setContent(t.getContent());
        });
    }

    // ── Helper : appliquer traduction sur une liste ──────────────────────────
    private void applyTraductions(List<News> newsList, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return;
        newsList.forEach(n -> applyTraduction(n, langue));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un article", tags = { "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article mis à jour", content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class)))
    })
    public ResponseEntity<ApiResponseDTO<News>> updateNews(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true) @PathVariable Long id,
            @RequestBody News news) {
        return ResponseEntity.ok(ApiResponseDTO.success(newsService.updateNews(id, news)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article", tags = { "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Article supprimé"),
            @ApiResponse(responseCode = "404", description = "Article introuvable")
    })
    public ResponseEntity<Void> deleteNews(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true) @PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    @Operation(summary = "Uploader une image pour un article", description = "Stocke l'image et retourne son URL publique. Format accepté : JPEG, PNG, WebP.", tags = {
            "News" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL de l'image uploadée", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"url\": \"/uploads/posters/news-1.jpg\"}"))),
            @ApiResponse(responseCode = "500", description = "Erreur lors du stockage du fichier")
    })
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> upload(
            @Parameter(description = "Fichier image à uploader", schema = @Schema(type = "string", format = "binary"), required = true) @RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.savePosterOrAvatar(file);
            return ResponseEntity.ok(ApiResponseDTO.success(Map.of("url", url)));
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
