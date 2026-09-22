package growzapp.backend.module.fournisseur.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.document.model.Document;
import growzapp.backend.module.document.service.DocumentService;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.files.FileUploadService;
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
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
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
    private final InvestissementRepository investissementRepository;
    private final CommandeTransactionHelper txHelper;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final FileUploadService fileUploadService;
    private final DocumentService documentService;
    private final CommandeFacturePdfService commandeFacturePdfService;
    private final WalletRepository walletRepository;

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

    // Restitue le stock réservé à la commande — appelé chaque fois qu'une
    // commande n'aboutit finalement pas (refus, rejet, litige perdu par le
    // fournisseur) après avoir décrémenté le stock à la création.
    private void restaurerStock(Commande commande) {
        for (CommandeLigne ligne : commande.getLignes()) {
            ArticleFournisseur article = ligne.getArticle();
            if (article != null && article.getStock() != null) {
                article.setStock(article.getStock() + ligne.getQuantite());
                articleFournisseurRepository.save(article);
            }
        }
    }

    private void ensureStatut(Commande commande, StatutCommande attendu, String action) {
        if (commande.getStatut() != attendu) {
            throw new IllegalStateException(
                    "Impossible de " + action + " : la commande n'est pas au statut attendu ("
                            + commande.getStatut() + ").");
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
            if (article.getStock() != null && article.getStock() < ligneDto.quantite()) {
                throw new IllegalArgumentException(
                        "Stock insuffisant pour " + article.getNom() + " (" + article.getStock() + " restant(s)).");
            }

            // Réservé dès la commande (pas seulement à l'acceptation) pour
            // éviter que deux porteurs commandent en même temps plus que le
            // stock réel — restauré si le fournisseur refuse, si l'admin
            // rejette, ou si un litige est arbitré en faveur du porteur.
            if (article.getStock() != null) {
                article.setStock(article.getStock() - ligneDto.quantite());
                articleFournisseurRepository.save(article);
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

        // Les achats fournisseur sont payés sur la trésorerie encore
        // séquestrée du projet (soldeBloque) — on vérifie donc ce solde-là,
        // pas soldeDisponible (réservé aux dépenses personnelles du porteur).
        Wallet walletProjet = walletRepository.findByProjetIdAndWalletType(projet.getId(), WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        if (walletProjet.getSoldeBloque().compareTo(total) < 0) {
            throw new IllegalArgumentException(
                    "Trésorerie du projet insuffisante pour cette commande (" + total.toPlainString()
                            + " FCFA requis, " + walletProjet.getSoldeBloque().toPlainString()
                            + " FCFA disponible en trésorerie bloquée).");
        }

        commande.setLignes(lignes);
        commande.setMontantTotal(total);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyAdmins(
                "Nouvelle commande fournisseur à valider",
                projet.getLibelle() + " → " + fournisseurNomAffiche(fournisseur)
                        + " (" + total.toPlainString() + " FCFA)",
                "/admin/commandes");

        return saved;
    }

    // ── Validation / rejet, côté admin ──────────────────────────────────────
    // Aucun mouvement de fonds à cette étape — seule l'acceptation du
    // fournisseur, l'expédition et la confirmation de réception ouvrent
    // droit au paiement, exécuté explicitement par l'admin ensuite.
    @Transactional
    public Commande validerAdmin(Long id) {
        Commande commande = getOrThrow(id);
        ensureStatut(commande, StatutCommande.EN_ATTENTE_VALIDATION, "valider cette commande");

        commande.setStatut(StatutCommande.EN_ATTENTE_ACCEPTATION);
        commande.setDateValidationAdmin(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getFournisseur().getUser(),
                "Nouvelle commande à accepter",
                "Commande #" + commande.getId() + " pour le projet " + commande.getProjet().getLibelle()
                        + " (" + commande.getMontantTotal().toPlainString() + " FCFA) — merci de l'accepter ou de la refuser.",
                null, "/mon-espace/fournisseur");
        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande validée",
                "Votre commande auprès de " + fournisseurNomAffiche(commande.getFournisseur())
                        + " a été validée par l'équipe GrowzApp, en attente d'acceptation du fournisseur.",
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    @Transactional
    public Commande rejeterAdmin(Long id, String motif) {
        Commande commande = getOrThrow(id);
        ensureStatut(commande, StatutCommande.EN_ATTENTE_VALIDATION, "rejeter cette commande");

        commande.setStatut(StatutCommande.REJETEE);
        commande.setMotifRejet(motif);
        restaurerStock(commande);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande rejetée",
                motif,
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    // ── Acceptation / refus, côté fournisseur ───────────────────────────────
    @Transactional
    public Commande accepterFournisseur(Long id, User fournisseurUser) {
        Commande commande = getOrThrow(id);
        ensureFournisseurProprietaire(commande, fournisseurUser);
        ensureStatut(commande, StatutCommande.EN_ATTENTE_ACCEPTATION, "accepter cette commande");

        commande.setStatut(StatutCommande.ACCEPTEE);
        commande.setDateAcceptation(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande acceptée",
                fournisseurNomAffiche(commande.getFournisseur()) + " a accepté votre commande #" + commande.getId()
                        + ".",
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    @Transactional
    public Commande refuserFournisseur(Long id, User fournisseurUser, String motif) {
        Commande commande = getOrThrow(id);
        ensureFournisseurProprietaire(commande, fournisseurUser);
        ensureStatut(commande, StatutCommande.EN_ATTENTE_ACCEPTATION, "refuser cette commande");

        commande.setStatut(StatutCommande.REFUSEE);
        commande.setMotifRefus(motif);
        restaurerStock(commande);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande refusée",
                fournisseurNomAffiche(commande.getFournisseur()) + " a refusé votre commande #" + commande.getId()
                        + " : " + motif,
                commande.getProjet().getId(), commande.getProjet().getSlug());
        notificationService.notifyAdmins(
                "Commande fournisseur refusée",
                "Commande #" + commande.getId() + " refusée par " + fournisseurNomAffiche(commande.getFournisseur())
                        + " : " + motif,
                "/admin/commandes");

        return saved;
    }

    // ── Expédition, côté fournisseur ─────────────────────────────────────────
    // La facture est générée automatiquement par le serveur à partir des
    // données de la commande — le fournisseur n'a rien à uploader.
    @Transactional
    public Commande marquerExpediee(Long id, User fournisseurUser) {
        Commande commande = getOrThrow(id);
        ensureFournisseurProprietaire(commande, fournisseurUser);
        ensureStatut(commande, StatutCommande.ACCEPTEE, "marquer cette commande comme expédiée");

        byte[] pdfBytes = commandeFacturePdfService.generateFacture(commande);
        String factureUrl = fileUploadService.enregistrerFactureGeneree(pdfBytes, commande.getId());

        commande.setStatut(StatutCommande.EXPEDIEE);
        commande.setDateExpedition(LocalDateTime.now());
        commande.setFactureUrl(factureUrl);
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getProjet().getPorteur(),
                "Commande expédiée",
                fournisseurNomAffiche(commande.getFournisseur())
                        + " a expédié votre commande #" + commande.getId()
                        + ". Merci de confirmer la réception dès son arrivée.",
                commande.getProjet().getId(), commande.getProjet().getSlug());

        return saved;
    }

    // ── Confirmation de réception / litige, côté porteur ────────────────────
    @Transactional
    public Commande confirmerReception(Long id, User porteur) {
        Commande commande = getOrThrow(id);
        ensurePorteurDuProjet(commande, porteur);
        ensureStatut(commande, StatutCommande.EXPEDIEE, "confirmer la réception de cette commande");

        commande.setStatut(StatutCommande.LIVREE);
        commande.setDateConfirmationReception(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        notificationService.notifyUser(
                commande.getFournisseur().getUser(),
                "Réception confirmée",
                "Le porteur a confirmé la réception de la commande #" + commande.getId()
                        + " — le paiement va être exécuté par l'équipe GrowzApp.",
                null, "/mon-espace/fournisseur");
        notificationService.notifyAdmins(
                "Commande à payer",
                "Commande #" + commande.getId() + " (" + commande.getProjet().getLibelle() + ") livrée et confirmée — paiement à exécuter.",
                "/admin/commandes");

        return saved;
    }

    @Transactional
    public Commande ouvrirLitige(Long id, User porteur, String motif) {
        Commande commande = getOrThrow(id);
        ensurePorteurDuProjet(commande, porteur);
        ensureStatut(commande, StatutCommande.EXPEDIEE, "ouvrir un litige sur cette commande");

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
    // Aucun mouvement de fonds n'est jamais nécessaire ici : l'argent n'a pas
    // encore bougé à ce stade du cycle.
    @Transactional
    public Commande arbitrerLitige(Long id, boolean enFaveurDuFournisseur, String motif) {
        Commande commande = getOrThrow(id);
        ensureStatut(commande, StatutCommande.LITIGE, "arbitrer cette commande");

        if (enFaveurDuFournisseur) {
            commande.setStatut(StatutCommande.LIVREE);
            commande.setDateConfirmationReception(LocalDateTime.now());
        } else {
            commande.setStatut(StatutCommande.ANNULEE);
            restaurerStock(commande);
        }
        commande.setMotifLitige((commande.getMotifLitige() != null ? commande.getMotifLitige() + " | " : "")
                + "Arbitrage admin : " + motif);
        return commandeRepository.save(commande);
    }

    // ── Paiement, côté admin (unique mouvement de fonds) ────────────────────
    @Transactional
    public Commande executerPaiement(Long id) {
        Commande commande = getOrThrow(id);
        ensureStatut(commande, StatutCommande.LIVREE, "payer cette commande");

        txHelper.executerPaiement(
                commande.getProjet().getId(),
                commande.getFournisseur().getUser().getId(),
                commande.getId(),
                commande.getMontantTotal());

        commande.setStatut(StatutCommande.PAYEE);
        commande.setDatePaiement(LocalDateTime.now());
        Commande saved = commandeRepository.save(commande);

        attacherFactureAuxDocuments(saved);
        notifierPaiement(saved);

        return saved;
    }

    private void attacherFactureAuxDocuments(Commande commande) {
        if (commande.getFactureUrl() == null) {
            return;
        }
        try {
            String filenameDocuments = fileUploadService.copierFactureVersDocuments(commande.getFactureUrl());
            Document document = new Document();
            document.setNom("Facture fournisseur — Commande #" + commande.getId());
            document.setFilename(filenameDocuments);
            document.setType("FACTURE");
            document.setDescription("Facture de " + fournisseurNomAffiche(commande.getFournisseur())
                    + " pour la commande #" + commande.getId());
            document.setProjet(commande.getProjet());
            documentService.save(document);
        } catch (Exception e) {
            // Ne bloque jamais le paiement déjà exécuté — juste tracé pour
            // intervention manuelle si la copie du fichier échoue.
            org.slf4j.LoggerFactory.getLogger(CommandeService.class)
                    .error("Échec de l'ajout de la facture aux documents du projet {} (commande {}) : {}",
                            commande.getProjet().getId(), commande.getId(), e.getMessage(), e);
        }
    }

    private void notifierPaiement(Commande commande) {
        String montantFormate = commande.getMontantTotal().toPlainString() + " FCFA";
        String fournisseurNom = fournisseurNomAffiche(commande.getFournisseur());

        notificationService.notifyUser(
                commande.getFournisseur().getUser(),
                "Paiement reçu",
                "Le paiement de " + montantFormate + " pour la commande #" + commande.getId()
                        + " est maintenant disponible sur votre wallet.",
                null, "/mon-espace/fournisseur");

        User porteur = commande.getProjet().getPorteur();
        notificationService.notifyUser(
                porteur,
                "Paiement fournisseur effectué",
                montantFormate + " versés à " + fournisseurNom + " pour la commande #" + commande.getId() + ".",
                commande.getProjet().getId(), commande.getProjet().getSlug());
        String emailPorteur = resolveEmail(porteur);
        if (emailPorteur != null) {
            emailService.envoyerPaiementFournisseurInvestisseur(
                    emailPorteur, (porteur.getPrenom() + " " + porteur.getNom()).trim(),
                    commande.getProjet().getLibelle(), fournisseurNom, montantFormate, commande.getId());
            if (commande.getFactureUrl() != null) {
                emailService.envoyerFactureDisponibleInvestisseur(
                        emailPorteur, (porteur.getPrenom() + " " + porteur.getNom()).trim(),
                        commande.getProjet().getLibelle(), commande.getId());
            }
        }

        for (User investisseur : getInvestisseursValides(commande.getProjet())) {
            notificationService.notifyUser(
                    investisseur,
                    "Paiement fournisseur effectué",
                    "Commande #" + commande.getId() + " — " + montantFormate + " versés à " + fournisseurNom
                            + " pour le projet " + commande.getProjet().getLibelle() + ".",
                    commande.getProjet().getId(), commande.getProjet().getSlug());

            String destinataire = resolveEmail(investisseur);
            if (destinataire != null) {
                emailService.envoyerPaiementFournisseurInvestisseur(
                        destinataire, (investisseur.getPrenom() + " " + investisseur.getNom()).trim(),
                        commande.getProjet().getLibelle(), fournisseurNom, montantFormate, commande.getId());
                if (commande.getFactureUrl() != null) {
                    emailService.envoyerFactureDisponibleInvestisseur(
                            destinataire, (investisseur.getPrenom() + " " + investisseur.getNom()).trim(),
                            commande.getProjet().getLibelle(), commande.getId());
                }
            }
        }
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

    public List<Commande> getALivrerAdmin() {
        return commandeRepository.findByStatutOrderByDateCommandeDesc(StatutCommande.LIVREE);
    }

    public List<Commande> getEnLitigeAdmin() {
        return commandeRepository.findByStatutOrderByDateCommandeDesc(StatutCommande.LITIGE);
    }

    private String fournisseurNomAffiche(Fournisseur f) {
        return f.getRaisonSociale() != null ? f.getRaisonSociale()
                : (f.getUser().getPrenom() + " " + f.getUser().getNom()).trim();
    }

    // Même repli que ContactService/FichePorteurController : certains comptes
    // n'ont qu'un login au format email, sans email renseigné explicitement.
    private String resolveEmail(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getLogin() != null && user.getLogin().contains("@")) {
            return user.getLogin();
        }
        return null;
    }

    private List<User> getInvestisseursValides(Projet projet) {
        return investissementRepository
                .findByProjetIdAndStatutPartInvestissement(projet.getId(), StatutPartInvestissement.VALIDE)
                .stream()
                .map(Investissement::getInvestisseur)
                .distinct()
                .toList();
    }

    // ── Consultation de la facture (porteur, fournisseur, investisseurs, admin) ─
    public boolean peutConsulterFacture(Commande commande, User user) {
        if (commande.getProjet().getPorteur() != null
                && commande.getProjet().getPorteur().getId().equals(user.getId())) {
            return true;
        }
        if (commande.getFournisseur().getUser().getId().equals(user.getId())) {
            return true;
        }
        return getInvestisseursValides(commande.getProjet()).stream()
                .anyMatch(investisseur -> investisseur.getId().equals(user.getId()));
    }

    public Commande getCommandeAvecAutorisation(Long id, User user, boolean estAdmin) {
        Commande commande = getOrThrow(id);
        if (!estAdmin && !peutConsulterFacture(commande, user)) {
            throw new SecurityException("Vous n'avez pas accès à cette commande.");
        }
        return commande;
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

        User porteur = c.getProjet().getPorteur();
        User fournisseurUser = c.getFournisseur().getUser();

        return new CommandeDTO(
                c.getId(),
                c.getProjet().getId(),
                c.getProjet().getLibelle(),
                porteur != null ? (porteur.getPrenom() + " " + porteur.getNom()).trim() : null,
                porteur != null ? resolveEmail(porteur) : null,
                c.getFournisseur().getId(),
                fournisseurNomAffiche(c.getFournisseur()),
                c.getFournisseur().getVille(),
                c.getFournisseur().getPays(),
                c.getFournisseur().getTelephone() != null ? c.getFournisseur().getTelephone()
                        : fournisseurUser.getContact(),
                c.getFournisseur().getEmail() != null ? c.getFournisseur().getEmail() : resolveEmail(fournisseurUser),
                c.getMontantTotal(),
                c.getStatut().name(),
                c.getDateCommande(),
                c.getDateValidationAdmin(),
                c.getDateAcceptation(),
                c.getDateExpedition(),
                c.getDateConfirmationReception(),
                c.getDatePaiement(),
                c.getMotifRejet(),
                c.getMotifRefus(),
                c.getMotifLitige(),
                c.getFactureUrl(),
                lignes);
    }
}
