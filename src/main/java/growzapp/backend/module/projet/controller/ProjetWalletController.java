package growzapp.backend.module.projet.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.dividende.dto.DividendeHistoriqueAdminDTO;
import growzapp.backend.module.dividende.dto.PayerDividendeGlobalRequest;
import growzapp.backend.module.dividende.service.DividendeService;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.dto.DeblocageProjetRequest;
import growzapp.backend.module.wallet.dto.RetraitProjetRequest;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import growzapp.backend.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin/projet-wallet", "/api/admin/projet-wallet"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Trésorerie Projets", description = "Gestion de la trésorerie des wallets projet : consultation des soldes, déblocage de trésorerie, distribution des dividendes et rapports financiers")
public class ProjetWalletController {

    private final WalletRepository walletRepository;
    private final ProjetRepository projetRepository;
    private final DividendeService dividendeService;
    private final TransactionRepository transactionRepository;
    private final InvestissementService investissementService;
    private final InvestissementRepository investissementRepository;
    private final WalletService walletService;
    private final ProjetTraductionRepository traductionRepository;
    private final UserService userService;

    @GetMapping("/solde-total")
    @Operation(
        summary = "Solde total de tous les wallets projet",
        description = "Retourne la somme des soldes disponibles de tous les wallets de type PROJET. Représente la trésorerie réelle séquestrée sur la plateforme.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solde total retourné",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "number", format = "double", example = "87500.00")))
    })
    public ResponseEntity<BigDecimal> getSoldeTotalProjets() {
        BigDecimal total = walletRepository.findByWalletType(WalletType.PROJET)
                .stream()
                .map(Wallet::getSoldeDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
    }

    @GetMapping("/montant-total-collecte")
    @Operation(
        summary = "Montant total collecté (affichage public)",
        description = "Retourne la somme des montants collectés affichés publiquement sur tous les projets. Peut différer de la trésorerie réelle (inclut les fonds déjà versés aux porteurs).",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Montant total collecté",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "number", format = "double", example = "125000.00")))
    })
    public ResponseEntity<BigDecimal> getMontantTotalCollecteGlobal() {
        BigDecimal total = projetRepository.findAll()
                .stream()
                .map(projet -> projet.getMontantCollecte())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(total);
    }

 // ═══════════════════════════════════════════════════════════════════
// PATCH ProjetWalletController.java — endpoint /list
// Ajoute la date du dernier investissement par projet, et trie les
// wallets du plus récent investissement au plus ancien.
// ═══════════════════════════════════════════════════════════════════

// AJOUTER cet import en haut du fichier :
// import growzapp.backend.module.investissement.model.Investissement;
// import java.util.Comparator;
// import java.util.stream.Collectors;

