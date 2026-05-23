package growzapp.backend.module.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.news.model.News;
import growzapp.backend.module.news.model.NewsCategory;
import growzapp.backend.module.news.repository.NewsRepository;
import growzapp.backend.module.news.service.NewsService;
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

@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "Articles d'actualité de la plateforme Growzapp")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsService newsService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "Lister les articles", description = "Retourne tous les articles, triés du plus récent au plus ancien. Filtrable par catégorie.", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des articles",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = News.class))))
    })
    public List<News> getAllNews(
            @Parameter(description = "Filtrer par catégorie",
                       schema = @Schema(allowableValues = {"PLATFORM_UPDATE", "INVESTMENT_OPPORTUNITY", "PERFORMANCE_REPORT", "EDUCATION", "SECURITY"}))
            @RequestParam(required = false) NewsCategory category) {
        if (category != null) {
            return newsRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        return newsRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    @Operation(summary = "Créer un article", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Article créé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class)))
    })
    public ResponseEntity<News> createNews(@RequestBody News news) {
        News createdNews = newsService.createNews(news);
        return ResponseEntity.ok(createdNews);
    }

    // 1. Mets la route RSS AVANT la route avec ID
    @GetMapping(value = "/rss", produces = "application/xml")
    @Operation(summary = "Flux RSS des articles", description = "Retourne le flux RSS au format XML pour abonnement externe.", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flux RSS XML",
            content = @Content(mediaType = "application/xml", schema = @Schema(type = "string", format = "xml")))
    })
    public ResponseEntity<String> getRssFeed() {
        return ResponseEntity.ok(newsService.generateRssFeed());
    }

    // 2. La route avec ID vient après
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un article", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Article trouvé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class))),
        @ApiResponse(responseCode = "404", description = "Article introuvable",
            content = @Content(schema = @Schema()))
    })
    public ResponseEntity<News> getNewsById(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un article", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Article mis à jour",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = News.class)))
    })
    public ResponseEntity<News> updateNews(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody News news) {
        return ResponseEntity.ok(newsService.updateNews(id, news));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article", tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Article supprimé"),
        @ApiResponse(responseCode = "404", description = "Article introuvable")
    })
    public ResponseEntity<Void> deleteNews(
            @Parameter(description = "Identifiant de l'article", example = "1", required = true)
            @PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    @Operation(summary = "Uploader une image pour un article",
               description = "Stocke l'image et retourne son URL publique. Format accepté : JPEG, PNG, WebP.",
               tags = {"News"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "URL de l'image uploadée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"url\": \"/uploads/posters/news-1.jpg\"}"))),
        @ApiResponse(responseCode = "500", description = "Erreur lors du stockage du fichier")
    })
    public ResponseEntity<Map<String, String>> upload(
            @Parameter(description = "Fichier image à uploader", schema = @Schema(type = "string", format = "binary"), required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.savePosterOrAvatar(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
