package growzapp.backend.module.growzmarket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.growzmarket.model.CommandeMarketLitigeMessage;

public interface CommandeMarketLitigeMessageRepository extends JpaRepository<CommandeMarketLitigeMessage, Long> {

    List<CommandeMarketLitigeMessage> findByCommandeIdOrderByDateEnvoiAsc(Long commandeId);
}
