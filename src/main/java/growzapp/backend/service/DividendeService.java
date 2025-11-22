package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.entite.Dividende;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.enumeration.StatutDividende;
import growzapp.backend.repository.DividendeRepository;
import growzapp.backend.repository.InvestissementRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendeService {

    private final DividendeRepository dividendeRepository;
    private final InvestissementRepository investissementRepository;
    private final DtoConverter converter;

    public List<DividendeDTO> getAll() {
        return dividendeRepository.findAll().stream()
                .map(converter::toDividendeDto)
                .toList();
    }

    // Admin
    public Page<DividendeDTO> getAllAdmin(Pageable pageable) {
        Page<Dividende> page = dividendeRepository.findAll(pageable);
        return page.map(converter::toDividendeDto); // ou ton converter
    }

    public DividendeDTO getById(Long id) {
        Dividende dividende = dividendeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dividende non trouvé : " + id));
        return converter.toDividendeDto(dividende);
    }


   

    @Transactional
    public DividendeDTO save(DividendeDTO dto) {
        // Validation : investissement obligatoire
        if (dto.investissementId() == null) {
            throw new IllegalArgumentException("L'investissement est obligatoire pour créer un dividende.");
        }

        // Récupération de l'investissement
        Investissement investissement = investissementRepository.findById(dto.investissementId())
                .orElseThrow(() -> new RuntimeException("Investissement non trouvé : " + dto.investissementId()));

        // Conversion DTO → Entité
        Dividende dividende = converter.toDividendeEntity(dto);

        // Association bidirectionnelle
        dividende.setInvestissement(investissement);

        // Sauvegarde
        dividende = dividendeRepository.save(dividende);

        // Retour DTO mis à jour (montantTotal recalculé, etc.)
        return converter.toDividendeDto(dividende);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!dividendeRepository.existsById(id)) {
            throw new RuntimeException("Dividende non trouvé : " + id);
        }
        dividendeRepository.deleteById(id);
    }

    // Pour afficher uniquement les dividendes d’un investisseur connecté
    public List<DividendeDTO> getByInvestisseurId(Long investisseurId) {
        return dividendeRepository.findByInvestissement_Investisseur_Id(investisseurId).stream()
                .map(converter::toDividendeDto) // ← C'EST BON ! (Dividende → DividendeDTO)
                .toList();
    }

    public Dividende findEntityById(Long dividendeId) {
       return dividendeRepository.findById(dividendeId).get();
    }

        // Dans DividendeService.java
    public double getTotalPercuPayeByInvestisseur(Long investisseurId) {
        return dividendeRepository.findByInvestissement_Investisseur_Id(investisseurId).stream()
                .filter(d -> d.getStatutDividende() == StatutDividende.PAYE)
                .mapToDouble(Dividende::getMontantTotal)
                .sum();
    }

}