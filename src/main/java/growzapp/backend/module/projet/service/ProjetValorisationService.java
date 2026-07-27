package growzapp.backend.module.projet.service;

import growzapp.backend.module.projet.enums.TypeEvenementValorisation;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetValorisation;
import growzapp.backend.module.projet.repository.ProjetValorisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetValorisationService {

    private final ProjetValorisationRepository valorisationRepository;

    @Transactional
    public void enregistrerSnapshot(Projet projet, TypeEvenementValorisation type, BigDecimal montantEvenement) {
        try {
            ProjetValorisation snapshot = new ProjetValorisation();
            snapshot.setProjet(projet);
            snapshot.setMontantValorisation(projet.getValuation());
            snapshot.setMontantCollecte(projet.getMontantCollecte());
            snapshot.setTypeEvenement(type);
            snapshot.setMontantEvenement(montantEvenement);
            valorisationRepository.save(snapshot);
            log.info("Snapshot valorisation enregistré — projet {} — type {}", projet.getId(), type);
        } catch (Exception e) {
            log.warn("Échec enregistrement snapshot valorisation pour projet {} : {}", projet.getId(), e.getMessage());
        }
    }

    
}