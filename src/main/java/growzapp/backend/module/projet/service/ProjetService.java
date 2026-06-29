package growzapp.backend.module.projet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.repository.LocalisationRepository;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.repository.SecteurRepository;
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


    // ========================
    // LECTURE
    // ========================

    public List<Projet> getAllValid() {
        return projetRepository.findByStatutProjet(StatutProjet.VALIDE);
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

        // 5. Initialisation du Wallet Projet
        initializeWallet(saved.getId());

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
        projet.setStatutProjet(nouveauStatut);

        Projet saved = projetRepository.save(projet);

        if (nouveauStatut == StatutProjet.VALIDE && ancienStatut != StatutProjet.VALIDE) {
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
                    .soldeRetirable(BigDecimal.ZERO)
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