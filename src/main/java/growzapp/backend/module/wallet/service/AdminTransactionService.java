package growzapp.backend.module.wallet.service;

import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.wallet.dto.TransactionAdminDTO;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assemble la vue "historique global des transactions" (dépôts, retraits,
 * investissements, versements porteur, dividendes...) pour l'admin.
 * Transaction.walletId n'est pas une relation JPA (juste un Long) — les
 * wallets et projets liés sont donc chargés en masse ici plutôt que via un
 * jointure directe, pour éviter le N+1 sans complexifier la requête JPQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ProjetRepository projetRepository;

    public Page<TransactionAdminDTO> getAllAdmin(
            TypeTransaction type,
            WalletType walletType,
            StatutTransaction statut,
            String search,
            Pageable pageable) {

        Page<Transaction> page = transactionRepository.findAdmin(type, walletType, statut, search, pageable);
        List<Transaction> transactions = page.getContent();

        List<Long> walletIds = transactions.stream()
                .map(Transaction::getWalletId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Wallet> walletsById = walletRepository.findByIdIn(walletIds).stream()
                .collect(Collectors.toMap(Wallet::getId, w -> w, (a, b) -> a));

        List<Long> projetIds = walletsById.values().stream()
                .filter(w -> w.getWalletType() == WalletType.PROJET && w.getProjetId() != null)
                .map(Wallet::getProjetId)
                .distinct()
                .toList();

        Map<Long, String> projetLibellesById = new HashMap<>();
        if (!projetIds.isEmpty()) {
            for (Projet p : projetRepository.findAllById(projetIds)) {
                projetLibellesById.put(p.getId(), p.getLibelle());
            }
        }

        List<TransactionAdminDTO> dtos = transactions.stream()
                .map(t -> toDto(t, walletsById, projetLibellesById))
                .toList();

        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private TransactionAdminDTO toDto(
            Transaction t,
            Map<Long, Wallet> walletsById,
            Map<Long, String> projetLibellesById) {

        Wallet wallet = t.getWalletId() != null ? walletsById.get(t.getWalletId()) : null;

        String utilisateurNom = null;
        String utilisateurEmail = null;
        String projetLibelle = null;

        if (wallet != null) {
            if (wallet.getWalletType() == WalletType.USER && wallet.getUser() != null) {
                User u = wallet.getUser();
                utilisateurNom = u.getPrenom() + " " + u.getNom();
                utilisateurEmail = u.getEmail();
            } else if (wallet.getWalletType() == WalletType.PROJET && wallet.getProjetId() != null) {
                projetLibelle = projetLibellesById.get(wallet.getProjetId());
            }
        }

        String destinataireNom = null;
        Wallet destinataire = t.getDestinataireWallet();
        if (destinataire != null && destinataire.getUser() != null) {
            User du = destinataire.getUser();
            destinataireNom = du.getPrenom() + " " + du.getNom();
        }

        return new TransactionAdminDTO(
                t.getId(),
                t.getType(),
                t.getWalletType(),
                t.getMontant(),
                t.getStatut(),
                t.getCreatedAt(),
                t.getCompletedAt(),
                t.getDescription(),
                t.getReferenceType(),
                t.getReferenceId(),
                t.getReferenceExterne(),
                utilisateurNom,
                utilisateurEmail,
                projetLibelle,
                destinataireNom);
    }
}
