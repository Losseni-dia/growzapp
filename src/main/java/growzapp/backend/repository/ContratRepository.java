package growzapp.backend.repository;

import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {
    List<Contrat> findByInvestissement(Investissement investissement);

    long count();

    Optional<Contrat> findByNumeroContrat(String numeroContrat);
}
