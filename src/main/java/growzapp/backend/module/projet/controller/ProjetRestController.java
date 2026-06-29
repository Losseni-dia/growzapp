package growzapp.backend.module.projet.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
import growzapp.backend.module.paiement.paydunya.PayDunyaService;
import growzapp.backend.module.paiement.stripe.StripeDepositService;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/projets")
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
    private final PayDunyaService payDunyaService;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    // ── LISTE PUBLIQUE ────────────────────────────────────────────────────────
    @Operation(summary = "Lister les projets validés")
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic() {
        return ApiResponseDTO.success(projetMapper.toDtoList(projetService.getAllValid()));
    }

    // ── DÉTAIL PAR ID ─────────────────────────────────────────────────────────
    @Operation(summary = "Détail d'un projet par ID")
    @ApiResponse(responseCode = "200", description = "Projet trouvé")
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(projetMapper.toDto(projetService.getById(id)));
    }

    // ── CRÉATION ──────────────────────────────────────────────────────────────
    @Operation(summary = "Soumettre un nouveau projet", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);
        try {
            ProjetCreateDTO createDto = objectMapper.readValue(projetJson, ProjetCreateDTO.class);
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

    // ── MES PROJETS ───────────────────────────────────────────────────────────
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(Authentication auth) {
        return ApiResponseDTO.success(
                projetMapper.toDtoList(projetService.getByPorteurId(getCurrentUser(auth).getId())));
    }

    // ── GÉO-RECHERCHE ─────────────────────────────────────────────────────────
    @Operation(summary = "Recherche géographique")
    @GetMapping("/proche-de-moi")
    public ApiResponseDTO<List<ProjetDTO>> getProjetsProches(
            @RequestParam double lat, @RequestParam double lon,
            @RequestParam(defaultValue = "100") double rayon) {
        return ApiResponseDTO.success(
                projetMapper.toDtoList(projetService.findProjetsProches(lat, lon, rayon)));
    }

    // ── PAR SLUG ──────────────────────────────────────────────────────────────
    @Operation(summary = "Récupérer un projet par slug")
    @GetMapping("/slug/{slug}")
    public ApiResponseDTO<ProjetDTO> getBySlug(@PathVariable String slug) {
        return ApiResponseDTO.success(projetMapper.toDto(projetService.getBySlug(slug)));
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

            var response = payDunyaService.createInvestissementSession(
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
                    .referenceExterne(response.invoiceToken())
                    .referenceType("INVESTISSEMENT")
                    .referenceId(projetId)
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

    // ── HELPER ────────────────────────────────────────────────────────────────
    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}