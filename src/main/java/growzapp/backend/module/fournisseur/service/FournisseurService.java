package growzapp.backend.module.fournisseur.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.fournisseur.dto.ArticleFournisseurCreateDTO;
import growzapp.backend.module.fournisseur.dto.ArticleFournisseurDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurInscriptionDTO;
import growzapp.backend.module.fournisseur.enums.StatutFournisseur;
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

    private Secteur resolveSecteur(String nom) {
        return secteurRepository.findByNomIgnoreCase(nom.trim())
                .orElseGet(() -> secteurRepository.save(new Secteur(nom.trim())));
    }

    @Transactional
    public Fournisseur inscrire(User user, FournisseurInscriptionDTO dto) {
        if (fournisseurRepository.findByUserId(user.getId()).isPresent()) {
            throw new IllegalStateException("Ce compte a déjà une fiche fournisseur.");
        }

        Fournisseur f = new Fournisseur();
        f.setUser(user);
        f.setStatutJuridique(dto.statutJuridique());
        f.setRaisonSociale(dto.raisonSociale());
        f.setSecteur(resolveSecteur(dto.secteurNom()));
        f.setVille(dto.ville().trim());
        f.setPays(dto.pays().trim());
        f.setTelephone(dto.telephone());
        f.setEmail(dto.email());
        f.setDescription(dto.description());
        Fournisseur saved = fournisseurRepository.save(f);

        notificationService.notifyAdmins(
                "Nouvelle inscription fournisseur",
                (user.getPrenom() + " " + user.getNom()).trim() + " — " + dto.ville(),
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

    public List<Fournisseur> rechercher(String ville, String pays, Long secteurId) {
        return fournisseurRepository.rechercher(ville, pays, secteurId);
    }

    // ── Catalogue d'articles ──────────────────────────────────────────────────

    @Transactional
    public ArticleFournisseur ajouterArticle(Long fournisseurId, ArticleFournisseurCreateDTO dto) {
        Fournisseur f = getOrThrow(fournisseurId);
        ArticleFournisseur article = new ArticleFournisseur();
        article.setFournisseur(f);
        article.setNom(dto.nom());
        article.setDescription(dto.description());
        article.setPrix(dto.prix());
        article.setUnite(dto.unite());
        article.setDisponible(dto.disponible());
        return articleFournisseurRepository.save(article);
    }

    @Transactional
    public ArticleFournisseur modifierArticle(Long articleId, Long fournisseurId, ArticleFournisseurCreateDTO dto) {
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
                f.getStatutJuridique().name(),
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
                a.isDisponible());
    }
}
