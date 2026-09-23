package growzapp.backend.module.growzmarket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Sécurité de bout de chaîne pour GrowzMarket V1 : sans ce job, une commande
// PRETE_AU_RETRAIT jamais récupérée par l'acheteur restait bloquée
// indéfiniment (aucun statut NON_RETIREE géré côté UI). On ouvre donc
// automatiquement un litige après le délai configuré, ce qui la fait
// apparaître directement dans l'écran d'arbitrage admin déjà existant.
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandeMarketExpirationScheduler {

    private final CommandeMarketService commandeMarketService;

    @Value("${growzmarket.delai-retrait-jours:14}")
    private int delaiRetraitJours;

    // Une fois par jour à 3h du matin — pas besoin de plus fréquent pour un
    // délai qui se compte en jours.
    @Scheduled(cron = "0 0 3 * * *")
    public void ouvrirLitigesAutomatiques() {
        try {
            commandeMarketService.traiterCommandesNonRetirees(delaiRetraitJours);
            log.info("Vérification des commandes GrowzMarket non retirées exécutée (délai {} jours)",
                    delaiRetraitJours);
        } catch (Exception e) {
            log.error("Échec de la vérification des commandes GrowzMarket non retirées", e);
        }
    }
}
