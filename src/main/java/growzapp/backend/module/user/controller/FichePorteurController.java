package growzapp.backend.module.user.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.FicheBioTraduction;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.FicheBioTraductionRepository;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import growzapp.backend.module.user.dto.FichePorteurAdminDTO;
import growzapp.backend.module.user.dto.FichePorteurPublicDTO;
import growzapp.backend.module.user.dto.FichePorteurSubmitDTO;
import growzapp.backend.module.user.dto.ProjetPrecedentDTO;
import growzapp.backend.module.user.enums.StatutFichePorteur;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * La fiche de présentation porteur est intégralement rédigée et validée par
 * l'admin (due diligence interne) — le porteur ne la remplit jamais
 * lui-même, il peut uniquement la consulter une fois créée.
 */
@RestController
@RequestMapping({ "/api/v1/porteur/fiche", "/api/porteur/fiche" })
@RequiredArgsConstructor
@Tag(name = "Fiche Porteur", description = "Fiche de présentation professionnelle du porteur de projet — rédigée et validée par l'admin, distincte du KYC (identité civile)")
public class FichePorteurController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileUploadService fileUploadService;
    private final ObjectMapper objectMapper;
    private final jakarta.validation.Validator validator;
    private final ProjetRepository projetRepository;
    private final ProjetTraductionRepository projetTraductionRepository;
    private final FicheBioTraductionRepository ficheBioTraductionRepository;
    private final DeepLTranslationService deepLTranslationService;

    // ── CONSULTER SA PROPRE FICHE (lecture seule, porteur) ──────────────────
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Consulter sa propre fiche de présentation et son statut (lecture seule)", tags = { "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getMaFiche(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        User user = userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(ApiResponseDTO.success(toMap(user, langue)));
    }

    // ── CONSULTER LA FICHE PUBLIQUE D'UN PORTEUR (page détail projet) ───────
    @GetMapping("/{porteurId}/publique")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Consulter la fiche publique d'un porteur (visible des investisseurs connectés)", description = "Ne retourne jamais de coordonnée de contact directe — GrowzApp reste le seul intermédiaire.", tags = {
            "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<FichePorteurPublicDTO>> getFichePublique(
            @PathVariable Long porteurId,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        User porteur = userRepository.findById(porteurId)
                .orElseThrow(() -> new RuntimeException("Porteur introuvable"));

        if (porteur.getFicheStatut() != StatutFichePorteur.VALIDEE) {
            return ResponseEntity.ok(ApiResponseDTO.error("Fiche non disponible"));
        }

        FichePorteurPublicDTO dto = new FichePorteurPublicDTO(
                porteur.getId(),
                (porteur.getPrenom() != null ? porteur.getPrenom() : "") + " "
                        + (porteur.getNom() != null ? porteur.getNom() : ""),
                photoAffichee(porteur),
                resolveBio(porteur, langue),
                porteur.getFicheStatutJuridique() != null ? porteur.getFicheStatutJuridique().name() : null,
                porteur.getFicheRaisonSociale(),
                porteur.getFicheAnneesExperience(),
                porteur.getFicheProjetsPrecedents(),
                resolveProjetsPrecedents(porteur, langue),
                true);

        return ResponseEntity.ok(ApiResponseDTO.success(dto));
    }

    // ── ADMIN : CRÉER OU MODIFIER LA FICHE D'UN PORTEUR ─────────────────────
    @PostMapping(value = "/admin/{userId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Créer ou modifier la fiche de présentation d'un porteur", description = "Seul l'admin rédige cette fiche (due diligence interne). statut=EN_ATTENTE (défaut) = enregistrement en brouillon interne, invisible des investisseurs. statut=VALIDEE = publie la fiche. statut=REJETEE = la marque refusée. La photo est facultative et distincte de l'avatar de compte du porteur.", tags = {
            "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<String>> creerOuModifier(
            @PathVariable Long userId,
            @RequestPart("fiche") String ficheJson,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @Parameter(description = "Statut à appliquer : EN_ATTENTE (défaut, brouillon non publié), VALIDEE (publie) ou REJETEE")
            @RequestParam(defaultValue = "EN_ATTENTE") StatutFichePorteur statut,
            @RequestParam(required = false) String commentaire) throws java.io.IOException {

        User porteur = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        FichePorteurSubmitDTO dto;
        try {
            dto = objectMapper.readValue(ficheJson, FichePorteurSubmitDTO.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Données de fiche invalides : " + e.getMessage()));
        }

        java.util.Set<jakarta.validation.ConstraintViolation<FichePorteurSubmitDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                    .collect(java.util.stream.Collectors.joining(", "));
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(errors));
        }

        boolean creation = porteur.getFicheStatut() == StatutFichePorteur.NON_SOUMISE;
        boolean etaitDejaValidee = porteur.getFicheStatut() == StatutFichePorteur.VALIDEE;

        porteur.setFicheBio(dto.bio());
        porteur.setFicheStatutJuridique(dto.statutJuridique());
        porteur.setFicheRaisonSociale(dto.raisonSociale());
        porteur.setFicheAnneesExperience(dto.anneesExperience());
        porteur.setFicheProjetsPrecedents(dto.projetsPrecedents());
        porteur.setFicheProjetsMisEnAvantIds(dto.projetsMisEnAvantIds() != null ? dto.projetsMisEnAvantIds() : new java.util.ArrayList<>());
        porteur.setFicheContactTelephone(dto.contactTelephone());
        porteur.setFicheContactEmail(dto.contactEmail());
        porteur.setFicheSiteWeb(dto.siteWeb());
        porteur.setFicheLinkedin(dto.linkedin());
        porteur.setFicheReseauxAutres(dto.reseauxAutres());
        if (photo != null && !photo.isEmpty()) {
            porteur.setFichePhotoUrl(fileUploadService.uploadFichePorteurPhoto(photo, userId));
        }
        porteur.setFicheStatut(statut);
        porteur.setFicheSubmittedAt(porteur.getFicheSubmittedAt() != null ? porteur.getFicheSubmittedAt() : LocalDateTime.now());
        porteur.setFicheValidatedAt(statut == StatutFichePorteur.EN_ATTENTE ? porteur.getFicheValidatedAt() : LocalDateTime.now());
        porteur.setFicheCommentaireRejet(statut == StatutFichePorteur.REJETEE ? commentaire : null);
        userRepository.save(porteur);
        deepLTranslationService.traduireFicheBio(porteur);

        String message;
        if (statut == StatutFichePorteur.VALIDEE) {
            notificationService.notifyUser(porteur,
                    creation ? "📋 Votre fiche de présentation a été créée !" : "📋 Votre fiche de présentation a été mise à jour",
                    "Votre fiche de présentation professionnelle est validée et publiée. Vous pouvez désormais soumettre vos projets pour financement.",
                    null, null);
            message = "Fiche publiée avec succès";
        } else if (statut == StatutFichePorteur.REJETEE) {
            notificationService.notifyUser(porteur, "❌ Fiche de présentation refusée",
                    "Votre fiche de présentation a été marquée comme non conforme"
                            + (commentaire != null && !commentaire.isBlank() ? " : " + commentaire : ".")
                            + " Contactez l'équipe GrowzApp pour plus de détails.",
                    null, null, commentaire);
            message = etaitDejaValidee ? "Fiche dépubliée et marquée non conforme" : "Fiche marquée non conforme";
        } else {
            // EN_ATTENTE : simple enregistrement interne, pas d'email/notif au
            // porteur — il n'est pas censé savoir qu'un brouillon existe.
            message = "Brouillon enregistré (non publié)";
        }

        return ResponseEntity.ok(ApiResponseDTO.<String>success(null).message(message));
    }

    // ── ADMIN : SUPPRIMER LA FICHE D'UN PORTEUR ─────────────────────────────
    @org.springframework.web.bind.annotation.DeleteMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Supprimer la fiche de présentation d'un porteur", description = "Efface toutes les informations et remet le statut à NON_SOUMISE — le porteur ne pourra plus soumettre de projet tant qu'une nouvelle fiche n'est pas recréée.", tags = {
            "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<String>> supprimer(@PathVariable Long userId) {
        User porteur = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        porteur.setFicheBio(null);
        porteur.setFicheStatutJuridique(null);
        porteur.setFicheRaisonSociale(null);
        porteur.setFicheAnneesExperience(null);
        porteur.setFicheProjetsPrecedents(null);
        porteur.setFicheProjetsMisEnAvantIds(new java.util.ArrayList<>());
        porteur.setFicheContactTelephone(null);
        porteur.setFicheContactEmail(null);
        porteur.setFicheSiteWeb(null);
        porteur.setFicheLinkedin(null);
        porteur.setFicheReseauxAutres(null);
        porteur.setFicheStatut(StatutFichePorteur.NON_SOUMISE);
        porteur.setFicheSubmittedAt(null);
        porteur.setFicheValidatedAt(null);
        porteur.setFicheCommentaireRejet(null);
        porteur.setFichePhotoUrl(null);
        userRepository.save(porteur);
        ficheBioTraductionRepository.deleteByUserId(userId);

        return ResponseEntity.ok(ApiResponseDTO.<String>success(null).message("Fiche supprimée"));
    }

    // ── ADMIN : CONSULTER LA FICHE (BRUTE, AVEC CONTACTS) D'UN PORTEUR ──────
    @GetMapping("/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Consulter la fiche complète (avec contacts internes) d'un porteur, pour édition", tags = {
            "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getFicheAdmin(@PathVariable Long userId) {
        User porteur = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(ApiResponseDTO.success(toMap(porteur, "fr")));
    }

    // ── ADMIN : RETRADUIRE LA BIO DE TOUTES LES FICHES (BACKFILL) ───────────
    @PostMapping("/admin/retraduire-bios")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Retraduit via DeepL la bio de toutes les fiches déjà soumises", description = "À exécuter une fois après le déploiement de la traduction automatique de la bio, pour les fiches créées avant cet ajout.", tags = {
            "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<String>> retraduireBios() {
        List<User> porteurs = userRepository
                .findFichesPorteurCreees(null, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        int count = deepLTranslationService.traduireToutesLesFichesBio(porteurs);
        return ResponseEntity.ok(ApiResponseDTO.<String>success(null)
                .message(count + " bio(s) retraduite(s)"));
    }

    // ── ADMIN : LISTER LES FICHES EXISTANTES ────────────────────────────────
    @GetMapping("/admin/liste")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Lister toutes les fiches porteur déjà créées (paginé)", tags = { "Fiche Porteur" })
    public ResponseEntity<ApiResponseDTO<Page<FichePorteurAdminDTO>>> getListe(
            @Parameter(description = "Recherche par nom, prénom ou email") @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "fr") String langue) {

        Pageable pageable = PageRequest.of(page, size);
        // Réutilise la requête existante en excluant NON_SOUMISE via une liste de
        // statuts n'a pas de query dédiée — plus simple : filtrer côté front ou
        // ajouter les statuts un à un n'apporterait rien ici, on liste tout ce qui
        // n'est pas NON_SOUMISE (fiches réellement créées par un admin).
        Page<FichePorteurAdminDTO> result = userRepository
                .findFichesPorteurCreees(search, pageable)
                .map(u -> toAdminDto(u, langue));
        return ResponseEntity.ok(ApiResponseDTO.success(result));
    }

    private Map<String, Object> toMap(User user, String langue) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("bio", resolveBio(user, langue));
        data.put("statutJuridique", user.getFicheStatutJuridique());
        data.put("raisonSociale", user.getFicheRaisonSociale());
        data.put("anneesExperience", user.getFicheAnneesExperience());
        data.put("projetsPrecedents", user.getFicheProjetsPrecedents());
        data.put("projetsMisEnAvantIds", user.getFicheProjetsMisEnAvantIds());
        data.put("projetsPrecedentsListe", resolveProjetsPrecedents(user, langue));
        // Contact déjà saisi sur la fiche, sinon on retombe sur les
        // coordonnées du compte (inscription) pour éviter à l'admin de
        // ressaisir une info déjà connue.
        data.put("contactTelephone",
                (user.getFicheContactTelephone() != null && !user.getFicheContactTelephone().isBlank())
                        ? user.getFicheContactTelephone()
                        : user.getContact());
        data.put("contactEmail",
                (user.getFicheContactEmail() != null && !user.getFicheContactEmail().isBlank())
                        ? user.getFicheContactEmail()
                        : (user.getEmail() != null && !user.getEmail().isBlank())
                                ? user.getEmail()
                                // Dernier repli : certains comptes (notamment les plus anciens)
                                // n'ont jamais eu de colonne "email" renseignée séparément,
                                // seulement un login au format adresse mail.
                                : (user.getLogin() != null && user.getLogin().contains("@"))
                                        ? user.getLogin()
                                        : null);
        data.put("siteWeb", user.getFicheSiteWeb());
        data.put("linkedin", user.getFicheLinkedin());
        data.put("reseauxAutres", user.getFicheReseauxAutres());
        data.put("ficheStatut", user.getFicheStatut());
        data.put("commentaireRejet", user.getFicheCommentaireRejet());
        data.put("photoUrl", photoAffichee(user));
        return data;
    }

    // Photo dédiée à la fiche si l'admin en a choisi une, sinon repli sur
    // l'avatar de compte du porteur (mieux que rien, mais toujours
    // remplaçable en éditant la fiche).
    private String photoAffichee(User user) {
        return (user.getFichePhotoUrl() != null && !user.getFichePhotoUrl().isBlank())
                ? user.getFichePhotoUrl()
                : user.getImage();
    }

    // Bio traduite (DeepL) selon la langue demandée — repli sur le texte
    // français si aucune traduction n'existe encore (ex : fiche créée avant
    // l'ajout de cette fonctionnalité, en attendant le prochain enregistrement).
    private String resolveBio(User porteur, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return porteur.getFicheBio();
        return ficheBioTraductionRepository.findByUserIdAndLangue(porteur.getId(), langue)
                .map(FicheBioTraduction::getBio)
                .filter(b -> b != null && !b.isBlank())
                .orElse(porteur.getFicheBio());
    }

    // Résout les ids de projets mis en avant en libellés déjà traduits (comme
    // pour les projets du catalogue) et statuts bruts — le statut est traduit
    // côté frontend via i18n (mêmes clés que la liste des projets), pas ici,
    // pour rester cohérent avec le reste de l'appli.
    private List<ProjetPrecedentDTO> resolveProjetsPrecedents(User porteur, String langue) {
        List<Long> ids = porteur.getFicheProjetsMisEnAvantIds();
        if (ids == null || ids.isEmpty())
            return List.of();
        List<Projet> projets = projetRepository.findAllById(ids);
        return projets.stream()
                .map(p -> {
                    String libelle = p.getLibelle();
                    if (langue != null && !langue.isBlank() && !langue.equals("fr")) {
                        Optional<ProjetTraductionProjection> traduction = projetTraductionRepository
                                .findProjectionByProjetIdAndLangue(p.getId(), langue);
                        if (traduction.isPresent() && traduction.get().getLibelle() != null
                                && !traduction.get().getLibelle().isBlank()) {
                            libelle = traduction.get().getLibelle();
                        }
                    }
                    int pct = p.getObjectifFinancement() != null
                            && p.getObjectifFinancement().compareTo(java.math.BigDecimal.ZERO) > 0
                                    ? p.getMontantCollecte().multiply(java.math.BigDecimal.valueOf(100))
                                            .divide(p.getObjectifFinancement(), 0, java.math.RoundingMode.HALF_UP)
                                            .intValue()
                                    : 0;
                    return new ProjetPrecedentDTO(p.getId(), libelle,
                            p.getStatutProjet() != null ? p.getStatutProjet().name() : null, pct);
                })
                .collect(Collectors.toList());
    }

    private FichePorteurAdminDTO toAdminDto(User u, String langue) {
        return new FichePorteurAdminDTO(
                u.getId(), u.getNom(), u.getPrenom(), u.getLogin(), u.getEmail(), photoAffichee(u),
                resolveBio(u, langue),
                u.getFicheStatutJuridique() != null ? u.getFicheStatutJuridique().name() : null,
                u.getFicheRaisonSociale(), u.getFicheAnneesExperience(), u.getFicheProjetsPrecedents(),
                u.getFicheProjetsMisEnAvantIds(),
                u.getFicheContactTelephone(), u.getFicheContactEmail(), u.getFicheSiteWeb(), u.getFicheLinkedin(),
                u.getFicheReseauxAutres(),
                u.getFicheStatut() != null ? u.getFicheStatut().name() : null,
                u.getFicheSubmittedAt(),
                u.getKycStatus() != null ? u.getKycStatus().name() : null);
    }
}
