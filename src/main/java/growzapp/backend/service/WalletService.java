// src/main/java/growzapp/backend/service/WalletService.java

package growzapp.backend.service;

import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.*;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.PayDunyaService.PayDunyaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ProjetRepository projetRepository;

    // CORRECTION D'INJECTION: Utilisation du service Payout Stripe existant
    private final StripePayoutService stripePayoutService;

    // CORRECTION D'INJECTION: Utilisation du service PayDunya unifié
    private final PayDunyaService payDunyaService;

    // ==================================================================
    // ================= DÉPÔT PAR MOBILE MONEY (CORRIGÉ) =================
    // ==================================================================
    /**
     * Initialise la session de dépôt Mobile Money via PayDunya.
     * Enregistre une transaction EN_ATTENTE_PAIEMENT et retourne l'URL de
     * redirection.
     * 
     * @return URL de redirection PayDunya
     */
   @Transactional
    public String initierDepotMobileMoney(Long userId, BigDecimal montant) { 
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        // On récupère le wallet pour obtenir son ID avec verrouillage
        Wallet wallet = getWalletWithLock(userId);

        // 1. Appel PayDunya pour obtenir l'URL et le Token (Réponse complète)
        PayDunyaResponse payDunyaRes = payDunyaService.createDepositCheckoutSession(montant, userId);
        // payDunyaRes contient : redirectUrl et invoiceToken

        // 2. On crée la Transaction avec le statut EN_ATTENTE_PAIEMENT (Trace)
        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Dépôt Mobile Money (redirection PayDunya)")
                .createdAt(LocalDateTime.now())
                // 3. ENREGISTREMENT DU TOKEN PAYDUNYA (Clé de la Réconciliation Webhook)
                .referenceExterne(payDunyaRes.invoiceToken()) 
                .build();

        // On sauvegarde la transaction
        transactionRepository.save(tx);

        // Retourne l'URL pour la redirection du Frontend
        return payDunyaRes.redirectUrl();
    }

    // ==================================================================
    // ======================= DÉPÔT DE FONDS (STRIPE/WEBHOOK) ==========
    // ==================================================================

    @Transactional
    public Wallet deposerFonds(Long userId, double montantDouble, String source) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletWithLock(userId);

        // Crédit
        wallet.crediterDisponible(montant);

        // Description intelligente
        String description = switch (source.toUpperCase()) {
            case "STRIPE_CARD" -> "Dépôt via carte bancaire (Stripe)";
            case "ORANGE_MONEY" -> "Dépôt via Orange Money";
            case "WAVE" -> "Dépôt via Wave";
            case "MTN_MOMO" -> "Dépôt via MTN Mobile Money";
            case "WALLET" -> "Dépôt depuis le portefeuille";
            default -> "Dépôt externe via " + source;
        };

        // Cette transaction est créée quand le paiement est EFFECTIVEMENT RÉUSSI
        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.SUCCESS)
                .description(description)
                .build();

        transactionRepository.save(tx);
        walletRepository.save(wallet);
        return wallet;
    }

    // ==================================================================
    // ========================== RETRAIT (DEMANDE UTILISATEUR)
    // ==============================
    // ==================================================================
    @Transactional
    public Transaction demanderRetrait(Long userId, double montantDouble) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        Wallet wallet = getWalletWithLock(userId);

        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant");
        }

        // Bloque les fonds en attendant la validation ou l'exécution manuelle/admin
        wallet.bloquerFonds(montant);

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.RETRAIT)
                .statut(StatutTransaction.EN_ATTENTE_VALIDATION)
                .description("Demande de retrait en attente")
                .build();

        transactionRepository.save(tx);
        walletRepository.save(wallet);
        return tx;
    }

    // ==================================================================
    // ======================= TRANSFERT P2P ===========================
    // ==================================================================
    @Transactional
    public void transfererFonds(
            Long expediteurId,
            Long destinataireUserId,
            double montantDouble,
            String source) { // "DISPONIBLE" ou "RETIRABLE"

        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant doit être positif");
        }
        if (expediteurId.equals(destinataireUserId)) {
            throw new IllegalArgumentException("Transfert vers soi-même interdit");
        }

        // Récupération + verrou pessimiste
        Wallet walletExp = walletRepository.findByUserIdWithPessimisticLock(expediteurId)
                .orElseThrow(() -> new IllegalStateException("Wallet expéditeur non trouvé"));

        Wallet walletDest = walletRepository.findByUserIdWithPessimisticLock(destinataireUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet destinataire non trouvé"));

        // Vérification du solde source
        BigDecimal soldeSource = "RETIRABLE".equalsIgnoreCase(source)
                ? walletExp.getSoldeRetirable()
                : walletExp.getSoldeDisponible();

        if (soldeSource.compareTo(montant) < 0) {
            throw new IllegalStateException("Solde " + source.toLowerCase() + " insuffisant");
        }

        // DÉBIT EXPÉDITEUR
        if ("RETIRABLE".equalsIgnoreCase(source)) {
            walletExp.setSoldeRetirable(walletExp.getSoldeRetirable().subtract(montant));
        } else {
            walletExp.setSoldeDisponible(walletExp.getSoldeDisponible().subtract(montant));
        }

        // CRÉDIT DESTINATAIRE → toujours en soldeDisponible
        walletDest.setSoldeDisponible(walletDest.getSoldeDisponible().add(montant));

        // Historique
        Transaction txOut = Transaction.builder()
                .walletId(walletExp.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_OUT)
                .statut(StatutTransaction.SUCCESS)
                .description("Transfert vers utilisateur " + destinataireUserId)
                .build();

        Transaction txIn = Transaction.builder()
                .walletId(walletDest.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_IN)
                .statut(StatutTransaction.SUCCESS)
                .description("Réception de transfert")
                .build();

        transactionRepository.save(txOut);
        transactionRepository.save(txIn);
        walletRepository.save(walletExp);
        walletRepository.save(walletDest);
    }

    // ==================================================================
    // ===================== WALLET PROJET – ADMIN ONLY ================
    // ==================================================================

    @PreAuthorize("hasRole('ADMIN')")
    public Wallet getProjetWallet(Long projetId) {
        return walletRepository.findByProjetId(projetId)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable pour le projet " + projetId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void verserAuPorteur(Long projetId, BigDecimal montant, String motif) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        // 1. Wallet projet avec verrou
        Wallet walletProjet = walletRepository.findByProjetIdWithLock(projetId)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Fonds insuffisants dans le wallet projet");
        }

        // 2. Projet + porteur
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        User porteur = projet.getPorteur();
        if (porteur == null || porteur.getWallet() == null) {
            throw new IllegalStateException("Le porteur n'a pas de wallet");
        }

        Wallet walletPorteur = walletRepository.findByUserIdWithPessimisticLock(porteur.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet porteur non trouvé"));

        // 3. TRANSFERT RÉEL
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletPorteur.setSoldeRetirable(walletPorteur.getSoldeRetirable().add(montant)); // ← gains validés

        // 4. Historique
        Transaction tx = Transaction.builder()
                .walletId(walletProjet.getId())
                .montant(montant)
                .type(TypeTransaction.VERSEMENT_PORTEUR)
                .statut(StatutTransaction.SUCCESS)
                .description(motif != null && !motif.isBlank() ? motif : "Versement au porteur du projet")
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        walletRepository.saveAll(List.of(walletProjet, walletPorteur));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void retirerDuProjetWallet(Long projetId, BigDecimal montant, String methode, String phone) {
        Wallet walletProjet = walletRepository
                .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde insuffisant");
        }

        // DÉBIT IMMÉDIAT + FORCE L'ÉCRITURE
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(walletProjet);

        // HISTORIQUE DÉBIT
        Transaction tx = Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.RETRAIT_ADMIN)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Retrait admin via " + methode)
                .referenceType("PROJET")
                .referenceId(projetId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.saveAndFlush(tx);

        // APPEL À L'EXÉCUTION DU PAYOUT EXTERNE
        try {
            if ("STRIPE".equalsIgnoreCase(methode)) {
                Projet projet = projetRepository.findById(projetId)
                        .orElseThrow(() -> new IllegalStateException("Projet introuvable"));
                User porteur = projet.getPorteur();
                if (porteur == null)
                    throw new IllegalStateException("Porteur non trouvé");

                // Execution du Payout Stripe
                stripePayoutService.createBankPayoutWithNewTransaction(porteur.getId(), montant, phone);
            } else if ("MOBILE_MONEY".equalsIgnoreCase(methode)) {
                if (phone == null || phone.trim().isEmpty()) {
                    throw new IllegalArgumentException("Téléphone requis");
                }
                // Execution du Payout Mobile Money (Retrait vers compte MM)
                payDunyaService.initiatePayout(montant, phone, TypeTransaction.PAYOUT_OM, tx.getId()); // ID de la Tx
                                                                                                       // pour référence
            }

            // Marque la transaction comme réussie si l'appel API ne lève pas d'exception
            tx.markAsSuccess();
        } catch (Exception e) {
            tx.markAsFailed();
            tx.setDescription("Échec payout : " + e.getMessage().substring(0, Math.min(490, e.getMessage().length())));
            log.error("Payout échoué : {}", e.getMessage());
        } finally {
            transactionRepository.save(tx);
        }
    }

    // ==================================================================
    // ========================== OUTILS ================================
    // ==================================================================
    private Wallet getWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé (user ID: " + userId + ")"));
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé"));
    }

    public BigDecimal getSoldeDisponible(Long userId) {
        return getWalletByUserId(userId).getSoldeDisponible();
    }

    public BigDecimal getSoldeBloque(Long userId) {
        return getWalletByUserId(userId).getSoldeBloque();
    }
}