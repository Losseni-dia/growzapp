package growzapp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.model.entite.Payout;

// PayoutRepository.java
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findByUserIdOrderByCreatedAtDesc(Long userId);
}