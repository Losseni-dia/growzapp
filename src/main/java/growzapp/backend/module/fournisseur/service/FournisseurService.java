package growzapp.backend.module.fournisseur.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.fournisseur.dto.ArticleFournisseurCreateDTO;
import growzapp.backend.module.fournisseur.dto.ArticleFournisseurDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurBrouillonDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurDTO;
import growzapp.backend.module.fournisseur.enums.StatutFournisseur;
import growzapp.backend.module.fournisseur.enums.StatutJuridiqueFournisseur;
import growzapp.backend.module.fournisseur.model.ArticleFournisseur;
import growzapp.backend.module.fournisseur.model.Fournisseur;
import growzapp.backend.module.fournisseur.repository.ArticleFournisseurRepository;
import growzapp.backend.module.fournisseur.repository.FournisseurRepository;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final ArticleFournisseurRepository articleFournisseurRepository;
    private final SecteurRepository secteurRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;

    private Secteur resolveSecteur(String nom) {
        if (nom == null || nom.isBlank()) {
            return null;
        }
        return secteurRepository.findByNomIgnoreCase(nom.trim())
                .orElseGet(() -> secteurRepository.save(new Secteur(nom.trim())));
    }

    // ── Brouillon : sauvegardable à tout moment, aucun champ obligatoire ──────
    @Transactional
    public Fournisseur enregistrerBrouillon(User user, FournisseurBrouillonDTO dto) {
        Fournisseur f = fournisseurRepository.findByUserId(user.getId()).orElse(null);

        if (f != null && f.getStatut() != StatutFournisseur.BROUILLON
                && f.getStatut() != StatutFournisseur.REJETE) {
            throw new IllegalStateException(
                    "Votre fiche est déjà " + f.getStatut().name().toLowerCase()
                            + " — elle ne peut plus être modifiée depuis cet écran.");
        }

        if (f == null) {
            f = new Fournisseur();
            f.setUser(user);
        }
        // Une fiche rejetée redevient un brouillon dès qu'on la retouche —
        // elle doit être explicitement resoumise, pas rester REJETEE alors
        // que son contenu a changé.
        f.setStatut(StatutFournisseur.BROUILLON);
        f.setMotifRejet(null);

        if (dto.statutJuridique() != null) {
            f.setStatutJuridique(dto.statutJuridique());
        }
        if (dto.raisonSociale() != null) {
            f.setRaisonSociale(dto.raisonSociale());
        }
        if (dto.secteurNom() != null) {
            f.setSecteur(resolveSecteur(dto.secteurNom()));
        }
        if (dto.ville() != null) {
            f.setVille(dto.ville().isBlank() ? null : dto.ville().trim());
        }
        if (dto.pays() != null) {
            f.setPays(dto.pays().isBlank() ? null : dto.pays().trim());
        }
        if (dto.telephone() != null) {
            f.setTelephone(dto.telephone());
        }
        if (dto.email() != null) {
            f.setEmail(dto.email());
        }
        if (dto.description() != null) {
            f.setDescription(dto.description());
        }

        return fournisseurRepository.save(f);
    }

    // ── Soumission finale : valide les champs obligatoires puis notifie l'admin ─
    @Transactional
    public Fournisseur soumettre(User user) {
        Fournisseur f = fournisseurRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun brouillon à soumettre — enregistrez d'abord votre fiche."));

        if (f.getStatut() != StatutFournisseur.BROUILLON) {
            throw new IllegalStateException("Cette fiche a déjà été soumise.");
        }
        if (f.getStatutJuridique() == null) {
            throw new IllegalArgumentException("Le type de fournisseur (Individuel/Entreprise) est obligatoire.");
        }
        if (f.getStatutJuridique() == StatutJuridiqueFournisseur.ENTREPRISE
                && (f.getRaisonSociale() == null || f.getRaisonSociale().isBlank())) {
            throw new IllegalArgumentException("La raison sociale est obligatoire pour une entreprise.");
        }
        if (f.getSecteur() == null) {
            throw new IllegalArgumentException("Le secteur d'activité est obligatoire.");
        }
        if (f.getVille() == null || f.getVille().isBlank() || f.getPays() == null || f.getPays().isBlank()) {
            throw new IllegalArgumentException("La ville et le pays sont obligatoires.");
        }

        f.setStatut(StatutFournisseur.EN_ATTENTE);
        f.setDateSoumission(LocalDateTime.now());
        Fournisseur saved = fournisseurRepository.save(f);

        notificationService.notifyAdmins(
                "Nouvelle inscription fournisseur",
                (user.getPrenom() + " " + user.getNom()).trim() + " — " + f.getVille(),
                "/admin/fournisseurs");

        return saved;
    }

    private Fournisseur getOrThrow(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fournisseur introuvable avec l'ID : " + id));
    }

    public Fournisseur getById(Long id) {
        return getOrThrow(id);
    }

    public Fournisseur getByUserId(Long userId) {
        return fournisseurRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Aucune fiche fournisseur pour cet utilisateur."));
    }

    public boolean possedeFiche(Long userId) {
        return fournisseurRepository.findByUserId(userId).isPresent();
    }

    @Transactional
    public Fournisseur valider(Long id) {
        Fournisseur f = getOrThrow(id);
        f.setStatut(StatutFournisseur.VALIDE);
        f.setDateValidation(LocalDateTime.now());
        f.setMotifRejet(null);
        Fournisseur saved = fournisseurRepository.save(f);

        userService.attribuerRoleSiAbsent(f.getUser().getId(), "FOURNISSEUR");
        notificationService.notifyUser(
                f.getUser(),
                "Votre fiche fournisseur a été validée",
                "Vous pouvez maintenant publier votre catalogue et recevoir des commandes.",
                null,
                "/mon-espace/fournisseur");

        return saved;
    }

    @Transactional
    public Fournisseur rejeter(Long id, String motif) {
        Fournisseur f = getOrThrow(id);
        f.setStatut(StatutFournisseur.REJETE);
        f.setMotifRejet(motif);
        Fournisseur saved = fournisseurRepository.save(f);

        notificationService.notifyUser(
                f.getUser(),
                "Votre fiche fournisseur a été rejetée",
                motif,
                null,
                "/mon-espace/fournisseur");

        return saved;
    }

    public List<Fournisseur> getEnAttente() {
        return fournisseurRepository.findByStatutOrderByDateSoumissionDesc(StatutFournisseur.EN_ATTENTE);
    }

    // Filtrage en Java plutôt qu'en JPQL : la clause classique
    // "(:param IS NULL OR ...)" fait échouer l'inférence de type du driver
    // PostgreSQL quand le paramètre vaut effectivement null ("could not
    // determine data type of parameter"). Le volume de fournisseurs validés
    // reste faible, un filtrage en mémoire est largement suffisant.
    public List<Fournisseur> rechercher(String ville, String pays, Long secteurId) {
        return fournisseurRepository.findByStatut(StatutFournisseur.VALIDE).stream()
                .filter(f -> ville == null || ville.isBlank() || ville.equalsIgnoreCase(f.getVille()))
                .filter(f -> pays == null || pays.isBlank() || pays.equalsIgnoreCase(f.getPays()))
                .filter(f -> secteurId == null || (f.getSecteur() != null && secteurId.equals(f.getSecteur().getId())))
                .toList();
    }

    // ── Catalogue d'articles ──────────────────────────────────────────────────

    @Transactional
    public ArticleFournisseur ajouterArticle(Long fournisseurId, ArticleFournisseurCreateDTO dto, MultipartFile photo) {
        Fournisseur f = getOrThrow(fournisseurId);
        ArticleFournisseur article = new ArticleFournisseur();
        article.setFournisseur(f);
        article.setNom(dto.nom());
        article.setDescription(dto.description());
        article.setPrix(dto.prix());
        article.setUnite(dto.unite());
        article.setDisponible(dto.disponible());
        ArticleFournisseur saved = articleFournisseurRepository.save(article);

        if (photo != null && !photo.isEmpty()) {
            saved.setPhotoUrl(fileUploadService.uploadArticlePhoto(photo, saved.getId()));
            saved = articleFournisseurRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    public ArticleFournisseur modifierArticle(Long articleId, Long fournisseurId, ArticleFournisseurCreateDTO dto,
            MultipartFile photo) {
        ArticleFournisseur article = articleFournisseurRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable avec l'ID : " + articleId));
        if (!article.getFournisseur().getId().equals(fournisseurId)) {
            throw new SecurityException("Cet article ne vous appartient pas.");
        }
        article.setNom(dto.nom());
        article.setDescription(dto.description());
        article.setPrix(dto.prix());
        article.setUnite(dto.unite());
        article.setDisponible(dto.disponible());

        if (photo != null && !photo.isEmpty()) {
            article.setPhotoUrl(fileUploadService.uploadArticlePhoto(photo, article.getId()));
        }

        return articleFournisseurRepository.save(article);
    }

    @Transactional
    public void supprimerArticle(Long articleId, Long fournisseurId) {
        ArticleFournisseur article = articleFournisseurRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable avec l'ID : " + articleId));
        if (!article.getFournisseur().getId().equals(fournisseurId)) {
            throw new SecurityException("Cet article ne vous appartient pas.");
        }
        articleFournisseurRepository.delete(article);
    }

    public List<ArticleFournisseur> getArticles(Long fournisseurId, boolean disponiblesUniquement) {
        return disponiblesUniquement
                ? articleFournisseurRepository.findByFournisseurIdAndDisponibleTrueOrderByCreatedAtDesc(fournisseurId)
                : articleFournisseurRepository.findByFournisseurIdOrderByCreatedAtDesc(fournisseurId);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public FournisseurDTO toDto(Fournisseur f) {
        return new FournisseurDTO(
                f.getId(),
                f.getUser().getId(),
                (f.getUser().getPrenom() + " " + f.getUser().getNom()).trim(),
                f.getStatutJuridique() != null ? f.getStatutJuridique().name() : null,
                f.getRaisonSociale(),
                f.getSecteur() != null ? f.getSecteur().getId() : null,
                f.getSecteur() != null ? f.getSecteur().getNom() : null,
                f.getVille(),
                f.getPays(),
                f.getTelephone(),
                f.getEmail(),
                f.getDescription(),
                f.getStatut().name(),
                f.getDateSoumission(),
                f.getDateValidation(),
                f.getMotifRejet());
    }

    public ArticleFournisseurDTO toDto(ArticleFournisseur a) {
        return new ArticleFournisseurDTO(
                a.getId(),
                a.getFournisseur().getId(),
                a.getNom(),
                a.getDescription(),
                a.getPrix(),
                a.getUnite(),
                a.isDisponible(),
                a.getPhotoUrl());
    }
}
