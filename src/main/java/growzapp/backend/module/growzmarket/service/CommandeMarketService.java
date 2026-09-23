package growzapp.backend.module.growzmarket.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.growzmarket.dto.CommandeMarketCreateDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketLigneCreateDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketLigneDTO;
import growzapp.backend.module.growzmarket.enums.StatutCommandeMarket;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.growzmarket.model.CommandeMarket;
import growzapp.backend.module.growzmarket.model.CommandeMarketLigne;
import growzapp.backend.module.growzmarket.repository.ArticleMarketRepository;
import growzapp.backend.module.growzmarket.repository.CommandeMarketRepository;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandeMarketService {

    private final CommandeMarketRepository commandeMarketRepository;
    private final ArticleMarketRepository articleMarketRepository;
    private final CommandeMarketTransactionHelper txHelper;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final CommandeMarketFacturePdfService facturePdfService;

    private CommandeMarket getOrThrow(Long id) {
        return commandeMarketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable avec l'ID : " + id));
    }

    private void ensureAcheteur(CommandeMarket commande, User user) {
        if (!commande.getAcheteur().getId().equals(user.getId())) {
            throw new SecurityException("Cette commande ne vous appartient pas.");
        }
    }

    private void ensurePorteurVendeur(CommandeMarket commande, User user) {
        if (commande.getProjet().getPorteur() == null
                || !commande.getProjet().getPorteur().getId().equals(user.getId())) {
            throw new SecurityException("Cette commande ne concerne pas un de vos projets.");
        }
    }

    private void ensureStatut(CommandeMarket commande, StatutCommandeMarket attendu, String action) {
        if (commande.getStatut() != attendu) {
            throw new IllegalStateException(
                    "Impossible de " + action + " : la commande n'est pas au statut attendu ("
                            + commande.getStatut() + ").");
        }
    }

    private void restaurerStock(CommandeMarket commande) {
        for (CommandeMarketLigne ligne : commande.getLignes()) {
            ArticleMarket article = ligne.getArticle();
            if (article != null && article.getStock() != null) {
                article.setStock(article.getStock() + ligne.getQuantite());
                articleMarketRepository.save(article);
            }
        }
    }

    // ── Achat, côté acheteur — paiement immédiat, pas de validation admin ────
    @Transactional
    public CommandeMarket creerCommande(User acheteur, CommandeMarketCreateDTO dto) {
        Projet projetVendeur = null;
        List<CommandeMarketLigne> lignes = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        CommandeMarket commande = new CommandeMarket();

        for (CommandeMarketLigneCreateDTO ligneDto : dto.lignes()) {
            ArticleMarket article = articleMarketRepository.findById(ligneDto.articleId())
                    .orElseThrow(() -> new EntityNotFoundException("Article introuvable : " + ligneDto.articleId()));

            if (projetVendeur == null) {
                projetVendeur = article.getProjet();
            } else if (!projetVendeur.getId().equals(article.getProjet().getId())) {
                throw new IllegalArgumentException(
                        "Tous les articles d'une même commande GrowzMarket doivent venir du même vendeur.");
            }

            if (!article.isDisponible()) {
                throw new IllegalArgumentException("L'article " + article.getNom() + " n'est plus disponible.");
            }
            if (article.getStock() != null && article.getStock() < ligneDto.quantite()) {
                throw new IllegalArgumentException(
                        "Stock insuffisant pour " + article.getNom() + " (" + article.getStock() + " restant(s)).");
            }
            if (article.getProjet().getPorteur() != null
                    && article.getProjet().getPorteur().getId().equals(acheteur.getId())) {
                throw new IllegalArgumentException("Vous ne pouvez pas acheter vos propres articles.");
            }

            if (article.getStock() != null) {
                article.setStock(article.getStock() - ligneDto.quantite());
                articleMarketRepository.save(article);
            }

            CommandeMarketLigne ligne = new CommandeMarketLigne();
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

        commande.setAcheteur(acheteur);
        commande.setProjet(projetVendeur);
        commande.setLignes(lignes);
        commande.setMontantTotal(total);
        commande.setConfirmationLieuRetrait(dto.confirmationLieuRetrait());
        commande.setStatut(StatutCommandeMarket.PAYEE);

        CommandeMarket saved = commandeMarketRepository.save(commande);

        // Paiement immédiat — pas d'étape intermédiaire, contrairement au
        // module Fournisseur où l'admin valide avant tout mouvement de fonds.
        txHelper.executerAchat(acheteur.getId(), projetVendeur.getId(), saved.getId(), total);

        User porteur = projetVendeur.getPorteur();
        if (porteur != null) {
            notificationService.notifyUser(
                    porteur,
                    "Nouvelle vente GrowzMarket",
                    "Commande #" + saved.getId() + " (" + total.toPlainString() + " FCFA) sur " + projetVendeur.getLibelle()
                            + " — préparez-la pour le retrait.",
                    projetVendeur.getId(), projetVendeur.getSlug());
        }

        return saved;
    }

    // ── Préparation, côté porteur-vendeur ────────────────────────────────────
    @Transactional
    public CommandeMarket marquerPrete(Long id, User porteur) {
        CommandeMarket commande = getOrThrow(id);
        ensurePorteurVendeur(commande, porteur);
        ensureStatut(commande, StatutCommandeMarket.PAYEE, "marquer cette commande comme prête");

        commande.setStatut(StatutCommandeMarket.PRETE_AU_RETRAIT);
        commande.setDatePrete(LocalDateTime.now());
        CommandeMarket saved = commandeMarketRepository.save(commande);

        notificationService.notifyUser(
                commande.getAcheteur(),
                "Votre commande GrowzMarket est prête",
                "Commande #" + commande.getId() + " est prête au retrait : " + commande.getProjet().getLibelle() + ".",
                null, "/growzmarket/mes-achats");

        return saved;
    }

    // ── Validation du retrait, côté vendeur ─────────────────────────────────
    // C'est le porteur-vendeur qui clôture la commande, pas l'acheteur : il
    // retrouve la commande dans "Mes ventes" via son numéro (montré/communiqué
    // par l'acheteur physiquement présent) et valide lui-même la remise. Un
    // acheteur ne peut pas s'auto-confirmer une réception qui n'a pas eu lieu.
    @Transactional
    public CommandeMarket validerRetrait(Long id, User porteurUser) {
        CommandeMarket commande = getOrThrow(id);
        ensurePorteurVendeur(commande, porteurUser);
        ensureStatut(commande, StatutCommandeMarket.PRETE_AU_RETRAIT, "valider le retrait de cette commande");

        commande.setStatut(StatutCommandeMarket.RETIREE);
        commande.setDateRetraitConfirme(LocalDateTime.now());

        byte[] pdfBytes = facturePdfService.generateFacture(commande);
        String factureUrl = fileUploadService.enregistrerFactureMarketGeneree(pdfBytes, commande.getId());
        commande.setFactureUrl(factureUrl);

        CommandeMarket saved = commandeMarketRepository.save(commande);

        notificationService.notifyUser(
                commande.getAcheteur(),
                "Retrait validé",
                "Le vendeur a validé la remise de votre commande #" + commande.getId() + ".",
                null, "/mon-espace/mes-achats-market");

        return saved;
    }

    @Transactional
    public CommandeMarket ouvrirLitige(Long id, User user, String motif) {
        CommandeMarket commande = getOrThrow(id);
        if (!commande.getAcheteur().getId().equals(user.getId())
                && (commande.getProjet().getPorteur() == null
                        || !commande.getProjet().getPorteur().getId().equals(user.getId()))) {
            throw new SecurityException("Cette commande ne vous concerne pas.");
        }
        if (commande.getStatut() != StatutCommandeMarket.PRETE_AU_RETRAIT
                && commande.getStatut() != StatutCommandeMarket.NON_RETIREE) {
            throw new IllegalStateException(
                    "Impossible d'ouvrir un litige sur cette commande (statut " + commande.getStatut() + ").");
        }

        commande.setStatut(StatutCommandeMarket.LITIGE);
        commande.setMotifLitige(motif);
        CommandeMarket saved = commandeMarketRepository.save(commande);

        notificationService.notifyAdmins(
                "Litige GrowzMarket",
                "Commande #" + commande.getId() + " — " + motif,
                "/admin/growzmarket");

        return saved;
    }

    // ── Arbitrage admin ──────────────────────────────────────────────────────
    @Transactional
    public CommandeMarket arbitrer(Long id, boolean enFaveurDuVendeur, String motif) {
        CommandeMarket commande = getOrThrow(id);
        ensureStatut(commande, StatutCommandeMarket.LITIGE, "arbitrer cette commande");

        if (enFaveurDuVendeur) {
            commande.setStatut(StatutCommandeMarket.RETIREE);
            commande.setDateRetraitConfirme(LocalDateTime.now());
        } else {
            commande.setStatut(StatutCommandeMarket.ANNULEE);
            restaurerStock(commande);
            txHelper.rembourserAcheteur(
                    commande.getAcheteur().getId(),
                    commande.getProjet().getId(),
                    commande.getId(),
                    commande.getMontantTotal());
        }
        commande.setMotifLitige((commande.getMotifLitige() != null ? commande.getMotifLitige() + " | " : "")
                + "Arbitrage admin : " + motif);

        return commandeMarketRepository.save(commande);
    }

    public List<CommandeMarket> getMesAchats(Long acheteurId) {
        return commandeMarketRepository.findByAcheteurIdOrderByDateCommandeDesc(acheteurId);
    }

    public List<CommandeMarket> getMesVentes(Long porteurId) {
        return commandeMarketRepository.findByProjetPorteurIdOrderByDateCommandeDesc(porteurId);
    }

    // ── Job planifié : commandes prêtes jamais retirées ─────────────────────
    // Ouvre automatiquement un litige (directement exploitable dans l'écran
    // d'arbitrage admin déjà existant) plutôt que de laisser une commande
    // NON_RETIREE sans aucun moyen de la traiter côté UI.
    @Transactional
    public void traiterCommandesNonRetirees(int delaiJours) {
        LocalDateTime seuil = LocalDateTime.now().minusDays(delaiJours);
        List<CommandeMarket> expirees = commandeMarketRepository
                .findByStatutAndDatePreteBefore(StatutCommandeMarket.PRETE_AU_RETRAIT, seuil);

        for (CommandeMarket commande : expirees) {
            commande.setStatut(StatutCommandeMarket.LITIGE);
            commande.setMotifLitige("Non retirée sous " + delaiJours + " jours — litige ouvert automatiquement.");
            commandeMarketRepository.save(commande);

            notificationService.notifyAdmins(
                    "Litige GrowzMarket (automatique)",
                    "Commande #" + commande.getId() + " jamais retirée après " + delaiJours + " jours.",
                    "/admin/growzmarket");

            User porteur = commande.getProjet().getPorteur();
            if (porteur != null) {
                notificationService.notifyUser(
                        porteur,
                        "Commande non retirée",
                        "La commande #" + commande.getId() + " n'a pas été retirée à temps — un litige a été ouvert.",
                        commande.getProjet().getId(), commande.getProjet().getSlug());
            }
            notificationService.notifyUser(
                    commande.getAcheteur(),
                    "Commande non retirée",
                    "Vous n'avez pas retiré la commande #" + commande.getId() + " à temps — un litige a été ouvert, contactez le support si besoin.",
                    null, "/mon-espace/mes-achats-market");
        }
    }

    public List<CommandeMarket> getEnLitigeAdmin() {
        return commandeMarketRepository.findByStatutOrderByDateCommandeDesc(StatutCommandeMarket.LITIGE);
    }

    public List<CommandeMarket> getToutesAdmin() {
        return commandeMarketRepository.findAllByOrderByDateCommandeDesc();
    }

    public CommandeMarket getCommandeAvecAutorisation(Long id, User user, boolean estAdmin) {
        CommandeMarket commande = getOrThrow(id);
        boolean estAcheteur = commande.getAcheteur().getId().equals(user.getId());
        boolean estVendeur = commande.getProjet().getPorteur() != null
                && commande.getProjet().getPorteur().getId().equals(user.getId());
        if (!estAdmin && !estAcheteur && !estVendeur) {
            throw new SecurityException("Vous n'avez pas accès à cette commande.");
        }
        return commande;
    }

    public byte[] chargerFacture(CommandeMarket commande) {
        return fileUploadService.chargerFactureMarket(commande.getFactureUrl());
    }

    public CommandeMarketDTO toDto(CommandeMarket c) {
        List<CommandeMarketLigneDTO> lignes = c.getLignes().stream()
                .map(l -> new CommandeMarketLigneDTO(
                        l.getId(),
                        l.getArticle() != null ? l.getArticle().getId() : null,
                        l.getLibelle(),
                        l.getPrixUnitaire(),
                        l.getQuantite(),
                        l.getSousTotal()))
                .toList();

        User porteur = c.getProjet().getPorteur();

        return new CommandeMarketDTO(
                c.getId(),
                c.getProjet().getId(),
                c.getProjet().getLibelle(),
                porteur != null ? (porteur.getPrenom() + " " + porteur.getNom()).trim() : null,
                c.getAcheteur().getId(),
                (c.getAcheteur().getPrenom() + " " + c.getAcheteur().getNom()).trim(),
                c.getMontantTotal(),
                c.getStatut().name(),
                c.isConfirmationLieuRetrait(),
                c.getDateCommande(),
                c.getDatePrete(),
                c.getDateRetraitConfirme(),
                c.getMotifLitige(),
                c.getFactureUrl(),
                lignes);
    }
}
