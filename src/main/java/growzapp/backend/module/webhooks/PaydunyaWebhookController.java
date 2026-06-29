package growzapp.backend.module.webhooks;

import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import growzapp.backend.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final InvestissementService investissementService;

    // PayDunya envoie application/x-www-form-urlencoded avec data[field][subfield]
    @PostMapping(consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE })
    @Transactional
    public ResponseEntity<String> handle(@RequestParam Map<String, String> params) {

        log.info("WEBHOOK PAYDUNYA reçu : {}", params.keySet());

        // PayDunya envoie data[invoice][token] et data[status]
        String token = params.get("data[invoice][token]");
        String status = params.getOrDefault("data[status]", params.get("status"));

        // Fallback
        if (token == null)
            token = params.get("token");

        if (token == null || status == null) {
            log.warn("Webhook PayDunya — token={} status={} — params={}", token, status, params);
            return ResponseEntity.badRequest().body("Missing token or status");
        }

        log.info("WEBHOOK PAYDUNYA : token={} status={}", token, status);

        // Extraire custom_data
        String type = params.getOrDefault("data[custom_data][type]", "DEPOSIT");
        String userIdStr = params.get("data[custom_data][user_id]");
        String projetIdStr = params.get("data[custom_data][projet_id]");
        String partsStr = params.get("data[custom_data][nombre_parts]");

        // ── 1. RETRAIT (PayoutModel) ──────────────────────────────────────
        Optional<PayoutModel> payoutOpt = payoutModelRepository.findByPaydunyaToken(token);
        if (payoutOpt.isPresent()) {
            PayoutModel p = payoutOpt.get();
            if ("completed".equals(status) || "paid".equals(status)) {
                p.setStatut(StatutTransaction.SUCCESS);
                log.info("RETRAIT PAYDUNYA PAYÉ → {}", p.getMontant());
            } else {
                p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                log.warn("RETRAIT PAYDUNYA ÉCHOUÉ → {}", status);
            }
            p.setPaydunyaStatus(status);
            p.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(p);
            return ResponseEntity.ok("Payout updated");
        }

        // ── 2. DÉPÔT ou INVESTISSEMENT (Transaction) ─────────────────────
        Optional<Transaction> txOpt = transactionRepository.findByReferenceExterne(token);
        if (txOpt.isPresent()) {
            Transaction tx = txOpt.get();

            if (tx.getStatut() == StatutTransaction.SUCCESS) {
                log.warn("Transaction déjà traitée : {}", token);
                return ResponseEntity.ok("Already processed");
            }

            if ("completed".equals(status) || "paid".equals(status)) {

                if ("INVESTISSEMENT".equals(type) && userIdStr != null && projetIdStr != null) {
                    // ── Flux investissement Mobile Money ─────────────────
                    Long userId = Long.parseLong(userIdStr);
                    Long projetId = Long.parseLong(projetIdStr);
                    int parts = Integer.parseInt(partsStr != null ? partsStr : "1");

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User introuvable : " + userId));

                    // Créditer wallet puis bloquer via investir()
                    Wallet wallet = walletRepository.findByUserId(userId)
                            .orElseThrow(() -> new RuntimeException("Wallet introuvable : " + userId));
                    wallet.setSoldeDisponible(wallet.getSoldeDisponible().add(tx.getMontant()));
                    walletRepository.save(wallet);

                    investissementService.investir(projetId, parts, user);

                    tx.setStatut(StatutTransaction.SUCCESS);
                    tx.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(tx);

                    log.info("INVESTISSEMENT PAYDUNYA EN_ATTENTE → user={} projet={} parts={} montant={}",
                            userId, projetId, parts, tx.getMontant());

                } else {
                    // ── Flux dépôt wallet classique ───────────────────────
                    Wallet associatedWallet = walletRepository.findById(tx.getWalletId())
                            .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));
                    Long userId = associatedWallet.getUser().getId();

                    walletService.deposerFonds(userId, tx.getMontant().doubleValue(), "PAYDUNYA_MM");

                    tx.setStatut(StatutTransaction.SUCCESS);
                    tx.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(tx);

                    log.info("DÉPÔT PAYDUNYA CRÉDITÉ → user={} montant={}", userId, tx.getMontant());
                }

            } else {
                tx.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                transactionRepository.save(tx);
                log.warn("PAIEMENT PAYDUNYA ÉCHOUÉ → token={} status={}", token, status);
            }

            return ResponseEntity.ok("Processed");
        }

        log.warn("Webhook PayDunya sans correspondance : token={}", token);
        return ResponseEntity.ok("No match found");
    }
}