package growzapp.backend.module.projet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.enums.TypeEvenementValorisation;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.LocalisationRepository;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Wallet;
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
    private final LocalisationRepository localisationRepository;
    private final LocaliteRepository localiteRepository;
    private final SecteurRepository secteurRepository;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final DeepLTranslationService deepLTranslationService;
    private final ProjetValorisationService projetValorisationService;
    private final InvestissementRepository investissementRepository;


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

    public Projet getBySlug(String slug) {
        return projetRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Slug introuvable : " + slug));
    }

    // ========================
    // CRÉATION ET MODIFICATION
    // ========================

    @Transactional
    public Projet create(Projet projet, String secteurNom, String localiteNom, User currentUser) {
        log.info("Traitement métier pour le nouveau projet : {}", projet.getLibelle());

        // 1. Gestion du Secteur (Récupération ou création)
        Secteur secteur = secteurRepository.findByNomIgnoreCase(secteurNom.trim())
                .orElseGet(() -> secteurRepository.save(new Secteur(secteurNom.trim())));

        // 2. Gestion de la Localité
        Localite localite = localiteRepository.findByNomIgnoreCase(localiteNom.trim())
                .orElseGet(() -> {
                    Localite l = new Localite();
                    l.setNom(localiteNom.trim());
                    l.setCodePostal("00000");
                    return localiteRepository.save(l);
                });

        // 3. Gestion du Site (Localisation)
        Localisation site = new Localisation();
        site.setNom("Site du projet : " + projet.getLibelle());
        site.setLocalite(localite);
        site.setResponsable(currentUser.getPrenom() + " " + currentUser.getNom());
        site.setContact(currentUser.getContact() != null ? currentUser.getContact() : "Non renseigné");
        site = localisationRepository.save(site);

        // 4. Finalisation du Projet
        projet.setPorteur(currentUser);
        projet.setSecteur(secteur);
        projet.setSiteProjet(site);
        projet.setStatutProjet(StatutProjet.SOUMIS);
        projet.setCreatedAt(LocalDateTime.now());
        projet.setPartsPrises(0);
        projet.setMontantCollecte(BigDecimal.ZERO);
        if (projet.getDureeMois() == null) {
            projet.setDureeMois(36); // Valeur par défaut
        }

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

    @Transactional
    public Projet update(Projet projet) {
        return projetRepository.save(projet);
    }

    @Transactional
    public void deleteById(Long id) {
        projetRepository.deleteById(id);
    }

    // ========================
    // LOGIQUE MÉTIER
    // ========================

    @Transactional
    public Projet changerStatut(Long id, StatutProjet nouveauStatut) {
        Projet projet = getById(id);
        StatutProjet ancienStatut = projet.getStatutProjet();
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

    public List<Projet> findProjetsProches(double lat, double lon, double rayonKm) {
        return projetRepository.findByStatutProjet(StatutProjet.VALIDE).stream()
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

@Transactional
public Projet updateFull(Long id, ProjetCreateDTO dto, MultipartFile poster) {
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
    projet.setStatutProjet(StatutProjet.valueOf(dto.statutProjet()));
    projet.setDateDebut(dto.dateDebut());
    projet.setDateFin(dto.dateFin());

    // 3. Gérer le poster s'il y en a un nouveau
    if (poster != null && !poster.isEmpty()) {
        // Utilise ton service d'upload existant
        String posterUrl = fileUploadService.uploadPoster(poster, id);
        projet.setPoster(posterUrl);
    }

    // 4. Sauvegarder les modifications
    return projetRepository.save(projet);
}
}