// REMPLACER la méthode getAllProjectWallets() par :

    @GetMapping("/list")
    @Operation(
        summary = "Liste de tous les wallets projet",
        description = "Retourne la liste complète des wallets de type PROJET avec leurs soldes, triée du projet ayant reçu l'investissement le plus récent au plus ancien.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des wallets projet triée par investissement récent",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<Map<String, Object>>> getAllProjectWallets() {
        List<Wallet> wallets = walletRepository.findByWalletType(WalletType.PROJET);

        List<Map<String, Object>> enriched = wallets.stream()
                .map(w -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", w.getId());
                    entry.put("projetId", w.getProjetId());
                    entry.put("soldeDisponible", w.getSoldeDisponible());
                    entry.put("soldeBloque", w.getSoldeBloque());
                    entry.put("walletType", w.getWalletType());

                    LocalDateTime dernierInvestissement = null;
                    if (w.getProjetId() != null) {
                        dernierInvestissement = investissementRepository
                                .findByProjetIdAndStatutPartInvestissement(
                                        w.getProjetId(), StatutPartInvestissement.VALIDE)
                                .stream()
                                .map(Investissement::getDate)
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);
                    }
                    entry.put("dernierInvestissement", dernierInvestissement);

                    return entry;
                })
                // Tri : projets avec investissement récent en premier,
                // puis ceux sans aucun investissement (null) à la fin.
                .sorted((a, b) -> {
                    LocalDateTime dateA = (LocalDateTime) a.get("dernierInvestissement");
                    LocalDateTime dateB = (LocalDateTime) b.get("dernierInvestissement");
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA); // plus récent d'abord
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(enriched);
    }

    @GetMapping("/{projetId}")
    @Operation(
        summary = "Wallet d'un projet",
        description = "Retourne le wallet associé à un projet spécifique.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Wallet du projet trouvé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Wallet introuvable pour ce projet",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<Wallet> getProjectWallet(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId) {
        return walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{projetId}/solde")
    @Operation(
        summary = "Solde disponible d'un projet",
        description = "Retourne uniquement le solde disponible du wallet d'un projet spécifique.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solde retourné (0 si wallet introuvable)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "number", format = "double", example = "12500.00")))
    })
    public ResponseEntity<BigDecimal> getProjectSolde(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId) {
        BigDecimal solde = walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .map(Wallet::getSoldeDisponible)
                .orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(solde);
    }


    @PostMapping("/{projetId}/debloquer")
    @Operation(
        summary = "Débloquer une partie de la trésorerie séquestrée d'un projet",
        description = "Transfère un montant du soldeBloque vers le soldeDisponible du wallet projet. Le porteur peut ensuite retirer ou transférer librement ce montant, sans validation admin supplémentaire à cette étape — le déblocage EST la validation. Peut être appelé plusieurs fois successivement tant que soldeBloque le permet.",
        tags = {"Admin - Trésorerie Projets"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Montant à débloquer et motif optionnel",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"montant\": 200.00, \"motif\": \"Avance sur travaux\"}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Déblocage effectué avec succès",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Montant invalide ou soldeBloque insuffisant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> debloquerTresorerie(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @Valid @RequestBody DeblocageProjetRequest request) {

        try {
            Long adminId = userService.getCurrentUser().getId();
            walletService.debloquerTresorerieProjet(projetId, request.montant(), request.motif(), adminId);
            return ResponseEntity.ok(
                    ApiResponseDTO.success("Déblocage effectué")
                            .message(request.montant().stripTrailingZeros().toPlainString()
                                    + " FCFA débloqués avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/{projetId}/investissements")
    @Operation(
        summary = "Investissements d'un projet",
        description = "Retourne la liste complète des investissements réalisés sur un projet spécifique.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des investissements du projet",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InvestissementDTO.class)))
    })
    public ResponseEntity<List<InvestissementDTO>> getInvestissementsDuProjet(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId) {
        List<InvestissementDTO> investissements = investissementService.getInvestissementsByProjetId(projetId);
        return ResponseEntity.ok(investissements);
    }

    @PostMapping("/{projetId}/payer-dividende")
    @Operation(
        summary = "Distribuer les dividendes au prorata",
        description = "Distribue un montant total de dividendes entre tous les investisseurs actifs du projet, au prorata de leur nombre de parts. Génère une facture par investisseur.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividendes distribués avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Erreur métier (aucun investisseur actif, solde insuffisant…)",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la distribution",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> payerDividendesProrata(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @Valid @RequestBody PayerDividendeGlobalRequest request) {

        log.info("=== REQUÊTE DISTRIBUTION REÇUE === projetId={}, montantTotal={}, motif={}, periode={}",
                projetId, request.montantTotal(), request.motif(), request.periode());

        try {
            dividendeService.payerDividendesProjetProrata(
                    projetId,
                    BigDecimal.valueOf(request.montantTotal()),
                    request.motif(),
                    request.periode());

            log.info("=== DISTRIBUTION TERMINÉE AVEC SUCCÈS === projetId={}, montant distribué={} €",
                    projetId, request.montantTotal());

            return ResponseEntity.ok(
                    ApiResponseDTO.success("Dividendes distribués avec succès")
                            .message(String.format(
                                    "Montant de %.2f € distribué au prorata des parts",
                                    request.montantTotal())));

        } catch (IllegalStateException e) {
            log.warn("Erreur métier lors de la distribution pour le projet {} : {}", projetId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Erreur inattendue lors de la distribution des dividendes pour le projet {}", projetId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponseDTO.error("Erreur serveur lors du paiement des dividendes. Veuillez réessayer."));
        }
    }

    @GetMapping("/{projetId}/dividendes")
    @Operation(
        summary = "Historique des dividendes d'un projet",
        description = "Retourne l'historique complet des dividendes distribués sur un projet, avec le détail par investisseur.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Historique des dividendes",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = DividendeHistoriqueAdminDTO.class)))
    })
    public ResponseEntity<List<DividendeHistoriqueAdminDTO>> getHistoriqueDividendes(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId) {
        List<DividendeHistoriqueAdminDTO> historique = dividendeService.getHistoriqueDividendesAvecDetails(projetId);
        return ResponseEntity.ok(historique);
    }

    @PostMapping("/{projetId}/retirer")
    @Transactional
    @Operation(
        summary = "Retrait depuis le wallet projet",
        description = "Initie un retrait depuis le wallet du projet vers un compte externe (Mobile Money ou Stripe). Réservé aux administrateurs.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrait initié avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Paramètres invalides ou solde insuffisant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> retirerDuProjetWallet(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @RequestBody RetraitProjetRequest request) {

        try {
            Long adminId = userService.getCurrentUser().getId();
            walletService.retirerDuProjetWallet(
                    projetId,
                    request.montant(),
                    request.methode(),
                    request.phone(),
                    adminId);

            return ResponseEntity.ok(ApiResponseDTO.success("Retrait initié avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/{projetId}/transactions")
    @Operation(
        summary = "Transactions d'un projet",
        description = "Retourne l'historique complet des transactions du wallet d'un projet (dépôts, versements, retraits).",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des transactions du projet",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Wallet introuvable pour ce projet",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<Transaction>> getProjectTransactions(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId) {
        Wallet wallet = walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        List<Transaction> transactions = transactionRepository.findByWalletTypeAndWalletId(
                WalletType.PROJET,
                wallet.getId());

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{projetId}/rapport-complet")
    @Operation(
        summary = "Rapport financier complet d'un projet",
        description = "Retourne un rapport synthétique incluant les montants collectés, la trésorerie réelle, le solde bloqué et les informations du porteur de projet.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rapport retourné",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"projetLibelle\": \"Ferme solaire\", \"montantCollectePublic\": 50000.00, \"tresorerieReelle\": 42000.00, \"soldeBloque\": 5000.00, \"porteurNom\": \"John Doe\", \"porteurContact\": \"+22670123456\"}"))),
        @ApiResponse(responseCode = "404", description = "Projet ou wallet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
  public ResponseEntity<Map<String, Object>> getFullFinanceReport(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @RequestParam(required = false, defaultValue = "fr") String langue) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        Wallet wallet = walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        Map<String, Object> report = new HashMap<>();

        report.put("projetLibelle", projet.getLibelle());

        if (langue != null && !langue.isBlank() && !langue.equals("fr")) {
            Optional<ProjetTraductionProjection> traduction = traductionRepository
                    .findProjectionByProjetIdAndLangue(projetId, langue);
            traduction.ifPresent(t -> {
                if (t.getLibelle() != null && !t.getLibelle().isBlank())
                    report.put("projetLibelleTradu", t.getLibelle());
            });
        }

        report.put("montantCollectePublic",
                projet.getMontantCollecte() != null ? projet.getMontantCollecte() : BigDecimal.ZERO);
        report.put("tresorerieReelle",
                wallet.getSoldeDisponible() != null ? wallet.getSoldeDisponible() : BigDecimal.ZERO);
        report.put("soldeBloque", wallet.getSoldeBloque() != null ? wallet.getSoldeBloque() : BigDecimal.ZERO);

        String nomComplet = "Non renseigné";
        String contact = "Pas de contact";

        if (projet.getPorteur() != null) {
            nomComplet = (projet.getPorteur().getPrenom() != null ? projet.getPorteur().getPrenom() : "")
                    + " " +
                    (projet.getPorteur().getNom() != null ? projet.getPorteur().getNom() : "");
            contact = projet.getPorteur().getContact() != null ? projet.getPorteur().getContact() : contact;
        }

        report.put("porteurNom", nomComplet.trim());
        report.put("porteurContact", contact);

        return ResponseEntity.ok(report);
    }

    
}
