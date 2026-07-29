package growzapp.backend.module.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.paiement.innerwallet.WithdrawalService;
import growzapp.backend.module.paiement.common.PaymentProviderRouter;
import growzapp.backend.module.paiement.common.PaymentProviderService.PaymentSessionResponse;
import growzapp.backend.module.paiement.common.PaymentProviderService.PayoutResponse;
import growzapp.backend.module.paiement.stripe.StripePayoutService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final StripePayoutService stripePayoutService;
    private final PaymentProviderRouter paymentProviderRouter;
    private final NotificationService notificationService;
    private final WithdrawalService withdrawalService;

    // ── DÉPÔT MOBILE MONEY ───────────────────────────────────────────────────
    @Transactional
    public String initierDepotMobileMoney(Long userId, BigDecimal montant) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        Wallet wallet = getWalletWithLock(userId);

        PaymentSessionResponse payDunyaRes = paymentProviderRouter.creerSessionDepot(montant, userId);

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Dépôt Mobile Money (redirection PayDunya)")
                .createdAt(LocalDateTime.now())
                .referenceExterne(payDunyaRes.sessionToken())
                .build();

        transactionRepository.save(tx);
        return payDunyaRes.redirectUrl();
    }

    // ── DÉPÔT MANUEL / EXTERNE ────────────────────────────────────────────────
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
            case "PAYDUNYA_MM" -> "Dépôt via Mobile Money (PayDunya)";
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
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);
        walletRepository.save(wallet);
        return wallet;
    }

    // ── RETRAIT AUTOMATIQUE (délègue à WithdrawalService) ───────────────────
    public String retirerFonds(Long userId, BigDecimal montant, String methode, String phone) {
        if ("STRIPE".equalsIgnoreCase(methode)) {
            // ── DÉSACTIVÉ TEMPORAIREMENT ─────────────────────────────────────
            // Payout.create() de Stripe envoie les fonds vers le compte bancaire
            // de la plateforme GrowzApp, PAS vers celui de l'utilisateur.
            // Voir STRIPE_PAYOUT_ROADMAP.md pour le détail et la roadmap (Stripe
            // Connect ou partenaire de virement tiers). Ne pas réactiver avant.
            throw new IllegalStateException(
                    "Le retrait vers un compte bancaire n'est pas encore disponible. "
                            + "Utilisez le retrait Mobile Money en attendant.");
        } else if ("MOBILE_MONEY".equalsIgnoreCase(methode)) {
            TypeTransaction mmType = resolveMobileMoneyType(phone);
            return withdrawalService.executerRetraitMobileMoney(userId, montant, mmType, phone);
        }
        throw new IllegalArgumentException("Méthode de retrait inconnue : " + methode);
    }

    private TypeTransaction resolveMobileMoneyType(String phone) {
        return TypeTransaction.PAYOUT_OM;
    }

    // ── TRANSFERT ENTRE UTILISATEURS ─────────────────────────────────────────
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

        if (walletExp.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde disponible insuffisant");
        }

        walletExp.setSoldeDisponible(walletExp.getSoldeDisponible().subtract(montant));
        walletDest.setSoldeDisponible(walletDest.getSoldeDisponible().add(montant));

        Transaction txOut = Transaction.builder()
                .walletId(walletExp.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_OUT)
                .statut(StatutTransaction.SUCCESS)
                .description("Transfert vers utilisateur " + destinataireUserId)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction txIn = Transaction.builder()
                .walletId(walletDest.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_IN)
                .statut(StatutTransaction.SUCCESS)
                .description("Réception de transfert depuis utilisateur " + expediteurId)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepository.save(txOut);
        transactionRepository.save(txIn);
        walletRepository.save(walletExp);
        walletRepository.save(walletDest);

        userRepository.findById(destinataireUserId).ifPresent(dest -> notificationService.notifyUser(
                dest,
                "💰 Vous avez reçu un transfert",
                "Vous avez reçu " + montant.toPlainString() + " FCFA sur votre portefeuille GrowzApp.",
                null, null, null));
    }

    // ── WALLET PROJET (admin) ────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    public Wallet getProjetWallet(Long projetId) {
        return walletRepository.findByProjetId(projetId)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable pour le projet " + projetId));
    }

    // NOTE : Cette méthode n'est plus utilisée par le flux principal.
    // Le vrai endpoint /api/admin/projet-wallet/{id}/verser-porteur est géré
    // directement par ProjetWalletController.verserAuPorteur(), qui contient
    // la logique complète (transactions liées au projet, notif+email porteur
    // ET investisseurs). Conservée ici en simple fallback / réutilisation
    // potentielle, sans dupliquer la logique de notification.
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

        String motifFinal = motif != null && !motif.isBlank() ? motif : "Versement au porteur du projet";

        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletPorteur.setSoldeDisponible(walletPorteur.getSoldeDisponible().add(montant));

        Transaction tx = Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.VERSEMENT_PORTEUR)
                .statut(StatutTransaction.SUCCESS)
                .description(motifFinal)
                .referenceType("PROJET")
                .referenceId(projetId)
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
                if (porteur == null)
                    throw new IllegalStateException("Porteur non trouvé");

                String payoutId = stripePayoutService.createBankPayoutDirect(porteur.getId(), montant, tx.getId());
                tx.setReferenceExterne(payoutId);

            } else if ("MOBILE_MONEY".equalsIgnoreCase(methode)) {
                if (phone == null || phone.trim().isEmpty()) {
                    throw new IllegalArgumentException("Téléphone requis");
                }
                PayoutResponse res = paymentProviderRouter.initierRetrait(
                        montant, phone, "orange-money-ci", tx.getId());
                tx.setReferenceExterne(res.externalTxId());
            }

            tx.markAsSuccess();

        } catch (Exception e) {
            Wallet walletRefund = walletRepository
                    .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                    .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
            walletRefund.setSoldeDisponible(walletRefund.getSoldeDisponible().add(montant));
            walletRepository.saveAndFlush(walletRefund);

            tx.markAsFailed();
            tx.setDescription("Échec payout — fonds remboursés au projet. Raison : "
                    + e.getMessage().substring(0, Math.min(400, e.getMessage().length())));
            log.error("Payout projet échoué : {}", e.getMessage());
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

    public List<Transaction> getHistoriqueTransactions(Long userId) {
        Wallet wallet = getWalletByUserId(userId);
        return transactionRepository.findByWalletTypeAndWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}