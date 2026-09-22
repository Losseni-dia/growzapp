package growzapp.backend.module.fournisseur.service;

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
 * Mouvement de trésorerie d'une commande fournisseur, isolé dans un bean
 * dédié pour la même raison que ProjetWithdrawalTransactionHelper :
 * @Transactional(REQUIRES_NEW) est ignoré en cas d'auto-invocation depuis
 * CommandeService, il faut passer par un bean Spring distinct pour que le
 * verrou pessimiste sur les wallets soit posé dans sa propre transaction.
 *
 * Un seul mouvement de fonds pour toute la commande : il n'a lieu qu'après
 * validation admin, acceptation fournisseur, expédition et confirmation de
 * réception par le porteur — l'admin déclenche alors explicitement le
 * paiement. Aucun séquestre intermédiaire n'est nécessaire puisque
 * l'engagement des deux parties est déjà acquis à ce stade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeTransactionHelper {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executerPaiement(Long projetId, Long fournisseurUserId, Long commandeId, BigDecimal montant) {
        Wallet walletProjet = walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException(
                    "Solde disponible insuffisant dans le wallet du projet pour payer cette commande.");
        }
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(walletProjet);

        Wallet walletFournisseur = walletRepository.findByUserIdWithPessimisticLock(fournisseurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet fournisseur introuvable"));
        walletFournisseur.crediterDisponible(montant);
        walletRepository.saveAndFlush(walletFournisseur);

        transactionRepository.save(Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.PAIEMENT_FOURNISSEUR)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande fournisseur #" + commandeId + " — paiement exécuté")
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        transactionRepository.save(Transaction.builder()
                .walletId(walletFournisseur.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.PAIEMENT_FOURNISSEUR)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande #" + commandeId + " — paiement reçu, fonds disponibles")
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        log.info("Commande {} payée : {} FCFA transférés au wallet fournisseur (user={})",
                commandeId, montant, fournisseurUserId);
    }
}
