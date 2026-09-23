package growzapp.backend.module.growzmarket.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.growzmarket.enums.StatutCommandeMarket;
import growzapp.backend.module.growzmarket.model.CommandeMarket;

public interface CommandeMarketRepository extends JpaRepository<CommandeMarket, Long> {

    List<CommandeMarket> findByAcheteurIdOrderByDateCommandeDesc(Long acheteurId);

    List<CommandeMarket> findByProjetPorteurIdOrderByDateCommandeDesc(Long porteurId);

    List<CommandeMarket> findByStatutOrderByDateCommandeDesc(StatutCommandeMarket statut);

    List<CommandeMarket> findAllByOrderByDateCommandeDesc();

    // Utilisé par le job planifié qui ouvre automatiquement un litige sur
    // les commandes prêtes mais jamais retirées après le délai configuré.
    List<CommandeMarket> findByStatutAndDatePreteBefore(StatutCommandeMarket statut, LocalDateTime seuil);
}
