package growzapp.backend.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import growzapp.backend.news.model.News;
import growzapp.backend.news.model.NewsCategory;
import growzapp.backend.news.repository.NewsRepository;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    @GetMapping
    public List<News> getAllNews(@RequestParam(required = false) NewsCategory category) {
        if (category != null) {
            return newsRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        return newsRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public News getNewsById(@SuppressWarnings("unused") @PathVariable Long id) {
        return newsRepository.findById(id).orElseThrow();
    }
}