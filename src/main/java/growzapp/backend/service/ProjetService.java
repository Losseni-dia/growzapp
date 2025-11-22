package growzapp.backend.service;


import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.Sexe;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import growzapp.backend.repository.LocalisationRepository;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.repository.PaysRepository;
import growzapp.backend.repository.SecteurRepository;
import lombok.RequiredArgsConstructor;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final LocalisationRepository localisationRepository;
    private final LocaliteRepository localiteRepository;
    private final PaysRepository paysRepository;
    private final SecteurRepository secteurRepository;
    private final DtoConverter converter;
   

    
    public List<ProjetDTO> getAll() { // ← Cette méthode est utilisée par /api/projets (liste publique)
        return projetRepository.findByStatutProjet(StatutProjet.VALIDE) // ← SEULEMENT LES VALIDÉS !
                .stream()
                .map(converter::toProjetDto)
                .toList();
    }

    public ProjetDTO getById(Long id) {
        Projet projet = projetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return converter.toProjetDto(projet);
    }

    //Admin
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
   
    // === SANS UPLOAD (API REST) ===
    @Transactional
    public ProjetDTO save(ProjetDTO dto) {
        Projet projet = doSave(dto);
        return converter.toProjetDto(projet);
    }

    // === LOGIQUE COMMUNE (PORTEUR, SITE, SECTEUR, etc.) ===
    private Projet doSave(ProjetDTO dto) {
        Projet projet = converter.toProjetEntity(dto);

        // === PORTEUR (priorité à l'ID en édition admin) ===
        if (dto.porteurId() != null && dto.porteurId() > 0) {
            projet.setPorteur(userRepository.findById(dto.porteurId())
                    .orElseThrow(() -> new RuntimeException("Porteur non trouvé")));
        } else if (dto.porteurNom() != null && !dto.porteurNom().trim().isEmpty()) {
            // Cas création classique
            String[] parts = dto.porteurNom().trim().split("\\s+", 2);
            String prenom = parts.length > 1 ? parts[0] : "";
            String nom = parts.length > 1 ? parts[1] : parts[0];
            User porteur = userRepository.findByPrenomIgnoreCaseAndNomIgnoreCase(prenom, nom)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setPrenom(prenom);
                        newUser.setNom(nom);
                        newUser.setEmail(prenom.toLowerCase() + "." + nom.toLowerCase() + "@growzapp.com");
                        newUser.setLogin(prenom.toLowerCase() + "." + nom.toLowerCase());
                        newUser.setPassword("default123");
                        newUser.setSexe(Sexe.M);
                        return userRepository.save(newUser);
                    });
            projet.setPorteur(porteur);
        } else {
            throw new IllegalArgumentException("Le porteur est obligatoire");
        }

        // === SECTEUR (priorité à l'ID en édition) ===
        if (dto.secteurId() != null && dto.secteurId() > 0) {
            projet.setSecteur(secteurRepository.findById(dto.secteurId())
                    .orElseThrow(() -> new RuntimeException("Secteur non trouvé")));
        } else if (dto.secteurNom() != null && !dto.secteurNom().trim().isEmpty()) {
            projet.setSecteur(secteurRepository.findByNomIgnoreCase(dto.secteurNom().trim())
                    .orElseThrow(() -> new RuntimeException("Secteur non trouvé")));
        } else {
            throw new IllegalArgumentException("Le secteur est obligatoire");
        }

        // === SITE (priorité à l'ID en édition) ===
        if (dto.siteId() != null && dto.siteId() > 0) {
            projet.setSiteProjet(localisationRepository.findById(dto.siteId())
                    .orElseThrow(() -> new RuntimeException("Site non trouvé")));
        } else if (dto.siteNom() != null && !dto.siteNom().trim().isEmpty()) {
            Localisation newSite = new Localisation();
            newSite.setNom(dto.siteNom().trim());
            newSite.setAdresse("À définir");
            newSite.setContact("contact@defaut.com");
            newSite.setResponsable("À définir");
            projet.setSiteProjet(localisationRepository.save(newSite));
        }

        // === LOCALITÉ (si tu as une relation directe) ===
        if (dto.localiteId() != null && dto.localiteId() > 0) {
            // Si ton entité Projet a une relation directe avec Localite
            // projet.setLocalite(localiteRepository.findById(dto.localiteId()).orElse(null));
        }

        // === RÉFÉRENCE ===
        if (dto.reference() != null) {
            projet.setReference(dto.reference());
        }

       

        return projetRepository.save(projet);
    }

    @Transactional
    public ProjetDTO createFromCreateDto(ProjetCreateDTO createDto, MultipartFile[] files, User currentUser) {

        /// SECTEUR
Secteur secteur = secteurRepository.findByNomIgnoreCase(createDto.secteurNom().trim())
    .orElseGet(() -> {
        Secteur nouveau = new Secteur();
        nouveau.setNom(createDto.secteurNom().trim());
        return secteurRepository.saveAndFlush(nouveau); // maintenant ça marche
    });

// PAY

if (createDto.paysNom() != null && !createDto.paysNom().trim().isEmpty()) {


   Pays  pays = paysRepository.findByNomIgnoreCase(createDto.paysNom().trim())
        .orElseGet(() -> {
             Pays nouveau = new Pays();
            nouveau.setNom(createDto.paysNom().trim());
            return paysRepository.saveAndFlush(nouveau);
        });
}

        // 3. Localité
        Localite localite = localiteRepository.findByNomIgnoreCase(createDto.localiteNom().trim())
        
                .orElseGet(() -> {
                    Localite nouvelle = new Localite();
                    nouvelle.setNom(createDto.localiteNom().trim());
                    nouvelle.setCodePostal("00000");
                    return localiteRepository.saveAndFlush(nouvelle);
                });

        // 4. Site (Localisation) — C’ÉTAIT ÇA QUI MANQUAIT !
        Localisation siteProjet = new Localisation();
        siteProjet.setNom("Site du projet : " + createDto.libelle());
        siteProjet.setAdresse("À définir par l'admin");
        siteProjet.setContact(currentUser.getContact() != null ? currentUser.getContact() : "Non renseigné");
        siteProjet.setResponsable(currentUser.getPrenom() + " " + currentUser.getNom());
        siteProjet.setLocalite(localite);
        siteProjet = localisationRepository.saveAndFlush(siteProjet);

        // 5. Projet
        Projet projet = new Projet();
        projet.setLibelle(createDto.libelle());
        projet.setDescription(createDto.description());
        projet.setStatutProjet(StatutProjet.SOUMIS);
        projet.setCreatedAt(LocalDateTime.now());
        projet.setPorteur(currentUser);
        projet.setSecteur(secteur);
        projet.setSiteProjet(siteProjet); // ← LA LIGNE QUI CHANGE TOUT

        // Poster
        if (files != null && files.length > 0 && !files[0].isEmpty()) {
            String posterUrl = uploadFile(files[0]); // ta méthode d'upload existante
            projet.setPoster(posterUrl);
        }

        Projet savedProjet = projetRepository.save(projet);

        return converter.toProjetDto(savedProjet);
    }

  


    @Transactional
    public void deleteById(Long id) {
        projetRepository.deleteById(id);
    }

    public ProjetDTO getProjetWithInvestissements(Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

        return converter.toProjetDto(projet);
    }


