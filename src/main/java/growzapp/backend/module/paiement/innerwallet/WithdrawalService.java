package growzapp.backend.module.paiement.innerwallet;

import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.paydunya.PayDunyaService;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.paiement.stripe.StripePayoutService;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final StripePayoutService stripePayoutService;
    private final PayDunyaService payDunyaService;
    private final PayoutModelRepository payoutModelRepository;
    private final WithdrawalTransactionHelper txHelper;

    public String executerRetraitBancaire(Long userId, BigDecimal montant, String phone, String idempotencyKey) {
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        PayoutModel payout = txHelper.debiterEtCreerPayout(userId, montant, phone, TypeTransaction.PAYOUT_STRIPE,
                idempotencyKey);

        try {
            String stripePayoutId = stripePayoutService.createBankPayoutDirect(userId, montant, payout.getId());
            txHelper.marquerSucces(payout.getId(), stripePayoutId);
            log.info("Retrait bancaire réussi : user={} montant={} payoutId={}", userId, montant, stripePayoutId);
            return stripePayoutId;
        } catch (Exception e) {
            txHelper.rembourserEtNotifier(userId, montant, payout.getId(), e);
            throw new RuntimeException("Échec de la transaction de retrait bancaire : " + e.getMessage(), e);
        }
    }

    public String executerRetraitMobileMoney(Long userId, BigDecimal montant, TypeTransaction mmType, String phone,
            String idempotencyKey) {
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        PayoutModel payout = txHelper.debiterEtCreerPayout(userId, montant, phone, mmType, idempotencyKey);

        try {
            String txId = payDunyaService.initiatePayout(montant, phone, mmType, payout.getId());
            txHelper.marquerSucces(payout.getId(), txId);
            log.info("Retrait Mobile Money réussi : user={} montant={} txId={}", userId, montant, txId);
            return txId;
        } catch (Exception e) {
            txHelper.rembourserEtNotifier(userId, montant, payout.getId(), e);
            throw new RuntimeException("Échec de la transaction de retrait Mobile Money : " + e.getMessage(), e);
        }
    }
}