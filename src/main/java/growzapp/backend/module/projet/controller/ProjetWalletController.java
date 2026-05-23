package growzapp.backend.module.projet.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.dividende.dto.DividendeHistoriqueAdminDTO;
import growzapp.backend.module.dividende.dto.PayerDividendeGlobalRequest;
import growzapp.backend.module.dividende.service.DividendeService;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.dto.RetraitProjetRequest;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
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
@RequestMapping("/api/admin/projet-wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Trésorerie Projets", description = "Gestion de la trésorerie des wallets projet : consultation des soldes, versement au porteur, distribution des dividendes et rapports financiers")
public class ProjetWalletController {

    private final WalletRepository walletRepository;
    private final ProjetRepository projetRepository;
    private final DividendeService dividendeService;
    private final TransactionRepository transactionRepository;
    private final InvestissementService investissementService;
    private final WalletService walletService;

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

    @GetMapping("/list")
    @Operation(
        summary = "Liste de tous les wallets projet",
        description = "Retourne la liste complète des wallets de type PROJET avec leurs soldes.",
        tags = {"Admin - Trésorerie Projets"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des wallets projet",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<Wallet>> getAllProjectWallets() {
        List<Wallet> wallets = walletRepository.findByWalletType(WalletType.PROJET);
        return ResponseEntity.ok(wallets);
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

    @PostMapping("/{projetId}/verser-porteur")
    @Transactional
    @Operation(
        summary = "Virer des fonds vers le porteur de projet",
        description = "Transfère un montant depuis le wallet du projet vers le solde disponible du porteur (propriétaire du projet). Une transaction de type VERSEMENT_PORTEUR est enregistrée.",
        tags = {"Admin - Trésorerie Projets"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Montant et motif du versement",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"montant\": 5000.00, \"motif\": \"Versement trimestriel T4 2025\"}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Versement effectué avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Montant invalide ou fonds insuffisants",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Wallet projet ou porteur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> verserAuPorteur(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @RequestBody Map<String, Object> body) {

        BigDecimal montant = new BigDecimal(body.get("montant").toString());
        String motif = (String) body.get("motif");

        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Le montant doit être supérieur à 0"));
        }

        Wallet walletProjet = walletRepository
                .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Fonds insuffisants dans le wallet projet"));
        }

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        User porteur = projet.getPorteur();
        Wallet walletPorteur = walletRepository.findByUserIdWithPessimisticLock(porteur.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet porteur introuvable"));

        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletPorteur.setSoldeDisponible(walletPorteur.getSoldeDisponible().add(montant));

        Transaction tx = Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.VERSEMENT_PORTEUR)
                .statut(StatutTransaction.SUCCESS)
                .description(motif != null && !motif.isBlank() ? motif : "Versement au porteur")
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        walletRepository.saveAll(List.of(walletProjet, walletPorteur));

        return ResponseEntity.ok(
                ApiResponseDTO.success("Versement effectué")
                        .message("Versement de " + montant.stripTrailingZeros().toPlainString()
                                + " € effectué avec succès"));
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
            walletService.retirerDuProjetWallet(
                    projetId,
                    request.montant(),
                    request.methode(),
                    request.phone());

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
            @PathVariable Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        Wallet wallet = walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        Map<String, Object> report = new HashMap<>();

        report.put("projetLibelle", projet.getLibelle());
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
