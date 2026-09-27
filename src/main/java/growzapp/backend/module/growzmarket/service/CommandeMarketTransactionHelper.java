package growzapp.backend.module.growzmarket.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.wallet.enums.SourcePaiement;
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

    // Part d'un achat multi-vendeur destinée à un projet donné — un panier
    // payé en une fois peut concerner plusieurs vendeurs, chacun avec sa
    // propre CommandeMarket, mais un seul mouvement de fonds acheteur.
    public record VendorPart(Long projetId, Long commandeId, BigDecimal montant) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executerAchatMultiVendeur(Long acheteurUserId, List<VendorPart> parts, BigDecimal montantTotal) {
        Wallet walletAcheteur = walletRepository.findByUserIdWithPessimisticLock(acheteurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet acheteur introuvable"));
        if (walletAcheteur.getSoldeDisponible().compareTo(montantTotal) < 0) {
            throw new IllegalStateException("Solde disponible insuffisant dans votre wallet pour cet achat.");
        }
        walletAcheteur.setSoldeDisponible(walletAcheteur.getSoldeDisponible().subtract(montantTotal));
        walletRepository.saveAndFlush(walletAcheteur);

        for (VendorPart part : parts) {
            Wallet walletProjet = walletRepository
                    .findByProjetIdAndWalletTypeWithLock(part.projetId(), WalletType.PROJET)
                    .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
            walletProjet.crediterBloqueProjet(part.montant());
            walletRepository.saveAndFlush(walletProjet);

            transactionRepository.save(Transaction.builder()
                    .walletId(walletAcheteur.getId())
                    .walletType(WalletType.USER)
                    .montant(part.montant())
                    .type(TypeTransaction.VENTE_MARKET)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Achat GrowzMarket #" + part.commandeId())
                    .referenceType("COMMANDE_MARKET")
                    .referenceId(part.commandeId())
                    .completedAt(LocalDateTime.now())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .walletId(walletProjet.getId())
                    .walletType(WalletType.PROJET)
                    .montant(part.montant())
                    .type(TypeTransaction.VENTE_MARKET)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Vente GrowzMarket #" + part.commandeId() + " — trésorerie créditée")
                    .referenceType("COMMANDE_MARKET")
                    .referenceId(part.commandeId())
                    .completedAt(LocalDateTime.now())
                    .build());
        }

        log.info("Achat GrowzMarket multi-vendeur : {} FCFA débités à {} pour {} commande(s)",
                montantTotal, acheteurUserId, parts.size());
    }

    // Achat payé directement par Mobile Money/Carte (jamais par le wallet
    // interne) : aucun débit du solde acheteur (l'argent n'y a jamais
    // transité), seuls les wallets projet sont crédités — même logique que
    // Investissement.investirDepuisPaiementExterne. On trace néanmoins
    // l'achat dans l'historique de l'acheteur (montant informatif, sans
    // impact sur son solde) pour qu'il y retrouve trace de son paiement
    // externe, comme pour un achat payé par wallet.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executerAchatExterneMultiVendeur(Long acheteurUserId, List<VendorPart> parts, SourcePaiement source) {
        Wallet walletAcheteur = walletRepository.findByUserId(acheteurUserId)
                .orElseThrow(() -> new IllegalStateException("Wallet acheteur introuvable"));

        for (VendorPart part : parts) {
            Wallet walletProjet = walletRepository
                    .findByProjetIdAndWalletTypeWithLock(part.projetId(), WalletType.PROJET)
                    .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));
            walletProjet.crediterBloqueProjet(part.montant());
            walletRepository.saveAndFlush(walletProjet);

            transactionRepository.save(Transaction.builder()
                    .walletId(walletProjet.getId())
                    .walletType(WalletType.PROJET)
                    .montant(part.montant())
                    .type(TypeTransaction.VENTE_MARKET)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Vente GrowzMarket #" + part.commandeId() + " (paiement externe) — trésorerie créditée")
                    .referenceType("COMMANDE_MARKET")
                    .referenceId(part.commandeId())
                    .sourcePaiement(source)
                    .completedAt(LocalDateTime.now())
                    .build());

            transactionRepository.save(Transaction.builder()
                    .walletId(walletAcheteur.getId())
                    .walletType(WalletType.USER)
                    .montant(part.montant())
                    .type(TypeTransaction.VENTE_MARKET)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Achat GrowzMarket #" + part.commandeId() + " (paiement externe)")
                    .referenceType("COMMANDE_MARKET")
                    .referenceId(part.commandeId())
                    .sourcePaiement(source)
                    .completedAt(LocalDateTime.now())
                    .build());
        }

        log.info("Achat GrowzMarket multi-vendeur (externe, {}) : {} commande(s) pour user {}",
                source, parts.size(), acheteurUserId);
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
