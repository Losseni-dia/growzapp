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
 * Mouvements de trésorerie d'une commande fournisseur, isolés dans un bean
 * dédié pour la même raison que ProjetWithdrawalTransactionHelper :
 * @Transactional(REQUIRES_NEW) est ignoré en cas d'auto-invocation depuis
 * CommandeService, il faut passer par un bean Spring distinct pour que le
 * verrou pessimiste sur les wallets soit posé dans sa propre transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandeTransactionHelper {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Validation admin d'une commande : débite le wallet du projet
     * (soldeDisponible) et crédite le wallet du fournisseur en séquestre
     * (soldeBloque) — l'argent ne transite jamais par le porteur.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void debiterProjetVersFournisseurBloque(Long projetId, Long fournisseurUserId, Long commandeId,
            BigDecimal montant) {
        Wallet walletProjet = walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        if (walletProjet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalStateException("Solde disponible insuffisant dans le wallet du projet pour cette commande.");
        }
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(walletProjet);

        Wallet walletFournisseur = walletRepository.findByUserIdWithPessimisticLock(fournisseurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet fournisseur introuvable"));
        walletFournisseur.crediterDirectementBloque(montant);
        walletRepository.saveAndFlush(walletFournisseur);

        transactionRepository.save(Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.PAIEMENT_FOURNISSEUR)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande fournisseur #" + commandeId + " — fonds séquestrés")
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
                .description("Commande #" + commandeId + " — fonds séquestrés en attente de confirmation de réception")
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        log.info("Commande {} validée : {} FCFA séquestrés au wallet fournisseur (user={})",
                commandeId, montant, fournisseurUserId);
    }

    /**
     * Rejet admin après un débit déjà effectué (ne devrait normalement pas
     * arriver — la validation et le débit sont atomiques — mais gardé en
     * filet de sécurité si un rejet intervient après coup).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rembourserProjet(Long projetId, BigDecimal montant, Long commandeId) {
        Wallet walletProjet = walletRepository.findByProjetIdAndWalletTypeWithLock(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().add(montant));
        walletRepository.saveAndFlush(walletProjet);

        transactionRepository.save(Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montant)
                .type(TypeTransaction.REMBOURSEMENT)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande #" + commandeId + " rejetée/en litige — fonds remboursés au wallet projet")
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Confirmation de réception par le porteur : libère les fonds séquestrés
     * du wallet fournisseur (soldeBloque -> soldeDisponible), désormais
     * retirables.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void libererVersFournisseur(Long fournisseurUserId, BigDecimal montant, Long commandeId) {
        Wallet walletFournisseur = walletRepository.findByUserIdWithPessimisticLock(fournisseurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet fournisseur introuvable"));
        walletFournisseur.debloquerFonds(montant);
        walletRepository.saveAndFlush(walletFournisseur);

        transactionRepository.save(Transaction.builder()
                .walletId(walletFournisseur.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.PAIEMENT_FOURNISSEUR)
                .statut(StatutTransaction.SUCCESS)
                .description("Commande #" + commandeId + " — réception confirmée, fonds disponibles")
                .referenceType("COMMANDE")
                .referenceId(commandeId)
                .completedAt(LocalDateTime.now())
                .build());

        log.info("Commande {} confirmée : {} FCFA libérés au wallet fournisseur (user={})",
                commandeId, montant, fournisseurUserId);
    }
}
