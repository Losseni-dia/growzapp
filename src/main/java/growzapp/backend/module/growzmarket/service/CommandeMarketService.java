package growzapp.backend.module.growzmarket.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.growzmarket.dto.CommandeMarketCreateDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketLigneCreateDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketLigneDTO;
import growzapp.backend.module.growzmarket.dto.LitigeMessageDTO;
import growzapp.backend.module.growzmarket.enums.StatutCommandeMarket;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.growzmarket.model.CommandeMarket;
import growzapp.backend.module.growzmarket.model.CommandeMarketLigne;
import growzapp.backend.module.growzmarket.model.CommandeMarketLitigeMessage;
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
    private final ArticleMarketService articleMarketService;

    private CommandeMarket getOrThrow(Long id) {
        return commandeMarketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable avec l'ID : " + id));
    }

    // Décode le format compact "articleId:quantite,articleId:quantite"
    // reçu en aller-retour via les métadonnées d'un paiement externe (voir
    // CommandeMarketController.encoderLignesCompact) — utilisé par les
    // webhooks Stripe/FedaPay/PayDunya pour reconstruire la commande réelle
    // uniquement après confirmation du paiement.
    public static CommandeMarketCreateDTO decoderCommandeCompacte(String lignesCompact,
            boolean confirmationLieuRetrait) {
        List<CommandeMarketLigneCreateDTO> lignes = new ArrayList<>();
        if (lignesCompact != null && !lignesCompact.isBlank()) {
            for (String part : lignesCompact.split(",")) {
                String[] pair = part.split(":");
                lignes.add(new CommandeMarketLigneCreateDTO(Long.parseLong(pair[0]), Integer.parseInt(pair[1])));
            }
        }
        return new CommandeMarketCreateDTO(lignes, confirmationLieuRetrait);
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

    // Un groupe = les lignes d'une commande GrowzMarket destinées à un même
    // projet vendeur — un panier peut désormais contenir plusieurs vendeurs,
    // chacun devient sa propre CommandeMarket, mais tous sont payés en une
    // seule fois (un seul débit wallet, ou une seule session Stripe/Mobile
    // Money) puis répartis vers chaque trésorerie projet.
    public record VendorGroup(Projet projetVendeur, List<CommandeMarketLigne> lignes, BigDecimal sousTotal) {
    }

    public record PrevisualisationResult(List<VendorGroup> groupes, BigDecimal total) {
    }

    // Valide les lignes (disponibilité, stock, pas d'auto-achat) et les
    // regroupe par vendeur — décrémente le stock uniquement si demandé,
    // appelé au moment où l'achat est réellement engagé (wallet :
    // immédiatement ; paiement externe : à la confirmation webhook, jamais
    // avant, pour ne bloquer le stock qu'un paiement réussi).
    private List<VendorGroup> grouperParVendeur(User acheteur, CommandeMarketCreateDTO dto,
            boolean decrementerStock) {
        Map<Long, Projet> projetsParId = new LinkedHashMap<>();
        Map<Long, List<CommandeMarketLigne>> lignesParProjet = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalParProjet = new LinkedHashMap<>();

        for (CommandeMarketLigneCreateDTO ligneDto : dto.lignes()) {
            ArticleMarket article = articleMarketRepository.findById(ligneDto.articleId())
                    .orElseThrow(() -> new EntityNotFoundException("Article introuvable : " + ligneDto.articleId()));

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

            if (decrementerStock && article.getStock() != null) {
                article.setStock(article.getStock() - ligneDto.quantite());
                articleMarketRepository.save(article);
            }

            Long projetId = article.getProjet().getId();
            projetsParId.putIfAbsent(projetId, article.getProjet());

            CommandeMarketLigne ligne = new CommandeMarketLigne();
            ligne.setArticle(article);
            ligne.setLibelle(article.getNom());
            ligne.setPrixUnitaire(article.getPrix());
            ligne.setQuantite(ligneDto.quantite());
            BigDecimal sousTotal = article.getPrix().multiply(BigDecimal.valueOf(ligneDto.quantite()));
            ligne.setSousTotal(sousTotal);

            lignesParProjet.computeIfAbsent(projetId, k -> new ArrayList<>()).add(ligne);
            totalParProjet.merge(projetId, sousTotal, BigDecimal::add);
        }

        List<VendorGroup> groupes = new ArrayList<>();
        for (Long projetId : projetsParId.keySet()) {
            groupes.add(new VendorGroup(projetsParId.get(projetId), lignesParProjet.get(projetId),
                    totalParProjet.get(projetId)));
        }
        return groupes;
    }

    private CommandeMarket construireCommande(User acheteur, VendorGroup groupe, boolean confirmationLieuRetrait) {
        CommandeMarket commande = new CommandeMarket();
        commande.setAcheteur(acheteur);
        commande.setProjet(groupe.projetVendeur());
        for (CommandeMarketLigne ligne : groupe.lignes()) {
            ligne.setCommande(commande);
        }
        commande.setLignes(groupe.lignes());
        commande.setMontantTotal(groupe.sousTotal());
        commande.setConfirmationLieuRetrait(confirmationLieuRetrait);
        commande.setStatut(StatutCommandeMarket.PAYEE);
        return commande;
    }

    // ── Achat, côté acheteur — paiement immédiat, pas de validation admin ────
    // Un seul débit wallet pour le panier entier, réparti vers chaque
    // trésorerie projet (voir CommandeMarketTransactionHelper.VendorPart).
    @Transactional
    public List<CommandeMarket> creerCommande(User acheteur, CommandeMarketCreateDTO dto) {
        List<VendorGroup> groupes = grouperParVendeur(acheteur, dto, true);

        List<CommandeMarket> commandes = new ArrayList<>();
        List<CommandeMarketTransactionHelper.VendorPart> parts = new ArrayList<>();
        BigDecimal montantTotal = BigDecimal.ZERO;

        for (VendorGroup groupe : groupes) {
            CommandeMarket saved = commandeMarketRepository.save(
                    construireCommande(acheteur, groupe, dto.confirmationLieuRetrait()));
            commandes.add(saved);
            parts.add(new CommandeMarketTransactionHelper.VendorPart(
                    groupe.projetVendeur().getId(), saved.getId(), groupe.sousTotal()));
            montantTotal = montantTotal.add(groupe.sousTotal());

            notifierNouvelleVente(saved, groupe.projetVendeur(), groupe.sousTotal());
        }

        txHelper.executerAchatMultiVendeur(acheteur.getId(), parts, montantTotal);
        return commandes;
    }

    // ── Achat payé directement par Mobile Money/Carte (jamais par le wallet
    // interne) — les commandes ne sont créées qu'à la confirmation webhook du
    // paiement, jamais avant (cf previsualiserCommande pour l'estimation
    // affichée au moment du choix du moyen de paiement).
    @Transactional
    public List<CommandeMarket> creerCommandeDepuisPaiementExterne(User acheteur, CommandeMarketCreateDTO dto,
            growzapp.backend.module.wallet.enums.SourcePaiement source) {
        List<VendorGroup> groupes = grouperParVendeur(acheteur, dto, true);

        List<CommandeMarket> commandes = new ArrayList<>();
        List<CommandeMarketTransactionHelper.VendorPart> parts = new ArrayList<>();

        for (VendorGroup groupe : groupes) {
            CommandeMarket saved = commandeMarketRepository.save(
                    construireCommande(acheteur, groupe, dto.confirmationLieuRetrait()));
            commandes.add(saved);
            parts.add(new CommandeMarketTransactionHelper.VendorPart(
                    groupe.projetVendeur().getId(), saved.getId(), groupe.sousTotal()));

            notifierNouvelleVente(saved, groupe.projetVendeur(), groupe.sousTotal());
        }

        txHelper.executerAchatExterneMultiVendeur(acheteur.getId(), parts, source);
        return commandes;
    }

    // Validation stricte en LECTURE SEULE (ne décrémente jamais le stock) —
    // utilisée pour calculer le montant exact avant de créer une session de
    // paiement externe (Mobile Money/Carte) : le stock n'est réellement
    // engagé qu'à la confirmation du paiement (voir
    // creerCommandeDepuisPaiementExterne), jamais avant.
    public PrevisualisationResult previsualiserCommande(User acheteur, CommandeMarketCreateDTO dto) {
        List<VendorGroup> groupes = grouperParVendeur(acheteur, dto, false);
        BigDecimal total = groupes.stream().map(VendorGroup::sousTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PrevisualisationResult(groupes, total);
    }

    private void notifierNouvelleVente(CommandeMarket saved, Projet projetVendeur, BigDecimal total) {
        User porteur = projetVendeur.getPorteur();
        if (porteur != null) {
            notificationService.notifyUser(
                    porteur,
                    "Nouvelle vente GrowzMarket",
                    "Commande #" + saved.getId() + " (" + total.toPlainString() + " FCFA) sur " + projetVendeur.getLibelle()
                            + " — préparez-la pour le retrait.",
                    null, "/mon-espace/mes-ventes-market");
        }
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
                null, "/mon-espace/mes-achats-market");

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

    // ── Échange sur un litige (acheteur, vendeur, admin) — avant clôture ────
    // destinataire : uniquement pour un message admin (ACHETEUR ou VENDEUR,
    // jamais les deux) — l'admin doit choisir à qui il s'adresse, l'autre
    // partie ne verra jamais ce message précis. Les messages acheteur/vendeur
    // restent visibles de toutes les parties, destinataire reste null pour eux.
    @Transactional
    public CommandeMarket ajouterMessageLitige(Long id, User auteur, String contenu, boolean estAdmin,
            String destinataire) {
        CommandeMarket commande = getOrThrow(id);
        boolean estAcheteur = commande.getAcheteur().getId().equals(auteur.getId());
        boolean estVendeur = commande.getProjet().getPorteur() != null
                && commande.getProjet().getPorteur().getId().equals(auteur.getId());
        if (!estAdmin && !estAcheteur && !estVendeur) {
            throw new SecurityException("Vous n'avez pas accès à cette commande.");
        }
        if (commande.getStatut() != StatutCommandeMarket.LITIGE) {
            throw new IllegalStateException(
                    "Cette commande n'est pas en litige (statut " + commande.getStatut() + ").");
        }

        CommandeMarketLitigeMessage message = new CommandeMarketLitigeMessage();
        message.setCommande(commande);
        message.setAuteur(auteur);
        message.setRole(estAdmin ? "ADMIN" : (estAcheteur ? "ACHETEUR" : "VENDEUR"));
        message.setContenu(contenu);
        message.setDestinataire(estAdmin ? destinataire : null);
        commande.getLitigeMessages().add(message);
        CommandeMarket saved = commandeMarketRepository.save(commande);

        // Notifie les autres parties, jamais l'auteur lui-même. Un message
        // admin ne notifie que le destinataire choisi, pas l'autre partie.
        String texte = "Commande #" + commande.getId() + " : nouveau message sur le litige.";
        if (estAdmin) {
            if ("ACHETEUR".equals(destinataire)) {
                notificationService.notifyUser(commande.getAcheteur(), "Litige GrowzMarket", texte,
                        null, "/mon-espace/mes-achats-market");
            } else if ("VENDEUR".equals(destinataire) && commande.getProjet().getPorteur() != null) {
                notificationService.notifyUser(commande.getProjet().getPorteur(), "Litige GrowzMarket", texte,
                        null, "/mon-espace/mes-ventes-market");
            }
        } else {
            if (!estAcheteur) {
                notificationService.notifyUser(commande.getAcheteur(), "Litige GrowzMarket", texte,
                        null, "/mon-espace/mes-achats-market");
            }
            if (!estVendeur && commande.getProjet().getPorteur() != null) {
                notificationService.notifyUser(commande.getProjet().getPorteur(), "Litige GrowzMarket", texte,
                        null, "/mon-espace/mes-ventes-market");
            }
            notificationService.notifyAdmins("Litige GrowzMarket", texte, "/admin/growzmarket");
        }

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

    // Vue admin par défaut — aucune restriction, l'admin voit à qui chaque
    // message a été adressé.
    public CommandeMarketDTO toDto(CommandeMarket c) {
        return toDto(c, null, true, "fr");
    }

    public CommandeMarketDTO toDto(CommandeMarket c, Long viewerId, boolean isAdmin) {
        return toDto(c, viewerId, isAdmin, "fr");
    }

    // Vue acheteur/vendeur : un message admin destiné à l'autre partie
    // reste invisible — seul l'admin et le destinataire choisi le voient.
    // Le libellé de chaque ligne est figé en français au moment de la
    // commande (snapshot) — on le retraduit à la volée via l'article
    // d'origine, comme pour le catalogue, plutôt que de le retraduire à la
    // commande (qui figerait une seule langue pour toujours).
    public CommandeMarketDTO toDto(CommandeMarket c, Long viewerId, boolean isAdmin, String langue) {
        List<CommandeMarketLigneDTO> lignes = c.getLignes().stream()
                .map(l -> new CommandeMarketLigneDTO(
                        l.getId(),
                        l.getArticle() != null ? l.getArticle().getId() : null,
                        articleMarketService.traduireNomArticle(
                                l.getArticle() != null ? l.getArticle().getId() : null, l.getLibelle(), langue),
                        l.getPrixUnitaire(),
                        l.getQuantite(),
                        l.getSousTotal()))
                .toList();

        boolean viewerEstAcheteur = !isAdmin && viewerId != null && c.getAcheteur().getId().equals(viewerId);
        boolean viewerEstVendeur = !isAdmin && viewerId != null && c.getProjet().getPorteur() != null
                && c.getProjet().getPorteur().getId().equals(viewerId);

        List<LitigeMessageDTO> litigeMessages = c.getLitigeMessages().stream()
                .filter(m -> {
                    if (isAdmin || !"ADMIN".equals(m.getRole())) {
                        return true;
                    }
                    if (viewerEstAcheteur) {
                        return "ACHETEUR".equals(m.getDestinataire());
                    }
                    if (viewerEstVendeur) {
                        return "VENDEUR".equals(m.getDestinataire());
                    }
                    return false;
                })
                .map(m -> new LitigeMessageDTO(
                        m.getId(),
                        m.getAuteur().getId(),
                        (m.getAuteur().getPrenom() + " " + m.getAuteur().getNom()).trim(),
                        m.getRole(),
                        m.getContenu(),
                        m.getDestinataire(),
                        m.getDateEnvoi()))
                .toList();

        User porteur = c.getProjet().getPorteur();

        return new CommandeMarketDTO(
                c.getId(),
                c.getProjet().getId(),
                articleMarketService.traduireProjetLibelle(c.getProjet().getId(), c.getProjet().getLibelle(), langue),
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
                lignes,
                litigeMessages);
    }
}
