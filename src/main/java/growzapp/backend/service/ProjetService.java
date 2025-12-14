package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private final TransactionRepository transactionRepository;


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
                            .user(saved.getPorteur()) // le porteur est lié, mais n'a pas accès
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
    if (node.has("secteurNom")) {
        Secteur secteur = secteurRepository.findByNomIgnoreCase(node.get("secteurNom").asText())
                .orElseGet(() -> secteurRepository.save(new Secteur(node.get("secteurNom").asText())));
        projet.setSecteur(secteur);
    }
    // ... autres champs

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
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + id));

        if (projet.getStatutProjet() == StatutProjet.TERMINE) {
            throw new IllegalStateException("Un projet terminé ne peut plus être modifié");
        }

        projet.setStatutProjet(nouveauStatut);
        Projet saved = projetRepository.save(projet);
        return converter.toProjetDto(saved);
    }






    @PreAuthorize("hasRole('ADMIN')")
public Wallet getProjetWallet(Long projetId) {
    return walletRepository.findByProjetId(projetId)
            .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
}





}