// Méthode pour mes projets
// Dans ProjetService.java – MÉTHODE UNIQUE ET PARFAITE

@Transactional
public ProjetDTO save(ProjetDTO dto, MultipartFile[] files) {
    // 1. Gestion du poster si présent
    if (files != null && files.length > 0 && !files[0].isEmpty()) {
        String posterUrl = uploadFile(files[0]);
        dto = dto.withPoster(posterUrl); // CORRECT // Record immutable → on recrée avec nouveau poster
    }

    // 2. Sauvegarde logique commune
    Projet projet = doSave(dto);

    return converter.toProjetDto(projet);
}

// Méthode surcharge pour appel sans fichier (REST JSON pur)
@Transactional
public ProjetDTO saveProjetDTO(ProjetDTO dto) {
    return save(dto, null); // On appelle la méthode principale
}

// Upload simple (exemple local)
private String uploadFile(MultipartFile file) {
    try {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get("uploads/posters/" + fileName);
        Files.createDirectories(path.getParent());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/posters/" + fileName;
    } catch (Exception e) {
        throw new RuntimeException("Erreur upload poster");
    }
}

public List<ProjetDTO> getByPorteurId(Long porteurId) {
    return projetRepository.findByPorteurId(porteurId).stream()
            .map(converter::toProjetDto)
            .toList();
}


@Transactional
public ProjetDTO changerStatut(Long id, StatutProjet nouveauStatut) {
    Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + id));

    // Optionnel : tu peux bloquer certains changements (ex: un projet TERMINE ne
    // peut plus être modifié)
    if (projet.getStatutProjet() == StatutProjet.TERMINE) {
        throw new IllegalStateException("Un projet terminé ne peut plus être modifié");
    }

    projet.setStatutProjet(nouveauStatut); // ← Changement de statut
    Projet saved = projetRepository.save(projet);

    return converter.toProjetDto(saved);
}



}