package growzapp.backend.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.news.model.News;
import growzapp.backend.news.model.NewsCategory;
import growzapp.backend.news.repository.NewsRepository;
import growzapp.backend.news.service.NewsService;
import growzapp.backend.service.FileStorageService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsService newsService;

    @Autowired
    private FileStorageService fileStorageService;


    @GetMapping
    public List<News> getAllNews(@RequestParam(required = false) NewsCategory category) {
        if (category != null) {
            return newsRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        return newsRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ResponseEntity<News> createNews(@RequestBody News news) {
        // Sauvegarde l'article en utilisant le service
        News createdNews = newsService.createNews(news);
        return ResponseEntity.ok(createdNews);
    }
    // 1. Mets la route RSS AVANT la route avec ID
    @GetMapping(value = "/rss", produces = "application/xml")
    public ResponseEntity<String> getRssFeed() {
        return ResponseEntity.ok(newsService.generateRssFeed());
    }

    // 2. La route avec ID vient après
    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsById(id));
    }

    // Dans growzapp.backend.news.controller.NewsController

    @PutMapping("/{id}")
    public ResponseEntity<News> updateNews(@PathVariable Long id, @RequestBody News news) {
        return ResponseEntity.ok(newsService.updateNews(id, news));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

   // Dans NewsController.java
   // src/main/java/growzapp/backend/news/controller/NewsController.java

   @PostMapping("/upload")
   public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
       try {
           // Utilise ta méthode existante qui renvoie "/uploads/posters/filename"
           String url = fileStorageService.savePosterOrAvatar(file);
           return ResponseEntity.ok(Map.of("url", url));
       } catch (IOException e) {
           return ResponseEntity.status(500).build();
       }
   }
}