package growzapp.backend.module.investissement.repository;

import growzapp.backend.module.investissement.model.DecisionEcheanceInvestissement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionEcheanceInvestissementRepository
        extends JpaRepository<DecisionEcheanceInvestissement, Long> {
}
