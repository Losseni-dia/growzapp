package growzapp.backend.module.paiement.common;

import growzapp.backend.module.paiement.fedapay.FedaPayService;
import growzapp.backend.module.paiement.paydunya.PayDunyaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Route les opérations de paiement vers FedaPay en priorité, avec bascule
 * automatique vers PayDunya en cas d'échec — transparent pour l'appelant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProviderRouter {

    private final FedaPayService fedaPayService;
    private final PayDunyaService payDunyaService;

    public PaymentProviderService.PaymentSessionResponse creerSessionDepot(BigDecimal montant, Long userId) {
        try {
            return fedaPayService.creerSessionDepot(montant, userId);
        } catch (Exception e) {
            log.warn("FedaPay indisponible pour le dépôt (user={}), bascule sur PayDunya : {}", userId, e.getMessage());
            return payDunyaService.creerSessionDepot(montant, userId);
        }
    }

    public PaymentProviderService.PaymentSessionResponse creerSessionInvestissement(
            BigDecimal montant, Long userId, Long projetId, int nombreParts,
            String projetLibelle, String projetSlug) {
        try {
            return fedaPayService.creerSessionInvestissement(
                    montant, userId, projetId, nombreParts, projetLibelle, projetSlug);
        } catch (Exception e) {
            log.warn("FedaPay indisponible pour l'investissement (user={}), bascule sur PayDunya : {}", userId, e.getMessage());
            return payDunyaService.creerSessionInvestissement(
                    montant, userId, projetId, nombreParts, projetLibelle, projetSlug);
        }
    }

    public PaymentProviderService.PayoutResponse initierRetrait(
            BigDecimal montant, String phone, String moyenPaiement, Long referenceId) {
        try {
            return fedaPayService.initierRetrait(montant, phone, moyenPaiement, referenceId);
        } catch (Exception e) {
            log.warn("FedaPay indisponible pour le retrait (ref={}), bascule sur PayDunya : {}", referenceId, e.getMessage());
            return payDunyaService.initierRetrait(montant, phone, moyenPaiement, referenceId);
        }
    }
}