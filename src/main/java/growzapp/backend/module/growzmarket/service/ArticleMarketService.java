package growzapp.backend.module.growzmarket.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.growzmarket.dto.ArticleMarketCreateDTO;
import growzapp.backend.module.growzmarket.dto.ArticleMarketDTO;
import growzapp.backend.module.growzmarket.enums.CategorieMarket;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.growzmarket.repository.ArticleMarketRepository;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleMarketService {

    private final ArticleMarketRepository articleMarketRepository;
    private final ProjetRepository projetRepository;
    private final FileUploadService fileUploadService;

    private Projet getProjetDontUserEstPorteur(Long projetId, Long userId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new EntityNotFoundException("Projet introuvable"));
        if (projet.getPorteur() == null || !projet.getPorteur().getId().equals(userId)) {
            throw new SecurityException("Vous n'êtes pas le porteur de ce projet.");
        }
        return projet;
    }

    @Transactional
    public ArticleMarket creerArticle(User porteur, ArticleMarketCreateDTO dto, List<MultipartFile> photos) {
        Projet projet = getProjetDontUserEstPorteur(dto.projetId(), porteur.getId());

        ArticleMarket article = new ArticleMarket();
        article.setProjet(projet);
        article.setNom(dto.nom());
        article.setDescription(dto.description());
        article.setPrix(dto.prix());
        article.setUnite(dto.unite());
        article.setDisponible(dto.disponible());
        article.setStock(dto.stock());
        article.setCategorie(parseCategorie(dto.categorie()));
        article.setDelaiPreparation(dto.delaiPreparation());
        article.setPointRetrait(dto.pointRetrait());
        article.setTelephoneContact(dto.telephoneContact());

        ArticleMarket saved = articleMarketRepository.save(article);

        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty()) {
                    saved.getPhotos().add(fileUploadService.uploadMarketPhoto(photo, saved.getId()));
                }
            }
            saved = articleMarketRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    public ArticleMarket modifierArticle(Long articleId, Long porteurId, ArticleMarketCreateDTO dto,
            List<MultipartFile> nouvellesPhotos) {
        ArticleMarket article = articleMarketRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable avec l'ID : " + articleId));
        if (!article.getProjet().getPorteur().getId().equals(porteurId)) {
            throw new SecurityException("Cet article ne vous appartient pas.");
        }
        // On revérifie l'appartenance du projet cible si le porteur en change.
        Projet projet = getProjetDontUserEstPorteur(dto.projetId(), porteurId);

        article.setProjet(projet);
        article.setNom(dto.nom());
        article.setDescription(dto.description());
        article.setPrix(dto.prix());
        article.setUnite(dto.unite());
        article.setDisponible(dto.disponible());
        article.setStock(dto.stock());
        article.setCategorie(parseCategorie(dto.categorie()));
        article.setDelaiPreparation(dto.delaiPreparation());
        article.setPointRetrait(dto.pointRetrait());
        article.setTelephoneContact(dto.telephoneContact());

        if (nouvellesPhotos != null) {
            for (MultipartFile photo : nouvellesPhotos) {
                if (photo != null && !photo.isEmpty()) {
                    article.getPhotos().add(fileUploadService.uploadMarketPhoto(photo, article.getId()));
                }
            }
        }

        return articleMarketRepository.save(article);
    }

    @Transactional
    public void supprimerArticle(Long articleId, Long porteurId) {
        ArticleMarket article = articleMarketRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable avec l'ID : " + articleId));
        if (!article.getProjet().getPorteur().getId().equals(porteurId)) {
            throw new SecurityException("Cet article ne vous appartient pas.");
        }
        articleMarketRepository.delete(article);
    }

    private CategorieMarket parseCategorie(String categorie) {
        if (categorie == null || categorie.isBlank()) {
            return CategorieMarket.AUTRE;
        }
        try {
            return CategorieMarket.valueOf(categorie.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CategorieMarket.AUTRE;
        }
    }

    public List<ArticleMarket> getCatalogue() {
        return articleMarketRepository.findByDisponibleTrueOrderByCreatedAtDesc();
    }

    public List<ArticleMarket> getMesArticles(Long porteurId) {
        return articleMarketRepository.findByProjetPorteurIdOrderByCreatedAtDesc(porteurId);
    }

    public ArticleMarket getById(Long id) {
        return articleMarketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable avec l'ID : " + id));
    }

    private String porteurNomAffiche(User porteur) {
        return (porteur.getPrenom() + " " + porteur.getNom()).trim();
    }

    public ArticleMarketDTO toDto(ArticleMarket a) {
        return new ArticleMarketDTO(
                a.getId(),
                a.getProjet().getId(),
                a.getProjet().getLibelle(),
                a.getProjet().getPorteur() != null ? porteurNomAffiche(a.getProjet().getPorteur()) : null,
                a.getNom(),
                a.getDescription(),
                a.getPrix(),
                a.getUnite(),
                a.isDisponible(),
                a.getStock(),
                a.getCategorie().name(),
                a.getDelaiPreparation(),
                a.getPhotos() != null ? a.getPhotos() : List.of(),
                a.getPointRetrait(),
                a.getTelephoneContact());
    }
}
