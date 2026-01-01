// src/main/java/growzapp/backend/controller/webhooks/PaydunyaWebhookController.java

package growzapp.backend.controller.webhooks;

import growzapp.backend.model.entite.PayoutModel;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository; // NOUVELLE INJECTION REQUISE
import growzapp.backend.service.WalletService; // Pour la logique de crédit
import growzapp.backend.model.entite.Wallet; // Import de l'entité Wallet
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/webhook/paydunya")
@RequiredArgsConstructor
public class PaydunyaWebhookController {

    @Value("${paydunya.webhook-secret}")
    private String webhookSecret;

    private final PayoutModelRepository payoutModelRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository; // AJOUT DE L'INJECTION POUR CHERCHER LE WALLET

    @PostMapping
    @Transactional
    public ResponseEntity<String> handle(@RequestBody Map<String, Object> payload) {
        // NOTE: En production, ajoutez la vérification de la signature ici !

        String token = (String) payload.get("token"); // Peut être l'ID de la facture PayDunya
        String status = (String) payload.get("status");

        if (token == null || status == null) {
            return ResponseEntity.badRequest().body("Missing data");
        }

        // 1. Tenter de traiter comme un RETRAIT (PayoutModel)
        Optional<PayoutModel> payoutOpt = payoutModelRepository.findByPaydunyaToken(token);

        if (payoutOpt.isPresent()) {
            PayoutModel p = payoutOpt.get();
            if ("completed".equals(status) || "paid".equals(status)) {
                p.setStatut(StatutTransaction.SUCCESS);
                log.info("RETRAIT PAYDUNYA PAYÉ (Payout) → {} €", p.getMontant());
            } else {
                p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                log.warn("RETRAIT PAYDUNYA ÉCHOUÉ (Payout) → {}", status);
                // Si échec : potentiellement recréditer le solde retirable (nécessite une autre
                // méthode dans WalletService)
            }
            p.setPaydunyaStatus(status);
            p.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(p);
            return ResponseEntity.ok("Payout updated");
        }

        // 2. Tenter de traiter comme un DÉPÔT (Transaction)
        // La transaction est recherchée par la référence externe (token PayDunya)
        Optional<Transaction> txOpt = transactionRepository.findByReferenceExterne(token);

        if (txOpt.isPresent()) {
            Transaction tx = txOpt.get();
            if (tx.getStatut() == StatutTransaction.SUCCESS) {
                return ResponseEntity.ok("Already processed");
            }

            if ("completed".equals(status) || "paid".equals(status)) {
                // CORRECTION DE LA LOGIQUE DE CRÉDIT

                // Charger le Wallet pour obtenir l'ID de l'utilisateur (méthode non optimale
                // mais fonctionnelle)
                Wallet associatedWallet = walletRepository.findById(tx.getWalletId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Wallet associé à la transaction non trouvé: " + tx.getWalletId()));

                Long userId = associatedWallet.getUser().getId();

                // ** CRÉDIT DU WALLET **
                walletService.deposerFonds(
                        userId, // Utilisation de l'ID utilisateur réel
                        tx.getMontant().doubleValue(),
                        "PAYDUNYA_MM");

                // Mise à jour de la transaction (si elle n'est pas mise à jour/remplacée par
                // deposerFonds)
                tx.setStatut(StatutTransaction.SUCCESS);
                tx.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(tx);

                log.info("DÉPÔT PAYDUNYA CRÉDITÉ (Transaction) → {} €", tx.getMontant());
            } else {
                tx.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                log.warn("DÉPÔT PAYDUNYA ÉCHOUÉ (Transaction) → {}", status);
                transactionRepository.save(tx);
            }
            return ResponseEntity.ok("Deposit updated");
        }

        log.warn("Webhook PayDunya reçu sans correspondance Payout/Transaction: {}", token);
        return ResponseEntity.ok("No match found");
    }

    private boolean verifySignature(Map<String, Object> payload, String signature) {
        // En prod, implémentez la vérification HMAC
        return true;
    }
}