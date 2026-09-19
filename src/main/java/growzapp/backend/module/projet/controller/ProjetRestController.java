package growzapp.backend.module.projet.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.module.files.FileUploadService;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.dto.InvestissementRequestDto;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.paiement.common.PaymentProviderRouter;
import growzapp.backend.module.paiement.stripe.StripeDepositService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.dto.ValorisationSnapshotDTO;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetValorisation;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.projet.repository.ProjetValorisationRepository;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraduction;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.traduction.DeepL.service.DeepLTranslationService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/v1/projets", "/api/projets"})
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Gestion du cycle de vie des investissements")
public class ProjetRestController {

    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final InvestissementService investissementService;
    private final ProjetMapper projetMapper;
    private final ObjectMapper objectMapper;
    private final StripeDepositService stripeDepositService;
    private final ProjetRepository projetRepository;
    private final PaymentProviderRouter paymentProviderRouter;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final Validator validator;
    private final ProjetTraductionRepository traductionRepository;
    private final DeepLTranslationService deepLTranslationService;
    private final ProjetValorisationRepository projetValorisationRepository;
    private final growzapp.backend.module.traduction.DeepL.repository.SecteurTraductionRepository secteurTraductionRepository;

    // ── LISTE PUBLIQUE ────────────────────────────────────────────────────────
    @Operation(summary = "Lister les projets validés")
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic(
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<ProjetDTO> dtos = projetMapper.toDtoList(projetService.getAllValid());
        return ApiResponseDTO.success(applyTraductions(dtos, langue));
    }

    // ── LISTE DES PROJETS FINANCÉS (vitrine) ────────────────────────────────
    @Operation(summary = "Lister les projets ayant atteint leur objectif de financement")
    @GetMapping("/finances")
    public ApiResponseDTO<List<ProjetDTO>> getAllFinances(
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<ProjetDTO> dtos = projetMapper.toDtoList(projetService.getAllFinances());
        return ApiResponseDTO.success(applyTraductions(dtos, langue));
    }

