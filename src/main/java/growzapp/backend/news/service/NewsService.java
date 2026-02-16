package growzapp.backend.news.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.news.model.News;
import growzapp.backend.news.model.NewsCategory;
import growzapp.backend.news.repository.NewsRepository;

import java.util.List;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<News> getNews(NewsCategory category) {
        if (category != null) {
            return newsRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        return newsRepository.findAllByOrderByCreatedAtDesc();
    }

    public News getNewsById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actualité non trouvée"));
    }

    @Transactional
    public News createNews(News news) {
        // Ici, on pourra ajouter de la logique (ex: formater le titre, vérifier
        // l'image)
        return newsRepository.save(news);
    }

    // On prépare la méthode pour le flux RSS (on la remplira plus tard)
    public String generateRssFeed() {
        List<News> allNews = newsRepository.findAllByOrderByCreatedAtDesc();
        // Logique de conversion List<News> -> XML RSS
        return "";
    }
}