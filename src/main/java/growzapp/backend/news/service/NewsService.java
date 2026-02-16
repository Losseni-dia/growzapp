package growzapp.backend.news.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.news.model.News;
import growzapp.backend.news.model.NewsCategory;
import growzapp.backend.news.repository.NewsRepository;

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
    // src/main/java/growzapp/backend/news/service/NewsService.java

    public String generateRssFeed() {
        List<News> allNews = newsRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder rss = new StringBuilder();

        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        rss.append("<rss version=\"2.0\">");
        rss.append("<channel>");
        rss.append("<title>Growzapp News</title>");
        rss.append("<link>https://growzapp.com</link>");
        rss.append("<description>Actualités et opportunités d'investissement Growzapp</description>");

        for (News news : allNews) {
            rss.append("<item>");
            rss.append("<title>").append(news.getTitle()).append("</title>");
            rss.append("<link>https://growzapp.com/news/").append(news.getId()).append("</link>");
            rss.append("<description>")
                    .append(news.getContent().substring(0, Math.min(news.getContent().length(), 150)))
                    .append("...</description>");
            rss.append("<category>").append(news.getCategory()).append("</category>");
            rss.append("<pubDate>").append(news.getCreatedAt()).append("</pubDate>");
            rss.append("</item>");
        }

        rss.append("</channel>");
        rss.append("</rss>");

        return rss.toString();
    }

    public News saveNews(News news) {
        news.setCreatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }
}