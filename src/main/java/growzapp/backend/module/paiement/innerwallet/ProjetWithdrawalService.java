package growzapp.backend.module.paiement.innerwallet;

import growzapp.backend.module.paiement.common.PaymentProviderRouter;
import growzapp.backend.module.paiement.common.PaymentProviderService.PayoutResponse;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Retrait externe (Mobile Money) initié par le porteur depuis le
 * soldeDisponible du wallet de son projet — pendant de WithdrawalService
 * (wallet utilisateur), même mécanique de transactions courtes indépendantes
 * via ProjetWithdrawalTransactionHelper.
 *
 * Le virement bancaire (Stripe) n'est volontairement pas proposé ici, pour
 * la même raison que côté wallet utilisateur (voir WalletService.retirerFonds
 * et STRIPE_PAYOUT_ROADMAP.md) : Payout.create() de Stripe envoie les fonds
 * vers le compte de la plateforme, pas vers celui du porteur, tant que
 * Stripe Connect n'est pas en place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetWithdrawalService {

    private final ProjetWithdrawalTransactionHelper txHelper;
    private final PaymentProviderRouter paymentProviderRouter;

    public String executerRetraitMobileMoney(Long projetId, BigDecimal montant, String phone,
            String idempotencyKey) {
        TypeTransaction mmType = TypeTransaction.PAYOUT_OM;

        Transaction tx = txHelper.debiterEtCreerTransaction(projetId, montant, mmType, idempotencyKey);

        try {
            PayoutResponse res = paymentProviderRouter.initierRetrait(montant, phone, "orange-money-ci", tx.getId());
            txHelper.marquerSucces(tx.getId(), res.externalTxId());
            log.info("Retrait Mobile Money projet réussi : projetId={} montant={} txId={}", projetId, montant,
                    res.externalTxId());
            return res.externalTxId();
        } catch (Exception e) {
            txHelper.rembourserEtNotifier(projetId, montant, tx.getId(), e);
            throw new RuntimeException("Échec de la transaction de retrait Mobile Money : " + e.getMessage(), e);
        }
    }
}
