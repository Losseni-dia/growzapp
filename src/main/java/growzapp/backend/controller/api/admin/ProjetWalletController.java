// src/main/java/growzapp/backend/controller/api/admin/PwrojetWalletControler.java
// CONTRÔLEUR UNIQUE POUR TOUT CE QUI CONCERNE LES PROJETS EN MODE ADMIN
// VERSION FINALE – 27 NOVEMBRE 2025

package growzapp.backend.controller.api.admin;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeHistoriqueAdminDTO;
import growzapp.backend.model.dto.dividendeDTO.PayerDividendeGlobalRequest;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.walletDTOs.RetraitProjetRequest;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.DividendeService;
import growzapp.backend.service.InvestissementService;
import growzapp.backend.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/admin/projet-wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProjetWalletController {

    private final WalletRepository walletRepository;
    private final ProjetRepository projetRepository;
    private final DividendeService dividendeService;
    private final TransactionRepository transactionRepository;
    private final InvestissementService investissementService;
    private final WalletService walletService;

    // 1. Trésorerie réelle (argent vraiment séquestré dans les wallets PROJET)
    @GetMapping("/solde-total")
    public ResponseEntity<BigDecimal> getSoldeTotalProjets() {
        BigDecimal total = walletRepository.findByWalletType(WalletType.PROJET)
                .stream()
                .map(Wallet::getSoldeDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
    }

    // 2. Montant total affiché publiquement (somme des montantCollecte des projets)
    @GetMapping("/montant-total-collecte")
    public ResponseEntity<BigDecimal> getMontantTotalCollecteGlobal() {
            BigDecimal total = projetRepository.findAll()
                            .stream()
                            // On s'assure que le stream sait qu'il manipule des Projet
                            .map(projet -> projet.getMontantCollecte())
                            // On filtre les valeurs nulles pour éviter le NullPointerException
                            .filter(Objects::nonNull)
                            // On réduit (additionne) tout
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(total);
    }

    // 3. Liste complète des wallets projet (pour la page admin de gestion)
    @GetMapping("/list")
    public ResponseEntity<List<Wallet>> getAllProjectWallets() {
        List<Wallet> wallets = walletRepository.findByWalletType(WalletType.PROJET);
        return ResponseEntity.ok(wallets);
    }

    // 4. Détail d'un wallet projet spécifique
    @GetMapping("/{projetId}")
    public ResponseEntity<Wallet> getProjectWallet(@PathVariable Long projetId) {
        return walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 5. Solde réel d'un projet spécifique (pratique pour affichage rapide)
    @GetMapping("/{projetId}/solde")
    public ResponseEntity<BigDecimal> getProjectSolde(@PathVariable Long projetId) {
        BigDecimal solde = walletRepository.findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .map(Wallet::getSoldeDisponible)
                .orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(solde);
    }

   

    @PostMapping("/{projetId}/verser-porteur")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponseDTO<String>> verserAuPorteur(
            @PathVariable Long projetId,
            @RequestBody Map<String, Object> body) {

        BigDecimal montant = new BigDecimal(body.get("montant").toString());
        String motif = (String) body.get("motif");

        // Validation
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Le montant doit être supérieur à 0"));
        }

        // 1. Wallet projet
        Wallet walletProjet = walletRepository
                .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Fonds insuffisants dans le wallet projet"));
        }

        // 2. Porteur + wallet
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        User porteur = projet.getPorteur();
        Wallet walletPorteur = walletRepository.findByUserIdWithPessimisticLock(porteur.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet porteur introuvable"));

        // TRANSFERT DIRECT VERS SOLDE DISPONIBLE
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletPorteur.setSoldeDisponible(walletPorteur.getSoldeDisponible().add(montant)); // ← DIRECT DANS DISPONIBLE

        // Historique
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


  
        /**
         * Historique complet des investissements d’un projet
         * → Utilisé par la page admin "Détail trésorerie"
         */
        @GetMapping("/{projetId}/investissements")
        public ResponseEntity<List<InvestissementDTO>> getInvestissementsDuProjet(
                        @PathVariable Long projetId) {

                List<InvestissementDTO> investissements = investissementService
                                .getInvestissementsByProjetId(projetId);

                return ResponseEntity.ok(investissements);
        }
        
        @PostMapping("/{projetId}/payer-dividende")
        public ResponseEntity<ApiResponseDTO<String>> payerDividendesProrata(
                        @PathVariable Long projetId,
                        @Valid @RequestBody PayerDividendeGlobalRequest request) {

                log.info("=== REQUÊTE DISTRIBUTION REÇUE === projetId={}, montantTotal={}, motif={}, periode={}",
                                projetId, request.montantTotal(), request.motif(), request.periode());

                try {
                        dividendeService.payerDividendesProjetProrata(
                                        projetId,
                                        BigDecimal.valueOf(request.montantTotal()), // ← Conversion Double → BigDecimal
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
                        log.warn("Erreur métier lors de la distribution pour le projet {} : {}", projetId,
                                        e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponseDTO.error(e.getMessage()));

                } catch (Exception e) {
                        log.error("Erreur inattendue lors de la distribution des dividendes pour le projet {}",
                                        projetId, e);
                        return ResponseEntity.status(500)
                                        .body(ApiResponseDTO.error(
                                                        "Erreur serveur lors du paiement des dividendes. Veuillez réessayer."));
                }
        }

        /**
         * Historique complet des dividendes payés sur un projet
         * → Utilisé par la page admin "Détail trésorerie"
         */
        @GetMapping("/{projetId}/dividendes")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<DividendeHistoriqueAdminDTO>> getHistoriqueDividendes(@PathVariable Long projetId) {
        List<DividendeHistoriqueAdminDTO> historique = dividendeService.getHistoriqueDividendesAvecDetails(projetId);
        return ResponseEntity.ok(historique);
        }
        

        @PostMapping("/{projetId}/retirer")
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional
        public ResponseEntity<ApiResponseDTO<String>> retirerDuProjetWallet(
                @PathVariable Long projetId,
                @RequestBody RetraitProjetRequest request) {

        try {
                walletService.retirerDuProjetWallet(
                projetId,
                request.montant(),
                request.methode(),
                request.phone() // obligatoire pour Mobile Money
                );

                return ResponseEntity.ok(ApiResponseDTO.success("Retrait initié avec succès"));
        } catch (Exception e) {
                return ResponseEntity.badRequest()
                .body(ApiResponseDTO.error(e.getMessage()));
        }
        }


        
}

