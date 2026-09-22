package growzapp.backend.module.fournisseur.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.fournisseur.dto.CommandeCreateDTO;
import growzapp.backend.module.fournisseur.dto.CommandeDTO;
import growzapp.backend.module.fournisseur.dto.CommandeLigneCreateDTO;
import growzapp.backend.module.fournisseur.dto.CommandeLigneDTO;
import growzapp.backend.module.fournisseur.enums.StatutCommande;
import growzapp.backend.module.fournisseur.model.ArticleFournisseur;
import growzapp.backend.module.fournisseur.model.Commande;
import growzapp.backend.module.fournisseur.model.CommandeLigne;
import growzapp.backend.module.fournisseur.model.Fournisseur;
import growzapp.backend.module.fournisseur.repository.ArticleFournisseurRepository;
import growzapp.backend.module.fournisseur.repository.CommandeRepository;
import growzapp.backend.module.fournisseur.repository.FournisseurRepository;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ArticleFournisseurRepository articleFournisseurRepository;
    private final ProjetRepository projetRepository;
    private final CommandeTransactionHelper txHelper;
    private final NotificationService notificationService;

    private Commande getOrThrow(Long id) {
        return commandeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable avec l'ID : " + id));
    }

    private void ensurePorteurDuProjet(Commande commande, User porteur) {
        if (commande.getProjet().getPorteur() == null
                || !commande.getProjet().getPorteur().getId().equals(porteur.getId())) {
            throw new SecurityException("Cette commande ne concerne pas un de vos projets.");
        }
    }

    private void ensureFournisseurProprietaire(Commande commande, User user) {
        if (!commande.getFournisseur().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Cette commande ne vous concerne pas.");
        }
    }

    // ── Création, côté porteur ───────────────────────────────────────────────
    @Transactional
    public Commande creerCommande(User porteur, CommandeCreateDTO dto) {
        Projet projet = projetRepository.findById(dto.projetId())
                .orElseThrow(() -> new EntityNotFoundException("Projet introuvable"));
        if (projet.getPorteur() == null || !projet.getPorteur().getId().equals(porteur.getId())) {
            throw new SecurityException("Vous ne pouvez commander que pour l'un de vos propres projets.");
        }

        Fournisseur fournisseur = fournisseurRepository.findById(dto.fournisseurId())
                .orElseThrow(() -> new EntityNotFoundException("Fournisseur introuvable"));

        Commande commande = new Commande();
        commande.setProjet(projet);
        commande.setFournisseur(fournisseur);

        List<CommandeLigne> lignes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CommandeLigneCreateDTO ligneDto : dto.lignes()) {
            ArticleFournisseur article = articleFournisseurRepository.findById(ligneDto.articleId())
                    .orElseThrow(() -> new EntityNotFoundException("Article introuvable : " + ligneDto.articleId()));
            if (!article.getFournisseur().getId().equals(fournisseur.getId())) {
                throw new IllegalArgumentException("L'article " + article.getNom()
                        + " n'appartient pas au fournisseur sélectionné.");
            }
            if (!article.isDisponible()) {
                throw new IllegalArgumentException("L'article " + article.getNom() + " n'est plus disponible.");
            }

            CommandeLigne ligne = new CommandeLigne();
            ligne.setCommande(commande);
            ligne.setArticle(article);
            ligne.setLibelle(article.getNom());
            ligne.setPrixUnitaire(article.getPrix());
            ligne.setQuantite(ligneDto.quantite());
            BigDecimal sousTotal = article.getPrix().multiply(BigDecimal.valueOf(ligneDto.quantite()));
            ligne.setSousTotal(sousTotal);
            total = total.add(sousTotal);
            lignes.add(ligne);
        }

        commande.setLignes(lignes);
        commande.setMontantTotal(total);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyAdmins(
                "Nouvelle commande fournisseur à valider",
                projet.getLibelle() + " → " + (fournisseur.getRaisonSociale() != null
                        ? fournisseur.getRaisonSociale()
                        : fournisseur.getUser().getPrenom() + " " + fournisseur.getUser().getNom())
                        + " (" + total.toPlainString() + " FCFA)",
                "/admin/commandes");

        return saved;
    }

    // ── Validation / rejet, côté admin ──────────────────────────────────────
    @Transactional
    public Commande validerAdmin(Long id) {
        Commande commande = getOrThrow(id);
        if (commande.getStatut() != StatutCommande.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Seule une commande en attente de validation peut être validée.");
        }

        txHelper.debiterProjetVersFournisseurBloque(
                commande.getProjet().getId(),
                commande.getFournisseur().getUser().getId(),
                commande.getId(),
                commande.getMontantTotal());

        commande.setStatut(StatutCommande.VALIDEE);
        commande.setDateValidationAdmin(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getFournisseur().getUser(),
                "Commande validée",
                "Votre commande #" + commande.getId() + " a été validée, les fonds sont séquestrés en votre faveur.",
                null, "/mon-espace/fournisseur/commandes");
        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande validée",
                "Votre commande auprès de " + fournisseurNomAffiche(commande.getFournisseur()) + " a été validée.",
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    @Transactional
    public Commande rejeterAdmin(Long id, String motif) {
        Commande commande = getOrThrow(id);
        if (commande.getStatut() != StatutCommande.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Seule une commande en attente de validation peut être rejetée.");
        }
        commande.setStatut(StatutCommande.REJETEE);
        commande.setMotifRejet(motif);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande rejetée",
                motif,
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    // ── Livraison, côté fournisseur ──────────────────────────────────────────
    @Transactional
    public Commande marquerLivree(Long id, User fournisseurUser) {
        Commande commande = getOrThrow(id);
        ensureFournisseurProprietaire(commande, fournisseurUser);
        if (commande.getStatut() != StatutCommande.VALIDEE) {
            throw new IllegalStateException("Seule une commande validée peut être marquée comme livrée.");
        }
        commande.setStatut(StatutCommande.LIVREE);
        commande.setDateLivraison(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande livrée",
                fournisseurNomAffiche(commande.getFournisseur())
                        + " a signalé la livraison de votre commande #" + commande.getId()
                        + ". Merci de confirmer la réception.",
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    // ── Confirmation de réception / litige, côté porteur ────────────────────
    @Transactional
    public Commande confirmerReception(Long id, User porteur) {
        Commande commande = getOrThrow(id);
        ensurePorteurDuProjet(commande, porteur);
        if (commande.getStatut() != StatutCommande.LIVREE) {
            throw new IllegalStateException("Seule une commande livrée peut être confirmée.");
        }

        txHelper.libererVersFournisseur(
                commande.getFournisseur().getUser().getId(),
                commande.getMontantTotal(),
                commande.getId());

        commande.setStatut(StatutCommande.CONFIRMEE);
        commande.setDateConfirmationReception(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getFournisseur().getUser(),
                "Réception confirmée",
                "Le porteur a confirmé la réception de la commande #" + commande.getId()
                        + " — les fonds sont maintenant disponibles.",
                null, "/mon-espace/fournisseur/commandes");

        return saved;
    }

    @Transactional
    public Commande ouvrirLitige(Long id, User porteur, String motif) {
        Commande commande = getOrThrow(id);
        ensurePorteurDuProjet(commande, porteur);
        if (commande.getStatut() != StatutCommande.LIVREE) {
            throw new IllegalStateException("Un litige ne peut être ouvert que sur une commande livrée non confirmée.");
        }
        commande.setStatut(StatutCommande.LITIGE);
        commande.setMotifLitige(motif);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyAdmins(
                "Litige sur une commande fournisseur",
                "Commande #" + commande.getId() + " — " + motif,
                "/admin/commandes");

        return saved;
    }

    // ── Arbitrage admin d'un litige ──────────────────────────────────────────
    @Transactional
    public Commande arbitrerLitige(Long id, boolean enFaveurDuFournisseur, String motif) {
        Commande commande = getOrThrow(id);
        if (commande.getStatut() != StatutCommande.LITIGE) {
            throw new IllegalStateException("Cette commande n'est pas en litige.");
        }

        if (enFaveurDuFournisseur) {
            txHelper.libererVersFournisseur(
                    commande.getFournisseur().getUser().getId(),
                    commande.getMontantTotal(),
                    commande.getId());
            commande.setStatut(StatutCommande.CONFIRMEE);
        } else {
            // Les fonds séquestrés côté fournisseur retournent au wallet du
            // projet — l'inverse exact du débit fait à la validation.
            txHelper.libererVersFournisseur(commande.getFournisseur().getUser().getId(), commande.getMontantTotal(),
                    commande.getId());
            txHelper.rembourserProjet(commande.getProjet().getId(), commande.getMontantTotal(), commande.getId());
            commande.setStatut(StatutCommande.REJETEE);
        }
        commande.setMotifLitige((commande.getMotifLitige() != null ? commande.getMotifLitige() + " | " : "")
                + "Arbitrage admin : " + motif);
        return commandeRepository.save(commande);
    }

    public List<Commande> getMesCommandesPorteur(Long porteurId) {
        return commandeRepository.findByProjetPorteurIdOrderByDateCommandeDesc(porteurId);
    }

    public List<Commande> getCommandesRecuesFournisseur(Long fournisseurId) {
        return commandeRepository.findByFournisseurIdOrderByDateCommandeDesc(fournisseurId);
    }

    public List<Commande> getEnAttenteAdmin() {
        return commandeRepository.findByStatutOrderByDateCommandeDesc(StatutCommande.EN_ATTENTE_VALIDATION);
    }

    public List<Commande> getEnLitigeAdmin() {
        return commandeRepository.findByStatutOrderByDateCommandeDesc(StatutCommande.LITIGE);
    }

    private String fournisseurNomAffiche(Fournisseur f) {
        return f.getRaisonSociale() != null ? f.getRaisonSociale()
                : (f.getUser().getPrenom() + " " + f.getUser().getNom()).trim();
    }

    public CommandeDTO toDto(Commande c) {
        List<CommandeLigneDTO> lignes = c.getLignes().stream()
                .map(l -> new CommandeLigneDTO(
                        l.getId(),
                        l.getArticle() != null ? l.getArticle().getId() : null,
                        l.getLibelle(),
                        l.getPrixUnitaire(),
                        l.getQuantite(),
                        l.getSousTotal()))
                .toList();

        return new CommandeDTO(
                c.getId(),
                c.getProjet().getId(),
                c.getProjet().getLibelle(),
                c.getFournisseur().getId(),
                fournisseurNomAffiche(c.getFournisseur()),
                c.getMontantTotal(),
                c.getStatut().name(),
                c.getDateCommande(),
                c.getDateValidationAdmin(),
                c.getDateLivraison(),
                c.getDateConfirmationReception(),
                c.getMotifRejet(),
                c.getMotifLitige(),
                lignes);
    }
}
