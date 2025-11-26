package growzapp.backend.controller.webhooks;


import com.fasterxml.jackson.databind.ObjectMapper;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook/paydunya")
@RequiredArgsConstructor
public class PaydunyaWebhookController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionRepository transactionRepository;
    private final PayoutModelRepository payoutModelRepository;

    @PostMapping
    public String handle(@RequestBody Map<String, Object> payload) {
        log.info("Webhook PayDunya reçu : {}", payload);

        String status = (String) payload.get("status");
        String invoiceToken = (String) payload.get("invoice_token"); // pour les dépôts
        String disburseToken = (String) payload.get("disburse_token"); // pour les retraits

        // ==================== DÉPÔT PAYDUNYA ====================
        if ("completed".equals(status) && invoiceToken != null) {
            transactionRepository.findById(extractIdFromToken(invoiceToken))
                    .ifPresent(tx -> {
                        tx.setStatut(StatutTransaction.SUCCESS);
                        tx.setCompletedAt(java.time.LocalDateTime.now());
                        tx.getWallet().crediterDisponible(tx.getMontant());
                        transactionRepository.save(tx);
                        log.info("DÉPÔT PAYDUNYA VALIDÉ → transaction {}", tx.getId());
                    });
        }

        // ==================== RETRAIT PAYDUNYA ====================
        if ("completed".equals(status) && disburseToken != null) {
            payoutModelRepository.findByPaydunyaToken(disburseToken)
                    .ifPresent(p -> {
                        p.setStatut(StatutTransaction.SUCCESS);
                        p.setPaydunyaStatus("completed");
                        payoutModelRepository.save(p);
                        log.info("RETRAIT PAYDUNYA PAYÉ → {}", disburseToken);
                    });
        }

        return "OK";
    }

    private Long extractIdFromToken(String token) {
        // Si tu utilises l’ID de ta transaction comme token PayDunya → return
        // Long.parseLong(token)
        // Sinon adapte selon ton format
        try {
            return Long.parseLong(token);
        } catch (Exception e) {
            return null;
        }
    }
}