    // ── DÉTAIL PAR ID ─────────────────────────────────────────────────────────
    @Operation(summary = "Détail d'un projet par ID")
    @ApiResponse(responseCode = "200", description = "Projet trouvé")
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        ProjetDTO dto = projetMapper.toDto(projetService.getById(id));
        return ApiResponseDTO.success(applyTraduction(dto, langue));
    }

    // ── CRÉATION ──────────────────────────────────────────────────────────────
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);
        try {
            ProjetCreateDTO createDto = objectMapper.readValue(projetJson, ProjetCreateDTO.class);

            // ── VALIDATION MANUELLE ──────────────────────────────────────
            Set<ConstraintViolation<ProjetCreateDTO>> violations = validator.validate(createDto);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return ApiResponseDTO.error(errors);
            }

            // Vérification dates
            if (createDto.dateDebut() != null && createDto.dateFin() != null
                    && createDto.dateFin().isBefore(createDto.dateDebut())) {
                return ApiResponseDTO.error("La date de fin doit être après la date de début");
            }

            // Vérification cohérence parts/prix/objectif
            if (createDto.prixUnePart() != null && createDto.partsDisponible() > 0
                    && createDto.objectifFinancement() != null) {
                BigDecimal totalParts = createDto.prixUnePart()
                        .multiply(BigDecimal.valueOf(createDto.partsDisponible()));
                if (totalParts.compareTo(createDto.objectifFinancement()) != 0) {
                    return ApiResponseDTO.error(
                            "Incohérence : prix/part × nombre de parts doit être égal à l'objectif de financement");
                }
            }
            // ────────────────────────────────────────────────────────────

            Projet projetInitial = projetMapper.toEntity(createDto);
            Projet saved = projetService.create(projetInitial, createDto.secteurNom(),
                    createDto.localiteNom(), currentUser);

            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024)
                    return ApiResponseDTO.error("Le poster ne doit pas dépasser 10 Mo");
                String posterUrl = fileUploadService.uploadPoster(poster, saved.getId());
                saved.setPoster(posterUrl);
                saved = projetService.update(saved);
            }

            return ApiResponseDTO.success(projetMapper.toDto(saved))
                    .message("Projet soumis avec succès !");

        } catch (Exception e) {
            log.error("Erreur création projet", e);
            return ApiResponseDTO.error("Erreur : " + e.getMessage());
        }
    }

    // ── BROUILLON — ENREGISTRER ──────────────────────────────────────────────
    @Operation(summary = "Enregistrer un nouveau brouillon de projet", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping(value = "/brouillon", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> creerBrouillon(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);
        try {
            growzapp.backend.module.projet.dto.ProjetBrouillonDTO dto = objectMapper.readValue(projetJson,
                    growzapp.backend.module.projet.dto.ProjetBrouillonDTO.class);

            Set<ConstraintViolation<growzapp.backend.module.projet.dto.ProjetBrouillonDTO>> violations = validator
                    .validate(dto);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return ApiResponseDTO.error(errors);
            }

            Projet projetPartiel = projetMapper.toEntity(dto);
            Projet saved = projetService.createBrouillon(projetPartiel, dto.secteurNom(), dto.localiteNom(),
                    currentUser);

            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024)
                    return ApiResponseDTO.error("Le poster ne doit pas dépasser 10 Mo");
                String posterUrl = fileUploadService.uploadPoster(poster, saved.getId());
                saved.setPoster(posterUrl);
                saved = projetService.update(saved);
            }

            return ApiResponseDTO.success(projetMapper.toDto(saved))
                    .message("Brouillon enregistré");
        } catch (Exception e) {
            log.error("Erreur enregistrement brouillon", e);
            return ApiResponseDTO.error("Erreur : " + e.getMessage());
        }
    }

    // ── BROUILLON — METTRE À JOUR ─────────────────────────────────────────────
    @Operation(summary = "Mettre à jour un brouillon de projet existant", security = @SecurityRequirement(name = "BearerAuth"))
    @org.springframework.web.bind.annotation.PutMapping(value = "/brouillon/{id}", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> mettreAJourBrouillon(
            Authentication authentication,
            @PathVariable Long id,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);
        try {
            growzapp.backend.module.projet.dto.ProjetBrouillonDTO dto = objectMapper.readValue(projetJson,
                    growzapp.backend.module.projet.dto.ProjetBrouillonDTO.class);

            Set<ConstraintViolation<growzapp.backend.module.projet.dto.ProjetBrouillonDTO>> violations = validator
                    .validate(dto);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return ApiResponseDTO.error(errors);
            }

            Projet projetPartiel = projetMapper.toEntity(dto);
            Projet saved = projetService.updateBrouillon(id, projetPartiel, dto.secteurNom(), dto.localiteNom(),
                    currentUser);

            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024)
                    return ApiResponseDTO.error("Le poster ne doit pas dépasser 10 Mo");
                String posterUrl = fileUploadService.uploadPoster(poster, saved.getId());
                saved.setPoster(posterUrl);
                saved = projetService.update(saved);
            }

            return ApiResponseDTO.success(projetMapper.toDto(saved))
                    .message("Brouillon mis à jour");
        } catch (Exception e) {
            log.error("Erreur mise à jour brouillon", e);
            return ApiResponseDTO.error("Erreur : " + e.getMessage());
        }
    }

    // ── BROUILLON — SOUMETTRE ─────────────────────────────────────────────────
    @Operation(summary = "Soumettre un brouillon de projet pour validation admin", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/brouillon/{id}/soumettre")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> soumettreBrouillon(Authentication authentication, @PathVariable Long id) {
        User currentUser = getCurrentUser(authentication);
        try {
            Projet saved = projetService.soumettreBrouillon(id, currentUser);
            return ApiResponseDTO.success(projetMapper.toDto(saved))
                    .message("Projet soumis avec succès !");
        } catch (Exception e) {
            log.error("Erreur soumission brouillon", e);
            return ApiResponseDTO.error(e.getMessage());
        }
    }

    // ── MES PROJETS ───────────────────────────────────────────────────────────
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<ProjetDTO> dtos = projetMapper.toDtoList(
                projetService.getByPorteurId(getCurrentUser(auth).getId()));
        return ApiResponseDTO.success(applyTraductions(dtos, langue));
    }

    // ── GÉO-RECHERCHE ─────────────────────────────────────────────────────────
    @Operation(summary = "Recherche géographique")
    @GetMapping("/proche-de-moi")
    public ApiResponseDTO<List<ProjetDTO>> getProjetsProches(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "100") double rayon,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        List<ProjetDTO> dtos = projetMapper.toDtoList(
                projetService.findProjetsProches(lat, lon, rayon));
        return ApiResponseDTO.success(applyTraductions(dtos, langue));
    }

    // ── PAR SLUG ──────────────────────────────────────────────────────────────
    @Operation(summary = "Récupérer un projet par slug")
    @GetMapping("/slug/{slug}")
    public ApiResponseDTO<ProjetDTO> getBySlug(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        ProjetDTO dto = projetMapper.toDto(projetService.getBySlug(slug));
        return ApiResponseDTO.success(applyTraduction(dto, langue));
    }

    // ── HISTORIQUE DE VALORISATION ─────────────────────────────────────────────
    @Operation(summary = "Historique de valorisation d'un projet")
    @GetMapping("/{id}/historique-valorisation")
    public ApiResponseDTO<List<ValorisationSnapshotDTO>> getHistoriqueValorisation(
            @PathVariable Long id) {
        List<ProjetValorisation> historique = projetValorisationRepository
                .findByProjetIdOrderByDateSnapshotAsc(id);

        List<ValorisationSnapshotDTO> dtos = historique.stream()
                .map(v -> new ValorisationSnapshotDTO(
                        v.getDateSnapshot(),
                        v.getMontantValorisation(),
                        v.getMontantCollecte(),
                        v.getTypeEvenement(),
                        v.getMontantEvenement()))
                .collect(Collectors.toList());

        return ApiResponseDTO.success(dtos);
    }

    // ── INVESTIR — WALLET INTERNE ─────────────────────────────────────────────
    @Operation(summary = "Investir via wallet", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/investir")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<InvestissementDTO> investir(
            @PathVariable Long projetId,
            @RequestBody InvestissementRequestDto dto,
            Authentication auth) {
        User user = getCurrentUser(auth);
        return ApiResponseDTO.success(investissementService.investir(projetId, dto.nombrePartsPris(), user));
    }

    // ── INVESTIR — CARTE BANCAIRE (STRIPE) ───────────────────────────────────
    @Operation(summary = "Investir par carte via Stripe", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/investir-carte")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> investirCarte(
            @PathVariable Long projetId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            int nombreParts = Integer.parseInt(body.get("nombreParts").toString());
            if (nombreParts < 1)
                return ResponseEntity.badRequest().body(Map.of("error", "Nombre de parts invalide"));

            Projet projet = projetRepository.findById(projetId)
                    .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projetId));

            if (projet.getPartsDisponible() - projet.getPartsPrises() < nombreParts)
                return ResponseEntity.badRequest().body(Map.of("error", "Parts insuffisantes"));

            String redirectUrl = stripeDepositService.createInvestissementSession(
                    user.getId(), projetId, projet.getSlug(),
                    nombreParts, projet.getLibelle(), projet.getPrixUnePart());

            log.info("Stripe session créée user={} projet={} parts={}", user.getId(), projetId, nombreParts);
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur Stripe projet={}", projetId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── INVESTIR — MOBILE MONEY (PAYDUNYA) ───────────────────────────────────
    @Operation(summary = "Investir par Mobile Money via PayDunya", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/investir-mobile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> investirMobile(
            @PathVariable Long projetId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            int nombreParts = Integer.parseInt(body.get("nombreParts").toString());
            if (nombreParts < 1)
                return ResponseEntity.badRequest().body(Map.of("error", "Nombre de parts invalide"));

            Projet projet = projetRepository.findById(projetId)
                    .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projetId));

            if (projet.getPartsDisponible() - projet.getPartsPrises() < nombreParts)
                return ResponseEntity.badRequest().body(Map.of("error", "Parts insuffisantes"));

            BigDecimal montantFCFA = projet.getPrixUnePart()
                    .multiply(BigDecimal.valueOf(nombreParts));

            // Enregistrer une transaction EN_ATTENTE_PAIEMENT pour le webhook
            Wallet wallet = walletRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

            var response = paymentProviderRouter.creerSessionInvestissement(
                    montantFCFA, user.getId(), projetId,
                    nombreParts, projet.getLibelle(), projet.getSlug());

            // Sauvegarder transaction EN_ATTENTE_PAIEMENT avec invoiceToken
            Transaction tx = Transaction.builder()
                    .walletId(wallet.getId())
                    .walletType(WalletType.USER)
                    .montant(montantFCFA)
                    .type(TypeTransaction.INVESTISSEMENT)
                    .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                    .description("Investissement Mobile Money — " + projet.getLibelle())
                    .createdAt(LocalDateTime.now())
                    .referenceExterne(response.sessionToken())
                    .referenceType("INVESTISSEMENT_INITIATION")
                    .referenceId(projetId)
                    .sourcePaiement(growzapp.backend.module.wallet.enums.SourcePaiement.MOBILE_MONEY)
                    .build();
            transactionRepository.save(tx);

            log.info("PayDunya MM investissement initié user={} projet={} parts={} montant={}",
                    user.getId(), projetId, nombreParts, montantFCFA);

            return ResponseEntity.ok(Map.of("redirectUrl", response.redirectUrl()));

        } catch (Exception e) {
            log.error("Erreur Mobile Money projet={}", projetId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── STATUT PREMIUM — ACHAT PAR LE PORTEUR ───────────────────────────────────

    @Operation(summary = "Vérifier un paiement Premium en attente (retour de redirection)", description = "Appelé automatiquement quand le porteur revient sur son projet après paiement Mobile Money/Carte (?premium=success) — interroge directement le fournisseur au lieu d'attendre le webhook, pour un déblocage fiable même si le webhook n'a pas pu joindre le backend.", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/premium/verifier")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifierPremium(@PathVariable Long projetId, Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            String resultat = projetService.verifierPremiumEnAttentePourPorteur(projetId, user);
            return ResponseEntity.ok(Map.of("resultat", resultat));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Acheter le statut Premium via le wallet interne", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/premium/wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acheterPremiumWallet(@PathVariable Long projetId, Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            projetService.acheterPremiumWallet(projetId, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Statut Premium activé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Acheter le statut Premium par carte bancaire (Stripe)", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/premium/carte")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acheterPremiumCarte(@PathVariable Long projetId, Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            Projet projet = projetService.verifierAchatPremiumAutorise(projetId, user);

            String url = stripeDepositService.createPremiumSession(
                    user.getId(), projetId, projet.getSlug(), ProjetService.PRIX_PREMIUM_FCFA);

            return ResponseEntity.ok(Map.of("redirectUrl", url));
        } catch (Exception e) {
            log.error("Erreur achat Premium par carte, projet={}", projetId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Acheter le statut Premium par Mobile Money", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/premium/mobile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acheterPremiumMobile(@PathVariable Long projetId, Authentication auth) {
        try {
            User user = getCurrentUser(auth);
            Projet projet = projetService.verifierAchatPremiumAutorise(projetId, user);

            var response = paymentProviderRouter.creerSessionPremium(
                    ProjetService.PRIX_PREMIUM_FCFA, user.getId(), projetId, projet.getSlug());

            Transaction tx = Transaction.builder()
                    .walletId(walletRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new RuntimeException("Wallet introuvable")).getId())
                    .walletType(WalletType.USER)
                    .montant(ProjetService.PRIX_PREMIUM_FCFA)
                    .type(TypeTransaction.PREMIUM_PROJET)
                    .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                    .description("Achat Premium Mobile Money — " + projet.getLibelle())
                    .createdAt(LocalDateTime.now())
                    .referenceExterne(response.sessionToken())
                    .referenceType("PREMIUM_INITIATION")
                    .referenceId(projetId)
                    .sourcePaiement(growzapp.backend.module.wallet.enums.SourcePaiement.MOBILE_MONEY)
                    .build();
            transactionRepository.save(tx);

            return ResponseEntity.ok(Map.of("redirectUrl", response.redirectUrl()));
        } catch (Exception e) {
            log.error("Erreur achat Premium Mobile Money, projet={}", projetId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────────
    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ── Helper : appliquer traduction sur un ProjetDTO ───────────────────────
    private ProjetDTO applyTraduction(ProjetDTO dto, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return dto;

        Optional<ProjetTraductionProjection> traduction = traductionRepository
                .findProjectionByProjetIdAndLangue(dto.getId(), langue);

        traduction.ifPresent(t -> {
            if (t.getLibelle() != null && !t.getLibelle().isBlank())
                dto.setLibelleTradu(t.getLibelle());
            if (t.getDescription() != null && !t.getDescription().isBlank())
                dto.setDescriptionTradu(t.getDescription());
        });

        if (dto.getSecteurId() != null) {
            secteurTraductionRepository.findBySecteurIdAndLangue(dto.getSecteurId(), langue)
                    .ifPresent(st -> {
                        if (st.getNom() != null && !st.getNom().isBlank())
                            dto.setSecteurNomTradu(st.getNom());
                    });
        }

        return dto;
    }

    // ── Helper : appliquer traduction sur une liste ──────────────────────────
    private List<ProjetDTO> applyTraductions(List<ProjetDTO> dtos, String langue) {
        if (langue == null || langue.isBlank() || langue.equals("fr"))
            return dtos;
        return dtos.stream()
                .map(dto -> applyTraduction(dto, langue))
                .collect(Collectors.toList());
    }
}