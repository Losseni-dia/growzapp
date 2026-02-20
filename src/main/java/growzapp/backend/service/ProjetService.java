package growzapp.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Localisation;
import growzapp.backend.model.entite.Localite;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.Secteur;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.notification.service.NotificationService;
import growzapp.backend.repository.LocalisationRepository;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.SecteurRepository;
import growzapp.backend.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final FileUploadService fileUploadService;
    private final LocalisationRepository localisationRepository;
    private final LocaliteRepository localiteRepository;
    private final SecteurRepository secteurRepository;
    private final DtoConverter converter;
    private final WalletRepository walletRepository;
    private final NotificationService notificationService;


    // ========================
    // GETTERS
    // ========================

    public List<ProjetDTO> getAll() {
        return projetRepository.findByStatutProjet(StatutProjet.VALIDE)
                .stream()
                .map(converter::toProjetDto)
                .toList();
    }

    public ProjetDTO getById(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return converter.toProjetDto(projet);
    }

    // Dans ProjetService.java

    // Garde ta méthode getById actuelle si d'autres classes l'utilisent,
    // mais ajoute celle-ci pour le Controller :
    public Projet getEntityById(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));
    }

    public List<ProjetDTO> getAllAdmin(String search) {
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            return projetRepository.findBySearchTerm(like)
                    .stream()
                    .map(converter::toProjetDto)
                    .toList();
        }
        return projetRepository.findAll()
                .stream()
                .map(converter::toProjetDto)
                .toList();
    }

    public List<ProjetDTO> getByPorteurId(Long porteurId) {
        return projetRepository.findByPorteurId(porteurId).stream()
                .map(converter::toProjetDto)
                .toList();
    }

    public ProjetDTO getProjetWithInvestissements(Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));
        return converter.toProjetDto(projet);
    }

    // ========================
    // CREATE (depuis admin ou porteur)
    // ========================

    @Transactional
    public ProjetDTO save(ProjetDTO dto, MultipartFile[] files) {
        Projet projet = new Projet();

        // Mise à jour des champs (même logique que l'update)
        converter.updateProjetFromDto(dto, projet);

        // Gestion du poster
        if (files != null && files.length > 0 && !files[0].isEmpty()) {
            String posterUrl = fileUploadService.uploadPoster(files[0], null); // null car pas encore d'ID
            projet.setPoster(posterUrl);
        }

        // Valeurs par défaut pour création
        projet.setCreatedAt(LocalDateTime.now());
        projet.setStatutProjet(StatutProjet.SOUMIS);
        projet.setPartsPrises(0);
        // Au lieu de : projet.setMontantCollecte(0.0);
        projet.setMontantCollecte(BigDecimal.ZERO);

        Projet saved = projetRepository.save(projet);

        // Mise à jour du poster avec l'ID réel si uploadé
        if (files != null && files.length > 0 && !files[0].isEmpty()) {
            String finalPosterUrl = fileUploadService.uploadPoster(files[0], saved.getId());
            saved.setPoster(finalPosterUrl);
            projetRepository.save(saved);
        }

        return converter.toProjetDto(saved);
    }

    // Surcharge sans fichier (REST JSON pur)
    @Transactional
    public ProjetDTO save(ProjetDTO dto) {
        return save(dto, null);
    }

    @Transactional
    public ProjetDTO createFromCreateDto(ProjetCreateDTO dto, User currentUser) {

        // === Secteur (créé si inexistant) ===
        Secteur secteur = secteurRepository
                .findByNomIgnoreCase(dto.secteurNom().trim())
                .orElseGet(() -> {
                    Secteur nouveau = new Secteur();
                    nouveau.setNom(dto.secteurNom().trim());
                    return nouveau; // ← on ne sauvegarde PAS ici
                });

        // === Localité (créé si inexistant) ===
        Localite localite = localiteRepository
                .findByNomIgnoreCase(dto.localiteNom().trim())
                .orElseGet(() -> {
                    Localite nouvelle = new Localite();
                    nouvelle.setNom(dto.localiteNom().trim());
                    nouvelle.setCodePostal("00000");
                    return nouvelle; // ← on ne sauvegarde PAS ici
                });

        // === Site projet ===
        Localisation siteProjet = new Localisation();
        siteProjet.setNom("Site du projet : " + dto.libelle());
        siteProjet.setAdresse("À définir par l'admin");
        siteProjet.setContact(currentUser.getContact() != null ? currentUser.getContact() : "Non renseigné");
        siteProjet.setResponsable(currentUser.getPrenom() + " " + currentUser.getNom());
        siteProjet.setLocalite(localite);

        // Initialisation des champs Géo à null par défaut
        siteProjet.setLatitude(null);
        siteProjet.setLongitude(null);
        siteProjet.setWhat3words(null);

        siteProjet = localisationRepository.saveAndFlush(siteProjet);

        // === Projet ===
        Projet projet = new Projet();
        projet.setLibelle(dto.libelle());
        projet.setDescription(dto.description());
        projet.setStatutProjet(StatutProjet.SOUMIS);
        projet.setCreatedAt(LocalDateTime.now());
        projet.setPorteur(currentUser);
        projet.setSecteur(secteur);
        projet.setSiteProjet(siteProjet);
        // poster sera rempli dans le controller après l’upload

        // Tout est sauvegardé d’un coup grâce à @Transactional + cascade ou flush final
        Projet saved = projetRepository.save(projet);

        // === CRÉATION AUTOMATIQUE DU WALLET PROJET (LE MANQUE CRITIQUE) ===
        walletRepository.findByProjetId(saved.getId())
                .orElseGet(() -> {
                    Wallet walletProjet = Wallet.builder()
                            .walletType(WalletType.PROJET)
                            .projetId(saved.getId())
                            .user(null) // le porteur est lié, mais n'a pas accès
                            .soldeDisponible(BigDecimal.ZERO)
                            .soldeBloque(BigDecimal.ZERO)
                            .soldeRetirable(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(walletProjet);
                });

        // On sauvegarde secteur & localité uniquement s’ils sont nouveaux
        if (secteur.getId() == null) {
            secteur = secteurRepository.save(secteur);
        }
        if (localite.getId() == null) {
            localite = localiteRepository.save(localite);
        }

        return converter.toProjetDto(saved);
    }
    // ========================
    // UPDATE (LA MÉTHODE QUI MARCHE À 100%)
    // ========================

  @Transactional
public ProjetDTO updateProjetFromJson(Long id, JsonNode node) {
    Projet projet = projetRepository.findById(id).orElseThrow();

    if (node.has("libelle")) projet.setLibelle(node.get("libelle").asText());
    if (node.has("description")) projet.setDescription(node.get("description").asText());
    // --- NOUVEAU : Champs financiers (C'est ce qui manquait !) ---
    if (node.has("objectifFinancement"))
        projet.setObjectifFinancement(node.get("objectifFinancement").decimalValue());

    if (node.has("prixUnePart"))
        projet.setPrixUnePart(node.get("prixUnePart").decimalValue());

    if (node.has("partsDisponible"))
        projet.setPartsDisponible(node.get("partsDisponible").asInt());

    if (node.has("valeurTotalePartsEnPourcent")) {
        projet.setValeurTotalePartsEnPourcent(node.get("valeurTotalePartsEnPourcent").asDouble());
    }

    if (node.has("roiProjete"))
        projet.setRoiProjete(node.get("roiProjete").asDouble());

    if (node.has("statutProjet"))
        projet.setStatutProjet(StatutProjet.valueOf(node.get("statutProjet").asText()));

    // --- MISE À JOUR DES DATES ---
    if (node.has("dateDebut") && !node.get("dateDebut").isNull() && !node.get("dateDebut").asText().isEmpty()) {
        projet.setDateDebut(LocalDateTime.parse(node.get("dateDebut").asText().split("T")[0] + "T00:00:00"));
    }
    if (node.has("dateFin") && !node.get("dateFin").isNull() && !node.get("dateFin").asText().isEmpty()) {
        projet.setDateFin(LocalDateTime.parse(node.get("dateFin").asText().split("T")[0] + "T00:00:00"));
    }
    // --- Secteur ---
    if (node.has("secteurNom")) {
        Secteur secteur = secteurRepository.findByNomIgnoreCase(node.get("secteurNom").asText())
                .orElseGet(() -> secteurRepository.save(new Secteur(node.get("secteurNom").asText())));
        projet.setSecteur(secteur);
    }
    // MISE À JOUR GÉOLOCALISATION DU SITE
    if (node.has("siteGps") && projet.getSiteProjet() != null) {
        JsonNode gpsNode = node.get("siteGps");
        Localisation site = projet.getSiteProjet();

        if (gpsNode.has("latitude"))
            site.setLatitude(new BigDecimal(gpsNode.get("latitude").asText()));
        if (gpsNode.has("longitude"))
            site.setLongitude(new BigDecimal(gpsNode.get("longitude").asText()));
        if (gpsNode.has("what3words"))
            site.setWhat3words(gpsNode.get("what3words").asText());
        if (gpsNode.has("adresse"))
            site.setAdresse(gpsNode.get("adresse").asText());

        localisationRepository.save(site);
    }

    Projet saved = projetRepository.save(projet);
    return converter.toProjetDto(saved);
}

    // ========================
    // AUTRES
    // ========================

    @Transactional
    public void deleteById(Long id) {
        projetRepository.deleteById(id);
    }


    @Transactional
    public ProjetDTO changerStatut(Long id, StatutProjet nouveauStatut) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

        StatutProjet ancienStatut = projet.getStatutProjet();
        projet.setStatutProjet(nouveauStatut);

        // On sauvegarde d'abord !
        Projet saved = projetRepository.save(projet);

        // PROTECTION : On ne notifie QUE si on passe de n'importe quoi à VALIDE
        if (nouveauStatut == StatutProjet.VALIDE && ancienStatut != StatutProjet.VALIDE) {
            if (notificationService != null) {
                // On ajoute l'ID du projet sauvegardé comme 3ème argument
                notificationService.notifyAllUsers(
                        "🚀 Nouveau projet !",
                        "Le projet '" + saved.getLibelle() + "' est disponible.",
                        saved.getId() // <--- AJOUT : Permet la redirection au clic
                );
            }
        }
        return converter.toProjetDto(saved);
    }


    @PreAuthorize("hasRole('ADMIN')")
public Wallet getProjetWallet(Long projetId) {
    return walletRepository.findByProjetId(projetId)
            .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
}


/**
 * Recherche les projets validés dans un rayon donné autour d'un point GPS.
 * 
 * @param userLat Latitude de l'investisseur
 * @param userLon Longitude de l'investisseur
 * @param rayonKm Rayon de recherche (ex: 50 km)
 */
public List<ProjetDTO> findProjetsProches(double userLat, double userLon, double rayonKm) {
    return projetRepository.findByStatutProjet(StatutProjet.VALIDE)
            .stream()
            .filter(p -> p.getSiteProjet() != null &&
                    p.getSiteProjet().getLatitude() != null &&
                    p.getSiteProjet().getLongitude() != null)
            .filter(p -> {
                double distance = calculerDistanceKm(
                        userLat,
                        userLon,
                        p.getSiteProjet().getLatitude().doubleValue(),
                        p.getSiteProjet().getLongitude().doubleValue());
                return distance <= rayonKm;
            })
            .map(converter::toProjetDto)
            .toList();
}

/**
 * Formule Haversine pour le calcul de distance
 */
private double calculerDistanceKm(double lat1, double lon1, double lat2, double lon2) {
    double R = 6371; // Rayon moyen de la Terre en km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}





}