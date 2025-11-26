package growzapp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.model.entite.PayoutModel;

// PayoutRepository.java
public interface PayoutModelRepository extends JpaRepository<PayoutModel, Long> {
    List<PayoutModel> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Cette ligne suffit → Spring la génère automatiquement
    Optional<PayoutModel> findByPaydunyaToken(String paydunyaToken);

    // Si tu veux être encore plus propre, tu peux aussi ajouter :
    Optional<PayoutModel> findByExternalPayoutId(String externalPayoutId);
     
   
}