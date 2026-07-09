package growzapp.backend.module.news.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.news.model.News;
import growzapp.backend.module.news.model.NewsCategory;
import growzapp.backend.module.news.repository.NewsRepository;
import growzapp.backend.module.notification.service.NotificationService;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

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
        News saved = newsRepository.save(news);

        // Notifier tous les utilisateurs de la nouvelle actualité
        String extrait = saved.getContent() != null && saved.getContent().length() > 120
                ? saved.getContent().substring(0, 120) + "..."
                : saved.getContent();

        notificationService.notifyAllUsersWithSlug(
                "📰 " + saved.getTitle(),
                extrait,
                null,
                "/news/" + saved.getId() // lien vers l'article
        );

        return saved;
    }

    @Transactional
    public News updateNews(Long id, News newsDetails) {
        News news = getNewsById(id);
        news.setTitle(newsDetails.getTitle());
        news.setContent(newsDetails.getContent());
        news.setImageUrl(newsDetails.getImageUrl());
        news.setCategory(newsDetails.getCategory());
        return newsRepository.save(news);
    }

    @Transactional
    public void deleteNews(Long id) {
        News news = getNewsById(id);
        newsRepository.delete(news);
    }

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
            rss.append("<link>").append(frontendUrl).append("/news/").append(news.getId()).append("</link>");
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
        News saved = newsRepository.save(news);

        // Notifier tous les utilisateurs
        String extrait = saved.getContent() != null && saved.getContent().length() > 120
                ? saved.getContent().substring(0, 120) + "..."
                : saved.getContent();

        notificationService.notifyAllUsersWithSlug(
                "📰 " + saved.getTitle(),
                extrait,
                null,
                "/news/" + saved.getId() // lien vers l'article
        );

        return saved;
    }
}