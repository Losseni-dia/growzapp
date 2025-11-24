// WalletService.java → VERSION FINALE PRO AVEC BigDecimal (25 NOV 2025)

package growzapp.backend.service;

import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    // ==================================================================
    // ======================= DÉPÔT DE FONDS ==========================
    // ==================================================================
    @Transactional
    public Wallet deposerFonds(Long userId, double montantDouble) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Le montant doit être positif");

        Wallet wallet = getWalletWithLock(userId);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .montant(montant) // Transaction attend double
                .type(TypeTransaction.DEPOT)
                .description("Dépôt de fonds sur le wallet")
                .build();

        wallet.crediterDisponible(montant);
        transaction.markAsSuccess();

        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        return wallet;
    }

    // ==================================================================
    // ========================== RETRAIT ==============================
    // ==================================================================
    @Transactional
    public Transaction demanderRetrait(Long userId, double montantDouble) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Montant invalide");

        Wallet wallet = getWalletWithLock(userId);

        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant pour le retrait");
        }

        wallet.bloquerFonds(montant);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .montant(montant)
                .type(TypeTransaction.RETRAIT)
                .statut(StatutTransaction.EN_ATTENTE_VALIDATION)
                .description("Demande de retrait en attente de validation")
                .build();

        transactionRepository.save(transaction);
        walletRepository.save(wallet);

        return transaction;
    }

    // ==================================================================
    // ======================= TRANSFERT P2P ===========================
    // ==================================================================
    @Transactional
    public void transfererFonds(Long expediteurId, Long destinataireUserId, double montantDouble) {
        BigDecimal montant = BigDecimal.valueOf(montantDouble);
        if (montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Montant doit être positif");
        if (expediteurId.equals(destinataireUserId)) {
            throw new IllegalArgumentException("Impossible de transférer à soi-même");
        }

        Wallet walletExp = getWalletByUserId(expediteurId);
        Wallet walletDest = getWalletByUserId(destinataireUserId);

        Long id1 = Math.min(walletExp.getId(), walletDest.getId());
        Long id2 = Math.max(walletExp.getId(), walletDest.getId());

        Wallet locked1 = walletRepository.findByIdWithPessimisticLock(id1)
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));
        Wallet locked2 = walletRepository.findByIdWithPessimisticLock(id2)
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        walletExp = walletExp.getId().equals(id1) ? locked1 : locked2;
        walletDest = walletDest.getId().equals(id1) ? locked1 : locked2;

        if (walletExp.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant");
        }

        // Débit expéditeur
        walletExp.debiterDisponible(montant);
        Transaction txOut = Transaction.builder()
                .wallet(walletExp)
                .destinataireWallet(walletDest)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_OUT)
                .description("Transfert vers utilisateur ID " + destinataireUserId)
                .build();
        txOut.markAsSuccess();

        // Crédit destinataire
        walletDest.crediterDisponible(montant);
        Transaction txIn = Transaction.builder()
                .wallet(walletDest)
                .destinataireWallet(walletExp)
                .montant(montant)
                .type(TypeTransaction.TRANSFER_IN)
                .description("Réception de transfert")
                .build();
        txIn.markAsSuccess();

        transactionRepository.save(txOut);
        transactionRepository.save(txIn);
        walletRepository.save(walletExp);
        walletRepository.save(walletDest);
    }

    // ==================================================================
    // ========================== OUTILS ================================
    // ==================================================================
    private Wallet getWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé pour l'utilisateur ID: " + userId));
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé"));
    }
}