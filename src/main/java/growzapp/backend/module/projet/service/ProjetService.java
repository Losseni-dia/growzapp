package growzapp.backend.module.projet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.facture.service.FactureService;
import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.paiement.common.PaymentProviderRouter;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.enums.TypeEvenementValorisation;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetPhoto;
import growzapp.backend.module.projet.repository.ProjetPhotoRepository;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.model.Pays;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.LocalisationRepository;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.repository.PaysRepository;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.enums.SourcePaiement;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final ProjetPhotoRepository projetPhotoRepository;
    private final LocalisationRepository localisationRepository;
    private final LocaliteRepository localiteRepository;
    private final PaysRepository paysRepository;
    private final SecteurRepository secteurRepository;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final DeepLTranslationService deepLTranslationService;
    private final ProjetValorisationService projetValorisationService;
    private final InvestissementRepository investissementRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final PaymentProviderRouter paymentProviderRouter;
    private final FactureService factureService;

    // === STATUT PREMIUM ===
    public static final BigDecimal PRIX_PREMIUM_FCFA = BigDecimal.valueOf(5000);
    private static final int DUREE_PREMIUM_MOIS = 3;


    // ========================
    // LECTURE
    // ========================

    public List<Projet> getAllValid() {
        return projetRepository.findByStatutProjet(StatutProjet.VALIDE);
    }

    public List<Projet> getAllFinances() {
        return projetRepository.findByStatutProjet(StatutProjet.FINANCE);
    }

    public Projet getById(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projet introuvable (ID: " + id + ")"));
    }

    public List<Projet> getAllAdmin(String search) {
        if (search != null && !search.isBlank()) {
            return projetRepository.findBySearchTerm("%" + search.toLowerCase() + "%");
        }
        return projetRepository.findAll();
    }

    public List<Projet> getByPorteurId(Long porteurId) {
        return projetRepository.findByPorteurId(porteurId);
    }

    // Projets "présentables" d'un porteur — exclut brouillon, soumis (pas
    // encore validé par l'admin), rejeté et en attente : seuls les projets
    // réellement publiés (en financement ou déjà financés) doivent pouvoir
    // être mis en avant dans sa fiche de présentation.
    public List<Projet> getProjetsPubliesByPorteurId(Long porteurId) {
        return projetRepository.findByPorteurId(porteurId).stream()
                .filter(p -> STATUTS_PROCHES_VISIBLES.contains(p.getStatutProjet()))
                .toList();
    }

    // ========================
    // STATUT PREMIUM
    // ========================

    /**
     * Vérifie que le porteur peut acheter le Premium pour ce projet (c'est
     * bien le sien, et il est publié) — appelé avant toute initiation de
     * paiement (wallet, carte ou mobile money).
     */
    public Projet verifierAchatPremiumAutorise(Long projetId, User porteur) {
        Projet projet = getById(projetId);
        if (projet.getPorteur() == null || !projet.getPorteur().getId().equals(porteur.getId())) {
            throw new IllegalStateException("Vous n'êtes pas le porteur de ce projet.");
        }
        if (projet.getStatutProjet() != StatutProjet.VALIDE) {
            throw new IllegalStateException("Seul un projet publié (validé) peut devenir Premium.");
        }
        return projet;
    }

    @Transactional
    public void acheterPremiumWallet(Long projetId, User porteur) {
        Projet projet = verifierAchatPremiumAutorise(projetId, porteur);

        Wallet wallet = walletRepository.findByUserIdWithPessimisticLock(porteur.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));
        if (wallet.getSoldeDisponible().compareTo(PRIX_PREMIUM_FCFA) < 0) {
            throw new IllegalStateException(
                    "Solde insuffisant pour activer le Premium (5 000 FCFA requis).");
        }
        wallet.setSoldeDisponible(wallet.getSoldeDisponible().subtract(PRIX_PREMIUM_FCFA));
        walletRepository.save(wallet);

        activerPremium(projet, SourcePaiement.WALLET_GROWZAPP, wallet.getId());
    }

    /** Appelé par les webhooks de paiement (Stripe, FedaPay, PayDunya) après confirmation. */
    @Transactional
    public void activerPremiumExterne(Long projetId, SourcePaiement source) {
        Projet projet = getById(projetId);
        Wallet wallet = walletRepository.findByUserId(projet.getPorteur().getId())
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));
        activerPremium(projet, source, wallet.getId());
    }

    @Transactional
    public void revoquerPremium(Long projetId) {
        Projet projet = getById(projetId);
        projet.setPremiumFin(LocalDateTime.now());
        projetRepository.save(projet);
    }

    /**
     * Achats Premium restés bloqués en EN_ATTENTE_PAIEMENT — typiquement
     * parce que le webhook du fournisseur (FedaPay/PayDunya) n'a jamais pu
     * joindre le backend (tunnel ngrok fermé, dashboard mal configuré...).
     */
    public List<Transaction> getPremiumEnAttente() {
        return transactionRepository.findByReferenceTypeAndStatut(
                "PREMIUM_INITIATION", StatutTransaction.EN_ATTENTE_PAIEMENT);
    }

    /**
     * Interroge directement le fournisseur de paiement pour savoir si un
     * achat Premium resté en attente a réellement été payé — et active le
     * Premium (ou annule la trace) en conséquence. Retourne "CONFIRME" ou
     * "ANNULE".
     */
    @Transactional
    public String reconcilierPremiumEnAttente(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction introuvable"));
        return reconcilier(tx);
    }

    /**
     * Variante self-service : le porteur revient sur son projet après avoir
     * payé (redirection ?premium=success) et l'app vérifie tout de suite,
     * sans dépendre du webhook ni d'une action admin. Pas d'erreur si aucune
     * initiation n'est trouvée (retour normal si déjà régularisée entre
     * temps par le webhook).
     */
    @Transactional
    public String verifierPremiumEnAttentePourPorteur(Long projetId, User porteur) {
        List<Transaction> initiations = transactionRepository
                .findByReferenceTypeAndStatut("PREMIUM_INITIATION", StatutTransaction.EN_ATTENTE_PAIEMENT)
                .stream()
                .filter(tx -> tx.getReferenceId().equals(projetId))
                .toList();

        if (initiations.isEmpty()) {
            Projet projet = getById(projetId);
            return projet.isPremiumActif() ? "CONFIRME" : "AUCUNE_INITIATION";
        }

        Projet projet = getById(projetId);
        if (projet.getPorteur() == null || !projet.getPorteur().getId().equals(porteur.getId())) {
            throw new IllegalStateException("Vous n'êtes pas le porteur de ce projet.");
        }

        String resultat = "ANNULE";
        for (Transaction tx : initiations) {
            resultat = reconcilier(tx);
        }
        return resultat;
    }

    private String reconcilier(Transaction tx) {
        if (!"PREMIUM_INITIATION".equals(tx.getReferenceType())) {
            throw new IllegalStateException("Cette transaction n'est pas une initiation de paiement Premium.");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_PAIEMENT) {
            throw new IllegalStateException("Cette transaction a déjà été traitée.");
        }

        boolean paye = paymentProviderRouter.verifierPaiementReussi(tx.getReferenceExterne());
        transactionRepository.delete(tx);

        if (paye) {
            activerPremiumExterne(tx.getReferenceId(), tx.getSourcePaiement());
            return "CONFIRME";
        }
        return "ANNULE";
    }

    private void activerPremium(Projet projet, SourcePaiement source, Long walletId) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime base = projet.isPremiumActif() ? projet.getPremiumFin() : maintenant;
        if (!projet.isPremiumActif()) {
            projet.setPremiumDebut(maintenant);
        }
        projet.setPremiumFin(base.plusMonths(DUREE_PREMIUM_MOIS));
        projetRepository.save(projet);

        Transaction tx = Transaction.builder()
                .walletId(walletId)
                .walletType(WalletType.USER)
                .montant(PRIX_PREMIUM_FCFA)
                .type(TypeTransaction.PREMIUM_PROJET)
                .statut(StatutTransaction.SUCCESS)
                .description("Statut Premium activé — " + projet.getLibelle())
                .referenceType("PREMIUM_PROJET")
                .referenceId(projet.getId())
                .createdAt(maintenant)
                .completedAt(maintenant)
                .sourcePaiement(source)
                .build();
        transactionRepository.save(tx);

        notificationService.notifyUser(
                projet.getPorteur(),
                "⭐ Statut Premium activé",
                "Votre projet « " + projet.getLibelle() + " » est maintenant Premium jusqu'au "
                        + projet.getPremiumFin().toLocalDate() + ".",
                projet.getId(),
                projet.getSlug());

        // Reçu de paiement — génère la facture (PDF + notification dédiée avec
        // lien de téléchargement) et l'ajoute à l'espace "Mes factures" du porteur.
        // On passe des IDs, pas les entités : genererFacturePremium tourne en
        // tâche de fond (@Async) dans une session Hibernate différente.
        factureService.genererFacturePremium(
                projet.getPorteur().getId(), projet.getId(), PRIX_PREMIUM_FCFA.doubleValue(),
                source.name());
    }

    public Projet getBySlug(String slug) {
        return projetRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Slug introuvable : " + slug));
    }

    // ========================
    // CRÉATION ET MODIFICATION
    // ========================

    // Un porteur doit avoir son KYC VALIDÉ par un administrateur avant de
    // pouvoir soumettre un projet — même exigence que pour investir
    // (InvestissementService.investir). Un KYC simplement soumis
    // (EN_ATTENTE) ne suffit pas : tant que l'admin n'a pas validé, aucune
    // soumission de projet ni aucun investissement n'est possible.
    private void requireKycValide(User currentUser) {
        if (currentUser.getKycStatus() != KycStatus.VALIDE) {
            throw new IllegalStateException(
                    "Votre dossier KYC doit être validé par un administrateur avant de pouvoir soumettre un projet.");
        }
    }

    // La fiche de présentation du porteur (crédibilité professionnelle,
    // distincte du KYC) doit elle aussi être VALIDEE par un admin avant toute
    // soumission de projet.
    private void requireFichePorteurValidee(User currentUser) {
        if (currentUser.getFicheStatut() != growzapp.backend.module.user.enums.StatutFichePorteur.VALIDEE) {
            throw new IllegalStateException(
                    "Votre fiche de présentation porteur doit être validée par un administrateur avant de pouvoir soumettre un projet.");
        }
    }

    private Secteur resolveSecteur(String secteurNom) {
        return secteurRepository.findByNomIgnoreCase(secteurNom.trim())
                .orElseGet(() -> {
                    Secteur nouveau = secteurRepository.save(new Secteur(secteurNom.trim()));
                    try {
                        deepLTranslationService.traduireSecteur(nouveau);
                    } catch (Exception e) {
                        log.warn("Traduction automatique échouée pour le secteur '{}' : {}",
                                nouveau.getNom(), e.getMessage());
                    }
                    return nouveau;
                });
    }

    private Pays resolvePays(String paysNom) {
        return paysRepository.findByNomIgnoreCase(paysNom.trim())
                .orElseGet(() -> {
                    Pays p = new Pays();
                    p.setNom(paysNom.trim());
                    return paysRepository.save(p);
                });
    }

    private Localite resolveLocalite(String localiteNom, String paysNom) {
        Localite localite = localiteRepository.findByNomIgnoreCase(localiteNom.trim())
                .orElseGet(() -> {
                    Localite l = new Localite();
                    l.setNom(localiteNom.trim());
                    l.setCodePostal("00000");
                    return l;
                });

        // Rattache/corrige le pays si fourni — ce champ était jusqu'ici
        // accepté par le formulaire et le DTO mais jamais exploité ici,
        // laissant Localite.pays toujours NULL (donc paysNom toujours vide
        // côté frontend, et le filtre pays du catalogue toujours désert).
        if (paysNom != null && !paysNom.isBlank()) {
            localite.setPays(resolvePays(paysNom));
        }

        return localiteRepository.save(localite);
    }

    private Localisation resolveSite(Localite localite, String libelle, User currentUser) {
        Localisation site = new Localisation();
        site.setNom("Site du projet : " + libelle);
        site.setLocalite(localite);
        site.setResponsable(currentUser.getPrenom() + " " + currentUser.getNom());
        site.setContact(currentUser.getContact() != null ? currentUser.getContact() : "Non renseigné");
        return localisationRepository.save(site);
    }

    // La relation Projet.siteProjet est un @ManyToOne : rien n'empêche en base
    // que plusieurs projets partagent la même ligne Localisation (données
    // historiques, ex. plusieurs projets "Douala" créés avant que chaque
    // projet n'ait systématiquement son propre site). Muter cette ligne
    // partagée en place (changement de ville/coordonnées) répercutait alors
    // le changement sur tous les projets qui la partagent. On vérifie donc
    // avant toute mutation si le site est exclusif à ce projet ; sinon on le
    // clone pour que ce projet ait désormais sa propre ligne indépendante.
    private Localisation ensureSiteExclusif(Projet projet) {
        Localisation site = projet.getSiteProjet();
        boolean partage = projetRepository.findBySiteProjetId(site.getId()).stream()
                .anyMatch(p -> !p.getId().equals(projet.getId()));
        if (!partage) {
            return site;
        }
        Localisation copie = new Localisation();
        copie.setNom(site.getNom());
        copie.setLocalite(site.getLocalite());
        copie.setResponsable(site.getResponsable());
        copie.setContact(site.getContact());
        copie.setLatitude(site.getLatitude());
        copie.setLongitude(site.getLongitude());
        copie.setAdresse(site.getAdresse());
        Localisation saved = localisationRepository.save(copie);
        projet.setSiteProjet(saved);
        return saved;
    }

    @Transactional
    public Projet create(Projet projet, String secteurNom, String localiteNom, String paysNom, User currentUser) {
        log.info("Traitement métier pour le nouveau projet : {}", projet.getLibelle());

        requireKycValide(currentUser);
        requireFichePorteurValidee(currentUser);

        Secteur secteur = resolveSecteur(secteurNom);
        Localite localite = resolveLocalite(localiteNom, paysNom);
        Localisation site = resolveSite(localite, projet.getLibelle(), currentUser);

        // 4. Finalisation du Projet
        projet.setPorteur(currentUser);
        projet.setSecteur(secteur);
        projet.setSiteProjet(site);
        projet.setStatutProjet(StatutProjet.SOUMIS);
        projet.setCreatedAt(LocalDateTime.now());
        projet.setPartsPrises(0);
        projet.setMontantCollecte(BigDecimal.ZERO);
        // dureeMois laissé tel quel : null = durée indéterminée, choix
        // explicite du porteur, pas une valeur à défaulter.

        Projet saved = projetRepository.save(projet);

        projetValorisationService.enregistrerSnapshot(saved, TypeEvenementValorisation.CREATION, null);

        // 5. Initialisation du Wallet Projet
        initializeWallet(saved.getId());

        // 6. Traduction automatique via DeepL (EN + ES)
        try {
            deepLTranslationService.traduireProjet(saved);
        } catch (Exception e) {
            log.warn("Traduction automatique échouée pour le projet {} — le projet est quand même créé : {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    // ========================
    // BROUILLON
    // ========================
    // Un porteur peut enregistrer un formulaire de projet incomplet (statut
    // BROUILLON) le temps de rassembler toutes les informations, puis le
    // soumettre explicitement une fois prêt (soumettreBrouillon). Aucune
    // validation stricte n'est appliquée tant que le projet reste en
    // BROUILLON — c'est justement l'intérêt de ce statut.

    @Transactional
    public Projet createBrouillon(Projet projetPartiel, String secteurNom, String localiteNom, String paysNom,
            User currentUser) {
        if (secteurNom != null && !secteurNom.isBlank()) {
            projetPartiel.setSecteur(resolveSecteur(secteurNom));
        }
        if (localiteNom != null && !localiteNom.isBlank()) {
            Localite localite = resolveLocalite(localiteNom, paysNom);
            projetPartiel.setSiteProjet(resolveSite(localite, projetPartiel.getLibelle(), currentUser));
        }

        projetPartiel.setPorteur(currentUser);
        projetPartiel.setStatutProjet(StatutProjet.BROUILLON);
        projetPartiel.setCreatedAt(LocalDateTime.now());
        projetPartiel.setPartsPrises(0);
        projetPartiel.setMontantCollecte(BigDecimal.ZERO);

        return projetRepository.save(projetPartiel);
    }

    @Transactional
    public Projet updateBrouillon(Long id, Projet projetPartiel, String secteurNom, String localiteNom,
            String paysNom, User currentUser) {
        Projet existant = getById(id);
        if (existant.getPorteur() == null || !existant.getPorteur().getId().equals(currentUser.getId())) {
            throw new SecurityException("Ce brouillon ne vous appartient pas.");
        }
        if (existant.getStatutProjet() != StatutProjet.BROUILLON) {
            throw new IllegalStateException("Ce projet n'est plus au statut brouillon, il ne peut plus être modifié via cet endpoint.");
        }

        existant.setLibelle(projetPartiel.getLibelle());
        existant.setDescription(projetPartiel.getDescription());
        existant.setObjectifFinancement(projetPartiel.getObjectifFinancement());
        existant.setPrixUnePart(projetPartiel.getPrixUnePart());
        existant.setPartsDisponible(projetPartiel.getPartsDisponible());
        existant.setRoiProjete(projetPartiel.getRoiProjete());
        existant.setValuation(projetPartiel.getValuation());
        existant.setDureeMois(projetPartiel.getDureeMois());
        existant.setDateDebut(projetPartiel.getDateDebut());
        existant.setDateFin(projetPartiel.getDateFin());

        if (secteurNom != null && !secteurNom.isBlank()) {
            existant.setSecteur(resolveSecteur(secteurNom));
        }
        if (localiteNom != null && !localiteNom.isBlank()) {
            Localite localite = resolveLocalite(localiteNom, paysNom);
            if (existant.getSiteProjet() == null) {
                existant.setSiteProjet(resolveSite(localite, existant.getLibelle(), currentUser));
            } else {
                Localisation site = ensureSiteExclusif(existant);
                site.setLocalite(localite);
                site.setNom("Site du projet : " + existant.getLibelle());
                localisationRepository.save(site);
            }
        }

        return projetRepository.save(existant);
    }

    @Transactional
    public Projet soumettreBrouillon(Long id, User currentUser) {
        Projet projet = getById(id);
        if (projet.getPorteur() == null || !projet.getPorteur().getId().equals(currentUser.getId())) {
            throw new SecurityException("Ce brouillon ne vous appartient pas.");
        }
        if (projet.getStatutProjet() != StatutProjet.BROUILLON) {
            throw new IllegalStateException("Ce projet n'est pas au statut brouillon.");
        }

        requireKycValide(currentUser);
        requireFichePorteurValidee(currentUser);

        List<String> manquants = new java.util.ArrayList<>();
        if (projet.getLibelle() == null || projet.getLibelle().isBlank())
            manquants.add("titre du projet");
        if (projet.getDescription() == null || projet.getDescription().isBlank() || projet.getDescription().length() < 20)
            manquants.add("pitch (au moins 20 caractères)");
        if (projet.getSecteur() == null)
            manquants.add("secteur d'activité");
        if (projet.getSiteProjet() == null || projet.getSiteProjet().getLocalite() == null)
            manquants.add("ville / localité");
        if (projet.getObjectifFinancement() == null || projet.getObjectifFinancement().compareTo(BigDecimal.ZERO) <= 0)
            manquants.add("montant à lever");
        if (projet.getPrixUnePart() == null || projet.getPrixUnePart().compareTo(BigDecimal.ZERO) <= 0)
            manquants.add("prix d'une part");
        if (projet.getPartsDisponible() <= 0)
            manquants.add("nombre de parts");
        if (projet.getValuation() == null || projet.getValuation().compareTo(BigDecimal.ZERO) <= 0)
            manquants.add("valorisation totale");
        if (projet.getDateDebut() == null)
            manquants.add("date de début");
        if (projet.getDateFin() == null)
            manquants.add("date de fin");
        if (projet.getDateDebut() != null && projet.getDateFin() != null
                && projet.getDateFin().isBefore(projet.getDateDebut()))
            manquants.add("date de fin doit être après la date de début");

        if (!manquants.isEmpty()) {
            throw new IllegalStateException(
                    "Impossible de soumettre : champs manquants ou invalides — " + String.join(", ", manquants));
        }

        projet.setStatutProjet(StatutProjet.SOUMIS);
        Projet saved = projetRepository.save(projet);

        projetValorisationService.enregistrerSnapshot(saved, TypeEvenementValorisation.CREATION, null);
        initializeWallet(saved.getId());

        try {
            deepLTranslationService.traduireProjet(saved);
        } catch (Exception e) {
            log.warn("Traduction automatique échouée pour le projet {} — le projet est quand même soumis : {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public Projet update(Projet projet) {
        return projetRepository.save(projet);
    }

    @Transactional
    public void softDeleteById(Long id, String adminLogin, String motif) {
        if (investissementRepository.existsByProjetId(id)) {
            throw new IllegalStateException(
                    "Impossible de supprimer ce projet : il a déjà reçu au moins un investissement.");
        }
        Projet projet = getById(id);
        projet.setSupprimeLe(java.time.LocalDateTime.now());
        projet.setSupprimePar(adminLogin);
        projet.setMotifSuppression(motif);
        projetRepository.save(projet);
    }

    @Transactional
    public void restaurer(Long id) {
        Projet projet = getById(id);
        projet.setSupprimeLe(null);
        projet.setSupprimePar(null);
        projet.setMotifSuppression(null);
        projetRepository.save(projet);
    }

    @Transactional
    public void purger(Long id) {
        if (investissementRepository.existsByProjetId(id)) {
            throw new IllegalStateException(
                    "Impossible de supprimer ce projet : il a déjà reçu au moins un investissement.");
        }
        projetRepository.deleteById(id);
    }

    public List<Projet> getArchived() {
        return projetRepository.findArchived();
    }

    // ========================
    // LOGIQUE MÉTIER
    // ========================

    @Transactional
    public Projet changerStatut(Long id, StatutProjet nouveauStatut) {
        Projet projet = getById(id);
        StatutProjet ancienStatut = projet.getStatutProjet();

        // Aucun projet ne peut être VALIDÉ tant que la fiche de présentation
        // de son porteur n'est pas elle-même VALIDEE par un admin — même si
        // le porteur a réussi à soumettre le projet (ex: fiche validée puis
        // invalidée entretemps, ou données historiques).
        if (nouveauStatut == StatutProjet.VALIDE) {
            User porteur = projet.getPorteur();
            if (porteur == null
                    || porteur.getFicheStatut() != growzapp.backend.module.user.enums.StatutFichePorteur.VALIDEE) {
                throw new IllegalStateException(
                        "Impossible de valider ce projet : la fiche de présentation de son porteur n'est pas validée.");
            }
        }

        log.info("changerStatut : projet {} — {} → {}", id, ancienStatut, nouveauStatut);
        projet.setStatutProjet(nouveauStatut);

        Projet saved = projetRepository.save(projet);

        boolean estUneNouvelleValidation = nouveauStatut == StatutProjet.VALIDE && ancienStatut != StatutProjet.VALIDE;
        if (!estUneNouvelleValidation && nouveauStatut == StatutProjet.VALIDE) {
            log.warn("changerStatut : projet {} déjà VALIDE (ancien statut = {}) — diffusion ignorée volontairement",
                    id, ancienStatut);
        }

        if (estUneNouvelleValidation) {
            projetValorisationService.enregistrerSnapshot(saved, TypeEvenementValorisation.VALIDATION, null);

            if (saved.getPorteur() != null) {
                userService.attribuerRoleSiAbsent(saved.getPorteur().getId(), "PORTEUR");
            }

            notificationService.notifyAllUsersWithSlug(
                    "🚀 Nouveau projet disponible !",
                    "Le projet « " + saved.getLibelle() + " » vient d'être publié. Découvrez-le dès maintenant !",
                    saved.getId(),
                    saved.getSlug());
        }

        return saved;
    }

    private void initializeWallet(Long projetId) {
        walletRepository.findByProjetId(projetId).orElseGet(() -> {
            Wallet wallet = Wallet.builder()
                    .walletType(WalletType.PROJET)
                    .projetId(projetId)
                    .soldeDisponible(BigDecimal.ZERO)
                    .soldeBloque(BigDecimal.ZERO)
                    .build();
            return walletRepository.save(wallet);
        });
    }

    // "Près de moi" montre tout projet réellement publié (financement en
    // cours ou déjà financé) — contrairement au catalogue public qui ne
    // liste que les projets VALIDE (en cours de financement). On exclut
    // seulement les statuts non publics : brouillon, en attente de
    // validation admin, ou rejeté.
    private static final java.util.Set<StatutProjet> STATUTS_PROCHES_VISIBLES = java.util.Set.of(
            StatutProjet.VALIDE, StatutProjet.EN_COURS, StatutProjet.TERMINE, StatutProjet.FINANCE);

    public List<Projet> findProjetsProches(double lat, double lon, double rayonKm) {
        return projetRepository.findAll().stream()
                .filter(p -> STATUTS_PROCHES_VISIBLES.contains(p.getStatutProjet()))
                .filter(p -> p.getSiteProjet() != null && p.getSiteProjet().getLatitude() != null)
                .filter(p -> calculerDistance(lat, lon,
                        p.getSiteProjet().getLatitude().doubleValue(),
                        p.getSiteProjet().getLongitude().doubleValue()) <= rayonKm)
                .toList();
    }

    private double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Transactional
    public Projet revaloriser(Long projetId, java.math.BigDecimal nouvelleValorisation, String motif) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        java.math.BigDecimal ancienneValorisation = projet.getValuation() != null
                ? projet.getValuation()
                : java.math.BigDecimal.ZERO;

        java.math.BigDecimal delta = nouvelleValorisation.subtract(ancienneValorisation);

        projet.setValuation(nouvelleValorisation);
        Projet saved = projetRepository.save(projet);

        projetValorisationService.enregistrerSnapshot(
                saved,
                growzapp.backend.module.projet.enums.TypeEvenementValorisation.REEVALUATION,
                delta);

        log.info("Projet {} revalorisé : {} → {} (motif: {})", projetId, ancienneValorisation,
                nouvelleValorisation, motif);

        return saved;
    }

    /**
     * Recalcule montantCollecte et partsPrises d'un projet à partir de la
     * somme réelle de ses investissements VALIDE, et corrige le projet si
     * un écart est trouvé. Outil de diagnostic/réparation : ne touche
     * jamais au wallet (soldeDisponible/soldeBloque) — si l'écart persiste
     * après recalcul, l'argent en trop dans le wallet n'est adossé à aucun
     * investissement réel et doit être traité séparément, pas automatiquement.
     */
    @Transactional
    public java.util.Map<String, Object> recalculerMontantCollecte(Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        List<Investissement> investissementsValides = investissementRepository
                .findByProjetIdAndStatutPartInvestissement(projetId, StatutPartInvestissement.VALIDE);

        BigDecimal montantReel = investissementsValides.stream()
                .map(Investissement::getMontantInvesti)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int partsReelles = investissementsValides.stream()
                .mapToInt(Investissement::getNombrePartsPris)
                .sum();

        BigDecimal montantAvant = projet.getMontantCollecte() != null ? projet.getMontantCollecte()
                : BigDecimal.ZERO;
        int partsAvant = projet.getPartsPrises();

        boolean ecart = montantAvant.compareTo(montantReel) != 0 || partsAvant != partsReelles;

        if (ecart) {
            projet.setMontantCollecte(montantReel);
            projet.setPartsPrises(partsReelles);
            projetRepository.save(projet);
            log.warn("Projet {} : écart corrigé — montantCollecte {} → {}, partsPrises {} → {}",
                    projetId, montantAvant, montantReel, partsAvant, partsReelles);
        }

        java.util.Map<String, Object> resultat = new java.util.HashMap<>();
        resultat.put("ecartDetecte", ecart);
        resultat.put("montantCollecteAvant", montantAvant);
        resultat.put("montantCollecteApres", montantReel);
        resultat.put("partsPrisesAvant", partsAvant);
        resultat.put("partsPrisesApres", partsReelles);
        resultat.put("nombreInvestissementsValides", investissementsValides.size());
        return resultat;
    }

    // Dans ProjetService.java

public Projet updateFull(Long id, ProjetCreateDTO dto, MultipartFile poster) {
    return updateFull(id, dto, poster, null);
}

@Transactional
public Projet updateFull(Long id, ProjetCreateDTO dto, MultipartFile poster, List<MultipartFile> photos) {
    // 1. Récupérer le projet existant
    Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Projet introuvable avec l'ID : " + id));

    // 2. Mettre à jour les champs de base (Tu peux utiliser un Mapper ici aussi)
    projet.setLibelle(dto.libelle());
    projet.setDescription(dto.description());
    projet.setObjectifFinancement(dto.objectifFinancement());
    projet.setPrixUnePart(dto.prixUnePart());
    projet.setPartsDisponible(dto.partsDisponible());
    projet.setRoiProjete(dto.roiProjete());
    projet.setDureeMois(dto.dureeMois());
    projet.setValuation(dto.valuation());

    // Secteur : même logique find-or-create que create() — jusqu'ici ce
    // champ n'était même pas appliqué par updateFull(), le rendant muet
    // depuis l'écran d'édition malgré le champ affiché.
    if (dto.secteurNom() != null && !dto.secteurNom().isBlank()) {
        Secteur secteur = secteurRepository.findByNomIgnoreCase(dto.secteurNom().trim())
                .orElseGet(() -> secteurRepository.save(new Secteur(dto.secteurNom().trim())));
        projet.setSecteur(secteur);
    }

    // Ville/pays : même bug historique que le secteur ci-dessus — jamais
    // appliqués par updateFull(), donc impossible de corriger le pays des
    // projets déjà créés avant que ce champ ne soit exploité (voir
    // resolveLocalite/resolvePays).
    if (dto.localiteNom() != null && !dto.localiteNom().isBlank()) {
        Localite localite = resolveLocalite(dto.localiteNom(), dto.paysNom());
        if (projet.getSiteProjet() == null) {
            projet.setSiteProjet(resolveSite(localite, projet.getLibelle(), projet.getPorteur()));
        } else {
            Localisation site = ensureSiteExclusif(projet);
            site.setLocalite(localite);
            localisationRepository.save(site);
        }
    }

    // Le statut ne se change plus ici volontairement : changerStatut() est le
    // seul chemin qui déclenche les effets de bord attendus (snapshot de
    // valorisation, diffusion "nouveau projet" à tous les utilisateurs). Un
    // formulaire d'édition générique ne doit pas pouvoir déclencher une
    // notification à toute la plateforme comme simple effet de bord d'un
    // enregistrement de champs — voir le bug où la validation passait par ce
    // endpoint et ne notifiait jamais personne.

    // Date de début : jamais dans le passé — mais seulement si elle change
    // réellement. Un vieux projet déjà en cours a forcément une date de
    // début passée ; il ne faut pas empêcher de modifier ses autres champs
    // (ex: corriger la description) juste parce que cette date historique
    // n'est plus dans le futur.
    boolean dateDebutModifiee = dto.dateDebut() != null
            && !dto.dateDebut().equals(projet.getDateDebut());
    if (dateDebutModifiee && dto.dateDebut().isBefore(java.time.LocalDate.now())) {
        throw new IllegalArgumentException("La date de début ne peut pas être dans le passé");
    }
    projet.setDateDebut(dto.dateDebut());
    projet.setDateFin(dto.dateFin());

    // 3. Gérer le poster s'il y en a un nouveau
    if (poster != null && !poster.isEmpty()) {
        // Utilise ton service d'upload existant
        String posterUrl = fileUploadService.uploadPoster(poster, id);
        projet.setPoster(posterUrl);
    }

    // Coordonnées exactes du site — saisies manuellement par l'admin (la
    // carte de localisation des projets, catalogue public, en dépend).
    // Le site (Localisation) existe toujours à ce stade : il est créé
    // automatiquement à la création du projet (voir resolveSite).
    if (dto.latitude() != null && dto.longitude() != null && projet.getSiteProjet() != null) {
        Localisation site = ensureSiteExclusif(projet);
        site.setLatitude(dto.latitude());
        site.setLongitude(dto.longitude());
        if (dto.adresse() != null && !dto.adresse().isBlank()) {
            site.setAdresse(dto.adresse());
        }
        localisationRepository.save(site);
    }

    // 4. Sauvegarder les modifications
    Projet saved = projetRepository.save(projet);

    // 5. Photos additionnelles de galerie, en plus (jamais à la place) du
    // poster épinglé — chaque appel ajoute, il ne remplace jamais la
    // galerie existante (la suppression passe par supprimerPhoto()).
    if (photos != null && !photos.isEmpty()) {
        ajouterPhotos(saved.getId(), photos);
    }

    return saved;
}

// ── GALERIE DE PHOTOS ADDITIONNELLES ────────────────────────────────────
@Transactional
public List<ProjetPhoto> ajouterPhotos(Long projetId, List<MultipartFile> photos) {
    Projet projet = getById(projetId);
    List<ProjetPhoto> ajoutees = new java.util.ArrayList<>();
    for (MultipartFile photo : photos) {
        if (photo == null || photo.isEmpty()) continue;
        String url = fileUploadService.uploadProjetPhoto(photo, projetId);
        ProjetPhoto entity = new ProjetPhoto();
        entity.setProjet(projet);
        entity.setUrl(url);
        ajoutees.add(projetPhotoRepository.save(entity));
    }
    return ajoutees;
}

public List<ProjetPhoto> getPhotos(Long projetId) {
    return projetPhotoRepository.findByProjetIdOrderByCreatedAtAsc(projetId);
}

@Transactional
public void supprimerPhoto(Long projetId, Long photoId) {
    projetPhotoRepository.deleteByIdAndProjetId(photoId, projetId);
}
}