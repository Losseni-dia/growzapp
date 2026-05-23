package growzapp.backend.module.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.paiement.paydunya.PayDunyaService;
import growzapp.backend.module.paiement.paydunya.PayDunyaService.PayDunyaResponse;
import growzapp.backend.module.paiement.stripe.StripePayoutService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ProjetRepository projetRepository;
    private final StripePayoutService stripePayoutService;
    private final PayDunyaService payDunyaService;

    @Transactional
    public String initierDepotMobileMoney(Long userId, BigDecimal montant) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletWithLock(userId);

        PayDunyaResponse payDunyaRes = payDunyaService.createDepositCheckoutSession(montant, userId);

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Dépôt Mobile Money (redirection PayDunya)")
                .createdAt(LocalDateTime.now())
                .referenceExterne(payDunyaRes.invoiceToken())
                .build();

        transactionRepository.save(tx);
        return payDunyaRes.redirectUrl();
    }

    @Transactional
    public Wallet deposerFonds(Long userId, double montantDouble, String source) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletWithLock(userId);
        wallet.crediterDisponible(montant);

        String description = switch (source.toUpperCase()) {
            case "STRIPE_CARD" -> "Dépôt via carte bancaire (Stripe)";
            case "ORANGE_MONEY" -> "Dépôt via Orange Money";
            case "WAVE" -> "Dépôt via Wave";
            case "MTN_MOMO" -> "Dépôt via MTN Mobile Money";
            case "WALLET" -> "Dépôt depuis le portefeuille";
            default -> "Dépôt externe via " + source;
        };

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

    @Transactional
    public void transfererFonds(Long expediteurId, Long destinataireUserId, double montantDouble, String source) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant doit être positif");
        }
        if (expediteurId.equals(destinataireUserId)) {
            throw new IllegalArgumentException("Transfert vers soi-même interdit");
        }

        Wallet walletExp = walletRepository.findByUserIdWithPessimisticLock(expediteurId)
                .orElseThrow(() -> new IllegalStateException("Wallet expéditeur non trouvé"));

        Wallet walletDest = walletRepository.findByUserIdWithPessimisticLock(destinataireUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet destinataire non trouvé"));

        BigDecimal soldeSource = "RETIRABLE".equalsIgnoreCase(source)
                ? walletExp.getSoldeRetirable()
                : walletExp.getSoldeDisponible();

        if (soldeSource.compareTo(montant) < 0) {
            throw new IllegalStateException("Solde " + source.toLowerCase() + " insuffisant");
        }

        if ("RETIRABLE".equalsIgnoreCase(source)) {
            walletExp.setSoldeRetirable(walletExp.getSoldeRetirable().subtract(montant));
        } else {
            walletExp.setSoldeDisponible(walletExp.getSoldeDisponible().subtract(montant));
        }

        walletDest.setSoldeDisponible(walletDest.getSoldeDisponible().add(montant));

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

        Wallet walletProjet = walletRepository.findByProjetIdWithLock(projetId)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Fonds insuffisants dans le wallet projet");
        }

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        User porteur = projet.getPorteur();
        if (porteur == null || porteur.getWallet() == null) {
            throw new IllegalStateException("Le porteur n'a pas de wallet");
        }

        Wallet walletPorteur = walletRepository.findByUserIdWithPessimisticLock(porteur.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet porteur non trouvé"));

        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletPorteur.setSoldeRetirable(walletPorteur.getSoldeRetirable().add(montant));

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

        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(walletProjet);

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

        try {
            if ("STRIPE".equalsIgnoreCase(methode)) {
                Projet projet = projetRepository.findById(projetId)
                        .orElseThrow(() -> new IllegalStateException("Projet introuvable"));
                User porteur = projet.getPorteur();
                if (porteur == null) throw new IllegalStateException("Porteur non trouvé");

                stripePayoutService.createBankPayoutWithNewTransaction(porteur.getId(), montant, phone);
            } else if ("MOBILE_MONEY".equalsIgnoreCase(methode)) {
                if (phone == null || phone.trim().isEmpty()) {
                    throw new IllegalArgumentException("Téléphone requis");
                }
                payDunyaService.initiatePayout(montant, phone, TypeTransaction.PAYOUT_OM, tx.getId());
            }

            tx.markAsSuccess();
        } catch (Exception e) {
            tx.markAsFailed();
            tx.setDescription("Échec payout : " + e.getMessage().substring(0, Math.min(490, e.getMessage().length())));
            log.error("Payout échoué : {}", e.getMessage());
        } finally {
            transactionRepository.save(tx);
        }
    }

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
