package growzapp.backend.controller.api;

import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.projet.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ProjetRepository projetRepository;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        String baseUrl = "https://growzapp.com/projet/"; // À changer par ton vrai domaine en prod

        String projectsXml = projetRepository.findAll().stream()
                .map(p -> String.format(
                        "<url><loc>%s</loc><lastmod>%s</lastmod><priority>0.80</priority></url>",
                        baseUrl + p.getSlug(),
                        p.getCreatedAt().toLocalDate().toString()))
                .collect(Collectors.joining(""));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">" +
                "<url><loc>https://growzapp.com/</loc><priority>1.00</priority></url>" +
                "<url><loc>https://growzapp.com/projets</loc><priority>0.90</priority></url>" +
                projectsXml +
                "</urlset>";
    }
}
