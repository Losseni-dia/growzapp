package growzapp.backend.module.paiement.innerwallet;

import growzapp.backend.module.notification.service.NotificationService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pendant de WithdrawalTransactionHelper pour le wallet PROJET (retrait
 * externe initié par le porteur). Extrait dans une classe séparée pour la
 * même raison : @Transactional(REQUIRES_NEW) est silencieusement ignoré en
 * cas d'appel interne (self-invocation) depuis ProjetWithdrawalService — il
 * faut passer par un bean Spring distinct.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetWithdrawalTransactionHelper {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ProjetRepository projetRepository;
    private final NotificationService notificationService;

    private Wallet getWalletProjetWithLock(Long projetId) {
        return walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction debiterEtCreerTransaction(Long projetId, BigDecimal montant, TypeTransaction type,
            String idempotencyKey) {
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        Wallet wallet = getWalletProjetWithLock(projetId);
        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant dans le wallet projet.");
        }

        wallet.setSoldeDisponible(wallet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(wallet);

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(type)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Retrait porteur depuis le wallet projet")
                .referenceType("PROJET")
                .referenceId(projetId)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        return transactionRepository.saveAndFlush(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marquerSucces(Long transactionId, String externalId) {
        transactionRepository.findById(transactionId).ifPresent(t -> {
            t.setReferenceExterne(externalId);
            t.markAsSuccess();
            transactionRepository.save(t);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rembourserEtNotifier(Long projetId, BigDecimal montant, Long transactionId, Exception e) {
        log.error("Échec retrait wallet projet={} montant={} — remboursement automatique", projetId, montant, e);

        Wallet walletRefund = getWalletProjetWithLock(projetId);
        walletRefund.setSoldeDisponible(walletRefund.getSoldeDisponible().add(montant));
        walletRepository.saveAndFlush(walletRefund);

        transactionRepository.findById(transactionId).ifPresent(t -> {
            t.markAsFailed();
            t.setDescription("Échec payout — fonds remboursés au wallet projet. Raison : "
                    + e.getMessage().substring(0, Math.min(400, e.getMessage() == null ? 0 : e.getMessage().length())));
            transactionRepository.save(t);
        });

        Projet projet = projetRepository.findById(projetId).orElse(null);
        User porteur = projet != null ? projet.getPorteur() : null;
        if (porteur != null) {
            notificationService.notifyUser(
                    porteur,
                    "❌ Échec du retrait",
                    "Votre retrait de " + montant.toPlainString()
                            + " FCFA depuis le wallet du projet « " + projet.getLibelle()
                            + " » a échoué. Les fonds ont été remboursés au wallet du projet.",
                    projet.getId(), projet.getSlug(),
                    e.getMessage());
        }
    }
}
