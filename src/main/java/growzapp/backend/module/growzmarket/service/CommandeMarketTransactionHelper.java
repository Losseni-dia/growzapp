package growzapp.backend.module.growzmarket.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mouvement de trésorerie d'un achat GrowzMarket, isolé dans un bean dédié
 * pour que @Transactional(REQUIRES_NEW) ne soit pas ignoré en cas
 * d'auto-invocation depuis CommandeMarketService (même raison que
 * CommandeTransactionHelper côté Fournisseur).
 *
 * Paiement immédiat à la commande : débite le wallet personnel de
 * l'acheteur, crédite le soldeBloque du wallet du projet vendeur — traité
 * comme un nouvel apport de trésorerie, débloqué ensuite par l'admin comme
 * l'argent des investisseurs (une seule gouvernance pour l'argent qui entre
 * dans un projet).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeMarketTransactionHelper {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executerAchat(Long acheteurUserId, Long projetId, Long commandeId, BigDecimal montant) {
        Wallet walletAcheteur = walletRepository.findByUserIdWithPessimisticLock(acheteurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet acheteur introuvable"));
        if (walletAcheteur.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde disponible insuffisant dans votre wallet pour cet achat.");
        }
        walletAcheteur.setSoldeDisponible(walletAcheteur.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(walletAcheteur);

        Wallet walletProjet = walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        walletProjet.crediterBloqueProjet(montant);
        walletRepository.saveAndFlush(walletProjet);

        transactionRepository.save(Transaction.builder()
                .walletId(walletAcheteur.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.VENTE_MARKET)
                .statut(StatutTransaction.SUCCESS)
                .description("Achat GrowzMarket #" + commandeId)
                .referenceType("COMMANDE_MARKET")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        transactionRepository.save(Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.VENTE_MARKET)
                .statut(StatutTransaction.SUCCESS)
                .description("Vente GrowzMarket #" + commandeId + " — trésorerie créditée")
                .referenceType("COMMANDE_MARKET")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        log.info("Commande GrowzMarket {} payée : {} FCFA de {} vers projet {}",
                commandeId, montant, acheteurUserId, projetId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rembourserAcheteur(Long acheteurUserId, Long projetId, Long commandeId, BigDecimal montant) {
        Wallet walletProjet = walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        walletProjet.debiterBloque(montant);
        walletRepository.saveAndFlush(walletProjet);

        Wallet walletAcheteur = walletRepository.findByUserIdWithPessimisticLock(acheteurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet acheteur introuvable"));
        walletAcheteur.crediterDisponible(montant);
        walletRepository.saveAndFlush(walletAcheteur);

        transactionRepository.save(Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.REMBOURSEMENT)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande GrowzMarket #" + commandeId + " annulée — remboursement acheteur")
                .referenceType("COMMANDE_MARKET")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        transactionRepository.save(Transaction.builder()
                .walletId(walletAcheteur.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.REMBOURSEMENT)
                .statut(StatutTransaction.SUCCESS)
                .description("Remboursement — commande GrowzMarket #" + commandeId + " annulée")
                .referenceType("COMMANDE_MARKET")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());
    }
}
